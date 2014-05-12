package controllers

import beyond.plugin.GamePlugin
import play.api._
import play.api.mvc._

object Plugin extends Controller {
  // FIXME: Don't hardcode the plugin filename.
  private val gamePlugin: GamePlugin = GamePlugin("main.js")

  def route(path: String) : Action[AnyContent] = Action { request =>
    val result = gamePlugin.handle(request)
    Ok(result)
  }
}

