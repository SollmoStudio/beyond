package controllers

import play.api._
import play.api.mvc._

object Plugin extends Controller {
  def route(path: String) : Action[AnyContent] = Action {
    Ok("Done")
  }
}

