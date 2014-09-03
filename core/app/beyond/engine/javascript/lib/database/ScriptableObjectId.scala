package beyond.engine.javascript.lib.database

import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import com.beyondframework.rhino.ContextOps._
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSFunction => JSFunctionAnnotation }
import org.mozilla.javascript.annotations.JSGetter
import reactivemongo.bson.BSONObjectID
import scalaz.syntax.std.boolean._

object ScriptableObjectId {
  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableObjectId = {
    (args.length == 0) ? new ScriptableObjectId() | {
      args(0) match {
        case id: String =>
          new ScriptableObjectId(BSONObjectID(id))
        case id: BSONObjectID =>
          new ScriptableObjectId(id)
        case _ =>
          throw new IllegalArgumentException("ScriptableObjectId needs string or BSONObjectID.")
      }
    }
  }

  @JSFunctionAnnotation
  def toJSON(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String =
    s"ObjectId(${thisObj.asInstanceOf[ScriptableObjectId].stringify})"

  private[database] def apply(context: Context, bson: BSONObjectID): ScriptableObjectId = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    context.newObject(scope, "ObjectId", bson).asInstanceOf[ScriptableObjectId]
  }
}

class ScriptableObjectId(val bson: BSONObjectID) extends ScriptableObject {
  def this() = this(BSONObjectID.generate)
  override def getClassName: String = "ObjectId"

  @JSGetter
  def stringify: String = bson.stringify
}

