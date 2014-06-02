package beyond.route

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.BeyondSupervisor.UserActionSupervisorPath
import beyond.UserActionActor.NewRoutingTable
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.NodeCache
import org.apache.curator.framework.recipes.cache.NodeCacheListener
import play.api.libs.json.JsArray
import play.api.libs.json.Json

object RoutingTableWatcher {
  val Name: String = "routingTableWatcher"
}

class RoutingTableWatcher(curatorFramework: CuratorFramework) extends NodeCacheListener with Actor with ActorLogging {
  import beyond.route.RoutingTableConfig._

  private val userActionSupervisor = {
    import play.api.libs.concurrent.Akka
    import play.api.Play.current
    import scala.concurrent.ExecutionContext
    implicit val ec: ExecutionContext = Akka.system.dispatcher
    Akka.system.actorSelection(UserActionSupervisorPath)
  }

  private val routingTableWatcher = {
    val nodeCache = new NodeCache(curatorFramework, RoutingTablePath)
    nodeCache.getListenable.addListener(this)
    nodeCache
  }

  override def nodeChanged() {
    val changedData = routingTableWatcher.getCurrentData.getData
    userActionSupervisor.tell(NewRoutingTable(Json.parse(changedData).as[JsArray]), sender)
  }

  override def preStart() {
    curatorFramework.create().inBackground().forPath(RoutingTablePath, "[]".getBytes("UTF-8"))
    routingTableWatcher.start()
    log.info("RoutingTableUpdateActor started")
  }

  override def postStop() {
    routingTableWatcher.close()
    log.info("RoutingTableUpdateActor stopped")
  }

  override def receive: Receive = {
    case _ =>
  }
}

