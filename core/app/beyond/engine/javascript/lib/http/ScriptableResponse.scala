package beyond.engine.javascript.lib.http

import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.ScriptableObject
import play.api.mvc.Result
import play.api.mvc.Results.Status

object ScriptableResponse {
  private def optionalArg[T](args: JSArray, idx: Int): Option[T] =
    if (args.isDefinedAt(idx)) {
      Some(args(idx).asInstanceOf[T])
    } else {
      None
    }

  private def optionalArgInt(args: JSArray, idx: Int): Option[Int] =
    optionalArg[AnyRef](args, idx).map(ScriptRuntime.toInt32)

  private def optionalArgString(args: JSArray, idx: Int): Option[String] =
    optionalArg[String](args, idx).map(ScriptRuntime.toString)

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableResponse = {
    val result = args(0).asInstanceOf[String]
    val contentType = optionalArgString(args, 1).getOrElse("plain/text")
    val statusCode = optionalArgInt(args, 2).getOrElse(play.api.http.Status.ACCEPTED)

    // FIXME: Support other HTTP status codes.
    // FIXME: Currently, the default type of contentType is plain/text,
    // but the play framework supports contentType inference. Fix it to use this feature.
    new ScriptableResponse(new Status(statusCode)(result).as(contentType))
  }
}

class ScriptableResponse(val result: Result) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "ResponseInternal"
}
