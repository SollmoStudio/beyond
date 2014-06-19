package beyond.tool

import beyond.plugin.BeyondGlobal
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.ScriptRuntime
import scala.annotation.switch

object BeyondShellGlobal {
  def quit(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function) {
    val exitCode = (args.length: @switch) match {
      case 0 => 0
      case _ => ScriptRuntime.toInt32(args(0))
    }
    System.exit(exitCode)
  }
}

class BeyondShellGlobal extends BeyondGlobal {
  override def init(cx: Context) {
    super.init(cx)

    val names = Array[String](
      "quit"
    )
    defineFunctionProperties(names, classOf[BeyondShellGlobal], ScriptableObject.DONTENUM)
  }
}

