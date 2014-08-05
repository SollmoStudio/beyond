package beyond.engine.javascript.lib

import beyond.engine.javascript.BeyondContext
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

object ScriptableConsole {
  def jsStaticFunction_log(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function) {
    context.asInstanceOf[BeyondContext].console.log(args(0).asInstanceOf[String])
  }

  def jsStaticFunction_info(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function) {
    context.asInstanceOf[BeyondContext].console.info(args(0).asInstanceOf[String])
  }

  def jsStaticFunction_warn(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function) {
    context.asInstanceOf[BeyondContext].console.warn(args(0).asInstanceOf[String])
  }

  def jsStaticFunction_debug(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function) {
    context.asInstanceOf[BeyondContext].console.debug(args(0).asInstanceOf[String])
  }

  def jsStaticFunction_error(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function) {
    context.asInstanceOf[BeyondContext].console.error(args(0).asInstanceOf[String])
  }

  def jsConstructor(context: Context, args: Array[AnyRef], constructor: Function, inNewExpr: Boolean): ScriptableConsole =
    new ScriptableConsole
}

class ScriptableConsole extends ScriptableObject {
  override def getClassName: String = "Console"
}
