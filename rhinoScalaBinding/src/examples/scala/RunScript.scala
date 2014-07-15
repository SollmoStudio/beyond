import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

/**
 * RunScript: simplest example of controlling execution of Rhino.
 *
 * Collects its arguments from the command line, executes the
 * script, and prints the result.
 */
object RunScript extends App {
  // Creates and enters a Context. The Context stores information
  // about the execution environment of a script.
  val cx: Context = Context.enter
  try {
    // Initialize the standard objects (Object, Function, etc.)
    // This must be done before scripts can be executed. Returns
    // a scope object that we use in later calls.
    val scope: Scriptable = cx.initStandardObjects()

    // Collect the arguments into a single string.
    val s = args.mkString

    // Now evaluate the string we've collected.
    val result: AnyRef = cx.evaluateString(scope, s, "<cmd>", 1, null)

    // Convert the result to a string and print it.
    System.err.println(Context.toString(result))
  } finally {
    // Exit from the context
    Context.exit()
  }
}
