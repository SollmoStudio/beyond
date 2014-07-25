package controllers.user

import beyond.JsonResponse
import db.UserMixin
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.Future

object User extends Controller with UserMixin {
  def create: Action[AnyContent] = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val data = accountForm.bindFromRequest.data
    val username = data("username")
    val password = data("password")

    val result = collection.find(Json.obj("username" -> username)).one[Account]

    result.map {
      case None =>
        collection.save(Json.toJson(createAccount(username, password)))
        JsonResponse.ok(Json.obj("message" -> "User Created", "username" -> username))
      case _ =>
        JsonResponse.badRequest(Json.obj("message" -> "Username Duplicated"))
    }
  }
}
