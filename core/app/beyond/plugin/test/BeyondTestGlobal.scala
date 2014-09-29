package beyond.plugin.test

import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import beyond.engine.javascript.BeyondGlobal

class BeyondTestGlobal extends BeyondGlobal {
  override def init(cx: Context) {
    super.init(cx)
    ScriptableObject.defineClass(this, classOf[TestReporter])
  }
}
