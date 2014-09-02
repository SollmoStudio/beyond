package beyond.plugin

import beyond.engine.javascript.BeyondJavaScriptEngine
import beyond.engine.javascript.lib.ScriptableFuture
import beyond.engine.javascript.lib.http.ScriptableResponse
import beyond.engine.javascript.provider.JavaScriptConsoleProvider
import beyond.engine.javascript.provider.JavaScriptTimerProvider
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import play.api.mvc.Request
import play.api.mvc.Result
import scala.concurrent.Future
import scala.util.Try

class NoHandlerFunctionFoundException extends Exception

object GamePlugin extends JavaScriptTimerProvider with JavaScriptConsoleProvider with Logging {
  import com.beyondframework.rhino.ContextOps._
  import com.beyondframework.rhino.RhinoConversions._
  import scala.concurrent.ExecutionContext.Implicits.global

  private val engine = new BeyondJavaScriptEngine(timer = this, console = this)

  private val handler: Function = engine.contextFactory.call { cx: Context =>
    var mainFilename = "main.js"
    val exports = engine.loadMain(mainFilename)
    // FIXME: Don't hardcode the name of handler function.
    val handler = exports.get("handle", exports)
    handler match {
      case _: Function =>
        handler
      case _ /* Scriptable.NOT_FOUND */ =>
        logger.error("No handler function is found")
        throw new NoHandlerFunctionFoundException
    }
  }.asInstanceOf[Function]

  def handle[A](request: Request[A]): Future[Result] = engine.contextFactory.call { cx: Context =>
    val scope = engine.global

    val scriptableRequest: Scriptable = cx.newObject(scope, "Request", request)
    val args: Array[AnyRef] = Array(scriptableRequest)
    val response = handler.call(cx, scope, scope, args)
    response match {
      case f: ScriptableFuture =>
        f.future.mapTo[ScriptableObject].map(
          ScriptableObject.getProperty(_, "_response").asInstanceOf[ScriptableResponse].result)
      case obj: ScriptableObject =>
        val scriptableResponse = ScriptableObject.getProperty(obj, "_response").asInstanceOf[ScriptableResponse]
        Future.successful(scriptableResponse.result)
    }
  }.asInstanceOf[Future[Result]]

  // JavaScriptConsoleProvider methods
  override def log(message: String): Unit = logger.info(message)
  override def info(message: String): Unit = logger.info(message)
  override def warn(message: String): Unit = logger.warn(message)
  override def debug(message: String): Unit = logger.debug(message)
  override def error(message: String): Unit = logger.error(message)

  // JavaScriptTimerProvider methods
  override def setTimeout(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[AnyRef] = ???
  override def setInterval(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[AnyRef] = ???
  override def clearTimeout(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[Unit] = ???
  override def clearInterval(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[Unit] = ???
}

