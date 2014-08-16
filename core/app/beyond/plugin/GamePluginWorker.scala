package beyond.plugin

import akka.actor.Actor
import akka.pattern.pipe
import beyond.engine.javascript.BeyondJavaScriptEngine
import beyond.engine.javascript.lib.ScriptableFuture
import beyond.engine.javascript.lib.ScriptableResponse
import com.beyondframework.rhino.ContextOps._
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import play.api.mvc.Request
import play.api.mvc.Result
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class GamePluginWorker(engine: BeyondJavaScriptEngine, handler: Function) extends Actor {
  import com.beyondframework.rhino.RhinoConversions._
  import beyond.plugin.GamePlugin._
  implicit val ec: ExecutionContext = context.dispatcher

  private def handle[A](request: Request[A]): Future[Result] = engine.contextFactory.call { cx: Context =>
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

