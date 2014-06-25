package beyond.engine.javascript

import org.mozilla.javascript.WrapFactory

object BeyondWrapFactory extends WrapFactory {
  // Make instances of String, Number, Boolean and Character directly available
  // as JavaScript primitive type
  setJavaPrimitiveWrap(false)
}

