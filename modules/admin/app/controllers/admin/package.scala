package controllers

import play.api.mvc.ActionBuilder
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.mvc.WrappedRequest
import scala.concurrent.Future

package object admin {
  private[admin] class AuthenticatedRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)

  private[admin] object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      request.session.get("username").map { username =>
        block(new AuthenticatedRequest(username, request))
      } getOrElse {
        Future.successful(Redirect(routes.Admin.login()))
      }
    }
  }
}
