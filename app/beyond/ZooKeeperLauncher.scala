package beyond

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorLogging
import akka.actor.Cancellable
import akka.io.IO
import akka.io.Tcp
import akka.util.ByteString
import java.net.InetSocketAddress
import org.apache.zookeeper.server.ServerConfig
import org.apache.zookeeper.server.ZooKeeperServerMain
import scala.concurrent.duration._

object ZooKeeperLauncher {
  case object Tick
}

class ZooKeeperLauncher extends Actor with ActorLogging {
  import play.api.libs.concurrent.Execution.Implicits._
  import ZooKeeperLauncher._
  import akka.io.Tcp._

  class BeyondZooKeeperServerMain extends ZooKeeperServerMain {
    // Make shutdown() public because it is protected in ZooKeeperServerMain.
    override def shutdown() {
      super.shutdown()
    }
  }

  // FIXME: Currently, ZooKeeperLauncher launches a standalone ZooKeeper server.
  // Setup a ZooKeeper cluster instead.
  private val zkServer: BeyondZooKeeperServerMain = new BeyondZooKeeperServerMain
  private val config: ServerConfig = {
    import play.api.Play.current
    val config = new ServerConfig
    config.parse(current.configuration.getString("beyond.zookeeper.config-path").getOrElse("conf/zoo.cfg"))
    config
  }

  private val zkServerThread: Thread = new Thread(new Runnable {
    override def run() {
      zkServer.runFromConfig(config)
    }
  })

  private val ServerNotRespondingTimeout = 30.seconds
  private val TickInterval = 1.second

  private val InitialDelay = 10.seconds
  private val RetryDelay = 5.seconds

  private val tickCancellable = context.system.scheduler.schedule(
    initialDelay = InitialDelay, interval = TickInterval, receiver = self, message = Tick)

  private var connectCancellable: Cancellable = _

  override def preStart() {
    zkServerThread.start()
    log.info("ZooKeeper started")

    scheduleConnect(InitialDelay)
  }

  override def postStop() {
    tickCancellable.cancel()
    connectCancellable.cancel()

    zkServer.shutdown()
    zkServerThread.join()
    log.info("ZooKeeper stopped")
  }

  private def scheduleConnect(delay: FiniteDuration) {
    connectCancellable = context.system.scheduler.scheduleOnce(delay) {
      import context.system
      val address = new InetSocketAddress("127.0.0.1", config.getClientPortAddress.getPort)
      IO(Tcp) ! Tcp.Connect(address)
    }
  }

  override def receive: Receive = connecting(ServerNotRespondingTimeout)

  private def connecting(timeout: FiniteDuration): Receive = {
    case Connected(_, _) =>
      val connection = sender
      connection ! Register(self)
      connection ! Write(ByteString("stat"))
      context.become(connected(connection, timeout))
    case CommandFailed(_: Connect) =>
      scheduleConnect(RetryDelay)
    case Tick =>
      val newTimeout = timeout - TickInterval
      if (newTimeout > Duration.Zero) {
        context.become(connecting(newTimeout))
      } else {
        throw new ServerNotRespondingException
      }
  }

  // In case there is a problem, simply close the connection and switch to connecting state
  // without resetting timeout value.
  private def connected(connection: ActorRef, timeout: FiniteDuration): Receive = {
    case CommandFailed(_: Write) =>
      connection ! Close
    case Received(data: ByteString) =>
      // FIXME: We assume that the received data is big enough to hold
      // "ZooKeeper version:" string. But if this assumption is not true,
      // we need to accumulate multiple Received messages.
      val response = data.decodeString("UTF8")
      if (response.startsWith("Zookeeper version:")) {
        // The server is well. Reset the timeout.
        context.become(connected(connection, ServerNotRespondingTimeout))
      }
      connection ! Close
    case _: ConnectionClosed =>
      scheduleConnect(RetryDelay)
      context.become(connecting(timeout))
    case Tick =>
      val newTimeout = timeout - TickInterval
      if (newTimeout > Duration.Zero) {
        context.become(connected(connection, newTimeout))
      } else {
        throw new ServerNotRespondingException
      }
  }
}

