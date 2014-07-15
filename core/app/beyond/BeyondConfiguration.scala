package beyond

import beyond.route.RouteAddress
import org.apache.curator.RetryPolicy
import org.apache.curator.retry.ExponentialBackoffRetry
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scalax.file.Path

object BeyondConfiguration {
  private def configuration = play.api.Play.current.configuration

  def requestTimeout: FiniteDuration =
    Duration(configuration.getString("beyond.request-timeout").get).asInstanceOf[FiniteDuration]

  def mongoDBPath: String = configuration.getString("beyond.mongodb.dbpath").get

  def zooKeeperConfigPath: String = configuration.getString("beyond.zookeeper.config-path").get

  def pidDirectory: String = configuration.getString("beyond.pid-dir").get

  def zooKeeperServers: Set[String] = {
    import scala.collection.JavaConverters._
    configuration.getStringList("beyond.zookeeper.servers").map(_.asScala).get.toSet
  }

  def pluginPaths: Seq[Path] = {
    import scala.collection.JavaConverters._
    val paths: Seq[String] = configuration.getStringList("beyond.plugin.path").map(_.asScala).get
    paths.map(Path.fromString)
  }

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
}
