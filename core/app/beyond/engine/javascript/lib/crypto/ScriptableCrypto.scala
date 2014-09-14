package beyond.engine.javascript.lib.crypto

import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.annotations.{ JSStaticFunction => JSStaticFunctionAnnotation }
import play.api.libs.Crypto

object ScriptableCrypto {
  @JSStaticFunctionAnnotation
  def compareSignedTokens(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): Boolean = {
    val tokenA = args(0).asInstanceOf[String]
    val tokenB = args(1).asInstanceOf[String]

    Crypto.compareSignedTokens(tokenA, tokenB)
  }

  @JSStaticFunctionAnnotation
  def constantTimeEquals(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): Boolean = {
    val a = args(0).asInstanceOf[String]
    val b = args(1).asInstanceOf[String]

    Crypto.constantTimeEquals(a, b)
  }

  @JSStaticFunctionAnnotation
  def decryptAES(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val value = args(0).asInstanceOf[String]

    if (args.isDefinedAt(1)) {
      val privateKey = args(1).asInstanceOf[String]
      Crypto.decryptAES(value, privateKey)
    } else {
      Crypto.decryptAES(value)
    }
  }

  @JSStaticFunctionAnnotation
  def encryptAES(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val value = args(0).asInstanceOf[String]

    if (args.isDefinedAt(1)) {
      val privateKey = args(1).asInstanceOf[String]
      Crypto.encryptAES(value, privateKey)
    } else {
      Crypto.encryptAES(value)
    }
  }

  @JSStaticFunctionAnnotation
  def extractSignedToken(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val token = args(0).asInstanceOf[String]

    Crypto.extractSignedToken(token).getOrElse(null)
  }

  @JSStaticFunctionAnnotation
  def generateSignedToken(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String =
    Crypto.generateSignedToken

  @JSStaticFunctionAnnotation
  def generateToken(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String =
    Crypto.generateToken

  @JSStaticFunctionAnnotation
  def sign(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val message = args(0).asInstanceOf[String]

    if (args.isDefinedAt(1)) {
      val key: Array[Byte] = try {
        args(1).asInstanceOf[String].getBytes
      } catch {
        case e: ClassCastException =>
          ScriptRuntime.getArrayElements(args(1).asInstanceOf[Scriptable]).map(ScriptRuntime.toInt32).map(_.toByte)
      }

      Crypto.sign(message, key)
    } else {
      Crypto.sign(message)
    }
  }

  @JSStaticFunctionAnnotation
  def signToken(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): String = {
    val token = args(0).asInstanceOf[String]

    Crypto.signToken(token)
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableCrypto =
    new ScriptableCrypto
}

class ScriptableCrypto extends ScriptableObject {
  override def getClassName: String = "Crypto"
}
