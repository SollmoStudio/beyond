package beyond

import akka.actor.Actor
import scala.concurrent.duration.FiniteDuration

object TickGenerator {
  case object Tick
  private[TickGenerator] trait PostStopCaller {
    def postStop()
  }
}

import TickGenerator._

trait TickGenerator extends PostStopCaller { this: Actor =>
  protected val tickInterval: FiniteDuration
  protected val initialDelay: FiniteDuration

  private val tickCancellable = context.system.scheduler.schedule(
    initialDelay = initialDelay, interval = tickInterval, receiver = self, message = Tick)(context.dispatcher)

  abstract override def postStop() {
    tickCancellable.cancel()
    super.postStop()
  }
}
