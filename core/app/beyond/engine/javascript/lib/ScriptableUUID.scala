package beyond.engine.javascript.lib

import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import com.beyondframework.rhino.ContextOps._
import java.sql.Timestamp
import java.util.Date
import java.util.UUID
import java.util.Random
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSStaticFunction => JSStaticFunctionAnnotation }

object ScriptableUUID {
  private val defaultNode: Long = (new Random).nextLong & 0xFFFFFFFFFFFFL
  private val version: Long = 1
  private val variant: Long = 0x8

  private var lastMsecs: Long = 0
  private var lastNsecs: Long = 0
  private var currentClockSequence: Long = (new Random).nextLong & 0x3FFFL

  // Milliseconds between Linux epoch(January 1, 1970, 00:00:00) and Gregorian epoch(15 October 1582, 00:00:00)
  private def convertToGregorian(ts: Long): Long = ts + 12219292800000L

  @JSStaticFunctionAnnotation
  def v1(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val options = OptionParser.parse(args, 0)

    val node: Long = options.getLong("node").getOrElse(defaultNode)
    val timestamp = new Timestamp((new Date).getTime)
    val msecs: Long = convertToGregorian(options.getLong("msecs").getOrElse(timestamp.getTime))
    val nsecs: Long = options.getLong("nsecs").getOrElse(timestamp.getNanos / 100000)

    val clockSequence = options.getLong("clockseq").getOrElse {
      // Bump clockseq on clock regression
      val dt = (msecs - lastMsecs) * 10000 + (nsecs - lastNsecs)
      if (dt < 0) {
        currentClockSequence += 1
      }
      currentClockSequence
    }

    // Set current values
    lastMsecs = msecs
    lastNsecs = nsecs
    currentClockSequence = clockSequence

    val mostSigBits: Long = {
      val timeLow = (msecs * 10000 + nsecs) % 0x100000000L
      val timeMidHigh = (msecs * 10000 / 0x10000000L) & 0xFFFFFFFL

      (timeLow << 32) + ((timeMidHigh & 0xFFFFL) << 16) + (version << 12) + (timeMidHigh >> 16)
    }
    val leastSigBits: Long = {
      val variantAndClockSequence = (variant << 12) + (clockSequence & 0x3FFF)

      (variantAndClockSequence << 48) + node
    }

    (new UUID(mostSigBits, leastSigBits)).toString
  }

  @JSStaticFunctionAnnotation
  def v4(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String =
    UUID.randomUUID().toString

  @JSStaticFunctionAnnotation
  def parse(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableUUID = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global

    val name = args(0).asInstanceOf[String]
    val uuid = UUID.fromString(name)
    context.newObject(scope, "UUID", uuid).asInstanceOf[ScriptableUUID]
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableUUID =
    new ScriptableUUID
}

class ScriptableUUID(val uuid: UUID) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "UUID"
}

