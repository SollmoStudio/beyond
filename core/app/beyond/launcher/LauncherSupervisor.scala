package beyond.launcher

import akka.actor.Actor
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._
import beyond.config.BeyondConfiguration
import beyond.config.MongoConfiguration
import beyond.config.ZooKeeperConfiguration
import beyond.launcher.mongodb.MongoDBConfigLauncher
import beyond.launcher.mongodb.MongoDBInstanceType
import beyond.launcher.mongodb.MongoDBStandaloneLauncher

class LauncherInitializationException extends Exception
class ServerNotRespondingException extends Exception

object LauncherSupervisor {
  val Name: String = "launcherSupervisor"
}

class LauncherSupervisor extends Actor {
  private def launchZooKeeperServerIfNecessary() {
    if (!BeyondConfiguration.isStandaloneMode) {

      if (!ZooKeeperConfiguration.isReplicatedMode || ZooKeeperConfiguration.isCurrentMachineInServerList) {
        context.actorOf(Props[ZooKeeperLauncher], name = "zooKeeperLauncher")
      }
    }
  }

  private def launchMongoDBServerIfNecessary() {
    MongoConfiguration.instanceType match {
      case MongoDBInstanceType.Standalone =>
        context.actorOf(Props[MongoDBStandaloneLauncher], name = "mongoDBStandaloneLauncher")
      case MongoDBInstanceType.Config =>
        context.actorOf(Props[MongoDBConfigLauncher], name = "mongoDBConfigLauncher")
      case MongoDBInstanceType.None =>
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

  override def receive: Receive = Actor.emptyBehavior
}

