package beyond.tool

import beyond.engine.javascript.BeyondGlobal
import java.io.IOException
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider
import org.mozilla.javascript.tools.ToolErrorReporter
import scala.annotation.switch
import scala.io.Codec
import scala.io.Source

object BeyondShellGlobal {
  private def loadFile(cx: Context, scope: Scriptable, path: String) {
    val src = Source.fromFile(path)(Codec.UTF8).getLines().mkString("\n")
    val script = cx.compileString(src, path, 1, null)
    script.exec(cx, scope)
  }

  def load(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function) {
    args.map(Context.toString).foreach { path =>
      try {
        loadFile(cx, thisObj, path)
      } catch {
        case ioex: IOException =>
          val msg = ToolErrorReporter.getMessage("msg.couldnt.read.source", path, ioex.getMessage)
          throw Context.reportRuntimeError(msg)
        case ex: VirtualMachineError =>
          // Treat StackOverflow and OutOfMemory as runtime errors
          ex.printStackTrace()
          val msg = ToolErrorReporter.getMessage("msg.uncaughtJSException", ex.toString)
          throw Context.reportRuntimeError(msg);
      }
    }
  }

  def quit(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function) {
    val exitCode = (args.length: @switch) match {
      case 0 => 0
      case _ => ScriptRuntime.toInt32(args(0))
    }
    System.exit(exitCode)
  }
}

class BeyondShellGlobal(libraryProvider: ModuleSourceProvider) extends BeyondGlobal(libraryProvider) {
  override def init(cx: Context) {
    super.init(cx)

    val names = Array[String](
      "load",
      "quit"
    )
    defineFunctionProperties(names, classOf[BeyondShellGlobal], ScriptableObject.DONTENUM)
  }
}

