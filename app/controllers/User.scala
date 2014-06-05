package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.Crypto
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.MongoController
import reactivemongo.api.Cursor
import scala.concurrent.Future
import java.util.Date
import com.fasterxml.jackson.databind.JsonNode

object User extends Controller with MongoController {
  private def collection: JSONCollection = db.collection[JSONCollection]("user.account")

  private val accountForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    )
  )

  private case class Account(username: String, password: String, created: String)
  private implicit val accountFormat = Json.format[Account]

  //  def create: Action[AnyContent] = Action.async { implicit request =>
  //    import play.api.libs.concurrent.Execution.Implicits._
  //    val data = accountForm.bindFromRequest.data
  //
  //    val cursor: Cursor[Account] = collection.find(Json.obj("username" -> data("username"))).cursor[Account]
  //    println(cursor)
  //
  //    val result: Future[List[Account]] = cursor.collect[List]()
  //
  //    result.map {
  //      case List() =>
  //        val newAccount = Account(data("username"), data("password"), new Date().toLocaleString)
  //        collection.save(newAccount)
  //        Ok(Json.stringify(Json.toJson(newAccount)))
  //      case account :: _ =>
  //        Ok("Already created account " + data("username")).withSession("username" -> data("username"))
  //    }
  //  }
  def create: Action[AnyContent] = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val data = accountForm.bindFromRequest.data

    val cursor: Cursor[Account] = collection.find(Json.obj("username" -> data("username"))).cursor[Account]

    val result: Future[List[Account]] = cursor.collect[List]()

    result.map {
      case List() =>
        val newAccount = Account(data("username"), Crypto.encryptAES(data("password")), new Date().toLocaleString)
        collection.save(newAccount)
        val resultAccount = Json.toJson(Account(newAccount.username, Crypto.decryptAES(newAccount.password), newAccount.created))
        println(resultAccount)
        val result = resultAccount \ "message" \ "Success"
        println(result)
        Ok(Json.stringify(result))
      case account :: _ =>
        Ok("Already created account " + data("username")).withSession("username" -> data("username"))
    }
  }

}
