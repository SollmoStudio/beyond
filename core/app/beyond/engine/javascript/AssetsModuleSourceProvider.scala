package beyond.engine.javascript

import com.typesafe.scalalogging.slf4j.StrictLogging
import java.io.StringReader
import java.net.URI
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.commonjs.module.provider.ModuleSource
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider
import play.api.Play
import scala.io.Source

class AssetsModuleSourceProvider extends ModuleSourceProvider with StrictLogging {
  override def loadSource(moduleId: String, paths: Scriptable, validator: AnyRef): ModuleSource = {
    import Play.current
    Play.resource(s"public/js_lib/$moduleId.js").map {
      resource =>
        val stream = resource.openStream()
        val source = Source.fromInputStream(stream).getLines().mkString("\n")
        new ModuleSource(new StringReader(source), null, resource.toURI, resource.toURI, validator) // scalastyle:ignore null
    }.orNull
  }

  // FIXME: I didn't implement it because there is no use case that this method
  //       is used.
  //       Implement it when it's used.
  override def loadSource(uri: URI, baseUri: URI, validator: AnyRef): ModuleSource =
    ???
}
