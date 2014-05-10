package beyond

import akka.actor.Actor
import java.io.File
import scala.sys.process.Process

// FIXME: Extract ProcessLauncher trait from MongoDBLauncher and reuse it
// once we have more than one process launchers.
// FIXME: Shutdown the server and crash this actor if the underlying MongoDB
// server does not respond.
class MongoDBLauncher extends Actor {
  // FIXME: Add more mongod paths.
  private val mongodPaths = Seq(
    "/usr/bin/mongod",
    "/opt/local/bin/mongod" // Max OS X Port default path
  )

  private var process: Option[Process] = _

  override def preStart() {
    import play.api.Play.current
    val dbPath = new File(current.configuration.getString("beyond.mongodb.dbpath").getOrElse("data"))
    if (!dbPath.exists()) {
      dbPath.mkdirs()
    }

    mongodPaths.find(new File(_).isFile).map {
      path =>
        val processBuilder = Process(Seq(path, "--dbpath", dbPath.getCanonicalPath))
        process = Some(processBuilder.run())
    }.getOrElse {
      throw new LauncherInitializationException
    }
  }

  override def postStop() {
    process.foreach(_.destroy())
    process = None
  }

  override def receive: Receive = {
    case _ =>
  }
}

