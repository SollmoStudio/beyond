package beyond.plugin

import beyond.config.BeyondConfiguration
import beyond.engine.javascript.AssetsModuleSourceProvider
import beyond.engine.javascript.BeyondGlobal
import beyond.engine.javascript.BeyondJavaScriptEngine
import beyond.engine.javascript.lib.ScriptableConsole
import beyond.engine.javascript.lib.ScriptableFuture
import beyond.engine.javascript.lib.http.ScriptableRequest
import beyond.engine.javascript.lib.http.ScriptableResponse
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import play.api.mvc.Request
import play.api.mvc.Result
import scala.concurrent.Future
import scalax.file.Path

class NoHandlerFunctionFoundException extends Exception

object GamePlugin extends Logging {
  import com.beyondframework.rhino.RhinoConversions._
  import scala.concurrent.ExecutionContext.Implicits.global

  private val library: AssetsModuleSourceProvider = new AssetsModuleSourceProvider

  private val engines: Map[String, BeyondJavaScriptEngine] =
    BeyondConfiguration.pluginPaths.map {
      case (prefix: String, paths: Seq[Path]) =>
        prefix -> new BeyondJavaScriptEngine(new BeyondGlobal(library), paths)
    }
  private lazy val defaultEngine: BeyondJavaScriptEngine =
    new BeyondJavaScriptEngine(new BeyondGlobal(library), BeyondConfiguration.deprecatedPluginPaths)

  ScriptableConsole.setRedirectConsoleToLogger(true)

  private def makeHandler(engine: BeyondJavaScriptEngine): Function =
    engine.contextFactory.call { cx: Context =>
      val mainFilename = "main.js"
      val exports = engine.loadMain(mainFilename)
      // FIXME: Don't hardcode the name of handler function.
      exports.get("handle", exports) match {
        case handler: Function =>
          handler
        case _ /* Scriptable.NOT_FOUND */ =>
          logger.error("No handler function is found")
          throw new NoHandlerFunctionFoundException
      }
    }.asInstanceOf[Function]

  private val handlers: Map[String, Function] = engines.map {
    case (prefix: String, engine: BeyondJavaScriptEngine) =>
      prefix -> makeHandler(engine)
  }

  private lazy val defaultHandler: Function =
    makeHandler(defaultEngine)

  private def handle[A](engine: BeyondJavaScriptEngine, handler: Function)(request: Request[A]): Future[Result] =
    engine.contextFactory.call { cx: Context =>
      val scope = engine.global
      val args: Array[AnyRef] = Array(ScriptableRequest(cx, request))
      val response = handler.call(cx, scope, scope, args)
      response match {
        case f: ScriptableFuture =>
          f.future.mapTo[ScriptableResponse].map(_.result)
        case res: ScriptableResponse =>
          Future.successful(res.result)
      }
    }.asInstanceOf[Future[Result]]

  def handle[A](request: Request[A]): Future[Result] = {
    val (pluginName, pluginNameRemovedReq): (String, Request[A]) = {
      val uris = request.uri.split("/").drop(1) // `request.uri` starts with "/".
      val pluginName = uris(1)
      val pluginNameRemovedUri = (uris(0) +: uris.drop(2).toSeq).mkString("/", "/", "")
      pluginName -> Request(request.copy(uri = pluginNameRemovedUri), request.body)
    }

    engines.get(pluginName) match {
      case None =>
        handle(defaultEngine, defaultHandler)(request)
      case Some(engine: BeyondJavaScriptEngine) =>
        handle(engine, handlers(pluginName))(pluginNameRemovedReq)
    }
  }
}

