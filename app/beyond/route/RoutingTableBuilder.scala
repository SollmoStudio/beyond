package beyond.route

import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.json.Json
import scala.collection.mutable.HashMap

object RoutingTableBuilder {
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
