import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSConstructor
import org.mozilla.javascript.annotations.JSFunction
import org.mozilla.javascript.annotations.JSGetter

class Counter @JSConstructor()(val a: Int) extends ScriptableObject {
  private var count = a

  // The zero-argument constructor used by Rhino runtime to create instances
  def this() = this(0)

  override def getClassName: String = "Counter"

  @JSGetter
  def getCount: Int = {
    // count++ in Java
    val currentCount = count
    count += 1
    currentCount
  }

  @JSFunction
  def resetCount() {
    count = 0
  }
}

