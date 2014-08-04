package beyond

import akka.actor.Actor
import akka.actor.ActorInitializationException
import akka.actor.ActorPath
import akka.actor.Address
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.RootActorPath
import akka.actor.SupervisorStrategy._
import beyond.launcher.LauncherSupervisor
import beyond.metrics.SystemMetricsSupervisor
import beyond.plugin.GamePlugin
import beyond.plugin.NoHandlerFunctionFoundException

object BeyondSupervisor {
  val Name: String = "beyondSupervisor"

  val RootActorPath: ActorPath = new RootActorPath(Address("akka", "application"))
  val BeyondSupervisorPath: ActorPath = RootActorPath / "user" / BeyondSupervisor.Name
}

class BeyondSupervisor extends Actor {
  import beyond.ThrowableOps._

  context.actorOf(Props[LauncherSupervisor], LauncherSupervisor.Name)
  // FIXME: Don't hardcode the plugin filename.
  context.actorOf(Props(classOf[GamePlugin], "main.js"), GamePlugin.Name)
  context.actorOf(Props[SystemMetricsSupervisor], SystemMetricsSupervisor.Name)
  context.actorOf(Props[CuratorSupervisor], CuratorSupervisor.Name)

  override val supervisorStrategy =
    OneForOneStrategy() {
      case ex: ActorInitializationException =>
        ex.getRootCause match {
          case _: NoHandlerFunctionFoundException => Escalate
          case _ => Stop
        }
      case t =>
        super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }

  override def receive: Receive = Map.empty
}

