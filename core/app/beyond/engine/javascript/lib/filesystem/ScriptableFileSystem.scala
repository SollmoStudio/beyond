package beyond.engine.javascript.lib.filesystem

import beyond.engine.javascript.BeyondContext
import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import beyond.engine.javascript.lib.ScriptableFuture
import java.io.File
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSStaticFunction => JSStaticFunctionAnnotation }
import play.api.Configuration
import scala.concurrent.ExecutionContext
import scala.concurrent.future
import scala.sys.process._
import scalax.io.Codec
import scalax.io.Resource

object ScriptableFileSystem {
  private def parseOptions(args: JSArray, idx: Int): Configuration = {
    if (!args.isDefinedAt(idx)) {
      Configuration.empty
    } else {
      val options = args(idx).asInstanceOf[ScriptableObject]
      Configuration.from(options.getIds.map { id =>
        val key = id.asInstanceOf[String]
        (key, options.get(key, options))
      } toMap)
    }
  }

  private def setMode(fileName: String, mode: Int) {
    // FIXME: Doesn't work for Windows
    val modeStr = mode.toOctalString
    s"chmod $modeStr $fileName" !!
  }

  val DefaultEncoding = "UTF-8"
  val DefaultMode = 438 // aka 0666

  @JSStaticFunctionAnnotation
  def readdir(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]

    val path = args(0).asInstanceOf[String]

    val readdirFuture = future {
      val dir = new File(path)
      dir.listFiles
    }

    import com.beyondframework.rhino.RhinoConversions._
    val convertedReaddirFuture = readdirFuture.map { files =>
      beyondContextFactory.call { context: Context =>
        files.map(ScriptableFile(context, _))
      }
    }
    ScriptableFuture(context, convertedReaddirFuture)
  }

  @JSStaticFunctionAnnotation
  def readFile(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext

    val fileName = args(0).asInstanceOf[String]
    val options = parseOptions(args, 1)

    implicit val encoding = options.getString("encoding").getOrElse(DefaultEncoding)

    val readFuture = future {
      val in = Resource.fromFile(fileName)
      in.string
    }
    ScriptableFuture(context, readFuture)
  }

  @JSStaticFunctionAnnotation
  def writeFile(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext

    val fileName = args(0).asInstanceOf[String]
    val data = args(1).asInstanceOf[String]
    val options = parseOptions(args, 2)

    implicit val encoding = options.getString("encoding").getOrElse(DefaultEncoding)
    val mode = options.getInt("mode").getOrElse(DefaultMode)

    val writeFuture = future {
      // FIXME: Loading a file and setting a mode to file should be atomic.
      val out = Resource.fromFile(fileName)
      out.write(data)
      setMode(fileName, mode)
    }
    ScriptableFuture(context, writeFuture)
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableFileSystem =
    new ScriptableFileSystem
}

class ScriptableFileSystem extends ScriptableObject {
  override def getClassName: String = "FileSystem"
}
