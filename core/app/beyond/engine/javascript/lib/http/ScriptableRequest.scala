package beyond.engine.javascript.lib.http

import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import com.beyondframework.rhino.ContextOps._
import com.beyondframework.rhino.ScriptableMap
import org.mozilla.javascript.annotations.JSGetter
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeJSON
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import play.api.libs.json.JsNull
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.mvc.Request

object ScriptableRequest {
  def apply[A](context: Context, request: Request[A]): ScriptableRequest = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    context.newObject(scope, "Request", request).asInstanceOf[ScriptableRequest]
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableRequest = {
    val request = args(0).asInstanceOf[Request[AnyContent]]
    new ScriptableRequest(request)
  }
}

// FIXME: Add more members.
class ScriptableRequest(val request: Request[AnyContent]) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "Request"

  @JSGetter
  def getBodyAsText: String = request.body.asText.getOrElse("")

  @JSGetter
  def getBodyAsFormUrlEncoded: Scriptable = {
    val formUrlEncoded: Map[String, Seq[String]] = request.body.asFormUrlEncoded.getOrElse(Map.empty)
    // FIXME: _.head ignores multiple values.
    new ScriptableMap(getParentScope, formUrlEncoded.mapValues(_.head))
  }

  @JSGetter
  def getBodyAsJsonString: String = {
    val jsValue: JsValue = request.body.asJson.getOrElse(JsNull)
    Json.stringify(jsValue)
  }

  @JSGetter
  def getBodyAsJson: AnyRef = {
    val context = Context.getCurrentContext
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    NativeJSON.parse(context, scope, getBodyAsJsonString, new Callable() {
      override def call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: JSArray): AnyRef = args(1)
    })
  }

  @JSGetter
  def getMethod: String = request.method

  @JSGetter
  def getUri: String = request.uri

  @JSGetter
  def getContentType: String = request.contentType.getOrElse("")

  @JSGetter
  def getSecure: Boolean = request.secure

  // FIXME: toSimpleMap ignores multiple values.
  @JSGetter
  def getHeaders: ScriptableMap = new ScriptableMap(getParentScope, request.headers.toSimpleMap)
}

