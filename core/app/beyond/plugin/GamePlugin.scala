package beyond.plugin

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Cancellable
import akka.actor.Props
import akka.routing.RoundRobinRouter
import beyond.engine.javascript.BeyondJavaScriptEngine
import beyond.engine.javascript.provider.JavaScriptConsoleProvider
import beyond.engine.javascript.provider.JavaScriptTimerProvider
import beyond.plugin.GamePlugin.InvokeFunction
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import play.api.mvc.Request
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object GamePlugin {
  val Name: String = "gamePlugin"

  case class Handle[A](request: Request[A])
  case class InvokeFunction(function: Function, args: Array[AnyRef])

  private[GamePlugin] class JavaScriptConsole extends JavaScriptConsoleProvider with Logging {
    override def log(message: String): Unit = logger.info(message)
    override def info(message: String): Unit = logger.info(message)
    override def warn(message: String): Unit = logger.warn(message)
    override def debug(message: String): Unit = logger.debug(message)
    override def error(message: String): Unit = logger.error(message)
  }
}

class NoHandlerFunctionFoundException extends Exception

// FIXME: Handle script errors.
class GamePlugin(filename: String) extends Actor with ActorLogging with JavaScriptTimerProvider {
  import com.beyondframework.rhino.RhinoConversions._

  private val engine = new BeyondJavaScriptEngine(timer = this)(context.dispatcher)

  private val handler: Function = engine.contextFactory.call { cx: Context =>
    val exports = engine.loadMain(filename)
    // FIXME: Don't hardcode the name of handler function.
    val handler = exports.get("handle", exports)
    handler match {
      case _: Function =>
        handler
      case _ /* Scriptable.NOT_FOUND */ =>
        log.error("No handler function is found")
        throw new NoHandlerFunctionFoundException
    }
  }.asInstanceOf[Function]

  private val workerActor = {
    val numProcessors = Runtime.getRuntime.availableProcessors()
    val router = RoundRobinRouter(nrOfInstances = numProcessors)
    val props = Props(classOf[GamePluginWorker], engine, handler).withRouter(router)
    context.actorOf(props, name = "gamePluginWorker")
  }

  override def receive: Receive = {
    case msg => workerActor forward msg
  }

  override def setTimeout(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[AnyRef] =
    if (args.length < 2) {
      Failure(new IllegalArgumentException("args.are.not.enough"))
    } else if (!args(0).isInstanceOf[Function]) {
      Failure(new IllegalArgumentException("first.arg.is.not.function"))
    } else {
      val callback = args(0).asInstanceOf[Function]
      val callbackArgs = args.drop(2)
      val delay = Context.toNumber(args(1)).millis

      import context.dispatcher
      Success(context.system.scheduler.scheduleOnce(delay, self, InvokeFunction(callback, callbackArgs)))
    }

  override def setInterval(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[AnyRef] =
    if (args.length < 2) {
      Failure(new IllegalArgumentException("args.are.not.enough"))
    } else if (!args(0).isInstanceOf[Function]) {
      Failure(new IllegalArgumentException("first.arg.is.not.function"))
    } else {
      val callback = args(0).asInstanceOf[Function]
      val callbackArgs = args.drop(2)
      val delay = Context.toNumber(args(1)).millis

      import context.dispatcher
      Success(context.system.scheduler.schedule(initialDelay = delay, interval = delay, self, InvokeFunction(callback, callbackArgs)))
    }

  override def clearTimeout(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[Unit] =
    if (args.length == 0) {
      Failure(new IllegalArgumentException("args.length.is.zero"))
    } else if (!args(0).isInstanceOf[Cancellable]) {
      Failure(new IllegalArgumentException("first.arg.is.not.timeout.object"))
    } else {
      val id = args(0).asInstanceOf[Cancellable]
      id.cancel()
      Success(Unit)
    }

  override def clearInterval(thisObj: Scriptable, args: Array[AnyRef], funObj: Function): Try[Unit] =
    if (args.length == 0) {
      Failure(new IllegalArgumentException("args.length.is.zero"))
    } else if (!args(0).isInstanceOf[Cancellable]) {
      Failure(new IllegalArgumentException("first.arg.is.not.timeout.object"))
    } else {
      val id = args(0).asInstanceOf[Cancellable]
      id.cancel()
      Success(Unit)
    }
}

