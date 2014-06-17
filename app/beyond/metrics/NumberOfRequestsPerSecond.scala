package beyond.metrics

import java.util.concurrent.atomic.AtomicInteger
import javax.management.AttributeChangeNotification
import javax.management.Notification
import javax.management.NotificationBroadcasterSupport
import javax.management.ObjectName

import play.api.libs.concurrent.Akka
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object NumberOfRequestsPerSecond {
  val ObjectName: ObjectName = new ObjectName("beyond.metrics:type=NumberOfRequestsPerSecond")
}

class NumberOfRequestsPerSecond extends NotificationBroadcasterSupport with NumberOfRequestsPerSecondMBean {
  private val backCount: AtomicInteger = new AtomicInteger(0)
  @volatile private var frontCount: Int = 0
  private var sequence: Long = 0

  import ExecutionContext.Implicits.global
  import play.api.Play.current

  Akka.system.scheduler.schedule(1.second, 1.second) {
    val oldCount = frontCount
    frontCount = backCount.getAndSet(0)
    sequence += 1
    val notification: Notification = new AttributeChangeNotification(
      this,
      sequence,
      System.currentTimeMillis(),
      "NumberOfRequestsPerSecond updated",
      "NumberOfRequestsPerSecond",
      "int",
      oldCount,
      frontCount)
    sendNotification(notification)
  }

  override def getNumberOfRequestsPerSecond: Int = frontCount

  override def increase() {
    backCount.incrementAndGet
  }
}
