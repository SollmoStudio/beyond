package beyond.config

import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import play.api.Configuration
import scala.util.Properties
import scalax.file.Path
import scalax.io.Codec
import scalax.io.Resource

object ZooKeeperConfiguration extends Logging {
  implicit private val configurationPrefix: String = "beyond.zookeeper"

  private case class ServerConfig(name: String, port1: Int, port2: Int)

  private def isLocalAddress(hostname: String): Boolean = {
    import scala.collection.JavaConversions._

    val localAddresses = (for {
      ni <- NetworkInterface.getNetworkInterfaces
      address <- ni.getInetAddresses
    } yield address).toSet

    localAddresses.contains(InetAddress.getByName(hostname))
  }

  private lazy val servers: Configuration =
    configuration(s"$configurationPrefix.servers")

  private lazy val serverConfigs: Map[Int, ServerConfig] = {
    for {
      key <- servers.subKeys
      name <- servers.getString(s"$key.name")
      port1 <- servers.getInt(s"$key.port1")
      port2 <- servers.getInt(s"$key.port2")
    } yield key.toInt -> ServerConfig(name, port1, port2)
  }.toMap

  private lazy val serverHostnames: Set[String] =
    serverConfigs.values.map(_.name).toSet

  lazy val isReplicatedMode: Boolean =
    serverHostnames.nonEmpty

  lazy val isCurrentMachineInServerList: Boolean =
    serverHostnames.exists(isLocalAddress)

  private val stringValueKeys = Seq("dataDir", "dataLogDir", "clientPortAddress", "peerType")
  private val intValueKeys = Seq("clientPort", "tickTime", "maxClientCnxns", "minSessionTimeout", "maxSessionTimeout",
    "initLimit", "syncLimit", "electionAlg", "autopurge.snapRetainCount", "autopurge.purgeInterval")
  private val booleanValueKeys = Seq("quorumListenOnAllIPs", "syncEnabled")

  private def createMyIdFileIfNeeded() {
    serverConfigs.find {
      case (key, ServerConfig(name, _, _)) =>
        isLocalAddress(name)
    }.headOption.foreach { // Make myid file only for first matched address.
      case (key, ServerConfig(name, _, _)) =>
        configuration.getString("dataDir").foreach { dataDirPath: String =>
          val dataDir = Path.fromString(dataDirPath)
          dataDir.createDirectory(createParents = true, failIfExists = false)
          (dataDir / "myid").write(key.toString)(Codec.UTF8)
        }
    }
  }

  lazy val filePath: String = {
    val temporaryConfigFile = File.createTempFile("beyond-", "-zookeeper.cfg")
    def getOptions[T](keys: Seq[String])(getter: String => Option[T]): Seq[String] = {
      for {
        key <- keys
        value <- getter(key)
      } yield s"$key=$value"
    }

    val stringValueOptions = getOptions(stringValueKeys)(key => configuration.getString(key))
    val intValueOptions = getOptions(intValueKeys)(key => configuration.getInt(key))
    val booleanValueOptions = getOptions(booleanValueKeys)(key => configuration.getInt(key))

    val serverOptions = serverConfigs.map {
      case (key, config) =>
        val name = config.name
        val port1 = config.port1
        val port2 = config.port2
        s"server.$key=$name:$port1:$port2"
    }

    val options = stringValueOptions ++: intValueOptions ++: booleanValueOptions ++: serverOptions

    Resource.fromFile(temporaryConfigFile).writeStrings(options, Properties.lineSeparator)(Codec.UTF8)

    createMyIdFileIfNeeded()

    logger.info(s"ZooKeeper configuration file is created at ${temporaryConfigFile.getCanonicalPath}")

    temporaryConfigFile.getCanonicalPath
  }
}
