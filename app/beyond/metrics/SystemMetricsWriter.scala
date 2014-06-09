package beyond.metrics

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.Mongo
import java.lang.management.MemoryUsage
import java.util.Date
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext

private class OWritesOps[A](writes: OWrites[A]) {
  def addTag(tag: String): OWrites[A] =
    (writes ~ (__ \ "tag").write[String])((a: A) => (a, tag))
}

private object OWritesOps {
  implicit def from[A](writes: OWrites[A]): OWritesOps[A] = new OWritesOps(writes)
}

object SystemMetricsWriter {

  import OWritesOps._

  val Name: String = "systemMetricsWriter"

  sealed trait SystemMetrics
  case class SystemLoadAverage(hostname: String, date: Date, loadAverage: Double) extends SystemMetrics
  case class HeapMemoryUsage(hostname: String, date: Date, usage: MemoryUsage) extends SystemMetrics
  case class NonHeapMemoryUsage(hostname: String, date: Date, usage: MemoryUsage) extends SystemMetrics
  case class SwapMemoryUsage(hostname: String, date: Date, free: Long, total: Long, used: Long) extends SystemMetrics

  implicit val memoryUsageWrites: Writes[MemoryUsage] = new Writes[MemoryUsage] {
    override def writes(usage: MemoryUsage): JsValue = Json.obj(
      "init" -> usage.getInit,
      "used" -> usage.getUsed,
      "committed" -> usage.getCommitted,
      "max" -> usage.getMax
    )
  }

  implicit val systemLoadAverageWrites: Writes[SystemLoadAverage] = (
    (JsPath \ "hostname").write[String] and
    (JsPath \ "date").write[Date] and
    (JsPath \ "loadAverage").write[Double]
  )(unlift(SystemLoadAverage.unapply)).addTag("SystemLoadAverage")

  implicit val heapMemoryUsageWrites: Writes[HeapMemoryUsage] = (
    (JsPath \ "hostname").write[String] and
    (JsPath \ "date").write[Date] and
    (JsPath \ "memoryUsage").write[MemoryUsage]
  )(unlift(HeapMemoryUsage.unapply)).addTag("HeapMemoryUsage")

  implicit val nonHeapMemoryUsageWrites: Writes[NonHeapMemoryUsage] = (
    (JsPath \ "hostname").write[String] and
    (JsPath \ "date").write[Date] and
    (JsPath \ "memoryUsage").write[MemoryUsage]
  )(unlift(NonHeapMemoryUsage.unapply)).addTag("NonHeapMemoryUsage")

  implicit val swapMemoryUsageWrites: Writes[SwapMemoryUsage] = (
    (JsPath \ "hostname").write[String] and
    (JsPath \ "date").write[Date] and
    (JsPath \ "free").write[Long] and
    (JsPath \ "total").write[Long] and
    (JsPath \ "used").write[Long]
  )(unlift(SwapMemoryUsage.unapply)).addTag("SwapMemoryUsage")
}

class SystemMetricsWriter extends Actor with ActorLogging with Mongo {

  import SystemMetricsWriter._
  import play.api.Play.current
  import play.api.libs.concurrent.Akka

  implicit val ec: ExecutionContext = Akka.system.dispatcher

  private def collection: JSONCollection = db.collection[JSONCollection]("admin.metrics")

  override def preStart() {
    log.info("SystemMetricsWriter started")
  }

  override def postStop() {
    log.info("SystemMetricsWriter stopped")
  }

  // FIXME: Needs error handling.
  override def receive: Receive = {
    case msg: SystemLoadAverage =>
      collection.save(Json.toJson(msg))
    case msg: HeapMemoryUsage =>
      collection.save(Json.toJson(msg))
    case msg: NonHeapMemoryUsage =>
      collection.save(Json.toJson(msg))
    case msg: SwapMemoryUsage =>
      collection.save(Json.toJson(msg))
  }
}

