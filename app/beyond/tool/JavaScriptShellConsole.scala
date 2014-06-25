package beyond.tool

import beyond.plugin.BeyondJavaScriptEngine
import beyond.plugin.JavaScriptTimerProvider
import java.util
import java.util.Timer
import java.util.TimerTask
import jline.console.ConsoleReader
import jline.console.completer.Completer
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.RhinoException
import org.mozilla.javascript.Script
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.tools.ToolErrorReporter
import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scalax.file.Path

class FlexibleCompletor(global: Scriptable) extends Completer {
  private object DottedName {
    def apply(buffer: String, cursor: Int): DottedName = {
      val m = buffer.lastIndexWhere(c => !Character.isJavaIdentifierPart(c) && c != '.', cursor - 1)
      val name = buffer.slice(m + 1, cursor)
      val names = name.split("\\.", -1)
      new DottedName(names)
    }
  }

  private class DottedName(val names: Seq[String]) {
    val lastPart = names.last
  }

  private def getScriptableFromDottedName(dottedName: DottedName): Option[Scriptable] = {
    dottedName.names.dropRight(1).foldLeft(Option(global)) {
      (optionObj, name) =>
        optionObj.flatMap { obj =>
          obj.get(name, global) match {
            case s: Scriptable => Some(s)
            case _ => None
          }
        }
    }
  }

  private def getIdsWithPrefix(obj: Scriptable, prefix: String): Seq[String] = {
    val ids = obj match {
      case s: ScriptableObject => s.getAllIds
      case _ => obj.getIds
    }
    ids.filter {
      case idString: String => idString.startsWith(prefix)
      case _ => false
    }.toSeq.asInstanceOf[Seq[String]]
  }

  override def complete(buffer: String, cursor: Int, candidates: util.List[CharSequence]): Int = {
    // Starting from "cursor" at the end of the buffer, look backward
    // and collect a list of identifiers separated by (possibly zero)
    // dots. Then look up each identifier in turn until getting to the
    // last, presumably incomplete fragment. Then enumerate all the
    // properties of the last object and find any that have the
    // fragment as a prefix and return those for autocompletion.

    val dottedName = DottedName(buffer, cursor) // e.g., console.lo
    val obj: Option[Scriptable] = getScriptableFromDottedName(dottedName)
    obj.fold(buffer.length) { obj =>
      import scala.collection.JavaConverters._

      val lastPart = dottedName.lastPart
      val ids = getIdsWithPrefix(obj, prefix = lastPart)
      candidates.addAll(ids.asJava)
      buffer.length() - lastPart.length()
    }
  }
}

object JavaScriptShellConsole extends App with JavaScriptTimerProvider {
  import com.beyondframework.rhino.RhinoConversions._

  val scope = new BeyondShellGlobal

  val pluginPaths = Seq(
    Path.fromString(System.getProperty("user.dir")) / "plugins",
    Path.fromString(System.getProperty("user.dir")) / "plugins" / "lib"
  )

  val engine = new BeyondJavaScriptEngine(scope, pluginPaths = pluginPaths.map(_.path))

  val errorReporter = new ToolErrorReporter(false, System.err)
  engine.contextFactory.setErrorReporter(errorReporter)

  val prompt = "> "

  val secondaryPrompt = "... "

  val consoleReader = new ConsoleReader
  consoleReader.addCompleter(new FlexibleCompletor(scope))

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

  private def createTimerTask(callback: Function, callbackArgs: Array[AnyRef]) =
    new TimerTask() {
      override def run() {
        engine.contextFactory.call { cx: Context =>
          callback.call(cx, scope, scope, callbackArgs)
        }
      }
    }

  override def setTimeout(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[AnyRef] =
    if (args.length < 2) {
      Failure(new IllegalArgumentException("args.are.not.enough"))
    } else if (!args(0).isInstanceOf[Function]) {
      Failure(new IllegalArgumentException("first.arg.is.not.function"))
    } else {
      val timer = new Timer()
      val callback = args(0).asInstanceOf[Function]
      val callbackArgs = args.drop(2)
      val delay = Context.toNumber(args(1)).toLong

      timer.schedule(createTimerTask(callback, callbackArgs), delay)
      Success(timer)
    }

  override def clearTimeout(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[Unit] =
    if (args.length == 0) {
      Failure(new IllegalArgumentException("args.length.is.zero"))
    } else if (!args(0).isInstanceOf[Timer]) {
      Failure(new IllegalArgumentException("first.arg.is.not.timeout.object"))
    } else {
      val timer = args(0).asInstanceOf[Timer]
      timer.cancel()
      Success(Unit)
    }

  override def setInterval(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[AnyRef] =
    if (args.length < 2) {
      Failure(new IllegalArgumentException("args.are.not.enough"))
    } else if (!args(0).isInstanceOf[Function]) {
      Failure(new IllegalArgumentException("first.arg.is.not.function"))
    } else {
      val timer = new Timer()
      val callback = args(0).asInstanceOf[Function]
      val callbackArgs = args.drop(2)
      val delay = Context.toNumber(args(1)).toLong

      timer.schedule(createTimerTask(callback, callbackArgs), delay, delay)
      Success(timer)
    }

  override def clearInterval(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[Unit] =
    if (args.length == 0) {
      Failure(new IllegalArgumentException("args.length.is.zero"))
    } else if (!args(0).isInstanceOf[Timer]) {
      Failure(new IllegalArgumentException("first.arg.is.not.timeout.object"))
    } else {
      val timer = args(0).asInstanceOf[Timer]
      timer.cancel()
      Success(Unit)
    }
}

