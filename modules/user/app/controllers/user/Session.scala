package controllers.user

import beyond.GameEvent
import beyond.UserAction
import controllers.User.Account
import org.apache.commons.codec.digest.DigestUtils
import play.api.data.Form
import play.api.data.Forms.text
import play.api.data.Forms.tuple
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor
import scala.concurrent.Future

object Session extends Controller with MongoController with GameEvent {
  private def collection: JSONCollection = db.collection[JSONCollection]("user.account")

  private val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    )
  )

  implicit val accountFormat = Json.format[Account]

  def login: Action[AnyContent] = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._
    val data = loginForm.bindFromRequest.data

    val username = data("username")

    val cursor: Cursor[Account] = collection.find(Json.obj("username" -> username)).cursor[Account]
    val result: Future[List[Account]] = cursor.collect[List]()

    result.map {
      case List() =>
        Ok(Json.obj("result" -> "Error", "message" -> "Cannot find an account", "username" -> username))
      case account :: _ =>
        if (DigestUtils.shaHex(data("password")) == account.password) {
          track("User Login", Json.obj("username" -> username))
          Ok(Json.obj("result" -> "OK", "message" -> "Hello myname")).withSession("username" -> username)
        } else {
          Ok(Json.obj("result" -> "Error", "message" -> "Invalid password"))
        }
    }
  }

  def logout: Action[AnyContent] = UserAction { implicit request =>
    trackUser("User Logout")
    Ok("Goodbye " + request.username).withNewSession
  }
}
