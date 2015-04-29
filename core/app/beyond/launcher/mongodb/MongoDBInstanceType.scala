package beyond.launcher.mongodb

object MongoDBInstanceType extends Enumeration {
  val Standalone, Config, Routing, Shard, None = Value
  val default = Standalone
}
