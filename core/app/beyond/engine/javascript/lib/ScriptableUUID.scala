package beyond.engine.javascript.lib

import beyond.engine.javascript.BeyondContext
import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import com.beyondframework.rhino.ContextOps._
import java.util.UUID
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSStaticFunction => JSStaticFunctionAnnotation }

object ScriptableUUID {
  @JSStaticFunctionAnnotation
  def v4(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String =
    UUID.randomUUID().toString

  @JSStaticFunctionAnnotation
  def parse(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableUUID = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global

    val name = args(0).asInstanceOf[String]
    val uuid = UUID.fromString(name)
    context.newObject(scope, "UUID", uuid).asInstanceOf[ScriptableUUID]
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableUUID =
    new ScriptableUUID
}

class ScriptableUUID(val uuid: UUID) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "UUID"
}

