package beyond.launcher.mongodb

import beyond.BeyondConfiguration
import java.io.File
import scala.sys.process._

class MongoDBConfigLauncher extends MongoDBLauncher {
  import beyond.launcher._

  override protected val launcherName: String = "MongoDBConfigLauncher"
  override protected val pidFileName: String = "mongo-config.pid"
  override protected val shouldCheckHealth: Boolean = false

  override protected def launchProcess() {
    val dbPath = new File(BeyondConfiguration.mongo.configDbPath)
    if (!dbPath.exists()) {
      dbPath.mkdirs()
    }

    val port = BeyondConfiguration.mongo.configPort

    val path: String = mongodPath.getOrElse {
      throw new LauncherInitializationException
    }

    val processBuilder = Process(Seq(path,
      "--configsvr",
      "--dbpath", dbPath.getCanonicalPath,
      "--port", port,
      "--pidfilepath", pidFilePath.path))
    processBuilder.run()
    log.info("MongoDB config server started")
  }
}
