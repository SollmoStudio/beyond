package beyond

import play.api.mvc.ActionBuilder
import play.api.mvc.Request
import play.api.mvc.Results.Forbidden
import play.api.mvc.SimpleResult
import play.api.mvc.WrappedRequest
import scala.concurrent.Future

class AuthenticatedRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)

object Authenticated extends ActionBuilder[AuthenticatedRequest] {
  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[SimpleResult]) : Future[SimpleResult] = {
    request.session.get("username").map { username =>
      block(new AuthenticatedRequest(username, request))
    } getOrElse {
      Future.successful(Forbidden)
    }
  }
}
