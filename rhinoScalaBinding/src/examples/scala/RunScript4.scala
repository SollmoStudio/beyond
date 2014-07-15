import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

object RunScript4 extends App {
  val cx: Context = Context.enter
  try {
    val scope: Scriptable = cx.initStandardObjects()

    // Use the Counter class to define a Counter constructor
    // and prototype in JavaScript.
    ScriptableObject.defineClass(scope, classOf[Counter])

    // Create an instance of Counter and assign it to
    // the top-level variable "myCounter". This is
    // equivalent to the JavaScript code
    //    myCounter = new Counter(7);
    val arg: Array[AnyRef] = Array(new Integer(7))
    val myCounter: Scriptable = cx.newObject(scope, "Counter", arg)
    scope.put("myCounter", scope, myCounter)

    val s = args.mkString
    val result = cx.evaluateString(scope, s, "<cmd>", 1, null)
    System.err.println(Context.toString(result))
  } finally {
    Context.exit()
  }
}
