package beyond

import akka.actor.ActorRef
import akka.actor.Actor
import akka.actor.Props
import akka.pattern.ask
import akka.pattern.pipe
import akka.routing.ConsistentHashingRouter
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers
import play.api.mvc.Request
import play.api.mvc.Results.Forbidden
import play.api.mvc.SimpleResult
import play.api.mvc.WrappedRequest
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UserActionRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)

object UserAction {
  private def createUserActionRouter() = {
    val numProcessors = Runtime.getRuntime().availableProcessors()
    val router = ConsistentHashingRouter(nrOfInstances = numProcessors)
    Akka.system.actorOf(Props[UserActionActor].withRouter(router), name = "userActionActor")
  }

  private val userActionActor: ActorRef = createUserActionRouter()

  def apply(block: => SimpleResult): Action[AnyContent] = apply(_ => block)

  def apply(block: UserActionRequest[AnyContent] => SimpleResult): Action[AnyContent] = apply(BodyParsers.parse.anyContent)(block)

  def apply[A](bodyParser: BodyParser[A])(block: UserActionRequest[A] => SimpleResult): Action[A] = async(bodyParser) { request =>
    Future.successful(block(request))
  }

  def async(block: => Future[SimpleResult]): Action[AnyContent] = async(_ => block)

  def async(block: UserActionRequest[AnyContent] => Future[SimpleResult]): Action[AnyContent] = async(BodyParsers.parse.anyContent)(block)

  def async[A](bodyParser: BodyParser[A])(block: UserActionRequest[A] => Future[SimpleResult]): Action[A] = new Action[A] {
    override def parser: BodyParser[A] = bodyParser

    override def apply(request: Request[A]): Future[SimpleResult] = {
      // FIXME: Verify if this request belongs to this server.
      request.session.get("username").map { username =>
        implicit val Timeout: Timeout= 1000
        ask(userActionActor, BlockAndRequest(block, new UserActionRequest(username, request))).asInstanceOf[Future[SimpleResult]]
      } getOrElse {
        Future.successful(Forbidden)
      }
    }
  }
}

private case class BlockAndRequest[A] (block: (UserActionRequest[A]) => Future[SimpleResult], request: UserActionRequest[A]) extends ConsistentHashable {
  // FIXME: Use a better hashing algorithm.
  override def consistentHashKey: Any = request.username.hashCode()
}

private class UserActionActor extends Actor {
  override def receive: Receive = {
    case BlockAndRequest(block, request) => {
      implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
      block(request) pipeTo sender
    }
  }
}
