package beyond.launcher.mongodb

import beyond.config.MongoConfiguration
import java.io.File
import scala.sys.process._

class MongoDBConfigLauncher extends MongoDBLauncher {
  import beyond.launcher._

  override protected val launcherName: String = "MongoDBConfigLauncher"
  override protected val pidFileName: String = "mongo-config.pid"
  override protected val shouldCheckHealth: Boolean = false

  override protected def launchProcess() {
    val dbPath = new File(MongoConfiguration.configDbPath)
    if (!dbPath.exists()) {
      dbPath.mkdirs()
    }

    val port = MongoConfiguration.configPort

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
