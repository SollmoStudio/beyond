package beyond

import akka.actor.Actor
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy._
import beyond.UserActionActor.UpdateRoutingTable
import beyond.plugin.GamePlugin
import beyond.route.RoutingTableLeader
import beyond.route.RoutingTableWorker
import beyond.launcher.LauncherSupervisor

object BeyondSupervisor {
  val BeyondSupervisorBasePath: String = "/user/beyondSupervisor/"

  val UserActionSupervisorPath: String = BeyondSupervisorBasePath + UserActionSupervisor.Name
  val UserActionActorPath: String = s"$UserActionSupervisorPath/${UserActionActor.Name}"
}

class BeyondSupervisor extends Actor {
  context.actorOf(Props[LauncherSupervisor], LauncherSupervisor.Name)
  // FIXME: Don't hardcode the plugin filename.
  context.actorOf(Props(classOf[GamePlugin], "main.js"), GamePlugin.Name)
  context.actorOf(Props[SystemMetricsActor], SystemMetricsActor.Name)
  context.actorOf(Props[CuratorSupervisor], CuratorSupervisor.Name)
  private val userActionSupervisor = context.actorOf(Props[UserActionSupervisor], UserActionSupervisor.Name)

  override val supervisorStrategy =
    OneForOneStrategy() {
      // FIXME: Need policy for all exceptions escalated by Beyond actors.
      case t =>
        super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }

  override def receive: Receive = {
    case msg: UpdateRoutingTable =>
      import play.api.libs.concurrent.Akka
      import play.api.Play.current
      import scala.concurrent.ExecutionContext
      implicit val ec: ExecutionContext = Akka.system.dispatcher
      userActionSupervisor.tell(msg, sender)
    case _ =>
  }
}

