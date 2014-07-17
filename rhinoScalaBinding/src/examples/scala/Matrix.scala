import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import scala.annotation.tailrec

/**
 * Matrix: An example host object class that implements the Scriptable interface.
 *
 * Built-in JavaScript arrays don't handle multiple dimensions gracefully: the
 * script writer must create every array in an array of arrays. The Matrix class
 * takes care of that by automatically allocating arrays for every index that
 * is accessed. What's more, the Matrix constructor takes a integer argument
 * that specifies the dimension of the Matrix. If m is a Matrix with dimension 3,
 * then m[0] will be a Matrix with dimension 1, and m[0][0] will be an Array.
 *
 * Here's a shell session showing the Matrix object in action:
 * <pre>
 * js> defineClass("Matrix")
 * js> var m = new Matrix(2); // A constructor call, see "Matrix(int dimension)"
 * js> m                      // Object.toString will call "Matrix.getClassName()"
 * [object Matrix]
 * js> m[0][0] = 3;
 * 3
 * js> uneval(m[0]);          // an array was created automatically!
 * [3]
 * js> uneval(m[1]);          // array is created even if we don't set a value
 * []
 * js> m.dim;                 // we can access the "dim" property
 * 2
 * js> m.dim = 3;
 * 3
 * js> m.dim;                 // but not modify the "dim" property
 * 2
 * </pre>
 *
 * @see org.mozilla.javascript.Context
 * @see org.mozilla.javascript.Scriptable
 *
 * The Scala constructor, also used to define the JavaScript constructor.
 */
class Matrix(dimension: Int) extends Scriptable {
  if (dimension <= 0) {
    throw Context.reportRuntimeError("Dimension of Matrix must be greater than zero")
  }

  private val dim: Int = dimension
  private val list: java.util.List[AnyRef] = new java.util.ArrayList[AnyRef]()
  private var prototype: Scriptable = _
  private var parent: Scriptable = _


  /**
   * The zero-parameter constructor.
   *
   * When ScriptableObject.defineClass is called with this class, it will
   * construct Matrix.prototype using this constructor.
   */
  def this() = this(1)

  /**
   * Returns the name of this JavaScript class, "Matrix".
   */
  override def getClassName: String = "Matrix"

  /**
   * Defines the "dim" property by returning true if name is
   * equal to "dim".
   * <p>
   * Defines no other properties, i.e., returns false for
   * all other names.
   *
   * @param name the name of the property
   * @param start the object where lookup began
   */
  override def has(name: String, start: Scriptable): Boolean = name == "dim"

  /**
   * Defines all numeric properties by returning true.
   *
   * @param index the index of the property
   * @param start the object where lookup began
   */
  override def has(index: Int, start: Scriptable): Boolean = true

  /**
   * Get the named property.
   * <p>
   * Handles the "dim" property and returns NOT_FOUND for all
   * other names.
   * @param name the property name
   * @param start the object where the lookup began
   */
  override def get(name: String, start: Scriptable): AnyRef = {
    if (name == "dim") {
      new Integer(dim)
    } else {
      Scriptable.NOT_FOUND
    }
  }

  /**
   * Get the indexed property.
   * <p>
   * Look up the element in the associated list and return
   * it if it exists. If it doesn't exist, create it.<p>
   * @param index the index of the integral property
   * @param start the object where the lookup began
   */
  override def get(index: Int, start: Scriptable): AnyRef = {
    while (index >= list.size()) {
      list.add(null)
    }
    var result = list.get(index)
    if (result == null) {
      if (dim > 2) {
        val m: Matrix = new Matrix(dim - 1)
        m.setParentScope(getParentScope)
        m.setPrototype(getPrototype)
        result = m
      } else {
        val cx: Context = Context.getCurrentContext
        val scope: Scriptable = ScriptableObject.getTopLevelScope(start)
        result = cx.newArray(scope, 0)
      }
      list.set(index, result)
    }
    result
  }

  /**
   * Set a named property.
   *
   * We do nothing here, so all properties are effectively read-only.
   */
  override def put(name: String, start: Scriptable, value: scala.Any) {}

  /**
   * Set an indexed property.
   *
   * We do nothing here, so all properties are effectively read-only.
   */
  override def put(index: Int, start: Scriptable, value: scala.Any) {}

  /**
   * Remove a named property.
   *
   * This method shouldn't even be called since we define all properties
   * as PERMANENT.
   */
  override def delete(name: String) {}

  /**
   * Remove an indexed property.
   *
   * This method shouldn't even be called since we define all properties
   * as PERMANENT.
   */
  override def delete(index: Int) {}

  /**
   * Get prototype.
   */
  override def getPrototype: Scriptable = prototype

  /**
   * Set prototype.
   */
  override def setPrototype(prototype: Scriptable) { this.prototype = prototype }

  /**
   * Get parent.
   */
  override def getParentScope: Scriptable = parent

  /**
   * Set parent.
   */
  override def setParentScope(parent: Scriptable) { this.parent = parent }

  /**
   * Get properties.
   *
   * We return an empty array since we define all properties to be DONTENUM.
   */
  override def getIds: Array[AnyRef] = Array()

  /**
   * Default value.
   *
   * Use the convenience method from Context that takes care of calling
   * toString, etc.
   */
  override def getDefaultValue(hint: Class[_]): AnyRef = "[object Matrix]"

  /**
   * instanceof operator.
   *
   * We mimick the normal JavaScript instanceof semantics, returning
   * true if <code>this</code> appears in <code>value</code>'s prototype
   * chain.
   */
  override def hasInstance(value: Scriptable): Boolean = {
    @tailrec
    def hasInstanceHelper(value: Scriptable): Boolean = {
      val proto: Scriptable = value.getPrototype
      if (proto == null) {
        false
      } else {
        if (proto == this) {
          true
        } else {
          hasInstanceHelper(proto)
        }
      }
    }

    hasInstanceHelper(value)
  }
}
