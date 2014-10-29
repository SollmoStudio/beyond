package beyond.engine.javascript.lib

import beyond.engine.javascript.BeyondContext
import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import com.beyondframework.rhino.ContextOps._
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import java.lang.{ Boolean => JavaBoolean }
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.RhinoException
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSFunction => JSFunctionAnnotation }
import org.mozilla.javascript.annotations.{ JSStaticFunction => JSStaticFunctionAnnotation }
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Success

object ScriptableFuture extends Logging {
  import com.beyondframework.rhino.RhinoConversions._

  private def executeCallback(contextFactory: ContextFactory, callback: JSFunction, callbackArgs: AnyRef*): AnyRef = {
    val beyondContextFactory = contextFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    try {
      beyondContextFactory.call { context: Context =>
        callback.call(context, scope, scope, callbackArgs.toArray)
      }
    } catch {
      case ex: RhinoException =>
        logger.debug(ex.getMessage, ex)
        throw new Exception(ex.details())
    }
  }

  @JSStaticFunctionAnnotation
  def successful(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    val newFuture = Future.successful(args(0))
    ScriptableFuture(context, newFuture)
  }

  @JSStaticFunctionAnnotation
  def sequence(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val futures: Seq[Future[AnyRef]] = args.map(_.asInstanceOf[ScriptableFuture].future).toSeq
    val newFuture: Future[JSArray] = Future.sequence(futures).map(_.toArray)
    ScriptableFuture(context, newFuture)
  }

  @JSStaticFunctionAnnotation
  def firstCompletedOf(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val futures: Seq[Future[AnyRef]] = args.map(_.asInstanceOf[ScriptableFuture].future).toSeq
    val newFuture: Future[AnyRef] = Future.firstCompletedOf(futures)
    ScriptableFuture(context, newFuture)
  }

  @JSFunctionAnnotation
  def onComplete(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[JSFunction]
    val thisFuture = thisObj.asInstanceOf[ScriptableFuture]
    thisFuture.future.onComplete {
      case Success(result) =>
        executeCallback(context.getFactory, callback, result, new JavaBoolean(true))
      case Failure(throwable) =>
        executeCallback(context.getFactory, callback, throwable.getMessage, new JavaBoolean(false))
    }
    thisFuture
  }

  @JSFunctionAnnotation
  def onSuccess(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[JSFunction]
    val thisFuture = thisObj.asInstanceOf[ScriptableFuture]
    thisFuture.future.onSuccess {
      case result =>
        executeCallback(context.getFactory, callback, result)
    }
    thisFuture
  }

  @JSFunctionAnnotation
  def onFailure(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[JSFunction]
    val thisFuture = thisObj.asInstanceOf[ScriptableFuture]
    thisFuture.future.onFailure {
      case throwable: Throwable =>
        executeCallback(context.getFactory, callback, throwable.getMessage)
    }
    thisFuture
  }

  @JSFunctionAnnotation
  def map(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[JSFunction]
    val newFuture = thisObj.asInstanceOf[ScriptableFuture].future.map { result =>
      executeCallback(context.getFactory, callback, result)
    }

    ScriptableFuture(context, newFuture)
  }

  @JSFunctionAnnotation
  def flatMap(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[JSFunction]
    val newFuture = thisObj.asInstanceOf[ScriptableFuture].future.flatMap { result =>
      executeCallback(context.getFactory, callback, result) match {
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

  @JSFunctionAnnotation
  def filter(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[JSFunction]
    val newFuture = thisObj.asInstanceOf[ScriptableFuture].future.filter { result =>
      val filterResult = executeCallback(context.getFactory, callback, result)
      ScriptRuntime.toBoolean(filterResult)
    }

    ScriptableFuture(context, newFuture)
  }

  @JSFunctionAnnotation
  def recover(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[JSFunction]
    val newFuture = thisObj.asInstanceOf[ScriptableFuture].future.recover {
      case throwable: Throwable =>
        executeCallback(context.getFactory, callback, throwable.getMessage)
    }

    ScriptableFuture(context, newFuture)
  }

  @JSFunctionAnnotation
  def transform(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val thisFuture = thisObj.asInstanceOf[ScriptableFuture]
    val callbackOnSuccess = args(0).asInstanceOf[JSFunction]
    val callbackOnFailure = args(1).asInstanceOf[JSFunction]

    val promise = Promise[AnyRef]()
    thisFuture.future.onComplete {
      case Success(result) =>
        promise.success(executeCallback(context.getFactory, callbackOnSuccess, result))
      case Failure(throwable) =>
        promise.failure(new Exception(executeCallback(context.getFactory, callbackOnFailure, throwable.getMessage).asInstanceOf[String]))
    }

    ScriptableFuture(context, promise.future)
  }

  @JSFunctionAnnotation
  def andThen(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val callback = args(0).asInstanceOf[JSFunction]
    val thisFuture = thisObj.asInstanceOf[ScriptableFuture]
    val newFuture = thisFuture.future.flatMap { _ =>
      executeCallback(context.getFactory, callback) match {
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

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val newFuture = args(0) match {
      case callback: JSFunction =>
        Future {
          executeCallback(context.getFactory, callback)
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
    context.newObject(scope, "Future", future).asInstanceOf[ScriptableFuture]
  }
}

class ScriptableFuture(val future: Future[AnyRef]) extends ScriptableObject {
  def this() = this(null)

  override def getClassName: String = "Future"
}
