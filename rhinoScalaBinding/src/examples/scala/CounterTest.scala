import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/**
 * An example illustrating how to create a JavaScript object and retrieve
 * properties and call methods.
 * <p>
 * Output should be:
 * <pre>
 * count = 0
 * count = 1
 * resetCount
 * count = 0
 * </pre>
 */
object CounterTest extends App {
  val cx: Context = Context.enter
  try {
    val scope: Scriptable = cx.initStandardObjects()
    ScriptableObject.defineClass(scope, classOf[Counter])

    val testCounter: Scriptable = cx.newObject(scope, "Counter")

    var count: AnyRef = ScriptableObject.getProperty(testCounter, "count")
    System.out.println("count = " + count)

    count = ScriptableObject.getProperty(testCounter, "count")
    System.out.println("count = " + count)

    ScriptableObject.callMethod(testCounter, "resetCount", Array())
    System.out.println("resetCount")

    count = ScriptableObject.getProperty(testCounter, "count")
    System.out.println("count = " + count)
  } finally {
    Context.exit()
  }
}
