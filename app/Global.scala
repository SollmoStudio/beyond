import concurrent.Future
import play.api.GlobalSettings
import play.api.Mode
import play.api.Play
import play.api.mvc.RequestHeader
import play.api.mvc.Results.NotFound
import play.api.mvc.SimpleResult

object Global extends GlobalSettings {
  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    Play.maybeApplication.filter(_.mode == Mode.Prod).map { _ =>
      Future.successful(NotFound)
    } getOrElse {
      super.onHandlerNotFound(request)
    }
  }
}
