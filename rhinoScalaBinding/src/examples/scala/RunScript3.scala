import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable

object RunScript3 extends App {
  val cx: Context = Context.enter
  try {
    val scope: Scriptable = cx.initStandardObjects()

    // Collect the arguments into a single string.
    val s = args.mkString

    // Now evaluate the string we've collected. We'll ignore the result.
    cx.evaluateString(scope, s, "<cmd>", 1, null)

    // Print the value of variable "x"
    val x: AnyRef = scope.get("x", scope)
    if (x == Scriptable.NOT_FOUND) {
      System.out.println("x is not defined.")
    } else {
      System.out.println("x = " + Context.toString(x))
    }

    // Call function "f('my arg')" and print its result.
    val fObj: AnyRef = scope.get("f", scope)
    if (!fObj.isInstanceOf[Function]) {
      System.out.println("f is undefined or not a function.")
    } else {
      val functionArgs: Array[AnyRef] = Array("my arg")
      val f: Function = fObj.asInstanceOf[Function]
      val result: AnyRef = f.call(cx, scope, scope, functionArgs)
      val report = "f('my args') = " + Context.toString(result)
      System.out.println(report)
    }
  } finally {
    Context.exit()
  }
}
