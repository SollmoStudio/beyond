package beyond.route

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import beyond.LeaderSelectorActor._
import beyond.PathChildrenCacheActor
import beyond.route.RoutingTableConfig._
import org.apache.curator.framework.CuratorFramework
import play.api.libs.json.Json

object RoutingTableUpdater {
  val Name: String = "routingTableUpdater"
}

class RoutingTableUpdater(curatorFramework: CuratorFramework) extends Actor with ActorLogging {
  import beyond.WorkerRegistrationActor._
  import PathChildrenCacheActor._

  private val routingTableBuilder: RoutingTableBuilder = new RoutingTableBuilder

  override def preStart() {
    context.system.eventStream.subscribe(self, classOf[LeadershipMessage])
    context.become(receiveWithoutLeadership)
    log.info("RoutingTableLeader started")
  }

  override def postStop() {
    log.info("RoutingTableLeader stopped")
  }

  private def saveRoutingTableToServer() {
    val routingTableToSave: Array[Byte] = Json.stringify(Json.toJson(routingTableBuilder)).getBytes("UTF-8")
    curatorFramework.setData().inBackground().forPath(RoutingTablePath, routingTableToSave)
  }

  private def receiveWithoutLeadership: Receive = {
    case LeadershipTaken =>
      val cache = context.actorOf(Props(classOf[PathChildrenCacheActor], curatorFramework, WorkersPath))
      context.become(receiveWithLeadership(cache))
  }

  private def receiveWithLeadership(cache: ActorRef): Receive = {
    case ChildAdded(childData) =>
      routingTableBuilder.add(childData)
      saveRoutingTableToServer()
    case ChildRemoved(childData) =>
      routingTableBuilder.remove(childData)
      saveRoutingTableToServer()
    case LeadershipLost =>
      context.stop(cache)
      context.become(receiveWithoutLeadership)
  }

  override def receive: Receive = Actor.emptyBehavior
}

