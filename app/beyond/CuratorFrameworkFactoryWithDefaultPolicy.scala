package beyond

import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory

object CuratorFrameworkFactoryWithDefaultPolicy extends Logging {
  def apply(serversToConnect: String): CuratorFramework = {
    logger.info("Create a connection to {} .", serversToConnect)
    CuratorFrameworkFactory.newClient(serversToConnect, BeyondConfiguration.curatorConnectionPolicy)
  }
}
