package controllers

import play.api._
import play.api.libs.json.Json
import play.api.mvc._

object Application extends Controller {

  def index : Action[play.api.mvc.AnyContent]= Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def ping : Action[AnyContent] = Action {
    Ok("pong")
  }
}
