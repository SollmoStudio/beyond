package beyond.route

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.CuratorConnection
import java.nio.charset.Charset
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.PathChildrenCache
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener
import org.apache.curator.framework.recipes.leader.CancelLeadershipException
import org.apache.curator.framework.recipes.leader.LeaderSelector
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter

class Leader extends LeaderSelectorListenerAdapter with Actor with ActorLogging {
  private val routingTable: MutableRoutingTable = new MutableRoutingTable

  private var connection: Option[CuratorConnection] = _

  private val (workersPath, routingTablePath, leaderPath): (String, String, String) = {
    import play.api.Play.current
    (current.configuration.getString("workers-path").getOrElse("/workers"),
      current.configuration.getString("routing-table-path").getOrElse("/routing-table"),
      current.configuration.getString("leader-path").getOrElse("/leader"))
  }

  override def preStart() {
    log.info("Curator LeaderCandidate started")

    // FIXME: Make connection string configurable.
    connection = Some(new CuratorConnection("localhost:2181"))
    connection.foreach { connection =>
      connection.start()
      val leaderSelector = new LeaderSelector(connection.framework, leaderPath, this)
      leaderSelector.autoRequeue()
      leaderSelector.start()
    }
  }

  override def postStop() {
    connection.get.close()

    log.info("Curator LeaderCandidate stopped")
  }

  override def receive: Receive = {
    case _ =>
  }

  override def takeLeadership(framework: CuratorFramework): Unit = {
    connection.foreach { connection =>
      // FIXME: Make cache use thread pool.
      val cache = new PathChildrenCache(framework, workersPath, true)
      try {
        cache.start()

        cache.getListenable().addListener(new PathChildrenCacheListener() {
          def updateCuratorServerData() {
            // FIXME: Make it async using inBackground.
            framework.setData().forPath(routingTablePath, routingTable.toJson().toString.getBytes(Charset.forName("UTF-8")))
          }
          override def childEvent(framework: CuratorFramework, event: PathChildrenCacheEvent) {
            event.getType match {
              case PathChildrenCacheEvent.Type.CHILD_ADDED => {
                routingTable.add(new String(event.getData.getData))
                updateCuratorServerData()
              }
              case PathChildrenCacheEvent.Type.CHILD_REMOVED => {
                routingTable.remove(new String(event.getData.getData))
                updateCuratorServerData()
              }
              case _ =>
            }
          }
        })
        // FIXME: Modify it to elegant way.
        while (true) {
          Thread.sleep(Int.MaxValue)
        }
      } catch {
        case ex: CancelLeadershipException =>
      } finally {
        cache.close()
      }
    }
  }
}
