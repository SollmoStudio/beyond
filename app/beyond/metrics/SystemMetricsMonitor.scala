package beyond.metrics

import akka.actor.Actor
import akka.actor.ActorLogging
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.MemoryUsage
import java.lang.management.OperatingSystemMXBean
import javax.management.MBeanServerConnection
import javax.management.ObjectName

object SystemMetricsMonitor {
  val Name: String = "systemMetricsMonitor"

  sealed trait SystemMetricsRequest
  case object SystemLoadAverageRequest extends SystemMetricsRequest
  case object HeapMemoryUsageRequest extends SystemMetricsRequest
  case object NonHeapMemoryUsageRequest extends SystemMetricsRequest
  case object SwapMemoryUsageRequest extends SystemMetricsRequest

  sealed trait SystemMetricsReply
  case class SystemLoadAverageReply(loadAverage: Double) extends SystemMetricsReply
  case class HeapMemoryUsageReply(usage: MemoryUsage) extends SystemMetricsReply
  case class NonHeapMemoryUsageReply(usage: MemoryUsage) extends SystemMetricsReply
  case class SwapMemoryUsageReply(free: Long, total: Long, used: Long) extends SystemMetricsReply
}

class SystemMetricsMonitor extends Actor with ActorLogging with JMXConnectorMixin {
  import SystemMetricsMonitor._

  private val mbsc: MBeanServerConnection = jmxConnector.getMBeanServerConnection
  private val osMXBean: OperatingSystemMXBean = ManagementFactory.newPlatformMXBeanProxy(
    mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, classOf[OperatingSystemMXBean])
  private val memoryMXBean: MemoryMXBean = ManagementFactory.newPlatformMXBeanProxy(
    mbsc, ManagementFactory.MEMORY_MXBEAN_NAME, classOf[MemoryMXBean])

  override def preStart() {
    log.info("SystemMetricsMonitor started")
  }

  override def postStop() {
    super.postStop()
    log.info("SystemMetricsMonitor stopped")
  }

  override def receive: Receive = {
    case SystemLoadAverageRequest =>
      sender ! SystemLoadAverageReply(osMXBean.getSystemLoadAverage)
    case HeapMemoryUsageRequest =>
      sender ! HeapMemoryUsageReply(memoryMXBean.getHeapMemoryUsage)
    case NonHeapMemoryUsageRequest =>
      sender ! NonHeapMemoryUsageReply(memoryMXBean.getNonHeapMemoryUsage)
    case SwapMemoryUsageRequest =>
      val sigarSwap = new ObjectName("sigar:type=Swap")
      val free = mbsc.getAttribute(sigarSwap, "Free").asInstanceOf[Long]
      val total = mbsc.getAttribute(sigarSwap, "Total").asInstanceOf[Long]
      val used = mbsc.getAttribute(sigarSwap, "Used").asInstanceOf[Long]
      sender ! SwapMemoryUsageReply(free, total, used)
  }
}

