package beyond.metrics

import akka.actor.Props
import beyond.BeyondConfiguration
import play.api.libs.concurrent.Akka
import play.api.mvc._
import scala.concurrent.Future

object RequestsCountFilter extends Filter {
  // This has to be lazy, because Filter is initialized before application has been started.
  private lazy val requestsCounter = {
    import play.api.Play.current
    if (BeyondConfiguration.enableMetrics) {
      Akka.system.actorOf(Props[RequestsCounter], name = RequestsCounter.Name)
    } else {
      Akka.system.deadLetters
    }
  }
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    requestsCounter ! RequestsCounter.Increase
    next(request)
  }
}
