package beyond.metrics

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.MongoMixin
import java.lang.management.MemoryUsage
import java.util.Date
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.LastError
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

private class OWritesOps[A](writes: OWrites[A]) {
  def addTag(tag: String): OWrites[A] =
    (writes ~ (__ \ "tag").write[String])((a: A) => (a, tag))
}

private object OWritesOps {
  implicit def from[A](writes: OWrites[A]): OWritesOps[A] = new OWritesOps(writes)
}

object SystemMetricsWriter extends MongoMixin {

  import OWritesOps._

  val Name: String = "systemMetricsWriter"

  sealed trait SystemMetrics
  case class SystemLoadAverage(hostname: String, date: Date, loadAverage: Double) extends SystemMetrics
  case class HeapMemoryUsage(hostname: String, date: Date, memoryUsage: MemoryUsage) extends SystemMetrics
  case class NonHeapMemoryUsage(hostname: String, date: Date, memoryUsage: MemoryUsage) extends SystemMetrics
  case class SwapMemoryUsage(hostname: String, date: Date, free: Long, total: Long, used: Long) extends SystemMetrics
  case class NumberOfRequestsPerSecond(hostname: String, date: Date, numberOfRequests: Int) extends SystemMetrics

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

  implicit val numberOfRequestsPerSecondWrites: Writes[NumberOfRequestsPerSecond] = (
    (JsPath \ "hostname").write[String] and
    (JsPath \ "date").write[Date] and
    (JsPath \ "numberOfRequests").write[Int]
  )(unlift(NumberOfRequestsPerSecond.unapply)).addTag("NumberOfRequestsPerSecond")

  private def collection: JSONCollection =
    db.collection[JSONCollection]("admin.metrics")

  def save(metrics: SystemLoadAverage)(implicit ec: ExecutionContext): Future[LastError] =
    collection.save(Json.toJson(metrics))
  def save(metrics: HeapMemoryUsage)(implicit ec: ExecutionContext): Future[LastError] =
    collection.save(Json.toJson(metrics))
  def save(metrics: NonHeapMemoryUsage)(implicit ec: ExecutionContext): Future[LastError] =
    collection.save(Json.toJson(metrics))
  def save(metrics: SwapMemoryUsage)(implicit ec: ExecutionContext): Future[LastError] =
    collection.save(Json.toJson(metrics))
  def save(metrics: NumberOfRequestsPerSecond)(implicit ec: ExecutionContext): Future[LastError] =
    collection.save(Json.toJson(metrics))
}
