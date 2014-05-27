package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import java.io.Closeable
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.leader.LeaderSelector
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter
import play.api.libs.concurrent.Akka
import scala.collection.mutable

object LeaderSelectorActor {
  val Name: String = "leaderSelector"
  val LeaderPath: String = "/leader"

  sealed trait LeadershipMessage
  case class LeadershipTaken(framework: CuratorFramework) extends LeadershipMessage
  case object LeadershipLost extends LeadershipMessage
}

class LeaderSelectorActor extends LeaderSelectorListenerAdapter with Actor with ActorLogging {
  import LeaderSelectorActor._

  private val curatorResources: mutable.Stack[Closeable] = mutable.Stack()

  override def preStart() {
    try {
      log.info("LeaderSelectorActor started")

      // FIXME: Make connection string configurable.
      val curatorFramework = CuratorFrameworkFactoryWithDefaultPolicy("localhost:2181")
      curatorFramework.start()
      curatorResources.push(curatorFramework)

      curatorFramework.create().inBackground().forPath(LeaderPath, Array[Byte](0))

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

    log.info("LeaderSelectorActor stopped")
  }

  private def closeAllCuratorResources() {
    curatorResources.foreach(_.close())
  }

  // takeLeadership is called by an internal Curator thread.
  override def takeLeadership(framework: CuratorFramework) {
    import play.api.Play.current

    log.info("Leadership is taken")
    Akka.system.eventStream.publish(LeadershipTaken(framework))

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

  override def receive: Receive = {
    case _ =>
  }
}

