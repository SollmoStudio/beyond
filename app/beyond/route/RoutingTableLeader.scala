package beyond.route

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.CuratorFrameworkFactoryWithDefaultPolicy
import beyond.route.RoutingTableConfig._
import java.io.Closeable
import java.nio.charset.Charset
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.ChildData
import org.apache.curator.framework.recipes.cache.PathChildrenCache
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener
import org.apache.curator.framework.recipes.leader.LeaderSelector
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter
import play.api.libs.json.Json
import scala.collection.mutable

class RoutingTableLeader extends LeaderSelectorListenerAdapter with Actor with ActorLogging {
  private val routingTableBuilder: RoutingTableBuilder = new RoutingTableBuilder

  private val curatorResources: mutable.Stack[Closeable] = mutable.Stack()

  override def preStart() {
    try {
      log.info("RoutingTableLeader started")

      // FIXME: Make connection string configurable.
      val curatorFramework = CuratorFrameworkFactoryWithDefaultPolicy("localhost:2181")
      curatorFramework.start()
      curatorResources.push(curatorFramework)

      Seq(WorkersPath, RoutingTablePath, LeaderPath)
        .foreach(curatorFramework.create().inBackground().forPath)

      val leaderSelector = new LeaderSelector(curatorFramework, LeaderPath, this)
      leaderSelector.autoRequeue()
      leaderSelector.start()
      curatorResources.push(leaderSelector)
    } catch {
      case ex: Throwable =>
        closeAllCuratorResources()
        throw ex
    }
  }

  override def postStop() {
    closeAllCuratorResources()

    log.info("RoutingTableLeader stopped")
  }

  override def receive: Receive = {
    case _ =>
  }

  private def closeAllCuratorResources() {
    curatorResources.foreach(_.close())
  }

  override def takeLeadership(framework: CuratorFramework) {
    withPathChildrenCache(WorkersPath) { cache =>
      cache.getListenable.addListener(new PathChildrenCacheListener() {
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
      })
      while (!Thread.currentThread.isInterrupted) {
        Thread.sleep(Int.MaxValue)
      }
    }

    def withPathChildrenCache(pathToCache: String)(block: PathChildrenCache => Unit) {
      val cache = new PathChildrenCache(framework, pathToCache, true)
      cache.start()
      try {
        block(cache)
      } catch {
        // Curator releases leadership when connection is Disconnected(SUSPEND in Curator term) or SessionExpired(LOST in curator term).
        case _: InterruptedException => Thread.currentThread.interrupt()
      } finally {
        cache.close()
      }
    }
  }
}
