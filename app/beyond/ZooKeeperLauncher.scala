package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import java.io.IOException
import org.apache.zookeeper.client.FourLetterWordMain.send4LetterWord
import org.apache.zookeeper.server.ServerConfig
import org.apache.zookeeper.server.ZooKeeperServerMain
import scala.annotation.tailrec
import scala.concurrent.duration._

class ZooKeeperLauncher extends Actor with ActorLogging {
  case object HeartBeat

  class BeyondZooKeeperServerMain extends ZooKeeperServerMain {
    // Make shutdown() public because it is protected in ZooKeeperServerMain.
    override def shutdown() {
      super.shutdown()
    }
  }

  // FIXME: Currently, ZooKeeperLauncher launches a standalone ZooKeeper server.
  // Setup a ZooKeeper cluster instead.
  private val zkServer: BeyondZooKeeperServerMain = new BeyondZooKeeperServerMain
  private val config: ServerConfig = new ServerConfig

  // FIXME: Don't hardcode the configuration file.
  config.parse("zoo.cfg")

  private val zkServerThread: Thread = new Thread(new Runnable {
    override def run() {
      zkServer.runFromConfig(config)
    }
  })

  private val host = "127.0.0.1"
  private val port = config.getClientPortAddress.getPort

  import play.api.libs.concurrent.Execution.Implicits._
  // FIXME: Check if the scheduler keeps sending messages while the actor is blocking on health check.
  val tick = context.system.scheduler.schedule(initialDelay = 10 seconds, interval = 1 seconds, self, HeartBeat)

  // Retry until the given block returns true.
  @tailrec
  private def retryWithinTimeRange(timeRange: Duration, interval: Duration)(block: => Boolean): Boolean = {
    val start = System.currentTimeMillis()
    if (block) {
      true
    } else {
      Thread.sleep(interval.toMillis)

      val elapsed = System.currentTimeMillis() - start
      val newTimeRange = timeRange - elapsed.millis
      if (newTimeRange > Duration.Zero) {
        retryWithinTimeRange(newTimeRange, interval)(block)
      } else {
        false
      }
    }
  }

  private def waitForServerUp() {
    val success = retryWithinTimeRange(timeRange = 10 seconds, interval = 250 millis) {
      try {
        send4LetterWord(host, port, "stat").contains("Zookeeper version:")
      } catch {
        // Ignore. this is expected if server is not up yet.
        case _: IOException => false
      }
    }
    if (!success) {
      throw new LauncherInitializationException
    }
  }

  private def waitForServerDown() {
    // FIXME: Log if shutdown fails.
    retryWithinTimeRange(timeRange = 10 seconds, interval = 250 millis) {
      try {
        send4LetterWord(host, port, "stat")
        false
      } catch {
        case _: IOException => true
      }
    }
  }

  private def healthCheck() {
    // FIXME: Adjust time range and interval.
    val success = retryWithinTimeRange(timeRange = 30 seconds, interval = 1 seconds) {
      try {
        send4LetterWord(host, port, "stat").contains("Zookeeper version:")
      } catch {
        case _: IOException =>
          throw new ServerNotRespondingException
      }
    }
    if (!success) {
      throw new ServerNotRespondingException
    }
  }

  override def preStart() {
    zkServerThread.start()
    waitForServerUp()
    log.info("ZooKeeper started")
  }

  override def postStop() {
    tick.cancel()

    zkServer.shutdown()
    zkServerThread.join()
    waitForServerDown()
    log.info("ZooKeeper stopped")
  }

  override def receive: Receive = {
    case HeartBeat => healthCheck()
  }
}

