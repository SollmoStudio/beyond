package beyond.engine.javascript.lib.filesystem

import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import com.beyondframework.rhino.ContextOps._
import java.io.File
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSGetter

object ScriptableFile {
  def apply(context: Context, file: File): ScriptableFile = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global

    context.newObject(scope, "File", file).asInstanceOf[ScriptableFile]
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableFile = {
    val file = args(0) match {
      case file: File =>
        file
      case path: String =>
        new File(path)
      case _ =>
        throw new IllegalArgumentException("type.is.not.matched")
    }
    new ScriptableFile(file)
  }
}

class ScriptableFile(file: File) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "File"

  @JSGetter
  def getName: String = file.getName

  @JSGetter
  def getPath: String = file.getPath

  @JSGetter
  def getIsFile: Boolean = file.isFile

  @JSGetter
  def getIsDirectory: Boolean = file.isDirectory
}
