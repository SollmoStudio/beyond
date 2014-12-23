package beyond.engine.javascript.lib

import beyond.config.BeyondConfiguration
import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import com.beyondframework.rhino.ContextOps._
import java.nio.ByteBuffer
import java.nio.charset.Charset
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.annotations.JSGetter
import org.mozilla.javascript.annotations.{ JSStaticFunction => JSStaticFunctionAnnotation }
import org.mozilla.javascript.annotations.{ JSFunction => JSFunctionAnnotation }

object ScriptableBuffer {
  private val defaultCharset: Charset = Charset.forName(BeyondConfiguration.encoding)

  private def optionalArg[T](args: JSArray, idx: Int): Option[T] =
    if (args.isDefinedAt(idx)) {
      Some(args(idx).asInstanceOf[T])
    } else {
      None
    }

  private def optionalArgInt(args: JSArray, idx: Int): Option[Int] =
    optionalArg[AnyRef](args, idx).map(ScriptRuntime.toInt32)

  private def optionalArgCharset(args: JSArray, idx: Int): Option[Charset] =
    optionalArg[String](args, idx).map(Charset.forName)

  private[lib] def apply(context: Context, internal: ByteBuffer): ScriptableBuffer = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    context.newObject(scope, "Buffer", internal).asInstanceOf[ScriptableBuffer]
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableBuffer = {
    val internal = args(0) match {
      case buffer: ByteBuffer =>
        buffer
      case str: String =>
        val encoding = optionalArgCharset(args, 1).getOrElse(defaultCharset)
        ByteBuffer.wrap(str.getBytes(encoding))
      case array: Scriptable =>
        ByteBuffer.wrap(ScriptRuntime.getArrayElements(array).map(ScriptRuntime.toInt32(_).toByte))
      case number: AnyRef => try {
        val size = ScriptRuntime.toInt32(number)
        ByteBuffer.allocate(size)
      } catch {
        case _: Exception => throw new IllegalArgumentException("type.is.not.matched")
      }
    }

    new ScriptableBuffer(internal)
  }

  @JSStaticFunctionAnnotation
  def isSupportedEncoding(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): Boolean = {
    val encoding = args(0).asInstanceOf[String]

    Charset.isSupported(encoding)
  }

  @JSFunctionAnnotation
  def write(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): Int = {
    val thisBuffer = thisObj.asInstanceOf[ScriptableBuffer]

    val str = args(0).asInstanceOf[String]
    val offset = optionalArgInt(args, 1).getOrElse(0)
    val length = optionalArgInt(args, 2).getOrElse(thisBuffer.getLength - offset)
    val encoding = optionalArgCharset(args, 3).getOrElse(defaultCharset)

    val bytes = str.getBytes(encoding)
    val lengthToWrite = bytes.length min length

    thisBuffer.putBytes(bytes, offset, lengthToWrite)

    lengthToWrite
  }

  // Cannot use the name toString because a static forwarder is not generated
  // when a method with the same name exists in the class and companion object.
  @JSFunctionAnnotation("toString")
  def jsToString(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val thisBuffer = thisObj.asInstanceOf[ScriptableBuffer]

    val encoding = optionalArgCharset(args, 0).getOrElse(defaultCharset)
    val start = optionalArgInt(args, 1).getOrElse(0)
    val end = optionalArgInt(args, 2).getOrElse(thisBuffer.getLength)
    val size = end - start

    new String(thisBuffer.getBytes(start, size), encoding)
  }

  @JSFunctionAnnotation
  def toJSON(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): Scriptable = {
    val thisBuffer = thisObj.asInstanceOf[ScriptableBuffer]

    val bytes = thisBuffer.getBytes(0, thisBuffer.getLength)

    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    context.newObject(scope, "Array", bytes.map(byte => new Integer(byte & 0xFF)))
  }

  @JSStaticFunctionAnnotation
  def byteLength(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): Int = {
    val str = args(0).asInstanceOf[String]
    val encoding = optionalArgCharset(args, 1).getOrElse(defaultCharset)

    str.getBytes(encoding).length
  }

  @JSStaticFunctionAnnotation
  def concat(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableBuffer = {
    val buffers = ScriptRuntime.getArrayElements(args(0).asInstanceOf[Scriptable]).map(_.asInstanceOf[ScriptableBuffer])
    val totalLength = optionalArgInt(args, 1).getOrElse {
      buffers.foldLeft(0) { (result, buffer) => result + buffer.getLength }
    }

    val result = ScriptableBuffer(context, ByteBuffer.allocate(totalLength))

    buffers.foreach { buffer =>
      val length = result.internal.remaining min buffer.getLength
      result.internal.put(buffer.internal.array, 0, length)
      result.internal.remaining > 0 // Continue condition
    }

    result
  }

  @JSFunctionAnnotation
  def copyTo(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction) {
    val thisBuffer = thisObj.asInstanceOf[ScriptableBuffer]

    val targetBuffer = args(0).asInstanceOf[ScriptableBuffer]
    val targetStart = optionalArgInt(args, 1).getOrElse(0)
    val sourceStart = optionalArgInt(args, 2).getOrElse(0)
    val sourceEnd = optionalArgInt(args, 3).getOrElse(thisBuffer.getLength)

    val length = sourceEnd - sourceStart
    val bytes = thisBuffer.getBytes(sourceStart, length)
    targetBuffer.putBytes(bytes, targetStart, length)
  }

  @JSFunctionAnnotation
  def slice(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableBuffer = {
    val thisBuffer = thisObj.asInstanceOf[ScriptableBuffer]

    val start = optionalArgInt(args, 0).getOrElse(0)
    val end = optionalArgInt(args, 1).getOrElse(thisBuffer.getLength)

    ScriptableBuffer(context, ByteBuffer.wrap(thisBuffer.internal.array, start, end - start).slice)
  }

  @JSFunctionAnnotation
  def fill(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction) {
    val thisBuffer = thisObj.asInstanceOf[ScriptableBuffer]

    val value = ScriptRuntime.toInt32(args(0)).toByte
    val offset = optionalArgInt(args, 1).getOrElse(0)
    val end = optionalArgInt(args, 2).getOrElse(thisBuffer.getLength)

    thisBuffer.internal.position(offset)
    while (thisBuffer.internal.position < end)
      thisBuffer.internal.put(value)
    thisBuffer.resetPosition()
  }
}

class ScriptableBuffer(val internal: ByteBuffer) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "Buffer"

  @JSGetter
  def getLength: Int = internal.capacity

  override def get(index: Int, start: Scriptable): AnyRef = {
    try {
      new Integer(internal.get(index) & 0xFF)
    } catch {
      case _: IndexOutOfBoundsException => null // scalastyle:ignore null
    }
  }

  override def put(index: Int, start: Scriptable, value: Any) {
    internal.put(index, ScriptRuntime.toInt32(value).toByte)
  }

  private def resetPosition() {
    internal.position(0)
  }

  private def putBytes(src: Array[Byte], offset: Int, length: Int) {
    internal.position(offset)
    internal.put(src, 0, length)
    resetPosition()
  }

  private def getBytes(offset: Int, length: Int): Array[Byte] = {
    val dst = new Array[Byte](length)

    internal.position(offset)
    internal.get(dst, 0, length)
    resetPosition()

    dst
  }
}
