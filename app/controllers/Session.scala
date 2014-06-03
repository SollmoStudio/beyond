package controllers

import beyond.UserAction
import play.api.data.Form
import play.api.data.Forms.text
import play.api.data.Forms.tuple
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor
import play.api.libs.json._
import scala.concurrent.Future

object Session extends Controller with MongoController {
  private def collection: JSONCollection = db.collection[JSONCollection]("session.account")

  private case class sessionAccount(username: String, password: String)
  private implicit val sessionAccountFormat = Json.format[sessionAccount]

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
    Ok("Hello " + username).withSession("username" -> username)
  }

  def logout: Action[AnyContent] = UserAction { request =>
    Ok("Goodbye " + request.username).withNewSession
  }

  def create: Action[AnyContent] = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._
    val data = loginForm.bindFromRequest.data

    val cursor: Cursor[sessionAccount] = collection.find(Json.obj("username" -> data("username"))).cursor[sessionAccount]
    val result: Future[List[sessionAccount]] = cursor.collect[List]()

    result.map {
      case List() =>
        val newSessionAccount = Json.obj(
          "username" -> data("username"),
          "password" -> data("password"),
          "created" -> new java.util.Date().getTime() / 1000    //unix time
        )
        collection.save(newSessionAccount)
        Ok("Welcome to " + data("username")).withSession("username" -> data("username"))
      case sessionAccount :: _ =>
        if (sessionAccount.password == data("password")) {
          Ok("Already created session account " + data("username")).withSession("username" -> data("username"))
        } else {
          Ok("Invalid password " + data("username"))
        }
    }
  }
}