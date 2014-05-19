package beyond

import akka.actor.Actor
import akka.actor.ActorLogging
import java.io.File
import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.MongoController
import reactivemongo.core.commands.Count
import scala.Some
import scala.sys.process.Process

// FIXME: Extract ProcessLauncher trait from MongoDBLauncher and reuse it
// once we have more than one process launchers.
// FIXME: Shutdown the server and crash this actor if the underlying MongoDB
// server does not respond.
class MongoDBLauncher extends Actor with ActorLogging with Controller with MongoController {
  // FIXME: Add more mongod paths.
  private val mongodPaths = Seq(
    "/usr/bin/mongod",
    "/opt/local/bin/mongod", // Max OS X Port default path
    "C:/Program Files/MongoDB 2.6 Standard/bin/mongod.exe" //window Port default path
  )

  private var process: Option[Process] = _

  private def ensureAccountExists() {
    import play.api.libs.concurrent.Execution.Implicits._

    val numberOfAdminAccount = db.command(new Count("admin.password"))

    numberOfAdminAccount.map {
      case 0 => {
        val collection = db.collection[JSONCollection]("admin.password")
        collection.insert(Json.obj("username" -> Global.mongoDBdefaultUsername, "password" -> Global.mongoDBdefaultPassword))
      }
    }
  }

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

    ensureAccountExists();
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

