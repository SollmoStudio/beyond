package beyond.engine.javascript.provider

trait JavaScriptConsoleProvider {
  def log(message: String): Unit
  def info(message: String): Unit
  def warn(message: String): Unit
  def debug(message: String): Unit
  def error(message: String): Unit
}
