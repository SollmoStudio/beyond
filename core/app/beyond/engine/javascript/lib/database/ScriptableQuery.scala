package beyond.engine.javascript.lib.database

import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSFunction

object ScriptableQuery {
  // FIXME: cannot find eq method when use JSFunction annotation.
  def jsFunction_eq(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableQuery =
    ???

  @JSFunction
  def neq(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableQuery =
    ???

  @JSFunction
  def lt(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableQuery =
    ???

  @JSFunction
  def lte(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableQuery =
    ???

  @JSFunction
  def gt(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableQuery =
    ???

  @JSFunction
  def gte(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableQuery =
    ???

  @JSFunction
  def where(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableQuery =
    ???

  @JSFunction
  def or(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableQuery =
    ???

  @JSFunction
  def and(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableQuery =
    ???

  def jsConstructor(context: Context, args: Array[AnyRef], constructor: Function, inNewExpr: Boolean): ScriptableQuery =
    new ScriptableQuery
}

class ScriptableQuery extends ScriptableObject {
  override val getClassName: String = "Query"
}
