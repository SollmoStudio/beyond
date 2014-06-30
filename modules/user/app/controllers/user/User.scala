package controllers.user

import java.util.Date
import org.apache.commons.codec.digest.DigestUtils
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
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

  case class Account(username: String, password: String, creationDate: Date)

  implicit val accountFormat = Json.format[Account]

  def create: Action[AnyContent] = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val data = accountForm.bindFromRequest.data
    val now = new Date()

    val cursor: Cursor[Account] = collection.find(Json.obj("username" -> data("username"))).cursor[Account]
    val result: Future[List[Account]] = cursor.collect[List]()

    result.map {
      case List() =>
        val newAccount = Account(data("username"), DigestUtils.shaHex(data("password")), now)
        collection.save(Json.toJson(newAccount))
        Ok(Json.obj("result" -> "OK", "message" -> "Account created", "username" -> newAccount.username))
      case _ =>
        Ok(Json.obj("result" -> "Error", "message" -> "Already exists account"))
    }
  }
}
