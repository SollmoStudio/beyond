package controllers

import beyond.UserAction
import play.api.data.Form
import play.api.data.Forms.text
import play.api.data.Forms.tuple
import play.api.mvc._

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
    Ok("Hello " + username).withSession("username" -> username)
  }

  def logout : Action[AnyContent] = UserAction { request =>
    Ok("Goodbye " + request.username).withNewSession
  }
}
