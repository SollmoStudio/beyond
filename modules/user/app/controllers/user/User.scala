package controllers.user

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
        Ok(Json.obj("result" -> "OK", "message" -> "Account created", "username" -> username))
      case _ =>
        Ok(Json.obj("result" -> "Error", "message" -> "Already exists account"))
    }
  }
}
