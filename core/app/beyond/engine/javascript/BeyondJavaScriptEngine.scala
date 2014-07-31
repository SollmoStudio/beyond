package beyond.engine.javascript

import beyond.BeyondConfiguration
import beyond.engine.javascript.provider.JavaScriptConsoleProvider
import beyond.engine.javascript.provider.JavaScriptTimerProvider
import com.beyondframework.rhino.ScriptableMap
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.commonjs.module.Require
import scala.concurrent.ExecutionContext
import scalax.file.Path

class BeyondJavaScriptEngine(val global: BeyondGlobal = new BeyondGlobal,
    pluginPaths: Seq[Path] = BeyondConfiguration.pluginPaths,
    timer: JavaScriptTimerProvider, console: JavaScriptConsoleProvider)(implicit val executionContext: ExecutionContext) extends Logging {
  import com.beyondframework.rhino.RhinoConversions._

  val contextFactory: BeyondContextFactory = new BeyondContextFactory(new BeyondContextFactoryConfig, global, timer, console)

  private val require: Require = contextFactory.call { cx: Context =>
    global.init(cx)
    ScriptableMap.init(global)

    // Sandboxed means that the require function doesn't have the "paths"
    // property, and also that the modules it loads don't export the
    // "module.uri" property.
    val sandboxed = true
    global.installRequire(cx, pluginPaths.map(_.path), sandboxed)
  }.asInstanceOf[Require]

  def loadMain(filename: String): Scriptable = contextFactory.call { cx: Context =>
    require.requireMain(cx, filename)
  }.asInstanceOf[Scriptable]
}
