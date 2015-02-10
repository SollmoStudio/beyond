package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.config.BeyondConfiguration
import java.nio.charset.Charset
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.nodes.PersistentEphemeralNode

object WorkerRegistrationActor {
  val Name: String = "workerRegistrationActor"
  val WorkersPath: String = "/workers"
}

class WorkerRegistrationActor(curatorFramework: CuratorFramework) extends Actor with ActorLogging {
  import WorkerRegistrationActor._

  private val workerNode = new PersistentEphemeralNode(
    curatorFramework, PersistentEphemeralNode.Mode.PROTECTED_EPHEMERAL_SEQUENTIAL, WorkersPath + "/w-",
    BeyondConfiguration.currentServerRouteAddress.getBytes(Charset.forName("UTF-8")))

  override def preStart() {
    curatorFramework.create().inBackground().forPath(WorkersPath, Array[Byte](0))

    workerNode.start()
    log.info("WorkerRegistrationActor started")
  }

  override def postStop() {
    workerNode.close()
    log.info("WorkerRegistrationActor stopped")
  }

  override def receive: Receive = Actor.emptyBehavior
}
