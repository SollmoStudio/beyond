package beyond.launcher.mongodb

import beyond.config.BeyondConfiguration
import java.io.File
import scala.sys.process._

class MongoDBStandaloneLauncher extends MongoDBLauncher {
  import beyond.launcher._

  override protected val launcherName: String = "MongoDBStandaloneLauncher"
  override protected val pidFileName: String = "mongo-standalone.pid"

  override protected def launchProcess() {
    val dbPath = new File(BeyondConfiguration.mongo.dbPath)
    if (!dbPath.exists()) {
      dbPath.mkdirs()
    }

    val path: String = mongodPath.getOrElse {
      throw new LauncherInitializationException
    }

    val processBuilder = Process(Seq(path, "--dbpath", dbPath.getCanonicalPath, "--pidfilepath", pidFilePath.path))
    processBuilder.run()
    log.info("MongoDB started")
  }
}
