package beyond.engine.javascript.lib.database

import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.ScriptRuntime

object ScriptableSchema {
  private[ScriptableSchema] val InvalidVersion: Int = -1

  // FIXME: Currently, schema only handles type. Add validation and other options.
  def jsConstructor(cx: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableSchema = {
    val version = ScriptRuntime.toInt32(args(0))
    val fieldsObject = args(1).asInstanceOf[ScriptableObject]

    val fields = fieldsObject.getIds.map { key =>
      val fieldName = key.toString
      val fieldType = fieldsObject.get(fieldName).asInstanceOf[ScriptableObject].get("type").toString
      Field(fieldName, fieldType)
    }
    new ScriptableSchema(version, fields)
  }
}

class ScriptableSchema(val version: Int, val fields: Seq[Field]) extends ScriptableObject {
  def this() = this(ScriptableSchema.InvalidVersion, Seq.empty)

  override val getClassName: String = "Schema"
}
