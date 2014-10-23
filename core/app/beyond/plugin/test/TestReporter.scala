package beyond.plugin.test

import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSStaticFunction

object TestReporter {
  object Color {
    private val ansiReset = "\u001b[0m"
    private val ansiRed = "\u001b[31m"
    private val ansiGreen = "\u001b[32m"
    private val ansiPurple = "\u001b[35m"
    private val ansiCyan = "\u001b[36m"
    private val ansiGray = "\u001b[90m"

    def red(str: String): String = s"$ansiRed$str$ansiReset"
    def green(str: String): String = s"$ansiGreen$str$ansiReset"
    def purple(str: String): String = s"$ansiPurple$str$ansiReset"
    def cyan(str: String): String = s"$ansiCyan$str$ansiReset"
    def gray(str: String): String = s"$ansiGray$str$ansiReset"
  }

  def fileStart(name: String) {
    println(s"\n[plugin-test] Starting '${Color.cyan(name)}'...")
  }

  def fileRuntimeError(name: String, message: String, stack: String) {
    println(s"\n[plugin-test] Runtime Error in '${Color.cyan(name)}'")
    println(Color.red(message))
    println(Color.gray(stack) + "\n")
  }

  def fileFinished(name: String) {
    println(s"\n[plugin-test] Finished '${Color.cyan(name)}'")
  }

  def fileFailedWithException(name: String, exception: Exception) {
    println("\n[plugin-test] Failed with an " + Color.red("exception"))
    println(Color.gray(exception.toString))
  }

  def fileFailed(name: String, failureCount: Int) {
    println(s"\n[plugin-test] Failed '${Color.cyan(name)}' with "
      + Color.red(failureCount + " error(s)"))
  }

  def testFinished(totalFailureCount: Int) {
    if (totalFailureCount > 0) {
      println(Color.red(s"\nTest failed with $totalFailureCount error(s)\n"))
    } else {
      println(Color.green("\nTest finished\n"))
    }
  }

  private def printWithIndent(depth: Int, str: String) {
    println("  " * depth + str)
  }

  @JSStaticFunction
  def taskGroupStart(depth: Int, name: String) {
    if (depth > 0) {
      if (depth == 1) {
        println()
      }
      printWithIndent(depth, name)
    }
  }

  @JSStaticFunction
  def taskGroupFinished(depth: Int, name: String, successCount: Int, failureCount: Int) {
    if (depth == 1) {
      println()
      if (successCount > 0) {
        printWithIndent(depth + 1, Color.green(successCount.toString + " passing"))
      }
      if (failureCount > 0) {
        printWithIndent(depth + 1, Color.red(failureCount.toString + " failing"))
      }
    }
  }

  @JSStaticFunction
  def taskFinished(depth: Int, name: String) {
    printWithIndent(depth + 1, Color.green("✓ ") + Color.gray(name))
  }

  private def arrayizeStack(stack: String): Array[String] = stack.trim.split("\n").map(_.trim)

  @JSStaticFunction
  def taskFailed(depth: Int, name: String, errorName: String, errorMessage: String, errorStack: String) {
    printWithIndent(depth + 1, Color.red("✗ " + name))
    printWithIndent(depth + 2, Color.purple(errorName + ": " + errorMessage))
    arrayizeStack(errorStack).foreach { stack: String =>
      printWithIndent(depth + 3, Color.gray(stack))
    }
  }

  @JSStaticFunction
  def rootTaskFinished(failureCount: Int) {
    TestRunner.currentFileFinished(failureCount)
  }
}

class TestReporter extends ScriptableObject {
  override def getClassName: String = "TestReporter"
}
