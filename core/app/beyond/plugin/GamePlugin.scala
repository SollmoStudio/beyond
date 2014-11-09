package beyond.plugin

import beyond.engine.javascript.BeyondJavaScriptEngine
import beyond.engine.javascript.lib.ScriptableConsole
import beyond.engine.javascript.lib.ScriptableFuture
import beyond.engine.javascript.lib.http.ScriptableResponse
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import play.api.mvc.Request
import play.api.mvc.Result
import scala.concurrent.Future

class NoHandlerFunctionFoundException extends Exception

object GamePlugin extends Logging {
  import com.beyondframework.rhino.ContextOps._
  import com.beyondframework.rhino.RhinoConversions._
  import scala.concurrent.ExecutionContext.Implicits.global

  private val engine = new BeyondJavaScriptEngine
  ScriptableConsole.setRedirectConsoleToLogger(true)

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
        f.future.mapTo[ScriptableResponse].map(
          _.asInstanceOf[ScriptableResponse].result)
      case res: ScriptableResponse =>
        Future.successful(res.result)
    }
  }.asInstanceOf[Future[Result]]
}

