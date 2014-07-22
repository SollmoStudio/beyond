package beyond

import akka.actor.ActorSelection
import akka.pattern.AskTimeoutException
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.mvc.Results.Forbidden
import play.api.mvc.Results.InternalServerError
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object UserAction {
  import play.api.Play.current
  import UserActionActor._

  private val userActionActor: ActorSelection = Akka.system.actorSelection(BeyondSupervisor.UserActionActorPath)

  def apply(block: => Result): Action[AnyContent] = apply(_ => block)

  def apply(block: RequestWithUsername[AnyContent] => Result): Action[AnyContent] = apply(BodyParsers.parse.anyContent)(block)

  def apply[A](bodyParser: BodyParser[A])(block: RequestWithUsername[A] => Result): Action[A] = async(bodyParser) { request =>
    Future.successful(block(request))
  }

  def async(block: => Future[Result]): Action[AnyContent] = async(_ => block)

  def async(block: RequestWithUsername[AnyContent] => Future[Result]): Action[AnyContent] = async(BodyParsers.parse.anyContent)(block)

  def async[A](bodyParser: BodyParser[A])(block: RequestWithUsername[A] => Future[Result]): Action[A] = new Action[A] {
    override def parser: BodyParser[A] = bodyParser

    override def apply(request: Request[A]): Future[Result] = {
      // FIXME: Verify if this request belongs to this server.
      request.session.get("username").map { username =>
        import play.api.libs.concurrent.Akka
        import play.api.Play.current
        implicit val ec: ExecutionContext = Akka.system.dispatcher
        implicit val timeout = Timeout(BeyondConfiguration.requestTimeout)
        val blockAndRequest = BlockAndRequest(block, new RequestWithUsername(username, request))
        (userActionActor ? blockAndRequest).asInstanceOf[Future[Result]].recover {
          case _: AskTimeoutException => InternalServerError("UserAction Timeout")
        }
      } getOrElse {
        Future.successful(Forbidden)
      }
    }
  }
}

