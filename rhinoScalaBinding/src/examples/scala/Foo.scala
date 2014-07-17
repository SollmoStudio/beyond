import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSGetter
import org.mozilla.javascript.annotations.JSFunction

/**
 * An example host object class.
 *
 * Here's a shell session showing the Foo object in action:
 * <pre>
 * js> defineClass("Foo")
 * js> foo = new Foo();         <i>A constructor call, see <a href="#Foo">Foo</a> below.</i>
 * [object Foo]                 <i>The "Foo" here comes from <a href"#getClassName">getClassName</a>.</i>
 * js> foo.counter;             <i>The counter property is defined by the <code>defineProperty</code></i>
 * 0                            <i>call below and implemented by the <a href="#getCounter">getCounter</a></i>
 * js> foo.counter;             <i>method below.</i>
 * 1
 * js> foo.counter;
 * 2
 * js> foo.resetCounter();      <i>Results in a call to <a href="#resetCounter">resetCounter</a>.</i>
 * js> foo.counter;             <i>Now the counter has been reset.</i>
 * 0
 * js> foo.counter;
 * 1
 * js> bar = new Foo(37);       <i>Create a new instance.</i>
 * [object Foo]
 * js> bar.counter;             <i>This instance's counter is distinct from</i>
 * 37
 * js> foo.varargs(3, "hi");    <i>Calls <a href="#varargs">varargs</a>.</i>
 * this = [object Foo]; args = [3, hi]
 * js> foo[7] = 34;             <i>Since we extended ScriptableObject, we get</i>
 * 34                           <i>all the behavior of a JavaScript object</i>
 * js> foo.a = 23;              <i>for free.</i>
 * 23
 * js> foo.a + foo[7];
 * 57
 * js>
 * </pre>
 *
 * @see org.mozilla.javascript.Context
 * @see org.mozilla.javascript.Scriptable
 * @see org.mozilla.javascript.ScriptableObject
 *
 * The Scala primary constructor defining the JavaScript Foo constructor.
 *
 * Takes an initial value for the counter property.
 * Note that in the example Shell session above, we didn't
 * supply a argument to the Foo constructor. This means that
 * the Undefined value is used as the value of the argument,
 * and when the argument is converted to an integer, Undefined
 * becomes 0.
 */
class Foo(counterStart: Int) extends ScriptableObject {
  /**
   * The zero-parameter constructor.
   *
   * When Context.defineClass is called with this class, it will
   * construct Foo.prototype using this constructor.
   */
  def this() = this(0)

  /**
   * Returns the name of this JavaScript class, "Foo".
   */
  override def getClassName: String = "Foo"

  /**
   * The Scala method defining the JavaScript resetCounter function.
   *
   * Resets the counter to 0.
   */
  @JSFunction
  def resetCounter() {
    counter = 0
  }

  /**
   * The Scala method implementing the getter for the counter property.
   * <p>
   * If "setCounter" had been defined in this class, the runtime would
   * call the setter when the property is assigned to.
   */
  @JSGetter
  def getCounter: Int = {
    var currentCounter = counter
    counter += 1
    currentCounter
  }

  private var counter = counterStart
}

object Foo {
  /**
   * An example of a variable-arguments method.
   *
   * All variable arguments methods must have the same number and
   * types of parameters, and must be static. <p>
   * @param cx the Context of the current thread
   * @param thisObj the JavaScript 'this' value.
   * @param args the array of arguments for this call
   * @param funObj the function object of the invoked JavaScript function
   *               This value is useful to compute a scope using
   *               Context.getTopLevelScope().
   * @return computes the string values and types of 'this' and
   * of each of the supplied arguments and returns them in a string.
   *
   * @see org.mozilla.javascript.ScriptableObject#getTopLevelScope
   */
  @JSFunction
  def varargs(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function): AnyRef = {
    var buf: StringBuffer = new StringBuffer
    buf.append("this = ")
    buf.append(Context.toString(thisObj))
    buf.append("; args = [")
    args.dropRight(1).foreach { arg =>
      buf.append(Context.toString(arg))
      buf.append(", ")
    }
    buf.append(Context.toString(args.last))
    buf.append("]")
    buf.toString
  }
}

