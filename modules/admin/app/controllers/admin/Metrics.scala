package controllers.admin

import java.lang.management.MemoryUsage
import java.util.Date
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller
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

  def systemLoadAverage(maxNumber: Int): Action[AnyContent] = AuthenticatedAction.async { request =>
    import play.api.libs.concurrent.Execution.Implicits._

    implicit val format = Json.format[SystemLoadAverage]
    metrics("SystemLoadAverage", maxNumber).map { metrics =>
      Ok(Json.toJson(metrics))
        .withHeaders("Cache-Control" -> "no-cache")
    }
  }

  def numberOfRequestsPerSecond(maxNumber: Int): Action[AnyContent] = AuthenticatedAction.async { request =>
    import play.api.libs.concurrent.Execution.Implicits._

    implicit val format = Json.format[NumberOfRequestsPerSecond]
    metrics("NumberOfRequestsPerSecond", maxNumber).map { metrics =>
      Ok(Json.toJson(metrics))
        .withHeaders("Cache-Control" -> "no-cache")
    }
  }
}
