package beyond.launcher

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._
import beyond.BeyondConfiguration
import beyond.launcher.ZooKeeperLauncher
import java.net.InetAddress
import java.net.NetworkInterface

class LauncherInitializationException extends RuntimeException
class ServerNotRespondingException extends RuntimeException

object LauncherSupervisor {
  val Name: String = "launcherSupervisor"
}

class LauncherSupervisor extends Actor with ActorLogging {
  override def preStart() {
    def launchZooKeeperServerIfNecessary() {
      import scala.collection.JavaConversions._

      val localAddresses = (for {
        ni <- NetworkInterface.getNetworkInterfaces.toIterator
        address <- ni.getInetAddresses
      } yield address).toSet
      log.info(s"Local addresses $localAddresses")

      val zooKeeperAddresses = BeyondConfiguration.zooKeeperServers.map(InetAddress.getByName)
      log.info(s"ZooKeeper addresses $zooKeeperAddresses")

      if (!(localAddresses & zooKeeperAddresses).isEmpty) {
        context.actorOf(Props[ZooKeeperLauncher], name = "zooKeeperLauncher")
      }
    }

    launchZooKeeperServerIfNecessary()
    context.actorOf(Props[MongoDBLauncher], name = "mongoDBLauncher")
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      // FIXME: Need policy for launcher exceptions.
      case _: ServerNotRespondingException => Restart
      case t =>
        super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }

  override def receive: Receive = {
    case _ =>
  }
}

