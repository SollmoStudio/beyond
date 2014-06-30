package beyond.metrics

import akka.actor.Actor
import beyond.BeyondRuntime
import com.sun.tools.attach.VirtualMachine
import java.io.File
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

object JMXConnectorMixin {
  private[JMXConnectorMixin] trait PostStopCaller {
    def postStop()
  }
}

import JMXConnectorMixin._
trait JMXConnectorMixin extends PostStopCaller { this: Actor =>
  protected val jmxConnector: JMXConnector = {
    val vm = VirtualMachine.attach(BeyondRuntime.processID)
    val ConnectorAddress = "com.sun.management.jmxremote.localConnectorAddress"
    val urlString = Option(vm.getAgentProperties.getProperty(ConnectorAddress)).getOrElse {
      val agent = vm.getSystemProperties.getProperty("java.home") +
        File.separator + "lib" + File.separator + "management-agent.jar"
      vm.loadAgent(agent)

      // agent is started, get the connector address again
      vm.getAgentProperties.getProperty(ConnectorAddress)
    }
    val url = new JMXServiceURL(urlString)
    JMXConnectorFactory.connect(url)
  }

  override abstract def postStop() {
    jmxConnector.close()
    super.postStop()
  }
}
