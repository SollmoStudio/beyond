package beyond

import akka.actor.ActorRef
import akka.actor.Props
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.apache.curator.RetryPolicy
import org.apache.curator.retry.ExponentialBackoffRetry
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
import scala.concurrent.duration._
import scala.concurrent.Future

private object TimeoutFilter extends Filter {
  def apply(next: (RequestHeader) => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext
    import play.api.Play.current

    val timeout = Duration(current.configuration.getString("beyond.request-timeout").getOrElse("30s"))
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

  def requestTimeout: FiniteDuration = {
    import play.api.Play.current
    Duration(current.configuration.getString("beyond.request-timeout").getOrElse("30s")).asInstanceOf[FiniteDuration]
  }

  def mongoDBPath: String = {
    import play.api.Play.current
    current.configuration.getString("beyond.mongodb.dbpath").getOrElse("data")
  }

  def zooKeeperConfigPath: String = {
    import play.api.Play.current
    current.configuration.getString("beyond.zookeeper.config-path").getOrElse("conf/zoo.cfg")
  }

  def pluginPaths: Seq[String] = {
    import play.api.Play.current
    import scala.collection.JavaConverters._
    val defaultModulePaths = Seq("plugins")
    current.configuration.getStringList("beyond.plugin.path").map(_.asScala).getOrElse(defaultModulePaths)
  }

  def curatorConnectionPolicy: RetryPolicy = {
    val configuration = play.api.Play.current.configuration
    val defaultMaxRetries = 10
    val curatorPath = "beyond.curator.connection."
    val baseSleepTimeMs = Duration(configuration.getString(curatorPath + "base-sleep-time").getOrElse("1000ms")).toMillis.toInt
    val maxRetries = configuration.getInt(curatorPath + "max-retries").getOrElse(defaultMaxRetries)
    val maxSleepMs = Duration(configuration.getString(curatorPath + "max-sleep").getOrElse("1000ms")).toMillis.toInt
    new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries, maxSleepMs)
  }
}

