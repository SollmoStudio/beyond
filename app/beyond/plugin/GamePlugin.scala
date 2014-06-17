package beyond.plugin

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.routing.RoundRobinRouter
import beyond.BeyondConfiguration
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import play.api.mvc.Request

object GamePlugin {
  val Name: String = "gamePlugin"

  case class Handle[A](request: Request[A])
  case class InvokeFunction(function: Function, args: Array[AnyRef])
}

class NoHandlerFunctionFoundException extends Exception

// FIXME: Handle script errors.
class GamePlugin(filename: String) extends Actor with ActorLogging {
  import com.beyondframework.rhino.RhinoConversions._

  private val contextFactory: BeyondContextFactory = new BeyondContextFactory(new BeyondContextFactoryConfig)

  private val global: BeyondGlobal = new BeyondGlobal(contextFactory)

  private val handler: Function = contextFactory.call { cx: Context =>
    // Sandboxed means that the require function doesn't have the "paths"
    // property, and also that the modules it loads don't export the
    // "module.uri" property.
    val sandboxed = true
    val require = global.installRequire(cx, BeyondConfiguration.pluginPaths, sandboxed)
    val exports = require.requireMain(cx, filename)
    // FIXME: Don't hardcode the name of handler function.
    val handler = exports.get("handle", exports)
    handler match {
      case _: Function =>
        handler
      case _ /* Scriptable.NOT_FOUND */ =>
        log.error("No handler function is found")
        throw new NoHandlerFunctionFoundException
    }
  }.asInstanceOf[Function]

  private val workerActor = {
    val numProcessors = Runtime.getRuntime.availableProcessors()
    val router = RoundRobinRouter(nrOfInstances = numProcessors)
    val props = Props(classOf[GamePluginWorker], contextFactory, global, handler).withRouter(router)
    context.actorOf(props, name = "gamePluginWorker")
  }

  override def receive: Receive = {
    case msg => workerActor forward msg
  }
}

