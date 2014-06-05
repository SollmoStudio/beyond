package beyond

import akka.actor.Actor
import akka.actor.ActorPath
import akka.actor.Address
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.RootActorPath
import akka.actor.SupervisorStrategy._
import beyond.UserActionActor.SyncRoutingTable
import beyond.plugin.GamePlugin
import beyond.launcher.LauncherSupervisor

object BeyondSupervisor {
  val RootActorPath: ActorPath = new RootActorPath(Address("akka", "application"))
  val BeyondSupervisorPath: ActorPath = RootActorPath / "user" / "beyondSupervisor"
  val UserActionSupervisorPath: ActorPath = BeyondSupervisorPath / UserActionSupervisor.Name
  val UserActionActorPath: ActorPath = UserActionSupervisorPath / UserActionActor.Name
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

  override def receive: Receive = Map.empty
}

