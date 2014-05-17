package beyond

import akka.actor.Actor
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy
import akka.actor.SupervisorStrategy._
import akka.routing.ConsistentHashingRouter
import beyond.plugin.GamePlugin
import beyond.route.RoutingTableLeader
import beyond.route.RoutingTableWorker

class BeyondSupervisor extends Actor {
  override def preStart() {
    val numProcessors = Runtime.getRuntime.availableProcessors()
    // Routers default to a strategy of "always escalate". This is problematic because
    //  a failure in a routee is escalated up to the router's supervisor for handling.
    //  If the router's supervisor decides to restart the child, (which is the default,
    //  unfortunately), the router and all of its routees are restarted.
    //
    //  So override the router's strategy with SupervisorStrategy.defaultStrategy which
    //  restarts only the failing child actor upon Exception.
    //
    // See Routers and Supervision section of
    //  http://doc.akka.io/docs/akka/2.2.1/scala/routing.html for further discussions.
    val router = ConsistentHashingRouter(nrOfInstances = numProcessors,
      supervisorStrategy = SupervisorStrategy.defaultStrategy)
    context.actorOf(Props[UserActionActor].withRouter(router), name = "userActionActor")
    context.actorOf(Props[LauncherSupervisor], name = "launcherSupervisor")
    // FIXME: Don't hardcode the plugin filename.
    context.actorOf(Props(classOf[GamePlugin], "main.js"), name = "gamePlugin")
    context.actorOf(Props[RoutingTableLeader], name = "routingTableLeader")
    context.actorOf(Props[RoutingTableWorker], name = "routingTableWorker")
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

