package beyond

import com.typesafe.scalalogging.slf4j.StrictLogging
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import java.io.Closeable

class CuratorConnection(connectionString: String) extends Closeable with StrictLogging {
  private val curatorFramework: CuratorFramework = {
    // FIXME: Make retry count configurable.
    val baseSleepTimeMs = 1000
    val maxRetries = 10000
    val maxSleepMs = 100
    CuratorFrameworkFactory.newClient(connectionString, new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries, maxSleepMs))
  }

  def framework: CuratorFramework = curatorFramework

  def start() {
    logger.info("Curator connection to %s started".format(connectionString))
    curatorFramework.start()
  }

  override def close() {
    curatorFramework.close()
    logger.info("Curator connection to %s closed".format(connectionString))
  }
}
