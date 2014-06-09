package beyond.launcher

import beyond.BeyondRuntime
import org.apache.zookeeper.server.ZooKeeperServerMain
import scalax.file.Path
import scalax.io.Codec
import scalax.io.Resource

object ZooKeeperServerMainWithPIDFile extends ZooKeeperServerMain {
  def main(args: Array[String]) {
    val pidPath = Path.fromString(args(0))
    for { file <- pidPath.createFile(createParents = true).fileOption } {
      Resource.fromFile(file).write(BeyondRuntime.processID)(Codec.UTF8)
    }

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        pidPath.delete(force = true)
      }
    })

    ZooKeeperServerMain.main(args.drop(1))
  }
}

class ZooKeeperServerMainWithPIDFile extends ZooKeeperServerMain {
}
