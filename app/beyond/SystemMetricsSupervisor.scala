package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

object SystemMetricsSupervisor {
  val Name: String = "systemMetricsSupervisor"
}

class SystemMetricsSupervisor extends Actor with ActorLogging {
  val monitor = context.actorOf(Props[SystemMetricsMonitor], SystemMetricsMonitor.Name)

  override def preStart() {
    log.info("SystemMetricsSupervisor started")
  }

  override def postStop() {
    log.info("SystemMetricsSupervisor stopped")
  }

  override def receive: Receive = {
    case _ =>
  }
}

