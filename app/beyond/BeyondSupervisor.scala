package beyond

import akka.actor.Actor
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._
import akka.routing.ConsistentHashingRouter

class BeyondSupervisor extends Actor {
  override def preStart() {
    val numProcessors = Runtime.getRuntime().availableProcessors()
    val router = ConsistentHashingRouter(nrOfInstances = numProcessors)
    context.actorOf(Props[UserActionActor].withRouter(router), name = "userActionActor")
    context.actorOf(Props[LauncherSupervisor], name = "launcherSupervisor")
  }

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

