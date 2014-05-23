package beyond

import akka.actor.ActorRef
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import java.io.File
import play.api.Application
import play.api.Configuration
import play.api.Mode
import play.api.Play
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Promise
import play.api.mvc.Filter
import play.api.mvc.RequestHeader
import play.api.mvc.Results.InternalServerError
import play.api.mvc.Results.NotFound
import play.api.mvc.SimpleResult
import play.api.mvc.WithFilters
import scala.concurrent.Future

private object TimeoutFilter extends Filter {
  def apply(next: (RequestHeader) => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    import play.api.Play.current

    val timeout = BeyondConfiguration.requestTimeout
    val timeoutFuture = Promise.timeout("Timeout", timeout)
    val resultFuture = next(request)
    Future.firstCompletedOf(Seq(resultFuture, timeoutFuture)).map {
      case result: SimpleResult => result
      case errorMessage: String => InternalServerError(errorMessage)
    }
  }
}

object Global extends WithFilters(TimeoutFilter) with Logging {
  private var beyondSupervisor: Option[ActorRef] = _

  override def onLoadConfig(defaultConfig: Configuration, path: File, classLoader: ClassLoader, mode: Mode.Mode): Configuration = {
    def loadConfigFromFile(configName: String): Configuration = {
      val file = new File(path, s"conf/$configName")
      if (file.exists) {
        logger.info(s"Load ${file.getCanonicalPath}")
        Configuration(ConfigFactory.parseFile(file))
      } else {
        Configuration.empty
      }
    }
    val modeSpecificConfiguration = loadConfigFromFile(s"application.${mode.toString.toLowerCase}.conf")

    val finalConfiguration = defaultConfig ++ modeSpecificConfiguration
    super.onLoadConfig(finalConfiguration, path, classLoader, mode)
  }

  override def onStart(app: Application) {
    logger.info("Beyond started")
    beyondSupervisor = Some(Akka.system(app).actorOf(Props[BeyondSupervisor], name = "beyondSupervisor"))
  }

  override def onStop(app: Application) {
    logger.info("Beyond stopped")
    beyondSupervisor.foreach(Akka.system(app).stop)
    beyondSupervisor = None
  }

  override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = {
    Play.maybeApplication.filter(_.mode == Mode.Prod).map { _ =>
      Future.successful(NotFound)
    } getOrElse {
      super.onHandlerNotFound(request)
    }
  }
}

