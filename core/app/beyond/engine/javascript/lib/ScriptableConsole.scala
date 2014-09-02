package beyond.engine.javascript.lib

import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSStaticFunction => JSStaticFunctionAnnotation }

object ScriptableConsole extends Logging {
  var redirectConsoleToLogger = false

  def setRedirectConsoleToLogger(redirect: Boolean) {
    redirectConsoleToLogger = redirect
  }

  @JSStaticFunctionAnnotation
  def log(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction) {
    var message = args(0).asInstanceOf[String]
    if (redirectConsoleToLogger)
      logger.info(message)
    else
      Console.println(message)
  }

  @JSStaticFunctionAnnotation
  def info(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction) {
    var message = args(0).asInstanceOf[String]
    if (redirectConsoleToLogger)
      logger.info(message)
    else
      Console.println(message)
  }

  @JSStaticFunctionAnnotation
  def warn(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction) {
    var message = args(0).asInstanceOf[String]
    if (redirectConsoleToLogger)
      logger.warn(message)
    else
      Console.println(message)
  }

  @JSStaticFunctionAnnotation
  def debug(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction) {
    var message = args(0).asInstanceOf[String]
    if (redirectConsoleToLogger)
      logger.debug(message)
    else
      Console.println(message)
  }

  @JSStaticFunctionAnnotation
  def error(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction) {
    var message = args(0).asInstanceOf[String]
    if (redirectConsoleToLogger)
      logger.error(message)
    else
      Console.println(message)
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableConsole =
    new ScriptableConsole
}

class ScriptableConsole extends ScriptableObject {
  override def getClassName: String = "Console"
}
