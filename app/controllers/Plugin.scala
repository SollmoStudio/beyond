package controllers

import beyond.plugin.GamePlugin
import play.api.mvc._

object Plugin extends Controller {
  def route(path: String): Action[AnyContent] = Action.async { request =>
    GamePlugin.handle[AnyContent](request)
  }
}
