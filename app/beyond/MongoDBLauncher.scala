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
    // FIXME: Don't hardcode MongoDB data directory.
    val dataPath = "data"
    // FIXME: Ensure that all parent directory exists.
    val dataDirectory = new File(dataPath)
    if (! dataDirectory.exists()) {
      dataDirectory.mkdir()
    }

    val maybePath = mongodPaths.find(new File(_).isFile)
    // FIXME: What if there is no mongod binary?
    maybePath.foreach {
      path =>
        val processBuilder = Process(Seq(path, "--dbpath", dataPath))
        process = Some(processBuilder.run())
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

