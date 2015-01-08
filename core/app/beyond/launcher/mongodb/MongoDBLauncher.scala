package beyond.launcher.mongodb

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.BeyondConfiguration
import beyond.MongoMixin
import beyond.TickGenerator
import java.io.IOException
import play.api.libs.concurrent.Akka
import reactivemongo.core.commands.Status
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.sys.process._
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scalax.file.Path

object MongoDBLauncher {
  val ServerNotRespondingTimeout = 30.seconds
  val RetryDelay = 5.seconds

  val RetryDelayAtInitialization = 3.seconds
}

abstract class MongoDBLauncher extends {
  override protected val initialDelay = 10.seconds
  override protected val tickInterval = 1.second
} with Actor with TickGenerator with ActorLogging with MongoMixin {
  import beyond.launcher._
  import MongoDBLauncher._

  protected val launcherName: String
  protected val pidFileName: String

  protected lazy val pidFilePath: Path = Path.fromString(BeyondConfiguration.pidDirectory) / pidFileName

  private def mongoBinPath(bin: String): Option[String] = try {
    Some((s"which $bin" !!).trim) // Locate Unix mongod path
  } catch {
    case _: IOException => try {
      Some((s"where.exe $bin.exe" !!).trim) // Locate Windows mongod path
    } catch {
      case _: IOException => None
    }
  }

  protected def mongodPath: Option[String] = mongoBinPath("mongod")
  protected def mongosPath: Option[String] = mongoBinPath("mongos")
  protected def mongoPath: Option[String] = mongoBinPath("mongo")

  override def preRestart(reason: Throwable, message: Option[Any]) {
    super.preRestart(reason, message)
    terminateProcessIfExists(pidFilePath)
  }

  protected val shouldCheckHealth: Boolean = true
  private def healthCheck(delay: FiniteDuration) {
    if (shouldCheckHealth) {
      import play.api.Play.current
      implicit val ec: ExecutionContext = Akka.system.dispatcher
      context.system.scheduler.scheduleOnce(delay) {
        db.command(Status).onComplete {
          case mongoServerStatus: Try[_] =>
            self ! mongoServerStatus
        }
      }
    }
  }

  protected def launchProcess(): Unit

  override def preStart() {
    log.info(launcherName + " started")
    launchProcess()
    healthCheck(RetryDelayAtInitialization)
  }

  override def postStop() {
    super.postStop()
    terminateProcessIfExists(pidFilePath)
    log.info(launcherName + " stopped")
  }

  override def receive: Receive = initializing

  private def initializing: Receive = {
    case Success(_) =>
      healthCheck(RetryDelay)
      context.become(initialized(ServerNotRespondingTimeout))
    case Failure(_) =>
      throw new ServerNotRespondingException
    case TickGenerator.Tick => // Ignore Tick on initializing.
  }

  private def initialized(timeout: FiniteDuration): Receive = {
    case Success(_) =>
      healthCheck(RetryDelay)
      context.become(initialized(ServerNotRespondingTimeout))
    case Failure(_) =>
      healthCheck(RetryDelay)
    case TickGenerator.Tick =>
      val newTimeout = timeout - tickInterval
      if (newTimeout > Duration.Zero) {
        context.become(initialized(newTimeout))
      } else {
        throw new ServerNotRespondingException
      }
  }
}
