package beyond.engine.javascript

import beyond.engine.javascript.lib.ScriptableBuffer
import beyond.engine.javascript.lib.ScriptableConsole
import beyond.engine.javascript.lib.ScriptableFuture
import beyond.engine.javascript.lib.ScriptableUUID
import beyond.engine.javascript.lib.crypto.ScriptableCrypto
import beyond.engine.javascript.lib.database.ScriptableCollection
import beyond.engine.javascript.lib.database.ScriptableDocument
import beyond.engine.javascript.lib.database.ScriptableObjectId
import beyond.engine.javascript.lib.database.ScriptableQuery
import beyond.engine.javascript.lib.database.ScriptableSchema
import beyond.engine.javascript.lib.filesystem.ScriptableFile
import beyond.engine.javascript.lib.filesystem.ScriptableFileSystem
import beyond.engine.javascript.lib.filesystem.ScriptablePath
import beyond.engine.javascript.lib.http.ScriptableHttpClient
import beyond.engine.javascript.lib.http.ScriptableHttpResult
import beyond.engine.javascript.lib.http.ScriptableRequest
import beyond.engine.javascript.lib.http.ScriptableResponse
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.Wrapper
import org.mozilla.javascript.commonjs.module.Require
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider
import org.mozilla.javascript.tools.ToolErrorReporter
import scalaz.syntax.std.boolean._

object BeyondGlobal {
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

class BeyondGlobal extends ImporterTopLevel {

  def init(cx: Context) {
    // Define some global functions particular to the beyond. Note
    // that these functions are not part of ECMA.
    val sealedStdLib = true
    initStandardObjects(cx, sealedStdLib)
    val names = Array[String](
      "defineClass",
      "seal"
    )
    defineFunctionProperties(names, classOf[BeyondGlobal], ScriptableObject.DONTENUM)

    val scriptableClasses = Array(
      classOf[ScriptableBuffer],
      classOf[ScriptableCollection],
      classOf[ScriptableConsole],
      classOf[ScriptableCrypto],
      classOf[ScriptableDocument],
      classOf[ScriptableFileSystem],
      classOf[ScriptableFile],
      classOf[ScriptableFuture],
      classOf[ScriptableHttpClient],
      classOf[ScriptableHttpResult],
      classOf[ScriptableObjectId],
      classOf[ScriptablePath],
      classOf[ScriptableQuery],
      classOf[ScriptableRequest],
      classOf[ScriptableResponse],
      classOf[ScriptableSchema],
      classOf[ScriptableUUID]
    )
    scriptableClasses.foreach(ScriptableObject.defineClass(this, _))

    val dirname = FileSystems.getDefault.getPath("").toAbsolutePath.toString
    defineProperty("__dirname", dirname, ScriptableObject.DONTENUM | ScriptableObject.READONLY)
  }

  def installRequire(cx: Context, modulePaths: Seq[String], sandboxed: Boolean): Require = {
    import scala.collection.JavaConverters._

    val rb = new RequireBuilder()
    rb.setSandboxed(sandboxed)

    val uris = modulePaths.map { path =>
      val uri: URI = new URI(path)
      // call resolve("") to canonify the path
      uri.isAbsolute ? uri | new File(path).toURI.resolve("")
    }.map { uri =>
      // make sure URI always terminates with slash to
      // avoid loading from unintended locations
      uri.toString.endsWith("/") ? uri | new URI(uri + "/")
    }

    rb.setModuleScriptProvider(
      new SoftCachingModuleScriptProvider(
        new UrlModuleSourceProvider(uris.asJava, null)))
    val require: Require = rb.createRequire(cx, this)
    require.install(this)
    require
  }
}

