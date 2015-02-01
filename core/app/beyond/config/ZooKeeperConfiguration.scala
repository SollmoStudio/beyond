package beyond.config

object ZooKeeperConfiguration {
  implicit private val configurationPrefix: String = "beyond.zookeeper"

  def filePath: String = configuration.getString("config-path").get

  def servers: Set[String] =
    configuration.getStringSeq("servers").get.toSet
}
