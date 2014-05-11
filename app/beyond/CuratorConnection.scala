package beyond

import com.typesafe.scalalogging.slf4j.StrictLogging
import java.io.Closeable
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.RetryPolicy

class CuratorConnection(serversToConnect: String) extends Closeable with StrictLogging {
  val framework: CuratorFramework  = {
    val retryPolicy: RetryPolicy = {
      import play.api.Play.current
      def getFromConfig(name: String, default: Int): Int =
        current.configuration.getInt("beyond.curator.connection." + name).getOrElse(default)
      val defaultBaseSleepTimeMs = 1000
      val defaultMaxRetries = 10
      val defaultMaxSleepMs = 1000
      val baseSleepTimeMs = getFromConfig("base-sleep-time-ms", defaultBaseSleepTimeMs)
      val maxRetries = getFromConfig("max-retries", defaultMaxRetries)
      val maxSleepMs = getFromConfig("max-sleep-ms", defaultMaxSleepMs)
      new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries, maxSleepMs)
    }
    CuratorFrameworkFactory.newClient(serversToConnect, retryPolicy)
  }

  def start() {
    logger.info("Curator connection to %s started".format(serversToConnect))
    framework.start()
  }

  override def close() {
    framework.close()
    logger.info("Curator connection to %s closed".format(serversToConnect))
  }
}
