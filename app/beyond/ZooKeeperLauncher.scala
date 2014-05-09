package beyond

import akka.actor.Actor
import java.io.IOException
import org.apache.zookeeper.client.FourLetterWordMain.send4LetterWord
import org.apache.zookeeper.server.ServerConfig
import org.apache.zookeeper.server.ZooKeeperServerMain
import scala.annotation.tailrec
import scala.concurrent.duration._

class LauncherInitializationException extends RuntimeException

// FIXME: Shutdown the server and crash this actor if the underlying ZooKeeper
// server does not respond.
class ZooKeeperLauncher extends Actor {
  class BeyondZooKeeperServerMain extends ZooKeeperServerMain {
    // Make shutdown() public because it is protected in ZooKeeperServerMain.
    override def shutdown() {
      super.shutdown()
    }
  }

  class ZooKeeperThread extends Thread {
    override def run() {
      zkServer.runFromConfig(config)
    }

    def shutdown() {
      zkServer.shutdown()
    }
  }

  // FIXME: Currently, ZooKeeperLauncher launches a standalone ZooKeeper server.
  // Setup a ZooKeeper cluster instead.
  private val zkServer: BeyondZooKeeperServerMain = new BeyondZooKeeperServerMain
  private val config: ServerConfig = new ServerConfig

  // FIXME: Don't hardcode the configuration file.
  config.parse("zoo.cfg")

  private val zkServerThread: ZooKeeperThread = new ZooKeeperThread

  private val host = "127.0.0.1"
  private val port = config.getClientPortAddress.getPort

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

  override def preStart() {
    zkServerThread.start()
    waitForServerUp()
  }

  override def postStop() {
    zkServerThread.shutdown()
    waitForServerDown()
  }

  override def receive: Receive = {
    case _ =>
  }
}

