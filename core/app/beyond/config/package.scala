package beyond

import java.io.File
import play.api.Configuration

package object config {
  private[config] def rootConfiguration =
    try {
      play.api.Play.current.configuration
    } catch {
      // If there's no running Play app, just load it with the current app path.
      // Mainly for tests or console
      case _: RuntimeException => Configuration.load(new File("."))
    }

  private[config] def configuration(implicit prefix: String) =
    rootConfiguration.getConfig(prefix).getOrElse(Configuration.empty)
}
