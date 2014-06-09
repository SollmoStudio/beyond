package controllers

import beyond.GameEvent
import beyond.UserAction
import play.api.data.Form
import play.api.data.Forms.text
import play.api.data.Forms.tuple
import play.api.libs.json._
import play.api.mvc._

object Session extends Controller with GameEvent {
  private val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    )
  )

  def login: Action[AnyContent] = Action { implicit request =>
    val data = loginForm.bindFromRequest.data
    // FIXME: Check password

    val username = data("username")
    track("User Login", Json.obj("username" -> username))
    Ok("Hello " + username).withSession("username" -> username)
  }

  def logout: Action[AnyContent] = UserAction { implicit request =>
    trackUser("User Logout")
    Ok("Goodbye " + request.username).withNewSession
  }
}
