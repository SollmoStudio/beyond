package beyond.engine.javascript.lib

import beyond.engine.javascript.BeyondContext
import beyond.engine.javascript.BeyondContextFactory
import java.lang.{ Boolean => JavaBoolean }
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Function
import org.mozilla.javascript.RhinoException
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

object ScriptableFuture {
  import com.beyondframework.rhino.RhinoConversions._

  private def executeCallback(contextFactory: ContextFactory, callback: Function, callbackArgs: Array[AnyRef]) = {
    val beyondContextFactory = contextFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    beyondContextFactory.call { context: Context =>
      callback.call(context, scope, scope, callbackArgs)
    }
  }

  def jsFunction_onComplete(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[Function]
    val thisFuture = thisObj.asInstanceOf[ScriptableFuture]
    thisFuture.future.onComplete { futureResult =>
      val callbackArgs: Array[AnyRef] = futureResult match {
        case Success(result) =>
          Array(result, new JavaBoolean(true))
        case Failure(ex: RhinoException) =>
          Array(ex.details(), new JavaBoolean(false))
        case Failure(throwable) =>
          Array(throwable.getMessage, new JavaBoolean(false))
      }
      executeCallback(context.getFactory, callback, callbackArgs)
    }
    thisFuture
  }

  def jsFunction_onSuccess(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[Function]
    val thisFuture = thisObj.asInstanceOf[ScriptableFuture]
    thisFuture.future.onSuccess {
      case result: AnyRef =>
        val callbackArgs: Array[AnyRef] = Array(result)
        executeCallback(context.getFactory, callback, callbackArgs)
    }
    thisFuture
  }

  def jsFunction_onFailure(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[Function]
    val thisFuture = thisObj.asInstanceOf[ScriptableFuture]
    thisFuture.future.onFailure {
      case ex: RhinoException =>
        val callbackArgs: Array[AnyRef] = Array(ex.details)
        executeCallback(context.getFactory, callback, callbackArgs)
      case throwable: Throwable =>
        val callbackArgs: Array[AnyRef] = Array(throwable.getMessage)
        executeCallback(context.getFactory, callback, callbackArgs)
    }
    thisFuture
  }

  def jsConstructor(context: Context, args: Array[AnyRef], constructor: Function, inNewExpr: Boolean): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val scalaFuture = Future {
      val callback = args(0).asInstanceOf[Function]
      val callbackArgs = Array.empty[AnyRef]
      executeCallback(context.getFactory, callback, callbackArgs)
    }
    new ScriptableFuture(scalaFuture)
  }
}

class ScriptableFuture(val future: Future[AnyRef]) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "Future"
}
