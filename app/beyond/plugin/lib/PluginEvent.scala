package beyond.plugin.lib

import beyond.GameEvent
import com.fasterxml.jackson.core.JsonParseException
import play.api.libs.json.JsNull
import play.api.libs.json.JsObject
import play.api.libs.json.Json

object PluginEvent {
  implicit def stringToJsObject(message: String): JsObject = {
    val emptyJsObject = JsObject(Seq())
    try {
      // We handle here only when message is object or null.
      // In other cases, throw MatchError.
      (Json.parse(message): @unchecked) match {
        case json: JsObject =>
          json
        case JsNull =>
          emptyJsObject
      }
    } catch {
      // Json parser in Play framework cannot parse undefined.
      case ex: JsonParseException if message == "undefined" =>
        emptyJsObject
      case ex: MatchError =>
        Json.toJson(Map("message" -> message)).asInstanceOf[JsObject]
    }
  }
}

class PluginEvent extends GameEvent {
  import PluginEvent._
  def track(tag: String, message: String) {
    super.track(tag, message)
  }
}
