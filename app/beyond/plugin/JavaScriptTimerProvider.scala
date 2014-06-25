package beyond.plugin

import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import scala.util.Failure
import scala.util.Try

trait JavaScriptTimerProvider {
  def setTimeout(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[AnyRef]
  def setInterval(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[AnyRef]
  def clearTimeout(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[Unit]
  def clearInterval(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[Unit]
}
