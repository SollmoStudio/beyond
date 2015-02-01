package beyond.config

import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import java.net.InetAddress
import java.net.NetworkInterface

object ZooKeeperConfiguration extends Logging {
  implicit private val configurationPrefix: String = "beyond.zookeeper"

  def filePath: String = configuration.getString("config-path").get

  def isCurrentMachineInServerList: Boolean = {
    import scala.collection.JavaConversions._

    val localAddresses = (for {
      ni <- NetworkInterface.getNetworkInterfaces
      address <- ni.getInetAddresses
    } yield address).toSet

    logger.info(s"Local addresses $localAddresses")

    val zooKeeperAddresses = servers.map(InetAddress.getByName)
    logger.info(s"ZooKeeper addresses $zooKeeperAddresses")

    (localAddresses & zooKeeperAddresses).nonEmpty
  }

  private def servers: Set[String] =
    configuration.getStringSeq("servers").get.toSet
}
