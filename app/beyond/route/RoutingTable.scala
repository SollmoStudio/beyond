package beyond.route

import com.typesafe.scalalogging.slf4j.StrictLogging
import java.util.NoSuchElementException
import org.apache.commons.codec.digest.DigestUtils
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import scala.collection.immutable.TreeMap

private[route] object RoutingTableGenerator {
  def apply(data: Option[JsArray] = None): TreeMap[Int, String] = data.fold {
      TreeMap.empty[Int, String]
    } { data: JsArray =>
      data.value.foldLeft(TreeMap.empty[Int, String]) {
        // FIXME: Handle unexpected format
        case (treeMap, token: JsObject) =>
          treeMap.updated((token \ "hash").as[Int], (token \ "address").as[String])
        case (treeMap, _) => treeMap
      }
    }
}

class MutableRoutingTable(data: Option[JsArray] = None) {
  private var routingTable: TreeMap[Int, String] = RoutingTableGenerator(data)

  def add(address: String) {
    val hash = (token: String) => {
      // FIXME: Make maximumHash configurable
      val maximumHash = 1000
      // FIXME: Use a better hashing algorithm.
      val startIndex = 25
      val endIndex = 31
      val digits = 16
      Integer.parseInt(DigestUtils.md5Hex(token).substring(startIndex, endIndex), digits) % maximumHash
    }

    routingTable = routingTable.updated(hash(address), address)
  }

  def remove(address: String) {
    routingTable = routingTable.filter {
      case (savedHash, savedAddress) => savedAddress != address
    }
  }

  def toJson(): JsArray = {
    routingTable.foldLeft(new JsArray) {
      case (array: JsArray, (hash, address)) =>
        array.append(Json.obj("address" -> address, "hash" -> hash))
    }
  }
}

object RoutingTableView {
  trait QueryResult
  case object HandleHere extends QueryResult
  case class RedirectTo(address: String) extends QueryResult
}

class RoutingTableView(currentAddress: String, data: Option[JsArray] = None) extends StrictLogging {
  import beyond.route.RoutingTableView._
  @volatile protected var routingTable: TreeMap[Int, String] = RoutingTableGenerator(data)

  def replace(newTable: RoutingTableView) {
    routingTable = newTable.routingTable
  }

  // Currently, time complexity of this method is O(n) (n = number of server in routing table).
  // I think O(n) is not bad, because the number of server will not be large.
  // But this method called every RequestWithUsername, so it can be a bottleneck.
  // If you can reduce time complexity, please fix it.
  def queryServerToHandle(hash: Int): QueryResult = {
    // FIXME: Modify below logic.
    // When Thare are server1(hash is 100), server2(hash is 300) and server3(hash is 700)
    // server1 will handle requests that hash is less than or equal to 100 and more than 700.
    // server2 will handle requests that hash is in (100, 300].
    // server3 will handle requests that hash is in (300, 700].
    def whoWillHandle(hash: Int): String = {
      try {
        routingTable.find {
          case (savedHash, savedAddress) => savedHash >= hash
        }.getOrElse(routingTable.head)._2 // If there is no matched element, the first server have to handle it.
      } catch {
        // If hashTable is empty, handle requests in current server.
        case _: NoSuchElementException => {
          logger.warn("There is no data in routing table.")
          currentAddress
        }
      }
    }
    val serverToHandle = whoWillHandle(hash)
    if (serverToHandle == currentAddress) {
      HandleHere
    } else {
      RedirectTo(whoWillHandle(hash))
    }
  }
}
