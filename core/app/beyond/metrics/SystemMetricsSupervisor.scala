package beyond.metrics

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import beyond.BeyondConfiguration
import beyond.TickGenerator
import java.util.Date
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object SystemMetricsSupervisor {
  val Name: String = "systemMetricsSupervisor"
  implicit val QueryTimeout = Timeout(10.seconds)
}

class SystemMetricsSupervisor extends {
  override protected val initialDelay = 10.seconds
  override protected val tickInterval = 10.seconds
} with Actor with TickGenerator with ActorLogging {

  import SystemMetricsMonitor._
  import SystemMetricsWriter._
  val monitor = context.actorOf(Props[SystemMetricsMonitor], SystemMetricsMonitor.Name)

  override def preStart() {
    log.info("SystemMetricsSupervisor started")
  }

  override def postStop() {
    super.postStop()
    log.info("SystemMetricsSupervisor stopped")
  }

  override def receive: Receive = {
    case TickGenerator.Tick =>
      val hostname = BeyondConfiguration.currentServerAddress
      val now = new Date()

      implicit val ec: ExecutionContext = context.dispatcher
      import SystemMetricsSupervisor.QueryTimeout

      for {
        SystemLoadAverageReply(loadAverage) <- monitor ? SystemLoadAverageRequest
        lastError <- SystemMetricsWriter.save(SystemLoadAverage(hostname, now, loadAverage))
      } yield lastError

      for {
        HeapMemoryUsageReply(memoryUsage) <- monitor ? HeapMemoryUsageRequest
        lastError <- SystemMetricsWriter.save(HeapMemoryUsage(hostname, now, memoryUsage))
      } yield lastError

      for {
        NonHeapMemoryUsageReply(nonHeapMemoryUsage) <- monitor ? NonHeapMemoryUsageRequest
        lastError <- SystemMetricsWriter.save(NonHeapMemoryUsage(hostname, now, nonHeapMemoryUsage))
      } yield lastError

      for {
        SwapMemoryUsageReply(swapMemoryFree, swapMemoryTotal, swapMemoryUsed) <- monitor ? SwapMemoryUsageRequest
        lastError <- SystemMetricsWriter.save(SwapMemoryUsage(hostname, now, swapMemoryFree, swapMemoryTotal, swapMemoryUsed))
      } yield lastError

      for {
        NumberOfRequestsPerSecondReply(count) <- monitor ? NumberOfRequestsPerSecondRequest
        lastError <- SystemMetricsWriter.save(NumberOfRequestsPerSecond(hostname, now, count))
      } yield lastError
  }
}

