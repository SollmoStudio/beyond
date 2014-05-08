package beyond

import org.apache.zookeeper.server.ServerConfig
import org.apache.zookeeper.server.ZooKeeperServerMain

class ZooKeeperLauncher {
  // FIXME: Currently, ZooKeeperLauncher launches a standalone ZooKeeper server.
  // Setup a ZooKeeper cluster instead.
  private val zkServer = new ZooKeeperServerMain
  private val config = new ServerConfig

  // FIXME: Don't hardcode the configuration file.
  config.parse("zoo.cfg")

  def launch() {
    // FIXME: Use Akka instead of creating a dedicated thread for ZooKeeper.
    new Thread() {
      override def run() {
        zkServer.runFromConfig(config)
      }
    }.start()
  }
}

