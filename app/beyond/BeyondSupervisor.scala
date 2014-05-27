package beyond

import akka.actor.Actor
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy
import akka.actor.SupervisorStrategy._
import akka.routing.Broadcast
import akka.routing.ConsistentHashingRouter
import beyond.UserActionActor.UpdateRoutingTable
import beyond.plugin.GamePlugin
import beyond.route.RoutingTableLeader
import beyond.route.RoutingTableWorker

object BeyondSupervisor {
  val BeyoundSupervisorBasePath: String = "/user/beyondSupervisor/"

  val UserActionActorPath: String = BeyoundSupervisorBasePath + UserActionActor.Name
}

class BeyondSupervisor extends Actor {
  import BeyondSupervisor._
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
    // There is no priority between the routing table update message and other messages.
    //  So updating the routing table is not immediately applied, but is applied after
    //  all the requests that are already in the mailbox.
    //  It's not a problem because Beyond is designed to ensure eventual consistency
    //  not strong consistency.
    context.actorOf(Props[UserActionActor].withRouter(router), UserActionActor.Name)
    context.actorOf(Props[LauncherSupervisor], LauncherSupervisor.Name)
    // FIXME: Don't hardcode the plugin filename.
    context.actorOf(Props(classOf[GamePlugin], "main.js"), GamePlugin.Name)
    context.actorOf(Props[SystemMetricsActor], SystemMetricsActor.Name)
    context.actorOf(Props[RoutingTableLeader], RoutingTableLeader.Name)
    context.actorOf(Props[RoutingTableWorker], RoutingTableWorker.Name)
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      // FIXME: Need policy for all exceptions escalated by Beyond actors.
      case t =>
        super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }

  override def receive: Receive = {
    case msg: UpdateRoutingTable =>
      import play.api.libs.concurrent.Akka
      import play.api.Play.current
      import scala.concurrent.ExecutionContext
      implicit val ec: ExecutionContext = Akka.system.dispatcher
      Akka.system.actorSelection(UserActionActorPath).tell(Broadcast(msg), sender)
    case _ =>
  }
}

