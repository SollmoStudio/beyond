package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import java.io.File
import scala.sys.process.Process

// FIXME: Extract ProcessLauncher trait from MongoDBLauncher and reuse it
// once we have more than one process launchers.
// FIXME: Shutdown the server and crash this actor if the underlying MongoDB
// server does not respond.
class MongoDBLauncher extends Actor with ActorLogging {
  // FIXME: Add more mongod paths.
  private val mongodPaths = Seq(
    "/usr/bin/mongod",
    "/opt/local/bin/mongod", // Max OS X Port default path
    "C:/Program Files/MongoDB 2.6 Standard/bin/mongod.exe" // Windows default path
  )

  private var process: Option[Process] = _

  override def preStart() {
    val dbPath = new File(Global.mongoDBPath)
    if (!dbPath.exists()) {
      dbPath.mkdirs()
    }

    mongodPaths.find(new File(_).isFile).map {
      path =>
        val processBuilder = Process(Seq(path, "--dbpath", dbPath.getCanonicalPath))
        process = Some(processBuilder.run())
        log.info("MongoDB started")
    }.getOrElse {
      throw new LauncherInitializationException
    }
  }

  override def postStop() {
    process.foreach(_.destroy())
    process = None
    log.info("MongoDB stopped")
  }

  override def receive: Receive = {
    case _ =>
  }
}

