package beyond

import play.api.libs.concurrent.Promise
import play.api.mvc._
import play.api.mvc.Results.InternalServerError
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object TimeoutFilter extends Filter {
  import ExecutionContext.Implicits.global
  def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    val timeout = BeyondConfiguration.requestTimeout
    val timeoutFuture = Promise.timeout("Timeout", timeout)
    val resultFuture = next(request)
    Future.firstCompletedOf(Seq(resultFuture, timeoutFuture)).map {
      case result: Result => result
      case errorMessage: String => InternalServerError(errorMessage)
    }
  }
}
