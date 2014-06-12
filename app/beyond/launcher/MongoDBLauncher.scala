package beyond.launcher

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.BeyondConfiguration
import beyond.MongoMixin
import beyond.TickGenerator
import java.io.File
import play.api.libs.concurrent.Akka
import reactivemongo.core.commands.Status
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.sys.process.Process
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scalax.file.Path

object MongoDBLauncher {
  val ServerNotRespondingTimeout = 30.seconds
  val RetryDelay = 5.seconds

  val RetryDelayAtInitialization = 3.seconds
}

// FIXME: Extract ProcessLauncher trait from MongoDBLauncher and reuse it
// once we have more than one process launchers.
class MongoDBLauncher extends {
  override protected val initialDelay = 10.seconds
  override protected val tickInterval = 1.second
} with Actor with TickGenerator with ActorLogging with MongoMixin {
  private val pidFilePath: Path = Path.fromString(BeyondConfiguration.pidDirectory) / "mongo.pid"
  // FIXME: Add more mongod paths.
  private val mongodPaths = Seq(
    "/usr/bin/mongod",
    "/opt/local/bin/mongod", // Max OS X Port default path
    "C:/Program Files/MongoDB 2.6 Standard/bin/mongod.exe" // Windows default path
  )

  override def preRestart(reason: Throwable, message: Option[Any]) {
    super.preRestart(reason, message)
    terminateProcessIfExists(pidFilePath)
  }

  import MongoDBLauncher._

  private def healthCheck(delay: FiniteDuration) {
    import play.api.Play.current
    implicit val ec: ExecutionContext = Akka.system.dispatcher
    context.system.scheduler.scheduleOnce(delay) {
      db.command(Status).onComplete {
        case mongoServerStatus: Try[_] =>
          self ! mongoServerStatus
      }
    }
  }

  override def preStart() {
    log.info("MongoDBLauncher started")
    val dbPath = new File(BeyondConfiguration.mongoDBPath)
    if (!dbPath.exists()) {
      dbPath.mkdirs()
    }

    mongodPaths.find(new File(_).isFile).map {
      path =>
        val processBuilder = Process(Seq(path, "--dbpath", dbPath.getCanonicalPath, "--pidfilepath", pidFilePath.path))
        processBuilder.run()
        log.info("MongoDB started")
    }.getOrElse {
      throw new LauncherInitializationException
    }
    healthCheck(RetryDelayAtInitialization)
  }

  override def postStop() {
    super.postStop()
    log.info("MongoDBLauncher stopped")
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
