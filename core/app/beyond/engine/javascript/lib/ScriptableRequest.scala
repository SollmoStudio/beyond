package beyond.engine.javascript.lib

import com.beyondframework.rhino.ScriptableMap
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSGetter
import play.api.mvc.Request

object ScriptableRequest {
  def jsConstructor(context: Context, args: Array[AnyRef], constructor: Function, inNewExpr: Boolean): ScriptableRequest = {
    var request = args(0).asInstanceOf[Request[_]]
    new ScriptableRequest(request)
  }
}

// FIXME: Add more members.
class ScriptableRequest(val request: Request[_]) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "Request"

  @JSGetter
  def getMethod: String = request.method

  @JSGetter
  def getUri: String = request.uri

  // FIXME: toSimpleMap ignores multiple values.
  @JSGetter
  def getHeaders: ScriptableMap = new ScriptableMap(getParentScope, request.headers.toSimpleMap)
}

