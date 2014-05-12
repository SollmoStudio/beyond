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
  private val curatorResources: Stack[Closeable] = Stack()

  private val (workersPath, routingTablePath) = {
    import play.api.Play.current
    def optionalConfigString(name: String): Option[String] = current.configuration.getString(name)
    (optionalConfigString("workers-path").getOrElse("/workers"),
      optionalConfigString("routing-table-path").getOrElse("/routing-table"))
  }

  override def preStart() {
    try {
      log.info("RoutingTableWorker started")
      // FIXME: Make connection string configurable.
      val connection: CuratorConnection = new CuratorConnection("localhost:2181")
      connection.start()
      curatorResources.push(connection)

      def ensurePathsExist() {
        def createPath(path: String) {
          connection.framework.create().inBackground().forPath(path)
        }
        createPath(workersPath)
        createPath(routingTablePath)
      }
      ensurePathsExist()

      import play.api.Play.current
      val defaultPort = 9000
      val hostAddress = current.configuration.getString("http.address").getOrElse(InetAddress.getLocalHost.getHostAddress)
      val port = current.configuration.getInt("http.port").getOrElse(defaultPort)

      val currentAddress = hostAddress + ":" + port.toString
      val workerNode = new PersistentEphemeralNode(
        connection.framework, PersistentEphemeralNode.Mode.PROTECTED_EPHEMERAL_SEQUENTIAL, workersPath + "/w-",
        currentAddress.getBytes(Charset.forName("UTF-8")))
      workerNode.start()
      curatorResources.push(workerNode)

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
      curatorResources.push(routingTable)
    } catch {
      case _: Throwable => closeAllCuratorResources()
    }
  }

  private def closeAllCuratorResources() {
    curatorResources.foreach { _.close() }
  }

  override def postStop() {
    closeAllCuratorResources()

    log.info("RoutingTableWorker stopped")
  }

  override def receive: Receive = {
    case _ =>
  }
}
