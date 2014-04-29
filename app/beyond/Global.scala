package beyond

import play.api.Application
import play.api.GlobalSettings
import play.api.Mode
import play.api.Play
import play.api.mvc.RequestHeader
import play.api.mvc.Results.NotFound
import play.api.mvc.SimpleResult
import reactivemongo.api.MongoConnection
import reactivemongo.api.MongoDriver
import scala.concurrent.Future

object Global extends GlobalSettings {
  // MongoDriver and MongoConnection involve creation costs
  // the driver may create a new ActorSystem, and the connection
  // will connect to the servers. It is a good idea to store the
  // driver and the connection to reuse them.
  private var connection: Option[MongoConnection] = None

  def mongoConnection: Option[MongoConnection] = connection

  override def onStart(app: Application) {
    // FIXME: MongoDriver creates a new Akka's ActorSystem.
    // Reuse an existing system to avoid wasting resources.
    val driver = new MongoDriver
    connection = Some(driver.connection(List("localhost")))
  }

  override def onStop(app: Application) {
    connection = None
  }

  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    Play.maybeApplication.filter(_.mode == Mode.Prod).map { _ =>
      Future.successful(NotFound)
    } getOrElse {
      super.onHandlerNotFound(request)
    }
  }
}
