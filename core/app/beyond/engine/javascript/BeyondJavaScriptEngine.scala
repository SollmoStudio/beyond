package beyond.engine.javascript

import beyond.BeyondConfiguration
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.commonjs.module.Require
import scala.concurrent.ExecutionContext

class BeyondJavaScriptEngine(val global: BeyondGlobal = new BeyondGlobal,
    pluginPaths: Seq[String] = BeyondConfiguration.pluginPaths,
    timer: JavaScriptTimerProvider)(implicit val executionContext: ExecutionContext) extends Logging {
  import com.beyondframework.rhino.RhinoConversions._

  val contextFactory: BeyondContextFactory = new BeyondContextFactory(new BeyondContextFactoryConfig, global, timer)

  private val require: Require = contextFactory.call { cx: Context =>
    global.init(cx)

    // Sandboxed means that the require function doesn't have the "paths"
    // property, and also that the modules it loads don't export the
    // "module.uri" property.
    val sandboxed = true
    global.installRequire(cx, pluginPaths, sandboxed)
  }.asInstanceOf[Require]

  def loadMain(filename: String): Scriptable = contextFactory.call { cx: Context =>
    require.requireMain(cx, filename)
  }.asInstanceOf[Scriptable]
}
