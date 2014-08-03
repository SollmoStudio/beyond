package controllers.user

import beyond.GameEvent
import beyond.JsonResponse
import beyond.UserAction
import db.UserMixin
import play.api.libs.json._
import play.api.mvc._

object Session extends Controller with GameEvent with UserMixin {
  def login: Action[AnyContent] = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val data = accountForm.bindFromRequest.data
    val username = data("username")
    val encryptedPassword = encryptPassword(data("password"))

    val result = collection.find(Json.obj("username" -> username, "password" -> encryptedPassword)).one[Account]

    result.map {
      case Some(_) => {
        track("User Login", Json.obj("username" -> username))
        JsonResponse.ok(Json.obj("message" -> "User Login", "username" -> username)).withSession("username" -> username)
      }
      case None => {
        track("User Login Failed", Json.obj("username" -> username))
        JsonResponse.unauthorized(Json.obj("message" -> "Invalid Username Or Password"))
      }
    }
  }

  def logout: Action[AnyContent] = UserAction { implicit request =>
    val username = request.session.get("username").getOrElse("No User")

    trackUser("User Logout", Json.obj("username" -> username))
    JsonResponse.ok(Json.obj("message" -> "User Logout", "username" -> username))
  }
}
