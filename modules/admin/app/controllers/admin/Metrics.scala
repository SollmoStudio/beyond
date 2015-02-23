package controllers.admin

import java.lang.management.MemoryUsage
import java.util.Date
import play.api.libs.json.Format
import play.api.libs.json.JsError
import play.api.libs.json.JsResult
import play.api.libs.json.JsResultException
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import play.api.mvc.Result
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.QueryOpts
import scala.concurrent.Future

object Metrics extends Controller with MongoController {
  // FIXME: Share below code with beyondCore.
  // Below code about SystemMetrics are copied from core/app/beyond/metrics/SystemMetricsWriter.scala
  // because I cannot figure out how to share code with another modules.
  sealed trait SystemMetrics
  case class SystemLoadAverage(hostname: String, date: Date, loadAverage: Double) extends SystemMetrics
  case class HeapMemoryUsage(hostname: String, date: Date, memoryUsage: MemoryUsage) extends SystemMetrics
  case class NonHeapMemoryUsage(hostname: String, date: Date, memoryUsage: MemoryUsage) extends SystemMetrics
  case class SwapMemoryUsage(hostname: String, date: Date, free: Long, total: Long, used: Long) extends SystemMetrics
  case class NumberOfRequestsPerSecond(hostname: String, date: Date, numberOfRequests: Int) extends SystemMetrics

  private def metricsCollection: JSONCollection = db.collection[JSONCollection]("admin.metrics")

  private def metrics[T](tag: String, maxNumber: Int)(implicit format: Format[T]): Future[Seq[T]] = {
    import play.api.libs.concurrent.Execution.Implicits._

    metricsCollection
      .find(Json.obj("tag" -> tag))
      .options(QueryOpts(batchSizeN = maxNumber))
      .sort(Json.obj("date" -> -1))
      .cursor[T]
      .collect[Seq](upTo = maxNumber)
  }

  private def metricsWithHeader[T](tag: String, maxNumber: Int)(implicit format: Format[T]): Future[Result] = {
    import play.api.libs.concurrent.Execution.Implicits._

    metrics[T](tag, maxNumber).map { metrics =>
      Ok(Json.toJson(metrics))
        .withHeaders("Cache-Control" -> "no-cache")
    }
  }

  def systemLoadAverage(maxNumber: Int): Action[AnyContent] = AuthenticatedAction.async { request =>
    implicit val format = Json.format[SystemLoadAverage]
    metricsWithHeader[SystemLoadAverage]("SystemLoadAverage", maxNumber)
  }

  def numberOfRequestsPerSecond(maxNumber: Int): Action[AnyContent] = AuthenticatedAction.async { request =>
    implicit val format = Json.format[NumberOfRequestsPerSecond]
    metricsWithHeader[NumberOfRequestsPerSecond]("NumberOfRequestsPerSecond", maxNumber)
  }

  private implicit val memoryUsageFormat: Format[MemoryUsage] = Format[MemoryUsage](
    new Reads[MemoryUsage] {
      override def reads(json: JsValue): JsResult[MemoryUsage] = {
        try {
          val init = (json \ "init").as[Long]
          val used = (json \ "used").as[Long]
          val committed = (json \ "committed").as[Long]
          val max = (json \ "max").as[Long]
          JsSuccess(new MemoryUsage(init, used, committed, max))
        } catch {
          case ex: JsResultException =>
            JsError(ex.errors)
        }
      }
    }, new Writes[MemoryUsage] {
      override def writes(usage: MemoryUsage): JsValue = Json.obj(
        "init" -> usage.getInit,
        "used" -> usage.getUsed,
        "committed" -> usage.getCommitted,
        "max" -> usage.getMax
      )
    }
  )
}
