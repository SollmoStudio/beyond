package beyond

import akka.actor.Actor
import akka.actor.Props
import akka.actor.SupervisorStrategy
import akka.routing.Broadcast
import akka.routing.ConsistentHashingRouter
import beyond.UserActionActor.SyncRoutingTable
import play.api.libs.json.JsArray

object UserActionSupervisor {
  val Name: String = "userActionSupervisor"

  case object RequestRoutingTable
}

class UserActionSupervisor extends Actor {
  import UserActionSupervisor._
  private var routingTableData: JsArray = JsArray()
  private val userActionActor = {
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
  }

  override def receive: Receive = {
    case msg: SyncRoutingTable =>
      routingTableData = msg.data
      userActionActor.tell(Broadcast(msg), sender)
    case RequestRoutingTable =>
      sender ! SyncRoutingTable(routingTableData)
  }
}
