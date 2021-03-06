package controllers

import play.api.mvc._

object Application extends Controller {

  def index: Action[AnyContent] = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def ping: Action[AnyContent] = Action {
    Ok("pong")
  }
}
