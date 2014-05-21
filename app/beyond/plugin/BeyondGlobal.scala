package beyond.plugin

import akka.actor.ActorRef
import akka.actor.Cancellable
import beyond.plugin.GamePlugin.InvokeFunction
import java.io.File
import java.net.URI
import org.mozilla.javascript.commonjs.module.Require
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Function
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.Wrapper
import org.mozilla.javascript.tools.ToolErrorReporter
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

object BeyondGlobal {
  // setTimeout/clearTimeout and setInterval/clearInterval are equivalent to
  // Node.js timers: http://nodejs.org/api/timers.html
  def setTimeout(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function): AnyRef = {
    if (args.length < 2 || !args(0).isInstanceOf[Function]) {
      throw reportRuntimeError("msg.setTimeout.bad.args")
    }

    val callback = args(0).asInstanceOf[Function]
    val callbackArgs = args.drop(2)
    val delay = Context.toNumber(args(1)).millis

    import play.api.Play.current
    implicit val ec: ExecutionContext = Akka.system.dispatcher
    val self: ActorRef = cx.asInstanceOf[BeyondContext].actor
    Akka.system.scheduler.scheduleOnce(delay, self, InvokeFunction(callback, callbackArgs))
  }

  def clearTimeout(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function) {
    if (args.length == 0 || !args(0).isInstanceOf[Cancellable]) {
      throw reportRuntimeError("msg.clearTimeout.bad.args")
    }

    val id = args(0).asInstanceOf[Cancellable]
    id.cancel()
  }

  def setInterval(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function): AnyRef = {
    if (args.length < 2 || !args(0).isInstanceOf[Function]) {
      throw reportRuntimeError("msg.setInterval.bad.args")
    }

    val callback = args(0).asInstanceOf[Function]
    val callbackArgs = args.drop(2)
    val delay = Context.toNumber(args(1)).millis

    import play.api.Play.current
    implicit val ec: ExecutionContext = Akka.system.dispatcher
    val self: ActorRef = cx.asInstanceOf[BeyondContext].actor
    Akka.system.scheduler.schedule(initialDelay = delay, interval = delay, self, InvokeFunction(callback, callbackArgs))
  }

  def clearInterval(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function) {
    if (args.length == 0 || !args(0).isInstanceOf[Cancellable]) {
      throw reportRuntimeError("msg.clearInterval.bad.args")
    }

    val id = args(0).asInstanceOf[Cancellable]
    id.cancel()
  }

  def seal(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function) {
    args.foreach { arg =>
      if (!(arg.isInstanceOf[ScriptableObject]) || arg == Undefined.instance) {
        if (!(arg.isInstanceOf[Scriptable]) || arg == Undefined.instance) {
          throw reportRuntimeError("msg.shell.seal.not.object")
        } else {
          throw reportRuntimeError("msg.shell.seal.not.scriptable")
        }
      }
    }

    args.foreach(_.asInstanceOf[ScriptableObject].sealObject())
  }

  def defineClass(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function) {
    val clazz: Class[_] = getClass(args)
    if (!classOf[Scriptable].isAssignableFrom(clazz)) {
      throw reportRuntimeError("msg.must.implement.Scriptable")
    }
    ScriptableObject.defineClass(thisObj, clazz.asInstanceOf[Class[_ <: Scriptable]])
  }

  private def getClass(args: Array[AnyRef]): Class[_] = {
    def getClassFromClassName(className: String): Class[_] = {
      try {
        Class.forName(className)
      } catch {
        case _: ClassNotFoundException =>
          throw reportRuntimeError("msg.class.not.found", className)
      }
    }

    if (args.length == 0) {
      throw reportRuntimeError("msg.expected.string.arg")
    }

    args(0) match {
      case arg0: Wrapper =>
        val wrapped: AnyRef = arg0.unwrap()
        wrapped match {
          case clazz: Class[_] =>
            clazz
          case _ =>
            val className = Context.toString(args(0))
            getClassFromClassName(className)
        }
      case _ =>
        val className = Context.toString(args(0))
        getClassFromClassName(className)
    }
  }

  private def reportRuntimeError(msgId: String): RuntimeException = {
    val message = ToolErrorReporter.getMessage(msgId)
    Context.reportRuntimeError(message)
  }

  private def reportRuntimeError(msgId: String, msgArg: String): RuntimeException = {
    val message = ToolErrorReporter.getMessage(msgId, msgArg)
    Context.reportRuntimeError(message)
  }
}

class BeyondGlobal(factory: ContextFactory,
    sealedStdLib: Boolean = false) extends ImporterTopLevel {
  import beyond.plugin.RhinoConversions._

  // Define some global functions particular to the beyond. Note
  // that these functions are not part of ECMA.
  factory.call { cx: Context =>
    initStandardObjects(cx, sealedStdLib)
    val names = Array[String](
      "setTimeout",
      "clearTimeout",
      "setInterval",
      "clearInterval",
      "defineClass",
      "seal"
    )
    defineFunctionProperties(names, classOf[BeyondGlobal], ScriptableObject.DONTENUM)
    ScriptableObject.defineClass(this, classOf[ScriptableRequest[_]])
    Unit
  }

  def installRequire(cx: Context, modulePaths: Seq[String], sandboxed: Boolean): Require = {
    import scala.collection.JavaConverters._

    val rb = new RequireBuilder()
    rb.setSandboxed(sandboxed)

    val uris = modulePaths.map { path =>
      val uri: URI = new URI(path)
      if (uri.isAbsolute) {
        uri
      } else {
        // call resolve("") to canonify the path
        new File(path).toURI.resolve("")
      }
    }.map { uri =>
      if (uri.toString.endsWith("/")) {
        uri
      } else {
        // make sure URI always terminates with slash to
        // avoid loading from unintended locations
        new URI(uri + "/")
      }
    }

    rb.setModuleScriptProvider(
      new SoftCachingModuleScriptProvider(
        new UrlModuleSourceProvider(uris.asJava, null)))
    val require: Require = rb.createRequire(cx, this)
    require.install(this)
    require
  }
}

