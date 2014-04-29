package controllers

import beyond.Global
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import scala.concurrent.Future

object Admin extends Controller {
  private case class LoginData(username: String, password: String)

  private val MinUsernameLength = 4
  private val MaxUsernameLength = 12

  private val loginForm = Form(
    mapping(
      "username" -> nonEmptyText(minLength = MinUsernameLength, maxLength = MaxUsernameLength),
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  def index : Action[AnyContent]= Action { request =>
    request.session.get("username").map { username =>
      Ok(views.html.admin_index())
    }.getOrElse {
      Redirect(routes.Admin.login)
    }
  }

  def login : Action[AnyContent] = Action {
    Ok(views.html.admin_login())
  }

  def doLogin : Action[AnyContent] = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        // FIXME: Create an error type instead of passing String.
        val result = BadRequest(views.html.admin_login("Invalid username"))
        Future.successful(result)
      },
      loginData => {
        import play.api.libs.concurrent.Execution.Implicits._

        val db = Global.mongoConnection.get.db("admin")
        val collection = db.collection[BSONCollection]("password")
        val query = BSONDocument("username" -> loginData.username)
        val filter = BSONDocument("password" -> 1, "_id" -> 0)

        val cursor = collection.find(query, filter).cursor[BSONDocument]
        // FIXME: How to handle network errors?
        val result: Future[List[BSONDocument]] = cursor.collect[List]()
        result.map[SimpleResult] {
          case List() => BadRequest(views.html.admin_login("No such username"))
          case passwordDoc :: _ =>
            val password = passwordDoc.getAs[String]("password").get
            if (password == loginData.password) {
              val session = "username" -> loginData.username
              Redirect(routes.Admin.index).withSession(session)
            } else {
              BadRequest(views.html.admin_login("Invalid password"))
            }
        }
      }
    )
  }
}
