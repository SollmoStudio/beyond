package beyond

import java.io.File
import java.lang.ClassLoader
import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean
import java.net.URLClassLoader
import play.api.Mode
import scalax.file.Path

object BeyondRuntime {
  lazy val classPath: String = {
    import play.api.Play.current
    val currentClassLoaderURLs = current.classloader.asInstanceOf[URLClassLoader].getURLs
    val urls = current.mode match {
      case Mode.Dev =>
        val parent: ClassLoader = current.classloader.getParent.getParent
        val parentURLs = parent.asInstanceOf[URLClassLoader].getURLs
        currentClassLoaderURLs ++ parentURLs
      case _ =>
        currentClassLoaderURLs
    }
    urls.mkString(File.pathSeparator)
  }

  val javaPath: String =
    (Path.fromString(System.getProperty("java.home")) / "bin" / "java").path

  val processID: String = {
    val bean: RuntimeMXBean = ManagementFactory.getRuntimeMXBean
    // Get the name representing the running Java virtual machine.
    // It returns something like 6460@AURORA. Where the value
    // before the @ symbol is the PID.
    val jvmName = bean.getName
    // Extract the PID by splitting the string returned by the
    // bean.getName() method.
    jvmName.split("@")(0)
  }
}
