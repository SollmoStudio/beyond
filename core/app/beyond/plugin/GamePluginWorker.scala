package beyond.plugin

import akka.actor.Actor
import akka.pattern.pipe
import beyond.engine.javascript.BeyondJavaScriptEngine
import beyond.engine.javascript.lib.ScriptableFuture
import beyond.engine.javascript.lib.ScriptableResponse
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import play.api.mvc.Request
import play.api.mvc.Result
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class GamePluginWorker(engine: BeyondJavaScriptEngine, handler: Function) extends Actor {
  import com.beyondframework.rhino.RhinoConversions._
  import beyond.plugin.GamePlugin._
  implicit val ec: ExecutionContext = context.dispatcher

  private def handle[A](request: Request[A]): Future[Result] = {
    val response = engine.contextFactory.call { cx: Context =>
      val scope = engine.global
      val scriptableRequest: Scriptable = cx.newObject(scope, "Request", Array(request))
      val args: Array[AnyRef] = Array(scriptableRequest)
      handler.call(cx, scope, scope, args)
    }
    response match {
      case scriptableResponse: ScriptableResponse => Future.successful(scriptableResponse.result)
      case scriptableFuture: ScriptableFuture => scriptableFuture.future.mapTo[ScriptableResponse].map(_.result)
    }
  }

  private def invokeFunction(function: Function, args: Array[AnyRef]) {
    engine.contextFactory.call { cx: Context =>
      val scope = engine.global
      function.call(cx, scope, scope, args)
    }
  }

  override def receive: Receive = {
    case Handle(request) =>
      handle(request) pipeTo sender
    case InvokeFunction(function, args) =>
      invokeFunction(function, args)
  }
}

