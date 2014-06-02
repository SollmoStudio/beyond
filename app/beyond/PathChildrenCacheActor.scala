package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.ChildData
import org.apache.curator.framework.recipes.cache.PathChildrenCache
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener

object PathChildrenCacheActor {
  sealed trait PathChildrenCacheMessage
  case class ChildAdded(childData: String) extends PathChildrenCacheMessage
  case class ChildRemoved(childData: String) extends PathChildrenCacheMessage
}

class PathChildrenCacheActor(curatorFramework: CuratorFramework, path: String) extends PathChildrenCacheListener with Actor with ActorLogging {
  import PathChildrenCacheActor._

  val cache = new PathChildrenCache(curatorFramework, path, true)

  override def preStart() {
    cache.start()
    cache.getListenable.addListener(this)
    log.info(s"PathChildrenCacheActor started: path=$path")
  }

  override def postStop() {
    cache.close()
    log.info(s"PathChildrenCacheActor stopped: path=$path")
  }

  // childEvent is called by an internal Curator thread.
  override def childEvent(framework: CuratorFramework, event: PathChildrenCacheEvent) {
    event.getType match {
      case PathChildrenCacheEvent.Type.CHILD_ADDED =>
        val worker: ChildData = event.getData
        context.parent ! ChildAdded(new String(worker.getData))
      case PathChildrenCacheEvent.Type.CHILD_REMOVED =>
        val worker: ChildData = event.getData
        context.parent ! ChildRemoved(new String(worker.getData))
      case _ => // Ignore other events.
    }
  }

  override def receive: Receive = {
    case _ =>
  }
}

