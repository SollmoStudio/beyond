package beyond.metrics

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import beyond.BeyondConfiguration
import java.util.Date
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object SystemMetricsSupervisor {
  val Name: String = "systemMetricsSupervisor"

  val TickInterval = 10.seconds
  val InitialDelay = 10.seconds
  implicit val QueryTimeout = Timeout(10.seconds)

  case object Tick
}

class SystemMetricsSupervisor extends Actor with ActorLogging {

  import SystemMetricsMonitor._
  import SystemMetricsSupervisor._
  import SystemMetricsWriter._
  val monitor = context.actorOf(Props[SystemMetricsMonitor], SystemMetricsMonitor.Name)
  val writer = context.actorOf(Props[SystemMetricsWriter], SystemMetricsWriter.Name)

  private implicit val ec: ExecutionContext = context.dispatcher
  private val tickCancellable = context.system.scheduler.schedule(
    initialDelay = InitialDelay, interval = TickInterval, receiver = self, message = Tick)

  override def preStart() {
    log.info("SystemMetricsSupervisor started")
  }

  override def postStop() {
    tickCancellable.cancel()
    log.info("SystemMetricsSupervisor stopped")
  }

  override def receive: Receive = {
    case Tick =>
      val hostname = BeyondConfiguration.currentServerAddress
      val now = new Date()

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
  }
}

