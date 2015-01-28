package beyond.launcher

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._
import beyond.BeyondConfiguration
import beyond.launcher.mongodb.MongoDBConfigLauncher
import beyond.launcher.mongodb.MongoDBInstanceType
import beyond.launcher.mongodb.MongoDBStandaloneLauncher
import java.net.InetAddress
import java.net.NetworkInterface

class LauncherInitializationException extends Exception
class ServerNotRespondingException extends Exception

object LauncherSupervisor {
  val Name: String = "launcherSupervisor"
}

class LauncherSupervisor extends Actor with ActorLogging {
  private def launchZooKeeperServerIfNecessary() {
    if (!BeyondConfiguration.isStandaloneMode) {
      import scala.collection.JavaConversions._

      val localAddresses = (for {
        ni <- NetworkInterface.getNetworkInterfaces.toIterator
        address <- ni.getInetAddresses
      } yield address).toSet
      log.info(s"Local addresses $localAddresses")

      val zooKeeperAddresses = BeyondConfiguration.zooKeeperServers.map(InetAddress.getByName)
      log.info(s"ZooKeeper addresses $zooKeeperAddresses")

      if ((localAddresses & zooKeeperAddresses).nonEmpty) {
        context.actorOf(Props[ZooKeeperLauncher], name = "zooKeeperLauncher")
      }
    }
  }

  private def launchMongoDBServerIfNecessary() {
    BeyondConfiguration.mongo.instanceType match {
      case MongoDBInstanceType.Standalone =>
        context.actorOf(Props[MongoDBStandaloneLauncher], name = "mongoDBStandaloneLauncher")
      case MongoDBInstanceType.Config =>
        context.actorOf(Props[MongoDBConfigLauncher], name = "mongoDBConfigLauncher")
      case _ =>
        // FIXME: launch a proper MongoDB instance for sharding.
        ???
    }
  }

  override def preStart() {
    launchZooKeeperServerIfNecessary()
    launchMongoDBServerIfNecessary()
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      // FIXME: Need policy for launcher exceptions.
      case _: ServerNotRespondingException => Restart
      case t =>
        super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }

  override def receive: Receive = Map.empty
}

