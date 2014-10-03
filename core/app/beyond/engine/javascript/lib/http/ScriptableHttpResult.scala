package beyond.engine.javascript.lib.http

import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import com.beyondframework.rhino.ContextOps._
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSGetter
import play.api.libs.ws._
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder

object ScriptableHttpResult {
  private[lib] def apply(context: Context, res: WSResponse): ScriptableHttpResult = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    context.newObject(scope, "HttpResult", res).asInstanceOf[ScriptableHttpResult]
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableHttpResult = {
    val res = args(0) match {
      case wsResponse: WSResponse =>
        wsResponse
      case _ => throw new IllegalArgumentException("type.is.not.matched")
    }

    new ScriptableHttpResult(context, res)
  }
}

class ScriptableHttpResult(context: Context, res: WSResponse) extends ScriptableObject {
  def this() = this(null, null)

  override def getClassName: String = "HttpResult"

  @JSGetter
  def status: Int = res.status

  @JSGetter
  def statusText: String = res.statusText

  @JSGetter
  def headers: Scriptable = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global

    val headerObj: Scriptable = context.newObject(scope)
    res.allHeaders.foreach {
      case (key, values) =>
        val value: AnyRef =
          if (values.length == 1) {
            values(0)
          } else {
            context.newArray(scope, values.toArray)
          }
        headerObj.put(key, headerObj, value)
    }
    headerObj
  }

  @JSGetter
  def body: String = res.body
}
