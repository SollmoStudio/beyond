package beyond.plugin.test

import beyond.engine.javascript.BeyondGlobal
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider

class BeyondTestGlobal(libraryProvider: ModuleSourceProvider) extends BeyondGlobal(libraryProvider) {
  override def init(cx: Context) {
    super.init(cx)
    ScriptableObject.defineClass(this, classOf[TestReporter])
  }
}
