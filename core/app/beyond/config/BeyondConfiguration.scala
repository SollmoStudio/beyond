package beyond.config

import beyond.route.RouteAddress
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.apache.curator.RetryPolicy
import org.apache.curator.retry.ExponentialBackoffRetry
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scalax.file.Path

object BeyondConfiguration extends Logging with ConfigurationMixin {
  lazy val mongo = MongoConfiguration

  def requestTimeout: FiniteDuration =
    Duration(configuration.getString("beyond.request-timeout").get).asInstanceOf[FiniteDuration]

  def zooKeeperConfigPath: String = configuration.getString("beyond.zookeeper.config-path").get

  def pidDirectory: String = configuration.getString("beyond.pid-dir").get

  def zooKeeperServers: Set[String] = {
    import scala.collection.JavaConverters._
    configuration.getStringList("beyond.zookeeper.servers").map(_.asScala).get.toSet
  }

  private val DeprecatedPluginPathsMessage =
    "`beyond.plugin.path` is deprecated. Use `beyond.plugin.paths.`"
  @deprecated(message = DeprecatedPluginPathsMessage)
  def deprecatedPluginPaths: Seq[Path] = {
    logger.warn(DeprecatedPluginPathsMessage)
    configuration.getStringSeq("beyond.plugin.path").getOrElse(Seq.empty).map(Path.fromString)
  }

  def pluginPaths: Map[String, Seq[Path]] =
    configuration.getConfig("beyond.plugin.paths").map { config =>
      config.keys.toSeq.map { pluginName =>
        pluginName -> config.getStringSeq(pluginName).get.map(Path.fromString)
      }.toMap[String, Seq[Path]]
    } getOrElse Map.empty

  def encoding: String = configuration.getString("beyond.encoding").get

  def curatorConnectionPolicy: RetryPolicy = {
    val curatorPath = "beyond.curator.connection"
    val baseSleepTimeMs = Duration(configuration.getString(s"$curatorPath.base-sleep-time").get).toMillis.toInt
    val maxRetries = configuration.getInt(s"$curatorPath.max-retries").get
    val maxSleepMs = Duration(configuration.getString(s"$curatorPath.max-sleep").get).toMillis.toInt
    new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries, maxSleepMs)
  }

  def currentServerAddress: String = configuration.getString("http.address").get

  def currentServerRouteAddress: RouteAddress = {
    val hostAddress = configuration.getString("http.address").get
    val port = configuration.getInt("http.port").get

    RouteAddress(hostAddress, port.toString)
  }

  def isStandaloneMode: Boolean =
    configuration.getBoolean("beyond.standalone-mode").getOrElse(false)

  def enableMetrics: Boolean =
    configuration.getBoolean("beyond.enable-metrics").getOrElse(true)
}
