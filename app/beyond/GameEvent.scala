package beyond

import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import play.api.libs.json.JsObject
import play.api.libs.json.Json

trait GameEvent extends Logging {
  def track(tag: String, event: JsObject) {
    // FIXME: Write events to HDFS for analysis.
    logger.info(s"GameEvent ${Json.stringify(event)}")
  }
}

