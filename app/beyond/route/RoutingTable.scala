package beyond.route

import com.typesafe.scalalogging.slf4j.StrictLogging
import java.util.NoSuchElementException
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.functional.syntax._
import play.api.libs.json._

object RoutingTable {
  type Address = String
  type Hash = Int

  implicit val writesServer: Writes[Server] =(
    (__ \ "hash").write[Hash] and
      (__ \ "address").write[Address]
    )(unlift(Server.unapply))

  implicit val readsServer: Reads[Server] = (
    (__ \ "hash").read[Hash] and
      (__ \ "address").read[Address]
  )(Server)

  implicit val locationFormat: Format[Server] =
    Format(readsServer, writesServer)

  case class Server(hash: Hash, address: Address)

  type RoutingTable = Seq[Server]
  val EmptyRoutingTable = Seq[Server]()
}

import RoutingTable._

object MutableRoutingTable {
  implicit object mutableRoutingTableWrites extends Writes[MutableRoutingTable]{
    implicit  override def writes(mutableRoutingTable: MutableRoutingTable): JsValue = {
      Json.toJson(mutableRoutingTable.routingTable)
    }
  }
}

private[route] object RoutingTableBuilder {
  def apply(data: Option[JsArray] = None): RoutingTable = data.fold(Seq.empty[Server]) { data =>
    Json.fromJson[RoutingTable](data).get
  }
}
class MutableRoutingTable(data: Option[JsArray] = None) {
  private var routingTable: RoutingTable = RoutingTableBuilder(data)

  def add(address: Address) {
    // This hash result will be used in consistent hashing for load balancing.
    def hash(address: Address): Hash = {
      // FIXME: Make maximumHash configurable
      val maximumHash = 1000
      // FIXME: Use a better hashing algorithm.
      val startIndex = 25
      val endIndex = 31
      val digits = 16
      Integer.parseInt(DigestUtils.md5Hex(address).substring(startIndex, endIndex), digits) % maximumHash
    }

    routingTable = routingTable :+ Server(hash(address), address)
  }

  def remove(address: Address) {
    routingTable = routingTable.filter { _.address != address }
  }
}

object RoutingTableView {
  trait ServerToHandle
  case object HandleHere extends ServerToHandle
  case class HandleIn(address: Address) extends ServerToHandle
}

class RoutingTableView(currentServer: Address, data: Option[JsArray] = None) extends StrictLogging {
  import beyond.route.RoutingTableView._
  // queryServerToHandle method needs sorted routing table for performance.
  // Sorting in constructing, because queryServerToHandle is called more frequently than Constructing RoutingTable.
  // And routing table have to be volatile because replace method and queryServerToHandle method can be called in different threads.
  @volatile private var routingTable: RoutingTable = RoutingTableBuilder(data).sorted(new Ordering[Server] {
    override def compare(lhs: Server, rhs: Server): Int = lhs.hash compare rhs.hash
  })

  def replace(newTable: RoutingTableView) {
    routingTable = newTable.routingTable
  }

  // Currently, time complexity of this method is O(n) when n = number of servers in routing table.
  // I think O(n) is not bad, because the number of server will not be large.
  // But this method called every RequestWithUsername, so it can be a bottleneck.
  // If you can reduce time complexity, please fix it.
  def queryServerToHandle(hash: Hash): ServerToHandle = {
    // FIXME: Modify below logic.
    // When There are server1(hash is 100), server2(hash is 300) and server3(hash is 700)
    // server1 will handle requests that hash is less than or equal to 100 and more than 700.
    // server2 will handle requests that hash is in (100, 300].
    // server3 will handle requests that hash is in (300, 700].
    def whoWillHandle(hash: Hash): Address = {
      try {
        routingTable.find { _.hash >= hash }.getOrElse(routingTable.head).address
         // If there is no matched element, the first server has to handle it.
      } catch {
        // If routingTable is empty, handle requests in current server.
        case _: NoSuchElementException => {
          logger.warn("There is no data in routing table.")
          currentServer
        }
      }
    }
    val serverToHandle = whoWillHandle(hash)
    if (serverToHandle == currentServer) {
      HandleHere
    } else {
      HandleIn(serverToHandle)
    }
  }
}
