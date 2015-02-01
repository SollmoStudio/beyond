package beyond.config

object ZooKeeperConfiguration {
  implicit private val configurationPrefix: String = "beyond.zookeeper"

  def filePath: String = configuration.getString("config-path").get

  def servers: Set[String] = {
    import scala.collection.JavaConverters._
    configuration.getStringList("servers").map(_.asScala).get.toSet
  }
}
