package beyond.launcher

import scala.sys.process.Process

class WindowsProcessCommand extends ProcessCommand {
  def terminate(pid: Int): Int = {
    Process(Seq("taskkill", "/F", "/PID", pid.toString)).run().exitValue()
  }
}
