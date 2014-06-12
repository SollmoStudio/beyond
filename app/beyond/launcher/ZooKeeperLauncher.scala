package beyond.launcher

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Cancellable
import akka.io.IO
import akka.io.Tcp
import akka.util.ByteString
import beyond.BeyondConfiguration
import beyond.BeyondRuntime
import beyond.TickGenerator
import java.net.InetSocketAddress
import org.apache.zookeeper.server.ServerConfig
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import scala.sys.process.Process
import scalax.file.Path

object ZooKeeperLauncher {
  val ServerNotRespondingTimeout = 30.seconds
  val RetryDelay = 5.seconds
}

class ZooKeeperLauncher extends {
  override protected val initialDelay = 10.seconds
  override protected val tickInterval = 1.second
} with Actor with TickGenerator with ActorLogging {
  import ZooKeeperLauncher._
  import akka.io.Tcp._
  import play.api.libs.concurrent.Execution.Implicits._

  private val pidFilePath: Path = Path.fromString(BeyondConfiguration.pidDirectory) / "zookeeper.pid"

  private var connectCancellable: Cancellable = _

  override def preRestart(reason: Throwable, message: Option[Any]) {
    super.preRestart(reason, message)
    terminateProcessIfExists(pidFilePath)
  }

  override def preStart() {
    log.info("ZooKeeperLauncher started")
    if (!pidFilePath.exists) {
      // FIXME: Currently, ZooKeeperLauncher launches a standalone ZooKeeper server.
      // Setup a ZooKeeper cluster instead.
      val mainClassOfZooKeeperServer = classOf[beyond.launcher.ZooKeeperServerMainWithPIDFile].getCanonicalName
      val zooKeeperServer = Process(
        Seq(
          BeyondRuntime.javaPath,
          mainClassOfZooKeeperServer,
          pidFilePath.path,
          BeyondConfiguration.zooKeeperConfigPath),
        cwd = None,
        extraEnv = "CLASSPATH" -> BeyondRuntime.classPath)
      zooKeeperServer.run()
      log.info("ZooKeeperServer started")
    }
    scheduleConnect(initialDelay)
  }

  override def postStop() {
    connectCancellable.cancel()
    super.postStop()
    log.info("ZooKeeperLauncher stopped")
  }

  private def scheduleConnect(delay: FiniteDuration) {
    connectCancellable = context.system.scheduler.scheduleOnce(delay) {
      import context.system
      val config: ServerConfig = {
        val config = new ServerConfig
        config.parse(BeyondConfiguration.zooKeeperConfigPath)
        config
      }

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
    case TickGenerator.Tick =>
      val newTimeout = timeout - tickInterval
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
    case TickGenerator.Tick =>
      val newTimeout = timeout - tickInterval
      if (newTimeout > Duration.Zero) {
        context.become(connected(connection, newTimeout))
      } else {
        throw new ServerNotRespondingException
      }
  }
}

