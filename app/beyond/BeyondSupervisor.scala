package beyond

import akka.actor.Actor
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._
import beyond.UserActionActor.SyncRoutingTable
import beyond.plugin.GamePlugin
import beyond.launcher.LauncherSupervisor

object BeyondSupervisor {
  val BeyondSupervisorBasePath: String = "/user/beyondSupervisor/"

  val UserActionSupervisorPath: String = BeyondSupervisorBasePath + UserActionSupervisor.Name
  val UserActionActorPath: String = s"$UserActionSupervisorPath/${UserActionActor.Name}"
}

class BeyondSupervisor extends Actor {
  context.actorOf(Props[LauncherSupervisor], LauncherSupervisor.Name)
  // FIXME: Don't hardcode the plugin filename.
  context.actorOf(Props(classOf[GamePlugin], "main.js"), GamePlugin.Name)
  context.actorOf(Props[SystemMetricsSupervisor], SystemMetricsSupervisor.Name)
  context.actorOf(Props[CuratorSupervisor], CuratorSupervisor.Name)
  context.actorOf(Props[UserActionSupervisor], UserActionSupervisor.Name)

  override val supervisorStrategy =
    OneForOneStrategy() {
      // FIXME: Need policy for all exceptions escalated by Beyond actors.
      case t =>
        super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }

  override def receive: Receive = {
    case _ =>
  }
}

