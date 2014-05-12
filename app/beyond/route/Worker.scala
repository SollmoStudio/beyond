package beyond.route

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.CuratorConnection
import java.io.Closeable
import java.net.InetAddress
import java.nio.charset.Charset
import org.apache.curator.framework.recipes.cache.NodeCache
import org.apache.curator.framework.recipes.cache.NodeCacheListener
import org.apache.curator.framework.recipes.nodes.PersistentEphemeralNode
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import scala.collection.mutable.Stack

class Worker extends Actor with ActorLogging {
  private val closeable: Stack[Closeable] = Stack()

  private var connection: Option[CuratorConnection] = _

  private val workersPath = {
    import play.api.Play.current
    current.configuration.getString("workers-path").getOrElse("/workers")
  }
  private val routingTablePath = {
    import play.api.Play.current
    current.configuration.getString("routing-table-path").getOrElse("/routing-table")
  }

  override def preStart() {
    super.preStart()
    log.info("RoutingTableWorker started")

    // FIXME: Make connection string configurable.
    connection = Some(new CuratorConnection("localhost:2181"))
    connection.foreach { connection =>
      connection.start()
      closeable.push(connection)

      connection.ensureZNodeExists(workersPath)
      connection.ensureZNodeExists(routingTablePath)

      import play.api.Play.current
      val defaultPort = 9000
      val hostAddress = current.configuration.getString("http.address").getOrElse(InetAddress.getLocalHost.getHostAddress)
      val port = current.configuration.getInt("http.port").getOrElse(defaultPort)

      val currentAddress = hostAddress + ":" + port.toString
      val workerNode = new PersistentEphemeralNode(
        connection.framework, PersistentEphemeralNode.Mode.PROTECTED_EPHEMERAL_SEQUENTIAL, workersPath + "/w-",
        currentAddress.getBytes(Charset.forName("UTF-8")))
      workerNode.start()
      closeable.push(workerNode)

      val routingTable = new NodeCache(connection.framework, routingTablePath)
      routingTable.getListenable.addListener(new NodeCacheListener {
        override def nodeChanged(): Unit = {
          Option(routingTable.getCurrentData.getData).fold {
            context.parent ! new RoutingTableView(currentAddress)
          } { data: Array[Byte] =>
            context.parent ! new RoutingTableView(currentAddress, Option(Json.parse(new String(data)).as[JsArray]))
          }
        }
      })
      routingTable.start()
      closeable.push(routingTable)
    }
  }

  override def postStop() {
    closeable.foreach { _.close() }
    closeable.clear()

    log.info("RoutingTableWorker stopped")
    super.postStop()
  }

  override def receive: Receive = {
    case _ =>
  }
}
