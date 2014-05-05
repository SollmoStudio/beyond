package controllers

import beyond.plugin.GamePlugin
import play.api._
import play.api.mvc._

object Plugin extends Controller {
  private val gamePlugin: GamePlugin = GamePlugin()

  def route(path: String) : Action[AnyContent] = Action { request =>
    val result = gamePlugin.handle(request.path)
    Ok(result)
  }
}

