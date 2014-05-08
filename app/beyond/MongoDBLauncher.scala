package beyond

import akka.actor.Actor
import java.io.File
import scala.sys.process.Process

object MongoDBLauncherCommand extends Enumeration {
  val Launch, Shutdown = Value
}

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

  override def receive: Receive = {
    case MongoDBLauncherCommand.Launch =>
      val maybePath = mongodPaths.find(new File(_).isFile)
      // FIXME: What if there is no mongod binary?
      maybePath.foreach {
        path =>
          // FIXME: Don't hardcode MongoDB data directory.
          val processBuilder = Process(Seq(path, "--dbpath", "data"))
          process = Some(processBuilder.run())
      }
    case MongoDBLauncherCommand.Shutdown =>
      process.foreach(_.destroy())
  }
}

