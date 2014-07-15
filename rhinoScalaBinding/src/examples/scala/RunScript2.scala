import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/**
 * RunScript2: Like RunScript, but reflects the System.out into JavaScript.
 */
object RunScript2 extends App {
  val cx: Context = Context.enter
  try {
    val scope: Scriptable = cx.initStandardObjects()

    // Add a global variable "out" that is a JavaScript reflection
    // of System.out
    val jsOut: AnyRef = Context.javaToJS(System.out, scope)
    ScriptableObject.putProperty(scope, "out", jsOut)

    val s = args.mkString

    val result: AnyRef = cx.evaluateString(scope, s, "<cmd>", 1, null)
    System.err.println(Context.toString(result))
  } finally {
    Context.exit()
  }
}
