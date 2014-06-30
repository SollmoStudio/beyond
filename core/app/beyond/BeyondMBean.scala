package beyond

import beyond.metrics.NumberOfRequestsPerSecond
import java.lang.management.ManagementFactory
import javax.management.MBeanServer
import javax.management.ObjectName
import org.hyperic.sigar.jmx.SigarRegistry

object BeyondMBean {
  private case class MBeanRegistry(mbean: AnyRef, name: ObjectName)
  private val mbeans = {
    val sigarRegistry = {
      val sigarMBean = new SigarRegistry
      MBeanRegistry(sigarMBean, new ObjectName(sigarMBean.getObjectName))
    }
    val numberOfRequestsPerSecondRegistry = {
      val numberOfRequestsPerSecondMBean = new NumberOfRequestsPerSecond
      MBeanRegistry(numberOfRequestsPerSecondMBean, NumberOfRequestsPerSecond.objectName)
    }
    Seq(sigarRegistry, numberOfRequestsPerSecondRegistry)
  }

  def register() {
    val server: MBeanServer = ManagementFactory.getPlatformMBeanServer
    mbeans.foreach { registry =>
      server.registerMBean(registry.mbean, registry.name)
    }
  }

  def unregister() {
    val server: MBeanServer = ManagementFactory.getPlatformMBeanServer
    mbeans.foreach { registry =>
      server.unregisterMBean(registry.name)
    }
  }
}
