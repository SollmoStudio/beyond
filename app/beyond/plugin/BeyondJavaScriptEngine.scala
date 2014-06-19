package beyond.plugin

import beyond.BeyondConfiguration
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

class BeyondJavaScriptEngine extends Logging {
  import com.beyondframework.rhino.RhinoConversions._

  val contextFactory: BeyondContextFactory = new BeyondContextFactory(new BeyondContextFactoryConfig)

  val global: BeyondGlobal = new BeyondGlobal
  contextFactory.call { cx: Context => global.init(cx); Unit }

  def load(filename: String): Scriptable = contextFactory.call { cx: Context =>
    // Sandboxed means that the require function doesn't have the "paths"
    // property, and also that the modules it loads don't export the
    // "module.uri" property.
    val sandboxed = true
    val require = global.installRequire(cx, BeyondConfiguration.pluginPaths, sandboxed)
    require.requireMain(cx, filename)
  }.asInstanceOf[Scriptable]
}
