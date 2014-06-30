package beyond.metrics

import akka.actor.Actor
import akka.actor.ActorLogging
import javax.management.JMX
import javax.management.MBeanServerConnection
import javax.management.ObjectName

object RequestsCounter {
  case object Increase
  val Name: String = "RequestCounter"
}

class RequestsCounter extends Actor with ActorLogging with JMXConnectorMixin {
  private val numberOfRequestsPerSecond: NumberOfRequestsPerSecondMBean = {
    val mbsc: MBeanServerConnection = jmxConnector.getMBeanServerConnection
    JMX.newMBeanProxy(mbsc, NumberOfRequestsPerSecond.objectName, classOf[NumberOfRequestsPerSecondMBean])
  }

  override def receive: Receive = {
    case RequestsCounter.Increase =>
      numberOfRequestsPerSecond.increase()
  }

  override def postStop() {
    super.postStop()
  }
}
