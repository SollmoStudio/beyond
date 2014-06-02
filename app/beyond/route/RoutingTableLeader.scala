package beyond.route

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.LeaderSelectorActor._
import beyond.route.RoutingTableConfig._
import java.io.Closeable
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.ChildData
import org.apache.curator.framework.recipes.cache.PathChildrenCache
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener
import play.api.libs.json.Json

object RoutingTableLeader {
  val Name: String = "routingTableLeader"
}

class RoutingTableLeader(curatorFramework: CuratorFramework) extends PathChildrenCacheListener with Actor with ActorLogging {
  private val routingTableBuilder: RoutingTableBuilder = new RoutingTableBuilder

  override def preStart() {
    context.system.eventStream.subscribe(self, classOf[LeadershipMessage])
    context.become(receiveWithoutLeadership)
    log.info("RoutingTableLeader started")
  }

  override def postStop() {
    log.info("RoutingTableLeader stopped")
  }

  override def childEvent(framework: CuratorFramework, event: PathChildrenCacheEvent) {
    def saveRoutingTableToServer(routingTableBuilder: RoutingTableBuilder) {
      val routingTableToSave: Array[Byte] = Json.stringify(Json.toJson(routingTableBuilder)).getBytes("UTF-8")
      framework.setData().inBackground().forPath(RoutingTablePath, routingTableToSave)
    }
    event.getType match {
      case PathChildrenCacheEvent.Type.CHILD_ADDED =>
        val addedWorker: ChildData = event.getData
        routingTableBuilder.add(new String(addedWorker.getData))
        saveRoutingTableToServer(routingTableBuilder)
      case PathChildrenCacheEvent.Type.CHILD_REMOVED =>
        val removedWorker: ChildData = event.getData
        routingTableBuilder.remove(new String(removedWorker.getData))
        saveRoutingTableToServer(routingTableBuilder)
      case _ => // Ignore other events.
    }
  }

  private def receiveWithoutLeadership: Receive = {
    case LeadershipTaken =>
      val cache = new PathChildrenCache(curatorFramework, WorkersPath, true)
      try {
        cache.start()
        cache.getListenable.addListener(this)
      } catch {
        case ex: Throwable =>
          cache.close()
          throw ex
      }
      context.become(receiveWithLeadership(cache))
  }

  private def receiveWithLeadership(cache: Closeable): Receive = {
    case LeadershipLost =>
      cache.close()
      context.become(receive)
  }

  override def receive: Receive = {
    case _ =>
  }
}

