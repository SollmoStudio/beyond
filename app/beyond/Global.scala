package beyond

import akka.actor.ActorRef
import akka.actor.Props
import play.api.Application
import play.api.libs.concurrent.Akka
import play.api.Mode
import play.api.Play
import play.api.libs.concurrent.Promise
import play.api.mvc.Filter
import play.api.mvc.RequestHeader
import play.api.mvc.Results.InternalServerError
import play.api.mvc.Results.NotFound
import play.api.mvc.SimpleResult
import play.api.mvc.WithFilters
import reactivemongo.api.MongoConnection
import reactivemongo.api.MongoDriver
import scala.concurrent.duration._
import scala.concurrent.Future

private object TimeoutFilter extends Filter {
  def apply(next: (RequestHeader) => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    // FIXME: Make TimeoutDuration configurable.
    val TimeoutDuration = 30.second
    val timeoutFuture = Promise.timeout("Timeout", TimeoutDuration)
    val resultFuture = next(request)
    Future.firstCompletedOf(Seq(resultFuture, timeoutFuture)).map {
      case result: SimpleResult => result
      case errorMessage: String => InternalServerError(errorMessage)
    }
  }
}

object Global extends WithFilters(TimeoutFilter) {
  // MongoDriver and MongoConnection involve creation costs
  // the driver may create a new ActorSystem, and the connection
  // will connect to the servers. It is a good idea to store the
  // driver and the connection to reuse them.
  private var connection: Option[MongoConnection] = None

  def mongoConnection: Option[MongoConnection] = connection

  // FIXME: When we have more servers, create a supervision tree here.
  private var zooKeeperLauncher: Option[ActorRef] = _

  private var mongoDBLauncher: Option[ActorRef] = _

  override def onStart(app: Application) {
    val driver = new MongoDriver(Akka.system(app))
    connection = Some(driver.connection(List("localhost")))

    zooKeeperLauncher = Some(Akka.system(app).actorOf(Props[ZooKeeperLauncher], name = "zooKeeperLauncher"))
    mongoDBLauncher = Some(Akka.system(app).actorOf(Props[MongoDBLauncher], name = "mongoDBLauncher"))
  }

  override def onStop(app: Application) {
    connection = None
    zooKeeperLauncher.foreach(Akka.system(app).stop(_))
    zooKeeperLauncher = None

    mongoDBLauncher.foreach(Akka.system(app).stop(_))
    mongoDBLauncher = None
  }

  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    Play.maybeApplication.filter(_.mode == Mode.Prod).map { _ =>
      Future.successful(NotFound)
    } getOrElse {
      super.onHandlerNotFound(request)
    }
  }
}
