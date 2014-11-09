package beyond.engine.javascript.lib.http

import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJSON
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSGetter
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.ACCEPTED
import play.api.libs.iteratee.Iteratee
import play.api.mvc.Result
import play.api.mvc.Results.Status
import scala.concurrent.Await
import scala.concurrent.duration._

object ScriptableResponse {
  val DefaultContentType: String = "plain/text"
  val DefaultStatusCode: Int = ACCEPTED

  private def optionalArg[T](args: JSArray, idx: Int): Option[T] =
    if (args.isDefinedAt(idx)) {
      Some(args(idx).asInstanceOf[T])
    } else {
      None
    }

  private def optionalArgInt(args: JSArray, idx: Int): Option[Int] =
    optionalArg[AnyRef](args, idx).map(ScriptRuntime.toInt32)

  private def stringify(context: Context, obj: AnyRef): String =
    obj match {
      case str: String =>
        str
      case other =>
        val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
        val scope = beyondContextFactory.global
        NativeJSON.stringify(context, scope, other, null, null).asInstanceOf[String]
    }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableResponse = {
    val body = stringify(context, args(0))
    val contentType = optionalArg[String](args, 1).getOrElse(DefaultContentType)
    val statusCode = optionalArgInt(args, 2).getOrElse(DefaultStatusCode)

    // FIXME: Support other HTTP status codes.
    // FIXME: Currently, the default type of contentType is plain/text,
    // but the play framework supports contentType inference. Fix it to use this feature.
    new ScriptableResponse(new Status(statusCode)(body).as(contentType))
  }
}

class ScriptableResponse(val result: Result) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "ResponseInternal"

  @JSGetter
  def getBody: String =
    new String(Await.result(result.body |>>> Iteratee.consume[Array[Byte]](), Duration.Inf))

  @JSGetter
  def getContentType: String = result.header.headers(CONTENT_TYPE)

  @JSGetter
  def getStatusCode: Int = result.header.status
}
