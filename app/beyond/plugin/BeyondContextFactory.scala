package beyond.plugin

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import scala.annotation.switch

case class BeyondContextFactoryConfig(strictMode: Boolean = false,
                                      strictVars: Boolean = true,
                                      warningAsError: Boolean = false,
                                      parentProtoProperties: Boolean = true)

class BeyondContextFactory(config: BeyondContextFactoryConfig) extends ContextFactory {
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

  protected override def hasFeature(cx: Context, featureIndex: Int): Boolean = {
    (featureIndex: @switch) match {
      case Context.FEATURE_STRICT_VARS => config.strictVars
      case Context.FEATURE_STRICT_EVAL => config.strictMode
      case Context.FEATURE_STRICT_MODE => config.strictMode
      case Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER => true
      case Context.FEATURE_WARNING_AS_ERROR => config.warningAsError
      case Context.FEATURE_PARENT_PROTO_PROPERTIES => config.parentProtoProperties
      case Context.FEATURE_LOCATION_INFORMATION_IN_ERROR => true
      case _ => super.hasFeature(cx, featureIndex);
    }
  }
}

