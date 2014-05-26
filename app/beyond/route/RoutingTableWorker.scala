package beyond.route

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.CuratorFrameworkFactoryWithDefaultPolicy
import beyond.UserActionActor.UpdateRoutingTable
import beyond.route.RoutingTableConfig._
import java.io.Closeable
import java.nio.charset.Charset
import org.apache.curator.framework.recipes.cache.NodeCache
import org.apache.curator.framework.recipes.cache.NodeCacheListener
import org.apache.curator.framework.recipes.nodes.PersistentEphemeralNode
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import scala.collection.mutable

class RoutingTableWorker extends Actor with ActorLogging {
  private val curatorResources: mutable.Stack[Closeable] = mutable.Stack()

  override def preStart() {
    try {
      log.info("RoutingTableWorker started")

      // FIXME: Make connection string configurable.
      val curatorFramework = CuratorFrameworkFactoryWithDefaultPolicy("localhost:2181")
      curatorFramework.start()
      curatorResources.push(curatorFramework)

      def ensurePath(path: String, data: Array[Byte] = Array[Byte](0)) {
        curatorFramework.create().inBackground().forPath(path, data)
      }
      ensurePath(WorkersPath)
      ensurePath(RoutingTablePath, "[]".getBytes("UTF-8"))

      val workerNode = new PersistentEphemeralNode(
        curatorFramework, PersistentEphemeralNode.Mode.PROTECTED_EPHEMERAL_SEQUENTIAL, WorkersPath + "/w-",
        beyond.Global.currentServerAddress.getBytes(Charset.forName("UTF-8")))
      workerNode.start()
      curatorResources.push(workerNode)

      val routingTableWatcher = new NodeCache(curatorFramework, RoutingTablePath)
      routingTableWatcher.getListenable.addListener(new NodeCacheListener {
        override def nodeChanged() {
          val changedData = routingTableWatcher.getCurrentData.getData
          context.parent ! UpdateRoutingTable(Json.parse(changedData).as[JsArray])
        }
      })
      routingTableWatcher.start()
      curatorResources.push(routingTableWatcher)
    } catch {
      case ex: Throwable =>
        closeAllCuratorResources()
        throw ex
    }
  }

  private def closeAllCuratorResources() {
    curatorResources.foreach(_.close())
  }

  override def postStop() {
    closeAllCuratorResources()

    log.info("RoutingTableWorker stopped")
  }

  override def receive: Receive = {
    case _ =>
  }
}
