package beyond.engine.javascript

import beyond.engine.javascript.provider.JavaScriptConsoleProvider
import beyond.engine.javascript.provider.JavaScriptTimerProvider
import org.mozilla.javascript.Context
import scala.concurrent.ExecutionContext

class BeyondContext(factory: BeyondContextFactory, val timer: JavaScriptTimerProvider, val console: JavaScriptConsoleProvider)(
  implicit val executionContext: ExecutionContext) extends Context(factory)

