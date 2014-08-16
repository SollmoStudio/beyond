package beyond.engine.javascript.lib.database

import reactivemongo.bson.BSONObjectID

case class ObjectId(bson: BSONObjectID) {
  def this(id: String) = this(BSONObjectID(id))

  override val toString: String = bson.stringify
  def toJSON(key: String): String = s"ObjectID($toString)"
}
