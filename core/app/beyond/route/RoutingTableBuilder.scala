package beyond.route

import play.api.libs.Codecs
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import scala.collection.mutable

object RoutingTableBuilder {
  type RoutingTableInternal = mutable.HashMap[RouteHash, RouteAddress]

  implicit val routingTableBuilderWrites = new Writes[RoutingTableBuilder] {
    def writes(builder: RoutingTableBuilder): JsValue =
      Json.toJson(builder.routingTable.map { case (hash, address) => RouteHashAndAddress(hash, address) })
  }
}

class RoutingTableBuilder {
  import RoutingTableBuilder._
  private val routingTable: RoutingTableInternal = new RoutingTableInternal

  // This hash result will be used in consistent hashing for load balancing.
  private def hash(address: RouteAddress): RouteHash = {
    // FIXME: Make maximumHash configurable
    val maximumHash = 1000
    // FIXME: Use a better hashing algorithm.
    val startIndex = 24
    val endIndex = 31
    val digits = 16
    // We use the last 7 characters for converting to signed int.
    Integer.parseInt(Codecs.md5(address.getBytes("UTF-8")).substring(startIndex, endIndex), digits) % maximumHash
  }

  def add(address: RouteAddress) {
    routingTable += ((hash(address), address))
  }

  def remove(address: RouteAddress) {
    routingTable - hash(address)
  }
}
