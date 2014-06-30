package beyond

import play.api.libs.concurrent.Promise
import play.api.mvc.Filter
import play.api.mvc.RequestHeader
import play.api.mvc.Results.InternalServerError
import play.api.mvc.SimpleResult
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object TimeoutFilter extends Filter {
  import ExecutionContext.Implicits.global
  def apply(next: (RequestHeader) => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
    val timeout = BeyondConfiguration.requestTimeout
    val timeoutFuture = Promise.timeout("Timeout", timeout)
    val resultFuture = next(request)
    Future.firstCompletedOf(Seq(resultFuture, timeoutFuture)).map {
      case result: SimpleResult => result
      case errorMessage: String => InternalServerError(errorMessage)
    }
  }
}
