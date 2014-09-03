package beyond.engine.javascript.lib

import beyond.engine.javascript.JSArray
import org.mozilla.javascript.ScriptableObject
import play.api.Configuration

object OptionParser {
  def parse(args: JSArray, idx: Int): Configuration = {
    if (!args.isDefinedAt(idx)) {
      Configuration.empty
    } else {
      val options = args(idx).asInstanceOf[ScriptableObject]
      parse(options)
    }
  }

  def parse(options: ScriptableObject): Configuration = {
    Configuration.from(options.getIds.map { id =>
      val key = id.asInstanceOf[String]
      (key, options.get(key, options))
    } toMap)
  }
}
