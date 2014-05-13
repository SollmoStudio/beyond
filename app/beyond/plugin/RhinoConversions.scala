package beyond.plugin

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextAction

object RhinoConversions {
  implicit def functionToContextAction(f: Context => AnyRef): ContextAction = {
    new ContextAction {
      override def run(cx: Context): AnyRef = {
        f(cx)
      }
    }
  }
}

