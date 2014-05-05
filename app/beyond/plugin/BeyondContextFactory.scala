package beyond.plugin

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory

object BeyondContextFactory extends ContextFactory {
  override def onContextCreated(cx: Context) {
    super.onContextCreated(cx)
    cx.setWrapFactory(BeyondWrapFactory)
  }

  // Strictly speaking, there is no need to override onContextReleased for now
  // because it simply calls its superclass method. However, it is better
  // to leave it as-is so that we won't forget this method when we modify
  // onContextCreated later.
  override def onContextReleased(cx: Context) {
    super.onContextReleased(cx)
  }
}

