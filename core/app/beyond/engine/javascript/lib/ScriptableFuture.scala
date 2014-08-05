package beyond.engine.javascript.lib

import beyond.engine.javascript.BeyondContext
import beyond.engine.javascript.BeyondContextFactory
import java.lang.{ Boolean => JavaBoolean }
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Function
import org.mozilla.javascript.RhinoException
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import scala.concurrent.Future
import scala.concurrent.Promise
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

  def jsStaticFunction_sequence(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val futures: Seq[Future[AnyRef]] = args.map(_.asInstanceOf[ScriptableFuture].future).toSeq
    val newFuture: Future[Array[AnyRef]] = Future.sequence(futures).map(_.toArray)
    ScriptableFuture(context, newFuture)
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

  def jsFunction_map(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[Function]
    val newFuture = thisObj.asInstanceOf[ScriptableFuture].future.map { result =>
      val callbackArgs: Array[AnyRef] = Array(result)
      executeCallback(context.getFactory, callback, callbackArgs)
    }

    ScriptableFuture(context, newFuture)
  }

  def jsFunction_flatMap(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[Function]
    val newFuture = thisObj.asInstanceOf[ScriptableFuture].future.flatMap { result =>
      val callbackArgs: Array[AnyRef] = Array(result)
      executeCallback(context.getFactory, callback, callbackArgs) match {
        case futureByCallback: ScriptableFuture =>
          val promise = Promise[AnyRef]()
          futureByCallback.future.onComplete {
            case Success(success) =>
              promise.success(success)
            case Failure(throwable) =>
              promise.failure(throwable)
          }
          promise.future
        case _ =>
          throw new Exception("result.of.callback.is.not.future")
      }
    }

    ScriptableFuture(context, newFuture)
  }

  def jsFunction_filter(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[Function]
    val newFuture = thisObj.asInstanceOf[ScriptableFuture].future.filter { result =>
      val callbackArgs: Array[AnyRef] = Array(result)
      val filterResult = executeCallback(context.getFactory, callback, callbackArgs)
      ScriptRuntime.toBoolean(filterResult)
    }

    ScriptableFuture(context, newFuture)
  }

  def jsFunction_recover(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[Function]
    val newFuture = thisObj.asInstanceOf[ScriptableFuture].future.recover {
      case exception: RhinoException =>
        val callbackArgs: Array[AnyRef] = Array(exception.details)
        executeCallback(context.getFactory, callback, callbackArgs)
      case throwable: Throwable =>
        val callbackArgs: Array[AnyRef] = Array(throwable.getMessage)
        executeCallback(context.getFactory, callback, callbackArgs)
    }

    ScriptableFuture(context, newFuture)
  }

  def jsFunction_transform(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val thisFuture = thisObj.asInstanceOf[ScriptableFuture]
    val promise = Promise[AnyRef]()
    thisFuture.future.onComplete {
      case Success(result) =>
        val callbackArgs: Array[AnyRef] = Array(result)
        val callbackOnSuccess = args(0).asInstanceOf[Function]
        promise.success(executeCallback(context.getFactory, callbackOnSuccess, callbackArgs))
      case Failure(exception: RhinoException) =>
        val callbackArgs: Array[AnyRef] = Array(exception.details)
        val callbackOnFailure = args(1).asInstanceOf[Function]
        promise.failure(new Exception(executeCallback(context.getFactory, callbackOnFailure, callbackArgs).asInstanceOf[String]))
      case Failure(throwable) =>
        val callbackArgs: Array[AnyRef] = Array(throwable.getMessage)
        val callbackOnFailure = args(1).asInstanceOf[Function]
        promise.failure(new Exception(executeCallback(context.getFactory, callbackOnFailure, callbackArgs).asInstanceOf[String]))
    }

    ScriptableFuture(context, promise.future)
  }

  def jsConstructor(context: Context, args: Array[AnyRef], constructor: Function, inNewExpr: Boolean): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val newFuture = args(0) match {
      case callback: Function =>
        Future {
          val callbackArgs = Array.empty[AnyRef]
          executeCallback(context.getFactory, callback, callbackArgs)
        }
      case future: Future[_] =>
        // Cannot check Future[AnyRef] because of type erasure.
        future.asInstanceOf[Future[AnyRef]]
      case _ =>
        throw new IllegalArgumentException("type.is.not.matched")
    }
    new ScriptableFuture(newFuture)
  }

  private[lib] def apply(context: Context, future: Future[_]): ScriptableFuture = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    val args: Array[AnyRef] = Array(future)
    context.newObject(scope, "Future", args).asInstanceOf[ScriptableFuture]
  }
}

class ScriptableFuture(val future: Future[AnyRef]) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "Future"
}
