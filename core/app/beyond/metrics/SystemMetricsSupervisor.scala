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
  val writer = context.actorOf(Props[SystemMetricsWriter], SystemMetricsWriter.Name)

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
      val systemLoadAverage = for {
        SystemLoadAverageReply(loadAverage) <- monitor ? SystemLoadAverageRequest
      } yield SystemLoadAverage(hostname, now, loadAverage)
      systemLoadAverage pipeTo writer

      val heapMemoryUsage = for {
        HeapMemoryUsageReply(memoryUsage) <- monitor ? HeapMemoryUsageRequest
      } yield HeapMemoryUsage(hostname, now, memoryUsage)
      heapMemoryUsage pipeTo writer

      val nonHeapMemoryUsage = for {
        NonHeapMemoryUsageReply(nonHeapMemoryUsage) <- monitor ? NonHeapMemoryUsageRequest
      } yield NonHeapMemoryUsage(hostname, now, nonHeapMemoryUsage)
      nonHeapMemoryUsage pipeTo writer

      val swapMemoryUsage = for {
        SwapMemoryUsageReply(swapMemoryFree, swapMemoryTotal, swapMemoryUsed) <- monitor ? SwapMemoryUsageRequest
      } yield SwapMemoryUsage(hostname, now, swapMemoryFree, swapMemoryTotal, swapMemoryUsed)
      swapMemoryUsage pipeTo writer

      val requestsPerSecond = for {
        NumberOfRequestsPerSecondReply(count) <- monitor ? NumberOfRequestsPerSecondRequest
      } yield NumberOfRequestsPerSecond(hostname, now, count)
      requestsPerSecond pipeTo writer
  }
}

