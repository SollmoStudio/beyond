package beyond.plugin.test

import beyond.engine.javascript.lib.ScriptableFuture
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSStaticFunction
import scala.concurrent.Await
import scala.concurrent.duration._

object TestHelper {
  @JSStaticFunction("wait")
  def jsWait(scriptableFuture: ScriptableFuture): AnyRef =
    Await.result(scriptableFuture.future, 60.second)
}

class TestHelper extends ScriptableObject {
  override def getClassName: String = "TestHelper"
}
