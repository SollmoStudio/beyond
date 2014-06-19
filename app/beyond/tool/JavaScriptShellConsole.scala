package beyond.tool

import beyond.plugin.BeyondJavaScriptEngine
import jline.console.ConsoleReader
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.RhinoException
import org.mozilla.javascript.Script
import org.mozilla.javascript.tools.ToolErrorReporter
import scala.annotation.tailrec

object JavaScriptShellConsole extends App {
  import com.beyondframework.rhino.RhinoConversions._

  val scope = new BeyondShellGlobal

  val engine = new BeyondJavaScriptEngine(scope)

  val prompt = "> "

  val secondaryPrompt = "... "

  val consoleReader = new ConsoleReader

  var hitEOF = false

  case class Source(text: String, lineNumber: Int)

  def readSourceAsCompilableUnit(cx: Context): Source = {
    @tailrec
    def readSourceAsCompilableUnitHelper(builder: StringBuilder, lineNumber: Int, prompt: String = prompt): Source = {
      val newLine = consoleReader.readLine(prompt)
      if (newLine == null) {
        hitEOF = true
        Source(builder.toString(), lineNumber)
      } else {
        builder.append(newLine)
        builder.append("\n")
        val source = builder.toString()
        if (cx.stringIsCompilableUnit(source)) {
          Source(source, lineNumber)
        } else {
          readSourceAsCompilableUnitHelper(builder, lineNumber + 1, secondaryPrompt)
        }
      }
    }

    readSourceAsCompilableUnitHelper(builder = new StringBuilder, lineNumber = 1)
  }

  def compileSource(cx: Context, source: Source): Script = {
    cx.compileString(source.text, "<stdin>", source.lineNumber, null)
  }

  def evaluateScript(cx: Context, script: Script): AnyRef = {
    script.exec(cx, scope)
  }

  engine.contextFactory.call { cx: Context =>
    Console.println(cx.getImplementationVersion)

    while (!hitEOF) {
      val source = readSourceAsCompilableUnit(cx)
      try {
        val script = compileSource(cx, source)
        val result = evaluateScript(cx, script)
        if (result != Context.getUndefinedValue
          && !(result.isInstanceOf[Function] && source.text.trim().startsWith("function"))) {
          Console.println(Context.toString(result))
        }
      } catch {
        case rex: RhinoException =>
          ToolErrorReporter.reportException(cx.getErrorReporter, rex)
      }
    }
    Console.println()
    Console.flush()
    Unit
  }
}

