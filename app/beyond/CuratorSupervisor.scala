package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import beyond.route.RoutingTableLeader
import beyond.route.RoutingTableWorker
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory

private object CuratorFrameworkFactoryWithDefaultPolicy extends Logging {
  def apply(serversToConnect: String): CuratorFramework = {
    logger.info("Create a connection to {} .", serversToConnect)
    CuratorFrameworkFactory.newClient(serversToConnect, BeyondConfiguration.curatorConnectionPolicy)
  }
}

object CuratorSupervisor {
  val Name: String = "curatorSupervisor"
}

class CuratorSupervisor extends Actor with ActorLogging {
  private val curatorFramework = {
    // FIXME: Make connection string configurable.
    val framework = CuratorFrameworkFactoryWithDefaultPolicy("localhost:2181")
    framework.start()
    framework
  }

  context.actorOf(Props(classOf[LeaderSelectorActor], curatorFramework), LeaderSelectorActor.Name)
  context.actorOf(Props(classOf[RoutingTableLeader], curatorFramework), RoutingTableLeader.Name)
  context.actorOf(Props(classOf[RoutingTableWorker], curatorFramework), RoutingTableWorker.Name)

  override def postStop() {
    curatorFramework.close()
  }

  override def receive: Receive = {
    case _ =>
  }
}

