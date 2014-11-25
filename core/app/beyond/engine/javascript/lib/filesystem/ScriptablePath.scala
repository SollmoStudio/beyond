package beyond.engine.javascript.lib.filesystem

import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import java.io.File
import java.nio.file.FileSystems
import org.mozilla.javascript.Context
import org.mozilla.javascript.FunctionObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSStaticFunction => JSStaticFunctionAnnotation }
import scalaz.syntax.std.boolean._

object ScriptablePath {
  private val defaultFileSystem = FileSystems.getDefault

  @JSStaticFunctionAnnotation
  def normalize(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val path = args(0).asInstanceOf[String]

    defaultFileSystem.getPath(path).normalize.toString
  }

  @JSStaticFunctionAnnotation
  def join(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val paths = args.map(_.asInstanceOf[String])

    defaultFileSystem.getPath(paths(0), paths.slice(1, paths.length): _*).normalize.toString
  }

  @JSStaticFunctionAnnotation
  def resolve(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val paths = args.map(_.asInstanceOf[String])

    val from = defaultFileSystem.getPath(paths(0))
    val toSeq = paths.slice(1, paths.length).toSeq

    toSeq.foldLeft(from) { (from, to) => from.resolve(to) }.normalize.toAbsolutePath.toString
  }

  @JSStaticFunctionAnnotation
  def relative(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val from = defaultFileSystem.getPath(args(0).asInstanceOf[String])
    val to = defaultFileSystem.getPath(args(1).asInstanceOf[String])

    from.relativize(to).toString
  }

  @JSStaticFunctionAnnotation
  def dirname(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val path = args(0).asInstanceOf[String]

    try {
      defaultFileSystem.getPath(path).getParent.toString
    } catch {
      case _: NullPointerException => "." // When there' no parent directory, return '.', which is the same with Node.js
    }
  }

  @JSStaticFunctionAnnotation
  def basename(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val path = args(0).asInstanceOf[String]

    val filename = try {
      defaultFileSystem.getPath(path).getFileName.toString
    } catch {
      case _: NullPointerException => "" // When there' no basename, return '', which is the same with Node.js
    }

    if (args.isDefinedAt(1)) {
      val ext = args(1).asInstanceOf[String]
      filename.endsWith(ext) ? filename.dropRight(ext.length) | filename
    } else {
      filename
    }
  }

  @JSStaticFunctionAnnotation
  def extname(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val path = args(0).asInstanceOf[String]

    val extensionIndex = path.lastIndexOf('.')
    val sepIndex = path.lastIndexOf(File.separator)

    if (extensionIndex > sepIndex) {
      path.substring(extensionIndex)
    } else {
      ""
    }
  }

  def finishInit(scope: Scriptable, ctor: FunctionObject, proto: Scriptable) {
    ctor.defineProperty("sep", File.separator, ScriptableObject.READONLY)
    ctor.defineProperty("delimiter", File.pathSeparator, ScriptableObject.READONLY)
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptablePath =
    new ScriptablePath
}

class ScriptablePath extends ScriptableObject {
  override def getClassName: String = "Path"
}
