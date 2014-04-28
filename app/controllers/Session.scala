package controllers

import concurrent.Future
import play.api.data.Form
import play.api.data.Forms.tuple
import play.api.data.Forms.text
import play.api.mvc._
import play.api.mvc.Results.Forbidden

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

object Session extends Controller {
  private val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    )
  )

  def login : Action[AnyContent] = Action { implicit request =>
    val data = loginForm.bindFromRequest.data
    // FIXME: Check password
    val username = data("username")
    val session = ("username", username)
    Ok("Hello " + username).withSession(session)
  }

  def logout : Action[AnyContent] = Authenticated { request =>
    Ok("Goodbye " + request.username).withNewSession
  }
}
