package com.beyondframework.rhino

import com.beyondframework.rhino.RhinoConversions._
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Undefined

object ScriptableMap {
  private val ClassName = "ScriptableMap"

  // Set up a custom constructor, for this class is somewhere between a host class and
  // a native wrapper, for which no standard constructor class exists
  def init(scope: Scriptable) {
    val ctor = new BaseFunction(scope, ScriptableObject.getFunctionPrototype(scope)) {
      override def construct(cx: Context, scope: Scriptable, args: Array[AnyRef]): Scriptable = {
        if (args.length > 1) {
          throw new EvaluatorException("ScriptableMap() called with too many arguments")
        }
        new ScriptableMap(scope, if (args.length == 0) null else args(0))
      }
      override def call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array[AnyRef]): AnyRef = {
        construct(cx, scope, args)
      }
    }
    ScriptableObject.defineProperty(scope, ClassName, ctor,
      ScriptableObject.DONTENUM | ScriptableObject.READONLY)
  }

  def objToMap(obj: AnyRef): Map[AnyRef, AnyRef] = {
    unwrapIfWrapped(obj) match {
      case map: Map[_, _] => map.asInstanceOf[Map[AnyRef, AnyRef]]
      case null => Map()
      case Undefined.instance => Map()
      case s: Scriptable =>
        Map(s.getIds.map {
          case id: String => (id, s.get(id, s))
          case id: Number => (id, s.get(id.intValue(), s))
        }: _*)
      case _ => throw new EvaluatorException("Invalid argument to ScriptableMap(): " + obj)
    }
  }
}

class ScriptableMap private (scope: Scriptable,
  obj: AnyRef,
  private val map: Map[AnyRef, AnyRef])
    extends NativeJavaObject(scope, obj, obj.getClass) {

  def this(scope: Scriptable, obj: AnyRef) {
    this(scope, ScriptableMap.objToMap(obj), ScriptableMap.objToMap(obj))
  }

  def this(scope: Scriptable, map: Map[AnyRef, AnyRef]) {
    this(scope, map, map)
  }

  initPrototype(scope)

  protected def initPrototype(scope: Scriptable) {
    val objectProto: Scriptable = ScriptableObject.getClassPrototype(scope, "Object")
    if (objectProto != null) {
      this.setPrototype(objectProto)
    }
  }

  override def get(name: String, start: Scriptable): AnyRef = {
    if (map == null || super.has(name, start)) {
      super.get(name, start)
    } else {
      getInternal(name)
    }
  }

  override def get(index: Int, start: Scriptable): AnyRef = {
    if (map == null) {
      super.get(index, start)
    } else {
      getInternal(new Integer(index))
    }
  }

  private def getInternal(key: AnyRef): AnyRef = {
    val value = map.get(key)
    if (value == null) {
      Scriptable.NOT_FOUND
    } else {
      RhinoConversions.javaToJS(value, getParentScope)
    }
  }

  override def has(name: String, start: Scriptable): Boolean = {
    if (map == null || super.has(name, start)) {
      super.has(name, start)
    } else {
      map.contains(name)
    }
  }

  override def has(index: Int, start: Scriptable): Boolean = {
    if (map == null) {
      super.has(index, start)
    } else {
      map.contains(new Integer(index))
    }
  }

  override def getIds: Array[AnyRef] = {
    if (map == null) {
      super.getIds
    } else {
      map.keySet.toArray
    }
  }

  override def toString: String = {
    if (map == null) {
      super.toString
    } else {
      map.toString()
    }
  }

  override def getDefaultValue(typeHint: Class[_]): AnyRef = toString

  override def unwrap: AnyRef = map

  override def getClassName: String = ScriptableMap.ClassName
}

