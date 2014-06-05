package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import com.sun.tools.attach.VirtualMachine
import java.io.Closeable
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.MemoryUsage
import java.lang.management.OperatingSystemMXBean
import java.lang.management.RuntimeMXBean
import javax.management.MBeanServerConnection
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import org.hyperic.sigar.jmx.SigarRegistry
import scala.collection.mutable

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

class SystemMetricsMonitor extends Actor with ActorLogging {
  import SystemMetricsMonitor._

  private val jmxResources: mutable.Stack[Closeable] = mutable.Stack()

  override def preStart() {
    def getProcessID: String = {
      val bean: RuntimeMXBean = ManagementFactory.getRuntimeMXBean
      // Get the name representing the running Java virtual machine.
      // It returns something like 6460@AURORA. Where the value
      // before the @ symbol is the PID.
      val jvmName = bean.getName
      // Extract the PID by splitting the string returned by the
      // bean.getName() method.
      jvmName.split("@")(0)
    }

    val server: MBeanServer = ManagementFactory.getPlatformMBeanServer
    server.registerMBean(new SigarRegistry, null)

    try {
      val vm = VirtualMachine.attach(getProcessID)
      val ConnectorAddress = "com.sun.management.jmxremote.localConnectorAddress"
      val urlString = Option(vm.getAgentProperties.getProperty(ConnectorAddress)).getOrElse {
        val agent = vm.getSystemProperties.getProperty("java.home") +
          File.separator + "lib" + File.separator + "management-agent.jar"
        vm.loadAgent(agent)

        // agent is started, get the connector address again
        vm.getAgentProperties.getProperty(ConnectorAddress)
      }

      val url = new JMXServiceURL(urlString)
      val jmxc: JMXConnector = JMXConnectorFactory.connect(url)
      jmxResources.push(jmxc)
      val mbsc: MBeanServerConnection = jmxc.getMBeanServerConnection()

      val osMXBean: OperatingSystemMXBean = ManagementFactory.newPlatformMXBeanProxy(
        mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, classOf[OperatingSystemMXBean])
      val memoryMXBean: MemoryMXBean = ManagementFactory.newPlatformMXBeanProxy(
        mbsc, ManagementFactory.MEMORY_MXBEAN_NAME, classOf[MemoryMXBean])
      val sigarSwap = new ObjectName("sigar:type=Swap")

      context.become(receiveWithMBeans(mbsc, osMXBean, memoryMXBean, sigarSwap))

    } catch {
      case ex: Throwable =>
        closeAllJMXResources()
        throw ex
    }

    log.info("SystemMetricsMonitor started")
  }

  override def postStop() {
    closeAllJMXResources()

    log.info("SystemMetricsMonitor stopped")
  }

  private def closeAllJMXResources() {
    jmxResources.foreach(_.close())
  }

  override def receive: Receive = Map.empty

  private def receiveWithMBeans(mbsc: MBeanServerConnection,
    osMXBean: OperatingSystemMXBean, memoryMXBean: MemoryMXBean, sigarSwap: ObjectName): Receive = {
    case SystemLoadAverageRequest =>
      sender ! SystemLoadAverageReply(osMXBean.getSystemLoadAverage)
    case HeapMemoryUsageRequest =>
      sender ! HeapMemoryUsageReply(memoryMXBean.getHeapMemoryUsage)
    case NonHeapMemoryUsageRequest =>
      sender ! NonHeapMemoryUsageReply(memoryMXBean.getNonHeapMemoryUsage)
    case SwapMemoryUsageRequest =>
      val free = mbsc.getAttribute(sigarSwap, "Free").asInstanceOf[Long]
      val total = mbsc.getAttribute(sigarSwap, "Total").asInstanceOf[Long]
      val used = mbsc.getAttribute(sigarSwap, "Used").asInstanceOf[Long]
      sender ! SwapMemoryUsageReply(free, total, used)
  }
}

