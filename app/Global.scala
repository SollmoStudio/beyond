import concurrent.Future
import play.api.GlobalSettings
import play.api.Logger
import play.api.Mode
import play.api.Play
import play.api.mvc.RequestHeader
import play.api.mvc.Results.NotFound
import play.api.mvc.SimpleResult

object Global extends GlobalSettings {
  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    Play.maybeApplication match {
      case Some(app) if app.mode == Mode.Prod => Future.successful(NotFound(""))
      case _ => super.onHandlerNotFound(request)
    }
  }
}
