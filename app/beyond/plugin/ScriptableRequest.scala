package beyond.plugin

import org.mozilla.javascript.annotations.JSConstructor
import org.mozilla.javascript.annotations.JSGetter
import org.mozilla.javascript.ScriptableObject
import play.api.mvc.Request

// FIXME: Add more members.
// The type of param must be AnyRef.
// Otherwise, Rhino throws an exception when checking parameter types.
@JSConstructor
class ScriptableRequest[A](param: AnyRef) extends ScriptableObject {
  def this() = this(null)

  private val request: Request[A] = param.asInstanceOf[Request[A]]

  override def getClassName: String = "Request"

  @JSGetter
  def getMethod: String = request.method

  @JSGetter
  def getUri: String = request.uri

  // FIXME: Wrap maps as scriptables.
  // FIXME: toSimpleMap ignores multiple values.
  @JSGetter
  def getHeaders: Map[String, String] = request.headers.toSimpleMap
}

