package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.pattern.pipe
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import beyond.UserActionSupervisor.RequestRoutingTable
import beyond.route.RouteAddress
import beyond.route.RoutingTableView
import beyond.route.RoutingTableView._
import play.api.libs.json.JsArray
import play.api.mvc.Request
import play.api.mvc.Results.Status
import play.api.mvc.SimpleResult
import play.api.mvc.WrappedRequest
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object UserActionActor {
  val Name: String = "userActionActor"

  case class RequestWithUsername[A](username: String, request: Request[A]) extends WrappedRequest[A](request) with ConsistentHashable {
    // FIXME: Use a better hashing algorithm.
    override def consistentHashKey: Any = username.##
  }
  case class BlockAndRequest[A](block: (RequestWithUsername[A]) => Future[SimpleResult], request: RequestWithUsername[A]) extends ConsistentHashable {
    override def consistentHashKey: Any = request.consistentHashKey
  }

  case class SyncRoutingTable(data: JsArray)
}

private class UserActionActor extends Actor with ActorLogging {
  private var routingTable: RoutingTableView = new RoutingTableView(BeyondConfiguration.currentServerRouteAddress)
  private val userActionSupervisor = {
    import play.api.libs.concurrent.Akka
    import play.api.Play.current
    implicit val ec: ExecutionContext = Akka.system.dispatcher
    Akka.system.actorSelection(BeyondSupervisor.UserActionSupervisorPath)
  }
  import UserActionActor._
  override def receive: Receive = {
    case BlockAndRequest(block, request) =>
      val hash = request.consistentHashKey.asInstanceOf[Int]
      routingTable.queryServerToHandle(hash) match {
        case HandleHere =>
          import play.api.libs.concurrent.Akka
          import play.api.Play.current
          implicit val ec: ExecutionContext = Akka.system.dispatcher
          block(request) pipeTo sender
        case HandleIn(address: RouteAddress) =>
          val RedirectStatusCode = 310 // This code is just one of a redirection code not used by HTTP. It can be changed.
          log.info("Request by {} is redirected to {}", request.username, address)
          sender ! new Status(RedirectStatusCode)(address)
      }
    case SyncRoutingTable(jsRoutingTable) =>
      log.info("Routing table is updated.")
      routingTable = new RoutingTableView(BeyondConfiguration.currentServerRouteAddress, jsRoutingTable)
  }

  override def preStart() {
    userActionSupervisor ! RequestRoutingTable
  }
}
