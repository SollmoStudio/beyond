package beyond

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Ok

object JsonResponse {
  class Response(val status: String) {
    private val statusObj = ("status" -> new JsString(status))
    def apply(json: JsObject): Result = Ok(json + statusObj)
  }

  object Response {
    def apply(status: String): Response = new Response(status)
  }

  val ok = Response("ok")
  val badRequest = Response("bad request")
}
