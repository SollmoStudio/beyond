package beyond

import akka.actor.Actor
import akka.actor.ActorPath
import akka.actor.Address
import akka.actor.Props
import akka.actor.RootActorPath
import beyond.launcher.LauncherSupervisor
import beyond.metrics.SystemMetricsSupervisor

object BeyondSupervisor {
  val Name: String = "beyondSupervisor"

  val RootActorPath: ActorPath = new RootActorPath(Address("akka", "application"))
  val BeyondSupervisorPath: ActorPath = RootActorPath / "user" / BeyondSupervisor.Name
}

class BeyondSupervisor extends Actor {
  context.actorOf(Props[LauncherSupervisor], LauncherSupervisor.Name)
  context.actorOf(Props[SystemMetricsSupervisor], SystemMetricsSupervisor.Name)
  context.actorOf(Props[CuratorSupervisor], CuratorSupervisor.Name)

  override def receive: Receive = Map.empty
}

