package beyond.launcher

import scala.sys.process.Process

class LinuxProcessCommand extends ProcessCommand {
  def terminate(pid: Int): Int = {
    Process(Seq("kill", "-SIGTERM", pid.toString)).run().exitValue()
  }
}
