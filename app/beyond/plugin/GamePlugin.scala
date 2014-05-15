package beyond.plugin

import akka.actor.Actor
import org.mozilla.javascript.commonjs.module.ModuleScope
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import play.api.mvc.Request

object GamePlugin {
  case class Handle[A](request: Request[A])
}

// FIXME: Handle script errors.
class GamePlugin(filename: String) extends Actor {
  import beyond.plugin.RhinoConversions._
  import GamePlugin._

  private val contextFactory: BeyondContextFactory = new BeyondContextFactory(new BeyondContextFactoryConfig)

  private val global: BeyondGlobal = new BeyondGlobal(contextFactory)

  private val (handler: Function, scope: ModuleScope) =  contextFactory.call { cx: Context =>
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
    val handler = exports.get("handle", exports).asInstanceOf[Function]

    // FIXME: Pass the module URI once we load scripts from file path.
    val scope = new ModuleScope(global, null, null)
    (handler, scope)
  }

  private def handle[A](request: Request[A]): String = contextFactory.call { cx: Context =>
    val scriptableRequest: Scriptable = cx.newObject(scope, "Request", Array(request))
    val args: Array[AnyRef] = Array(scriptableRequest)
    handler.call(cx, scope, scope, args)
  }.toString

  override def receive: Receive = {
    case Handle(request) =>
      sender ! handle(request)
  }
}

