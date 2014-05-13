package beyond

import akka.actor.ActorRef
import akka.actor.Props
import play.api.Application
import play.api.libs.concurrent.Akka
import play.api.Mode
import play.api.Play
import play.api.libs.concurrent.Promise
import play.api.mvc.Filter
import play.api.mvc.RequestHeader
import play.api.mvc.Results.InternalServerError
import play.api.mvc.Results.NotFound
import play.api.mvc.SimpleResult
import play.api.mvc.WithFilters
import scala.concurrent.duration._
import scala.concurrent.Future

private object TimeoutFilter extends Filter {
  def apply(next: (RequestHeader) => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    import play.api.Play.current

    val timeout = Duration(current.configuration.getString("beyond.request-timeout").getOrElse("30s"))
    val timeoutFuture = Promise.timeout("Timeout", timeout)
    val resultFuture = next(request)
    Future.firstCompletedOf(Seq(resultFuture, timeoutFuture)).map {
      case result: SimpleResult => result
      case errorMessage: String => InternalServerError(errorMessage)
    }
  }
}

object Global extends WithFilters(TimeoutFilter) {
  private var beyondSupervisor: Option[ActorRef] = _

  override def onStart(app: Application) {
    beyondSupervisor = Some(Akka.system(app).actorOf(Props[BeyondSupervisor], name = "beyondSupervisor"))
  }

  override def onStop(app: Application) {
    beyondSupervisor.foreach(Akka.system(app).stop(_))
    beyondSupervisor = None
  }

  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    Play.maybeApplication.filter(_.mode == Mode.Prod).map { _ =>
      Future.successful(NotFound)
    } getOrElse {
      super.onHandlerNotFound(request)
    }
  }
}
