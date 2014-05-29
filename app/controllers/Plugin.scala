package controllers

import akka.actor.ActorSelection
import akka.pattern.AskTimeoutException
import akka.pattern.ask
import akka.util.Timeout
import beyond.BeyondConfiguration
import beyond.plugin.GamePlugin.Handle
import play.api.libs.concurrent.Akka
import play.api.mvc._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object Plugin extends Controller {
  import play.api.Play.current
  private val gamePlugin: ActorSelection = Akka.system.actorSelection("/user/beyondSupervisor/gamePlugin")

  def route(path: String): Action[AnyContent] = Action.async { request =>
    implicit val timeout = Timeout(BeyondConfiguration.requestTimeout)
    implicit val ec: ExecutionContext = Akka.system.dispatcher

    (gamePlugin ? Handle(request)).asInstanceOf[Future[String]].map(Ok(_)).recover {
      case _: AskTimeoutException => InternalServerError("Plugin Timeout")
    }
  }
}
