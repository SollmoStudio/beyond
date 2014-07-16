import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.IOException
import java.io.LineNumberReader
import java.io.OutputStreamWriter
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSConstructor
import org.mozilla.javascript.annotations.JSFunction
import org.mozilla.javascript.annotations.JSGetter

/**
 * Define a simple JavaScript File object.
 *
 * This isn't intended to be any sort of definitive attempt at a
 * standard File object for JavaScript, but instead is an example
 * of a more involved definition of a host object.
 *
 * Example of use of the File object:
 * <pre>
 * js> defineClass("File")
 * js> file = new File("myfile.txt");
 * [object File]
 * js> file.writeLine("one");                       <i>only now is file actually opened</i>
 * js> file.writeLine("two");
 * js> file.writeLine("thr", "ee");
 * js> file.close();                                <i>must close file before we can reopen for reading</i>
 * js> var a = file.readLines();                    <i>creates and fills an array with the contents of the file</i>
 * js> a;
 * one,two,three
 * js>
 * </pre>
 *
 * File errors or end-of-file signaled by thrown Java exceptions will
 * be wrapped as JavaScript exceptions when called from JavaScript,
 * and may be caught within JavaScript.
 *
 * The zero-parameter constructor.
 *
 * When Context.defineClass is called with this class, it will
 * construct File.prototype using this constructor.
 */
class File extends ScriptableObject {
  override def getClassName: String = "File"

  /**
   * Get the name of the file.
   *
   * Used to define the "name" property.
   */
  @JSGetter
  def getName: String = name

  /**
   * Read the remaining lines in the file and return them in an array.
   *
   * Implements a JavaScript function.<p>
   *
   * This is a good example of creating a new array and setting
   * elements in that array.
   */
  @JSFunction
  def readLines: AnyRef = {
    val iterator = Iterator.continually(readLine()).takeWhile(_ != null)
    val lines: Array[String] = iterator.toArray
    val scope = ScriptableObject.getTopLevelScope(this)
    val cx = Context.getCurrentContext
    cx.newObject(scope, "Array", lines.asInstanceOf[Array[AnyRef]])
  }

  /**
   * Read a line.
   *
   * Implements a JavaScript function.
   */
  @JSFunction
  def readLine(): String = getReader.readLine()

  /**
   * Read a character.
   */
  @JSFunction
  def readChar(): String = {
    val i = getReader.read()
    if (i == -1) {
      null
    } else {
      val charArray = new Array[Char](i)
      new String(charArray)
    }
  }

  @JSGetter
  def getLineNumber: Int = getReader.getLineNumber

  @JSFunction
  def close() {
    if (reader != null) {
      reader.close()
      reader = null
    } else if (writer != null) {
      writer.close()
      writer = null
    }
  }

  /**
   * Finalizer.
   *
   * Close the file when this object is collected.
   */
  override def finalize() {
    try {
      close()
    } catch {
      case _: IOException =>
    }
  }

  /**
   * Get the Java reader.
   */
  @JSFunction("getReader")
  def getJSReader: AnyRef = {
    if (reader == null) {
      null
    } else {
      // Here we use toObject() to "wrap" the BufferedReader object
      // in a Scriptable object so that it can be manipulated by
      // JavaScript.
      val parent: Scriptable = ScriptableObject.getTopLevelScope(this)
      Context.javaToJS(reader, parent)
    }
  }

  /**
   * Get the Java writer.
   *
   * @see File#getReader
   */
  @JSFunction
  def getWriter: AnyRef = {
    if (writer == null) {
      null
    } else {
      val parent: Scriptable = ScriptableObject.getTopLevelScope(this)
      Context.javaToJS(writer, parent)
    }
  }

  /**
   * Get the reader, checking that we're not already writing this file.
   */
  private def getReader: LineNumberReader = {
    if (writer != null) {
      throw Context.reportRuntimeError("already writing file \"" + name + "\"")
    }
    if (reader == null) {
      val underlyingReader = if (file == null) {
        new InputStreamReader(System.in)
      } else {
        new FileReader(file)
      }
      reader = new LineNumberReader(underlyingReader)
    }
    reader
  }

  private var name: String = _
  private var file: java.io.File = _
  private var reader: LineNumberReader = _
  private var writer: BufferedWriter = _
}

object File {
  /**
   * The Scala method defining the JavaScript File constructor.
   *
   * If the constructor has one or more arguments, and the
   * first argument is not undefined, the argument is converted
   * to a string as used as the filename.<p>
   *
   * Otherwise System.in or System.out is assumed as appropriate
   * to the use.
   */
  @JSConstructor
  def jsConstructor(cx: Context, args: Array[AnyRef], ctorObj: Function, inNewExpr: Boolean): File = {
    val result: File = new File
    if (args.length == 0 || args(0) == Context.getUndefinedValue) {
      result.name = ""
      result.file = null
    } else {
      result.name = Context.toString(args(0))
      result.file = new java.io.File(result.name)
    }
    result
  }

  /**
   * Write strings.
   *
   * Implements a JavaScript function.
   *
   * This function takes a variable number of arguments, converts
   * each argument to a string, and writes that string to the file.
   */
  @JSFunction
  def write(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function) = {
    write0(thisObj, args, eol = false)
  }

  /**
   * Write strings and a newline.
   *
   * Implements a JavaScript function.
   */
  @JSFunction
  def writeLine(cx: Context, thisObj: Scriptable, args: Array[AnyRef], funObj: Function) = {
    write0(thisObj, args, eol = true)
  }

  /**
   * Perform the instanceof check and return the downcasted File object.
   *
   * This is necessary since methods may reside in the File.prototype
   * object and scripts can dynamically alter prototype chains. For example:
   * <pre>
   * js> defineClass("File");
   * js> o = {};
   * [object Object]
   * js> o.__proto__ = File.prototype;
   * [object File]
   * js> o.write("hi");
   * js: called on incompatible object
   * </pre>
   * The runtime will take care of such checks when non-static Java methods
   * are defined as JavaScript functions.
   */
  private def checkInstance(obj: Scriptable): File = {
    if (obj == null || !obj.isInstanceOf[File]) {
      throw Context.reportRuntimeError("called on incompatible object")
    }
    obj.asInstanceOf[File]
  }

  /**
   * Perform the guts of write and writeLine.
   *
   * Since the two functions differ only in whether they write a
   * newline character, move the code into a common subroutine.
   */
  private def write0(thisObj: Scriptable, args: Array[AnyRef], eol: Boolean) = {
    val thisFile: File = checkInstance(thisObj)
    if (thisFile.reader != null) {
      throw Context.reportRuntimeError("already writing file \"" + thisFile.name + "\"")
    }
    if (thisFile.writer == null) {
      val underlyingWriter = if (thisFile.file == null) {
        new OutputStreamWriter(System.out)
      } else {
        new FileWriter(thisFile.file)
      }
      thisFile.writer = new BufferedWriter(underlyingWriter)
    }
    args.foreach { arg =>
      val s = Context.toString(arg)
      thisFile.writer.write(s, 0, s.length())
    }
    if (eol) {
      thisFile.writer.newLine()
    }
  }
}

