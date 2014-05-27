package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSelection
import akka.pattern.AskTimeoutException
import akka.pattern.ask
import akka.pattern.pipe
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import akka.util.Timeout
import beyond.route.Address
import beyond.route.RoutingTableView
import beyond.route.RoutingTableView._
import play.api.libs.concurrent.Akka
import play.api.libs.json.JsArray
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers
import play.api.mvc.Request
import play.api.mvc.Results.Forbidden
import play.api.mvc.Results.InternalServerError
import play.api.mvc.Results.Status
import play.api.mvc.SimpleResult
import play.api.mvc.WrappedRequest
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object UserAction {
  import play.api.Play.current
  import UserActionActor._

  private val userActionActor: ActorSelection = Akka.system.actorSelection(BeyondSupervisor.UserActionActorPath)

  def apply(block: => SimpleResult): Action[AnyContent] = apply(_ => block)

  def apply(block: RequestWithUsername[AnyContent] => SimpleResult): Action[AnyContent] = apply(BodyParsers.parse.anyContent)(block)

  def apply[A](bodyParser: BodyParser[A])(block: RequestWithUsername[A] => SimpleResult): Action[A] = async(bodyParser) { request =>
    Future.successful(block(request))
  }

  def async(block: => Future[SimpleResult]): Action[AnyContent] = async(_ => block)

  def async(block: RequestWithUsername[AnyContent] => Future[SimpleResult]): Action[AnyContent] = async(BodyParsers.parse.anyContent)(block)

  def async[A](bodyParser: BodyParser[A])(block: RequestWithUsername[A] => Future[SimpleResult]): Action[A] = new Action[A] {
    override def parser: BodyParser[A] = bodyParser

    override def apply(request: Request[A]): Future[SimpleResult] = {
      // FIXME: Verify if this request belongs to this server.
      request.session.get("username").map { username =>
        import play.api.libs.concurrent.Akka
        import play.api.Play.current
        implicit val ec: ExecutionContext = Akka.system.dispatcher
        implicit val timeout = Timeout(BeyondConfiguration.requestTimeout)
        val blockAndRequest = BlockAndRequest(block, new RequestWithUsername(username, request))
        (userActionActor ? blockAndRequest).asInstanceOf[Future[SimpleResult]].recover {
          case _: AskTimeoutException => InternalServerError("UserAction Timeout")
        }
      } getOrElse {
        Future.successful(Forbidden)
      }
    }
  }
}

object UserActionActor {
  val Name: String = "userActionActor"

  case class RequestWithUsername[A](username: String, request: Request[A]) extends WrappedRequest[A](request) with ConsistentHashable {
    // FIXME: Use a better hashing algorithm.
    override def consistentHashKey: Any = username.##
  }
  case class BlockAndRequest[A](block: (RequestWithUsername[A]) => Future[SimpleResult], request: RequestWithUsername[A]) extends ConsistentHashable {
    override def consistentHashKey: Any = request.consistentHashKey
  }

  case class UpdateRoutingTable(data: JsArray)
}

private class UserActionActor extends Actor with ActorLogging {
  var routingTable: RoutingTableView = new RoutingTableView(BeyondConfiguration.currentServerAddress)
  import UserActionActor._
  override def receive: Receive = {
    case BlockAndRequest(block, request) => {
      val hash = request.consistentHashKey.asInstanceOf[Int]
      routingTable.queryServerToHandle(hash) match {
        case HandleHere =>
          import play.api.libs.concurrent.Akka
          import play.api.Play.current
          implicit val ec: ExecutionContext = Akka.system.dispatcher
          block(request) pipeTo sender
        case HandleIn(address: Address) =>
          val RedirectStatusCode = 310 // This code is just one of a redirection code not used by HTTP. It can be changed.
          log.info("Request by {} is redirected to {}", request.username, address)
          sender ! new Status(RedirectStatusCode)(address)
      }
    }
    case UpdateRoutingTable(jsRoutingTable) =>
      log.info("Routing table is updated.")
      routingTable = new RoutingTableView(BeyondConfiguration.currentServerAddress, jsRoutingTable)
  }
}
