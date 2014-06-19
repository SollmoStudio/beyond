package beyond.plugin.lib

import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }

class PluginConsole extends Logging {
  def log(message: String) {
    logger.info(message)
  }

  def info(message: String) {
    logger.info(message)
  }

  def debug(message: String) {
    logger.debug(message)
  }

  def warn(message: String) {
    logger.warn(message)
  }

  def error(message: String) {
    logger.error(message)
  }
}
