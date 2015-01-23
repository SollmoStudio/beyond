package beyond

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
}
