package beyond.route

import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scala.collection.mutable.HashMap

object RoutingTableBuilder {
  type Address = String
  type Hash = Int

  implicit val serverWrites: Writes[Server] = (
    (__ \ "hash").write[Hash] and
    (__ \ "address").write[Address]
  )(unlift(Server.unapply))

  case class Server(hash: Hash, address: Address)

  type RoutingTableInternal = HashMap[Hash, Address]
}

class RoutingTableBuilder {
  import RoutingTableBuilder._
  private val routingTable: RoutingTableInternal = new RoutingTableInternal

  // This hash result will be used in consistent hashing for load balancing.
  private def hash(address: Address): Hash = {
    // FIXME: Make maximumHash configurable
    val maximumHash = 1000
    // FIXME: Use a better hashing algorithm.
    val startIndex = 24
    val endIndex = 31
    val digits = 16
    // Use the last 7 characters for converting to signed int.
    Integer.parseInt(DigestUtils.md5Hex(address).substring(startIndex, endIndex), digits) % maximumHash
  }

  def add(address: Address) {
    routingTable += ((hash(address), address))
  }

  def remove(address: Address) {
    routingTable - hash(address)
  }

  def serialize(): Array[Byte] = {
    Json.stringify(Json.toJson(routingTable.map { case (hash, address) => Server(hash, address) })).getBytes("UTF-8")
  }
}
