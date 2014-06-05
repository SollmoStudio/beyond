package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.MongoController
import reactivemongo.api.Cursor
import scala.concurrent.Future
import java.util.Date

object User extends Controller with MongoController {
  private def collection: JSONCollection = db.collection[JSONCollection]("user.account")

  private val accountForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    )
  )

  private case class Account(username: String, password: String)
  private implicit val accountFormat = Json.format[Account]

  def create: Action[AnyContent] = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._
    val data = accountForm.bindFromRequest.data

    val cursor: Cursor[Account] = collection.find(Json.obj("username" -> data("username"))).cursor[Account]
    val result: Future[List[Account]] = cursor.collect[List]()

    result.map {
      case List() =>
        val newAccount = Json.obj(
          "username" -> data("username"),
          "password" -> data("password"),
          "created" -> new Date().toLocaleString
        )
        collection.save(newAccount)
        Ok("Welcome to " + data("username")).withSession("username" -> data("username"))
      case account :: _ =>
        if (account.password == data("password")) {
          Ok("Already created session account " + data("username")).withSession("username" -> data("username"))
        } else {
          Ok("Invalid password " + data("username"))
        }
    }
  }
}
