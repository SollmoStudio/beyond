package beyond.plugin

import akka.actor.Actor
import akka.actor.Props
import akka.routing.RoundRobinRouter
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import play.api.mvc.Request

object GamePlugin {
  case class Handle[A](request: Request[A])
  case class InvokeFunction(function: Function, args: Array[AnyRef])
}

// FIXME: Handle script errors.
class GamePlugin(filename: String) extends Actor {
  import beyond.plugin.RhinoConversions._

  private val contextFactory: BeyondContextFactory = new BeyondContextFactory(new BeyondContextFactoryConfig)

  private val global: BeyondGlobal = new BeyondGlobal(contextFactory)

  private val handler: Function = contextFactory.call { cx: Context =>
    import scala.collection.JavaConverters._
    import play.api.Play.current

    val defaultModulePaths = Seq("plugins")
    val modulePaths = current.configuration.getStringList("beyond.plugin.path").map(_.asScala).getOrElse(defaultModulePaths)

    // Sandboxed means that the require function doesn't have the "paths"
    // property, and also that the modules it loads don't export the
    // "module.uri" property.
    val sandboxed = true
    val require = global.installRequire(cx, modulePaths, sandboxed)
    val exports = require.requireMain(cx, filename)
    // FIXME: Don't hardcode the name of handler function.
    // FIXME: handler might be Scriptable.NOT_FOUND if there is no function named "handle".
    // Also, it might not be an instance of Function.
    exports.get("handle", exports)
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

