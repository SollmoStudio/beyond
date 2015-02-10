package beyond.config

import beyond.launcher.mongodb.MongoDBInstanceType

object MongoConfiguration {
  implicit private val configurationPrefix: String = "beyond.mongodb"

  lazy val instanceType: MongoDBInstanceType.Value =
    configuration.getString("type")
      .map {
        case "standalone" => MongoDBInstanceType.Standalone
        case "config" => MongoDBInstanceType.Config
        case "routing" => MongoDBInstanceType.Routing
        case "shard" => MongoDBInstanceType.Shard
        case _ => throw new IllegalArgumentException("wrong.mongodb.type")
      }
      .getOrElse(MongoDBInstanceType.default)

  lazy val dbPath: String = configuration.getString("dbpath").get

  lazy val configDbPath: String =
    configuration.getString("config.dbpath").get

  lazy val configPort: String =
    configuration.getString("config.port").get
}
