package beyond.engine.javascript

import beyond.engine.javascript.provider.JavaScriptConsoleProvider
import org.mozilla.javascript.Context
import scala.concurrent.ExecutionContext

class BeyondContext(factory: BeyondContextFactory, val console: JavaScriptConsoleProvider)(
  implicit val executionContext: ExecutionContext) extends Context(factory)

