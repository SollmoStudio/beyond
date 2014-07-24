package beyond.engine.javascript.lib.database

import beyond.engine.javascript.lib.ScriptableFuture
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSFunction

object ScriptableCollection {
  @JSFunction
  def insert(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture =
    ???

  @JSFunction
  def find(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture =
    ???

  @JSFunction
  def findOne(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture =
    ???

  @JSFunction
  def remove(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture =
    ???

  @JSFunction
  def removeOne(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture =
    ???

  @JSFunction
  def save(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableDocument =
    ???

  def jsConstructor(context: Context, args: Array[AnyRef], constructor: Function, inNewExpr: Boolean): ScriptableCollection =
    new ScriptableCollection
}

class ScriptableCollection extends ScriptableObject {
  override val getClassName: String = "Collection"
}
