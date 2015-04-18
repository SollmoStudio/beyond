package beyond

import java.io.File

package object config {
  private[config] def rootConfiguration =
    Configuration(try {
      play.api.Play.current.configuration
    } catch {
      // If there's no running Play app, just load it with the current app path.
      // Mainly for tests or console
      case _: RuntimeException => play.api.Configuration.load(new File("."))
    })

  private[config] def configuration(implicit prefix: String) =
    rootConfiguration.getConfig(prefix).getOrElse(Configuration.empty)
}
