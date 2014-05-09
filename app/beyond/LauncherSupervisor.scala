package beyond

import akka.actor.Actor
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._

class LauncherInitializationException extends RuntimeException
class ServerNotRespondingException extends RuntimeException

class LauncherSupervisor extends Actor {
  context.actorOf(Props[ZooKeeperLauncher].withDispatcher("akka.io.pinned-dispatcher"), name = "zooKeeperLauncher")
  context.actorOf(Props[MongoDBLauncher], name = "mongoDBLauncher")

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

