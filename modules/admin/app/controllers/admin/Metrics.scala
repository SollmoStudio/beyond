package controllers.admin

import java.lang.management.MemoryUsage
import java.util.Date
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController

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
}
