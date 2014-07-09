package beyond.engine.javascript.lib.database

import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject

object ScriptableSchema {
  def jsConstructor(cx: Context, args: Array[AnyRef], constructor: Function, inNewExpr: Boolean): ScriptableSchema =
    new ScriptableSchema
}

class ScriptableSchema extends ScriptableObject {
  override val getClassName: String = "Schema"
}
