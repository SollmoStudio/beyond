package beyond.plugin.test

import beyond.engine.javascript.BeyondJavaScriptEngine
import java.io.File
import java.io.FileReader
import java.net.URI
import org.mozilla.javascript.Context
import org.mozilla.javascript.EcmaError
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider
import play.api.DefaultApplication
import play.api.Mode
import scalax.file.Path
import scalaz.syntax.std.boolean._

object TestRunner extends App {
  import com.beyondframework.rhino.RhinoConversions._

  private val testApp =
    new DefaultApplication(new File("."), this.getClass.getClassLoader, None, Mode.Test)

  play.api.Play.start(testApp)

  private val testPath = "plugins/test"

  private val testFiles: Array[File] =
    new File(testPath).listFiles.filter { f => f.isFile && f.getName.endsWith(".js") }

  val library = {
    val libraryPath: Seq[URI] =
      Seq(
        Path.fromString(System.getProperty("user.dir")) / "core" / "public" / "js_lib"
      ).map { path =>
          val uri = new URI(path.path)
          // call resolve("") to canonify the path
          uri.isAbsolute ? uri | new File(path.path).toURI.resolve("")
        }.map { uri =>
          // make sure URI always terminates with slash to avoid loading from unintended locations
          uri.toString.endsWith("/") ? uri | new URI(uri + "/")
        }
    import scala.collection.JavaConversions.asJavaIterable
    new UrlModuleSourceProvider(libraryPath, null) // scalastyle:ignore null
  }

  private val scope = new BeyondTestGlobal(library)

  private val pluginPaths = Seq(
    Path.fromString(System.getProperty("user.dir")) / "plugins",
    Path.fromString(System.getProperty("user.dir")) / "plugins" / "test" / "lib"
  )

  private val engine = {
    import scala.concurrent.ExecutionContext.Implicits.global
    new BeyondJavaScriptEngine(scope, pluginPaths = pluginPaths)(global)
  }

  private var totalFailureCount = 0
  private var currentIndex = -1 // to ensure runNextFile starts with index 0 in the first time
  private def currentFile: File = testFiles(currentIndex)

  def runNextFile() {
    currentIndex += 1
    engine.contextFactory.call { cx: Context =>
      try {
        TestReporter.fileStart(currentFile.getName)

        val scriptScope = cx.initStandardObjects(scope)
        cx.compileReader(new FileReader(currentFile), currentFile.getName, 0, null).exec(cx, scriptScope) // scalastyle:ignore null
        cx.compileString("run()", currentFile.getName, 0, null).exec(cx, scriptScope) // scalastyle:ignore null
      } catch {
        case e: EcmaError =>
          TestReporter.fileRuntimeError(currentFile.getName, e.getErrorMessage, e.getScriptStackTrace)
          testEndsWith(1)
        case _: ArrayIndexOutOfBoundsException =>
          TestReporter.testFinished(totalFailureCount)
          testEndsWith { (totalFailureCount > 0) ? 1 | 0 }
        case e: Exception =>
          TestReporter.fileFailedWithException(currentFile.getName, e)
          testEndsWith(1)
      }
      Unit
    }
  }

  def currentFileFinished(failureCount: Int) {
    if (failureCount > 0) {
      TestReporter.fileFailed(currentFile.getName, failureCount)
    } else {
      TestReporter.fileFinished(currentFile.getName)
    }

    totalFailureCount += failureCount
    runNextFile()
  }

  def testEndsWith(code: Int) {
    play.api.Play.stop()
    Runtime.getRuntime.halt(code)
  }

  runNextFile()
}
