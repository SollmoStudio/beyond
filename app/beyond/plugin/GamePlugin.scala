package beyond.plugin

import org.mozilla.javascript.commonjs.module.ModuleScope
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextAction
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.tools.shell.Global
import org.mozilla.javascript.tools.shell.QuitAction
import play.api.mvc.Request

trait GamePlugin {
  // FIXME: Make it possible to return the result asynchronously.
  def handle[A](request: Request[A]): String
}

// FIXME: Handle script errors.
object GamePlugin {
  private val contextFactory: BeyondContextFactory = new BeyondContextFactory(new BeyondContextFactoryConfig)

  // FIXME: Need a new class extending org.mozilla.javascript.TopLevel
  // because we don't need all powers of Global.
  private val global: Global = new Global

  // FIXME: We don't want to allow plugins to terminate the system.
  // Delete this once we replace Global with our own global scope.
  global.initQuitAction {
    (_: Context, exitCode: Int) => System.exit(exitCode)
  }

  global.init(contextFactory)
  ScriptableObject.defineClass(global, classOf[ScriptableRequest[_]])

  // FIXME: Move implicit functions for Rhino interoperability to another package.
  private implicit def functionToQuitAction(f: (Context, Int) => Unit): QuitAction = {
    new QuitAction {
      override def quit(cx: Context, exitCode: Int) {
        f(cx, exitCode)
      }
    }
  }

  private implicit def functionToContextAction(f: Context => AnyRef): ContextAction = {
    new ContextAction {
      override def run(cx: Context): AnyRef = {
        f(cx)
      }
    }
  }

  def apply(): GamePlugin = {
    // FIXME: Don't hardcode plugin source code here.
    val source = "function handle(req) { return 'Hello ' + req.uri; }"
    val cx: Context = contextFactory.enterContext()
    try {
      // FIXME: Pass the module URI once we load scripts from file path.
      val scope = new ModuleScope(global, null, null)
      // FIXME: Cache compiled scripts for faster execution later.
      val script = cx.compileString(source, "source", 1, null)
      script.exec(cx, scope)

      // FIXME: Don't hardcode the name of handler function.
      // FIXME: handler might be Scriptable.NOT_FOUND if there is no function named "handle".
      // Also, it might not be an instance of Function.
      val handler = scope.get("handle", scope).asInstanceOf[Function]
      new GamePluginImpl(scope, handler)
    } finally {
      Context.exit()
    }
  }

  private class GamePluginImpl(scope: ModuleScope, handler: Function) extends GamePlugin {
    def handle[A](request: Request[A]): String = {
      val result = contextFactory.call { cx: Context =>
        val scriptableRequest: Scriptable = cx.newObject(scope, "Request", Array(request))
        val args: Array[AnyRef] = Array(scriptableRequest)
        handler.call(cx, scope, scope, args)
      }
      result.toString
    }
  }
}


