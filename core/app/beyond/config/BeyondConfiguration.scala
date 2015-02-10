package beyond.config

import beyond.route.RouteAddress
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.apache.curator.RetryPolicy
import org.apache.curator.retry.ExponentialBackoffRetry
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scalax.file.Path

object BeyondConfiguration extends Logging {
  implicit private val configurationPrefix: String = "beyond"

  lazy val mongo = MongoConfiguration

  def requestTimeout: FiniteDuration =
    Duration(configuration.getString("request-timeout").get).asInstanceOf[FiniteDuration]

  def zooKeeperConfigPath: String = configuration.getString("zookeeper.config-path").get

  def pidDirectory: String = configuration.getString("pid-dir").get

  def zooKeeperServers: Set[String] = {
    import scala.collection.JavaConverters._
    configuration.getStringList("zookeeper.servers").map(_.asScala).get.toSet
  }

  private val DeprecatedPluginPathsMessage =
    "`beyond.plugin.path` is deprecated. Use `beyond.plugin.paths.`"
  @deprecated(message = DeprecatedPluginPathsMessage)
  def deprecatedPluginPaths: Seq[Path] = {
    logger.warn(DeprecatedPluginPathsMessage)
    configuration.getStringSeq("plugin.path").getOrElse(Seq.empty).map(Path.fromString)
  }

  def pluginPaths: Map[String, Seq[Path]] =
    configuration.getConfig("plugin.paths").map { config =>
      config.keys.toSeq.map { pluginName =>
        pluginName -> config.getStringSeq(pluginName).get.map(Path.fromString)
      }.toMap[String, Seq[Path]]
    } getOrElse Map.empty

  def encoding: String = configuration.getString("encoding").get

  def curatorConnectionPolicy: RetryPolicy = {
    val curatorPath = "curator.connection"
    val baseSleepTimeMs = Duration(configuration.getString(s"$curatorPath.base-sleep-time").get).toMillis.toInt
    val maxRetries = configuration.getInt(s"$curatorPath.max-retries").get
    val maxSleepMs = Duration(configuration.getString(s"$curatorPath.max-sleep").get).toMillis.toInt
    new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries, maxSleepMs)
  }

  def currentServerAddress: String = rootConfiguration.getString("http.address").get

  def currentServerRouteAddress: RouteAddress = {
    val hostAddress = rootConfiguration.getString("http.address").get
    val port = rootConfiguration.getInt("http.port").get

    RouteAddress(hostAddress, port.toString)
  }

  def isStandaloneMode: Boolean =
    configuration.getBoolean("standalone-mode").getOrElse(false)

  def enableMetrics: Boolean =
    configuration.getBoolean("enable-metrics").getOrElse(true)
}
