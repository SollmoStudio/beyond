package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

object Admin extends Controller {
  private case class LoginData(username: String, password: String)

  private val MinUsernameLength = 4
  private val MaxUsernameLength = 12

  private val loginForm = Form(
    mapping(
      "username" -> nonEmptyText(minLength = MinUsernameLength, maxLength = MaxUsernameLength),
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  def index : Action[AnyContent]= Action { request =>
    request.session.get("username").map { username =>
      Ok(views.html.admin_index())
    }.getOrElse {
      Redirect(routes.Admin.login)
    }
  }

  def login : Action[AnyContent] = Action {
    Ok(views.html.admin_login())
  }

  def doLogin : Action[AnyContent] = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        // FIXME: Create an error type instead of passing String.
        BadRequest(views.html.admin_login("Invalid username"))
      },
      loginData => {
        // FIXME: Check password and redirect to login page if the given password is incorrect.
        val session = "username" -> loginData.username
        Redirect(routes.Admin.index).withSession(session)
      }
    )
  }
}
