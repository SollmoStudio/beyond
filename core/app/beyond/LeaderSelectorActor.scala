package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.leader.LeaderSelector
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter
import play.api.libs.concurrent.Akka

object LeaderSelectorActor {
  val Name: String = "leaderSelector"
  val LeaderPath: String = "/leader"

  sealed trait LeadershipMessage
  case object LeadershipTaken extends LeadershipMessage
  case object LeadershipLost extends LeadershipMessage
}

class LeaderSelectorActor(curatorFramework: CuratorFramework) extends LeaderSelectorListenerAdapter with Actor with ActorLogging {
  import LeaderSelectorActor._

  private val leaderSelector = new LeaderSelector(curatorFramework, LeaderPath, this)

  override def preStart() {
    curatorFramework.create().inBackground().forPath(LeaderPath, Array[Byte](0))

    leaderSelector.autoRequeue()
    leaderSelector.start()
    log.info("LeaderSelectorActor started")
  }

  override def postStop() {
    leaderSelector.close()
    log.info("LeaderSelectorActor stopped")
  }

  // takeLeadership is called by an internal Curator thread.
  override def takeLeadership(framework: CuratorFramework) {
    import play.api.Play.current

    log.info("Leadership is taken")
    Akka.system.eventStream.publish(LeadershipTaken)

    try {
      while (!Thread.currentThread.isInterrupted) {
        Thread.sleep(Int.MaxValue)
      }
    } catch {
      case _: InterruptedException => Thread.currentThread().interrupt()
    } finally {
      Akka.system.eventStream.publish(LeadershipLost)
      log.info("Leadership is lost")
    }
  }

  override def receive: Receive = Actor.emptyBehavior
}

