package beyond.config

import beyond.launcher.mongodb.MongoDBInstanceType

object MongoConfiguration extends ConfigurationMixin {
  lazy val instanceType: MongoDBInstanceType.Value =
    configuration.getString("beyond.mongodb.type")
      .map {
        case "standalone" => MongoDBInstanceType.Standalone
        case "config" => MongoDBInstanceType.Config
        case "routing" => MongoDBInstanceType.Routing
        case "shard" => MongoDBInstanceType.Shard
        case _ => throw new IllegalArgumentException("wrong.mongodb.type")
      }
      .getOrElse(MongoDBInstanceType.default)

  lazy val dbPath: String = configuration.getString("beyond.mongodb.dbpath").get

  lazy val configDbPath: String =
    configuration.getString("beyond.mongodb.config.dbpath").get

  lazy val configPort: String =
    configuration.getString("beyond.mongodb.config.port").get
}
