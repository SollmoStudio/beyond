package beyond

import java.io.File
import play.api.Configuration

trait ConfigurationMixin {
  protected def configuration =
    try {
      play.api.Play.current.configuration
    } catch {
      // If there's no running Play app, just load it with the current app path.
      // Mainly for tests or console
      case _: RuntimeException => Configuration.load(new File("."))
    }
}
