package beyond

import beyond.UserActionActor.RequestWithUsername
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import play.api.libs.json._

// FIXME: Write events to HDFS for analysis.
trait GameEvent extends Logging {
  def track(tag: String, event: JsObject = Json.obj()) {
    logger.info(s"GameEvent ${Json.stringify(event)}")
  }

  def trackUser[A](tag: String, event: JsObject = Json.obj())(implicit request: RequestWithUsername[A]) {
    val eventWithUsername = event + ("username", JsString(request.username))
    this.track(tag, eventWithUsername)
  }
}

