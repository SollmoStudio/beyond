package beyond.engine.javascript.lib.database

import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject

object ScriptableDocument {
  def jsConstructor(context: Context, args: Array[AnyRef], constructor: Function, inNewExpr: Boolean): ScriptableDocument =
    new ScriptableDocument
}

class ScriptableDocument extends ScriptableObject {
  override val getClassName: String = "Document"
}
