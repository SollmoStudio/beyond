package beyond.launcher.mongodb

object MongoDBInstanceType extends Enumeration {
  val Standalone, Config, Routing, Shard = Value
  val default = Standalone
}
