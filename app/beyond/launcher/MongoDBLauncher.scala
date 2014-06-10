package beyond.launcher

import akka.actor.Actor
import akka.actor.ActorLogging
import beyond.BeyondConfiguration
import java.io.File
import scala.sys.process.Process
import scalax.file.Path

// FIXME: Extract ProcessLauncher trait from MongoDBLauncher and reuse it
// once we have more than one process launchers.
// FIXME: Shutdown the server and crash this actor if the underlying MongoDB
// server does not respond.
class MongoDBLauncher extends Actor with ActorLogging {
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
  }

  override def postStop() {
    log.info("MongoDBLauncher stopped")
  }

  override def receive: Receive = Map.empty
}

