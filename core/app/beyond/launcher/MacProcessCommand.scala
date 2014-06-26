package beyond.launcher

import scala.sys.process.Process

class MacProcessCommand extends ProcessCommand {
  def terminate(pid: Int): Int = {
    Process(Seq("kill", "-TERM", pid.toString)).run().exitValue()
  }
}
