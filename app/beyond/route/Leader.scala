package beyond.route

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.CuratorConnection
import java.nio.charset.Charset
import java.io.Closeable
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.PathChildrenCache
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener
import org.apache.curator.framework.recipes.leader.LeaderSelector
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter
import play.api.libs.json.Json
import scala.collection.mutable.Stack

class Leader extends LeaderSelectorListenerAdapter with Actor with ActorLogging {
  private val routingTable: MutableRoutingTable = new MutableRoutingTable

  private val curatorResources: Stack[Closeable] = Stack()

  private val (workersPath, routingTablePath, leaderPath): (String, String, String) = {
    import play.api.Play.current
    def getConfigString(name: String, default: String): String =
      current.configuration.getString(name).getOrElse(default)
    (getConfigString("workers-path", "/workers"),
      getConfigString("routing-table-path", "/routing-table"),
      getConfigString("leader-path", "/leader"))
  }

  override def preStart() {
    try {
      log.info("Curator LeaderCandidate started")

      val connection: CuratorConnection = new CuratorConnection("localhost:2181")
      // FIXME: Make connection string configurable.
      connection.start()
      curatorResources.push(connection)

      def ensurePathsExist() {
        def createPaths(path: String) {
          connection.framework.create().inBackground().forPath(path)
        }
        createPaths(workersPath)
        createPaths(routingTablePath)
        createPaths(leaderPath)
      }
      ensurePathsExist()

      val leaderSelector: LeaderSelector = new LeaderSelector(connection.framework, leaderPath, this)
      leaderSelector.autoRequeue()
      leaderSelector.start()
      curatorResources.push(leaderSelector)
    } catch {
      case _: Throwable => closeAllCuratorResources()
    }
  }

  override def postStop() {
    closeAllCuratorResources()

    log.info("Curator LeaderCandidate stopped")
  }

  override def receive: Receive = {
    case _ =>
  }

  private def closeAllCuratorResources() {
    curatorResources.foreach { _.close() }
  }

  override def takeLeadership(framework: CuratorFramework) {
    // FIXME: Make cache use thread pool.
    val cache = new PathChildrenCache(framework, workersPath, true)
    try {
      cache.start()

      cache.getListenable.addListener(new PathChildrenCacheListener() {
        def updateCuratorServerData() {
          val routingTableToSave: Array[Byte] = Json.stringify(Json.toJson(routingTable)).getBytes(Charset.forName("UTF-8"))
          framework.setData().inBackground().forPath(routingTablePath, routingTableToSave)
        }
        override def childEvent(framework: CuratorFramework, event: PathChildrenCacheEvent) {
          event.getType match {
            case PathChildrenCacheEvent.Type.CHILD_ADDED => {
              val addedWorker = event.getData
              routingTable.add(new String(addedWorker.getData))
              updateCuratorServerData()
            }
            case PathChildrenCacheEvent.Type.CHILD_REMOVED => {
              val removedWorker = event.getData
              routingTable.remove(new String(removedWorker.getData))
              updateCuratorServerData()
            }
            case _ =>
          }
        }
      })
      // FIXME: Modify it to elegant way.
      while ( !Thread.currentThread.isInterrupted ) {
        Thread.sleep(Int.MaxValue)
      }
    } catch {
      // Curator stop release leadership when connection is Disconnected(SUSPEND in Curator term) or SessionExpired(LOST in curator term).
      case ex: InterruptedException => Thread.currentThread.interrupt()
    } finally {
      cache.close()
    }
  }
}
