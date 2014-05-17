package beyond.plugin

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextAction
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Wrapper
import scala.annotation.tailrec

object RhinoConversions {
  implicit def functionToContextAction(f: Context => AnyRef): ContextAction = {
    new ContextAction {
      override def run(cx: Context): AnyRef = {
        f(cx)
      }
    }
  }

  def javaToJS(obj: AnyRef, scope: Scriptable): AnyRef = {
    obj match {
      case _: Scriptable =>
        obj match {
          case scriptableObj: ScriptableObject =>
            if (scriptableObj.getParentScope == null && scriptableObj.getPrototype == null) {
              ScriptRuntime.setObjectProtoAndParent(scriptableObj, scope)
            }
          case _ => // Ignore
        }
        obj
      case seqObj: Seq[_] => new ScriptableSeq(scope, seqObj.asInstanceOf[Seq[AnyRef]])
      case mapObj: Map[_, _] => new ScriptableMap(scope, mapObj.asInstanceOf[Map[AnyRef, AnyRef]])
      case _ => Context.javaToJS(obj, scope)
    }
  }

  @tailrec
  def jsToJava(obj: AnyRef): AnyRef = {
    if (obj.isInstanceOf[Wrapper]) {
      jsToJava(obj.asInstanceOf[Wrapper].unwrap())
    } else {
      obj
    }
  }

  def unwrapIfWrapped: PartialFunction[AnyRef, AnyRef] = {
    case wrapped: Wrapper => wrapped.unwrap()
    case obj => obj
  }
}

