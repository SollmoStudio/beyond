package beyond.engine.javascript.lib

import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import java.util.UUID
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSStaticFunction => JSStaticFunctionAnnotation }

object ScriptableUUID {
  @JSStaticFunctionAnnotation
  def v4(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String =
    UUID.randomUUID().toString

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableUUID =
    new ScriptableUUID
}

class ScriptableUUID extends ScriptableObject {
  override def getClassName: String = "UUID"
}

