package beyond.route

import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import play.api.libs.json.JsArray
import play.api.libs.json.Json

object RoutingTableView {
  type RoutingTableInternal = Seq[Server]
  def emptyRoutingTable: RoutingTableInternal = Seq[Server]()

  sealed trait ServerToHandle
  case object HandleHere extends ServerToHandle
  case class HandleIn(address: Address) extends ServerToHandle
}

class RoutingTableView(currentServer: Address, data: JsArray = new JsArray) extends Logging {
  import RoutingTableView._

  private val routingTable: RoutingTableInternal = Json.fromJson[RoutingTableInternal](data).get.sorted(new Ordering[Server] {
    override def compare(lhs: Server, rhs: Server): Int = lhs.hash compare rhs.hash
  })

  // Currently, time complexity of this method is O(n) when n = number of
  // servers in the routing table. I think O(n) is not bad because the number
  // of server will not be large. But this method is called for every
  // RequestWithUsername, so it can be a bottleneck. If you can reduce
  // time complexity, please fix it.
  def queryServerToHandle(hash: Hash): ServerToHandle = {
    // When there are three servers, and hash of server1 is 100, server2 is 300 and server3 is 700,
    // server1 will handle requests whose hash is less than or equal to 100 and more than 700.
    // server2 will handle requests whose hash is in (100, 300].
    // server3 will handle requests whose hash is in (300, 700].
    def whoWillHandle(hash: Hash): Address = {
      // Basically, find the first server that the hash of server is less than
      // or equal to the hash of the request.
      //
      //  There are two exceptions:
      //   1. When the server topology is like above and the hash of the
      //      request is below 100. This request must be handled in the first
      //      server.
      //   2. When the routing table is empty,
      //      Actually, this is an abnormal case. There might be a routing table
      //      synchronization failure for some reason. In this case, we handle
      //      requests in the current server. This is okay because each server
      //      doesn't have internal states, and consistent hashing is used just
      //      for load balancing.
      val fallbackAddress: Address = routingTable.headOption.fold({
        logger.warn("There is no data in the routing table.")
        currentServer
      })(_.address)
      routingTable.find(_.hash >= hash).fold(fallbackAddress)(_.address)
    }

    whoWillHandle(hash) match {
      case address: Address if address != currentServer => HandleIn(address)
      case _ => HandleHere
    }
  }
}
