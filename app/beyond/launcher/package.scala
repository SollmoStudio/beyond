package beyond

import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import scala.io.Codec
import scala.io.Source
import scalax.file.Path

package object launcher extends Logging {
  def terminateProcessIfExists(pidFilePath: Path) {
    if (pidFilePath.exists) {
      try {
        val filePath = pidFilePath.fileOption.get
        val pid = Source.fromFile(filePath)(Codec.UTF8).getLines().mkString.stripLineEnd
        ProcessCommand().terminate(pid.toInt)
      } catch {
        case ex: NumberFormatException =>
          logger.warn(s"${pidFilePath.path} is corrupted.")
      } finally {
        pidFilePath.delete(force = true)
      }
    }
  }
}
