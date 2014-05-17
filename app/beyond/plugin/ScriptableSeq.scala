package beyond.plugin

import beyond.plugin.RhinoConversions._
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

object ScriptableSeq {
  private val ClassName = "ScriptableSeq"

  // Set up a custom constructor, for this class is somewhere between a host class and
  // a native wrapper, for which no standard constructor class exists
  def init(scope: Scriptable) {
    val ctor = new BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
      override def construct(cx: Context, scope: Scriptable, args: Array[AnyRef]): Scriptable = {
        if (args.length > 1) {
          throw new EvaluatorException("ScriptableSeq() requires a scala.collection.Seq argument")
        }
        new ScriptableSeq(scope, if (args.length == 0) null else args(0))
      }
      override def call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array[AnyRef]): AnyRef = {
        construct(cx, scope, args)
      }
    }
    ScriptableObject.defineProperty(scope, ClassName, ctor,
      ScriptableObject.DONTENUM | ScriptableObject.READONLY)
  }

  private def objToSeq(obj: AnyRef): Seq[AnyRef] = {
    unwrapIfWrapped(obj) match {
      case seq: Seq[_] => seq.asInstanceOf[Seq[AnyRef]]
      case map: Map[_, _] => map.values.toSeq.asInstanceOf[Seq[AnyRef]]
      case iter: Iterable[_] => iter.toSeq.asInstanceOf[Seq[AnyRef]]
      case null => Seq()
      case Undefined.instance => Seq()
      case _ => throw new EvaluatorException("Invalid argument to ScriptableSeq(): " + obj)
    }
  }
}

class ScriptableSeq private (scope: Scriptable,
  obj: AnyRef,
  private val seq: Seq[AnyRef])
    extends NativeJavaObject(scope, obj, obj.getClass) {

  def this(scope: Scriptable, obj: AnyRef) {
    this(scope, ScriptableSeq.objToSeq(obj), ScriptableSeq.objToSeq(obj))
  }

  def this(scope: Scriptable, seq: Seq[AnyRef]) {
    this(scope, seq, seq)
  }

  initPrototype(scope)

  protected def initPrototype(scope: Scriptable) {
    val arrayProto: Scriptable = ScriptableObject.getClassPrototype(scope, "Array")
    if (arrayProto != null) {
      this.setPrototype(arrayProto)
    }
  }

  override def get(index: Int, start: Scriptable): AnyRef = {
    if (seq == null) {
      super.get(index, start)
    } else {
      try {
        if (index < 0 || index >= seq.length) {
          Undefined.instance
        } else {
          RhinoConversions.javaToJS(seq(index), getParentScope)
        }
      } catch {
        case e: RuntimeException =>
          throw Context.throwAsScriptRuntimeEx(e)
      }
    }
  }

  override def has(index: Int, start: Scriptable): Boolean = {
    if (seq == null) {
      super.has(index, start)
    } else {
      index >= 0 && index < seq.length
    }
  }

  override def get(name: String, start: Scriptable): AnyRef = {
    if ("length".equals(name) && seq != null) {
      new Integer(seq.length)
    } else {
      super.get(name, start)
    }
  }

  override def getIds: Array[AnyRef] = {
    if (seq == null) {
      super.getIds
    } else {
      val ids = 0 until seq.length
      ids.map(Integer.valueOf).toArray
    }
  }

  override def toString: String = {
    if (seq == null) {
      super.toString
    } else {
      seq.toString()
    }
  }

  override def getDefaultValue(typeHint: Class[_]): AnyRef = toString

  override def unwrap: AnyRef = seq

  override def getClassName: String = ScriptableSeq.ClassName
}
