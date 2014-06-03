package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.Crypto
import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.MongoController
import reactivemongo.api.Cursor
import scala.concurrent.Future

object User extends Controller with MongoController {
  private def collection: JSONCollection = db.collection[JSONCollection]("user.account")

  private val accountForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    )
  )

  private case class Account(username: String, password: String, created_at: String)
  private implicit val accountFormat = Json.format[Account]

  def create: Action[AnyContent] = Action.async { implicit request =>

    import org.joda.time.DateTime
    import org.joda.time.format.DateTimeFormat
    import play.api.libs.concurrent.Execution.Implicits._

    val data = accountForm.bindFromRequest.data

    val createdTime = DateTimeFormat.forPattern("yyyy-mm-dd hh:mm:ss").print(new DateTime)
    val cursor: Cursor[Account] = collection.find(Json.obj("username" -> data("username"))).cursor[Account]
    val result: Future[List[Account]] = cursor.collect[List]()

    result.map {
      case List() =>
        val newAccount = Account(data("username"), Crypto.encryptAES(data("password")), createdTime)
        collection.save(newAccount)
        Ok(Json.obj("result" -> "OK", "message" -> "saved a new user account", "username" -> newAccount.username,
          "password" -> Crypto.decryptAES(newAccount.password), "created_at" -> newAccount.created_at))
      case account :: _ =>
        Ok(Json.obj("result" -> "Error", "message" -> "Already created account"))
    }
  }
}
