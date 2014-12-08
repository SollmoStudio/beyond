package controllers.admin

import java.util.Date
import play.api.Play
import play.api.Mode
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.core.PlayVersion
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor
import scala.concurrent.Future
import scala.util.Properties

object Admin extends Controller with MongoController {
  private def userCollection: JSONCollection = db.collection[JSONCollection]("admin.password")
  private def metricCollection: JSONCollection = db.collection[JSONCollection]("admin.metrics")

  private case class AdminUser(username: String, password: String)
  private case class SystemLoadAverage(hostname: String, date: Date, loadAverage: Double)
  private case class MemoryUsage(init: Long, used: Long, committed: Long, max: Long)
  private case class HeapMemoryUsage(hostname: String, date: Date, memoryUsage: MemoryUsage)
  private case class NonHeapMemoryUsage(hostname: String, date: Date, memoryUsage: MemoryUsage)
  private case class SwapMemoryUsage(hostname: String, date: Date, free: Long, total: Long, used: Long)
  private case class NumberOfRequestsPerSecond(hostname: String, date: Date, numberOfRequests: Int)

  private implicit val adminUserFormat = Json.format[AdminUser]
  private implicit val memoryUsageFormat = Json.format[MemoryUsage]

  private implicit val systemLoadAverageReads = Json.reads[SystemLoadAverage]
  private implicit val heapMemoryUsageReads = Json.reads[HeapMemoryUsage]
  private implicit val nonHeapMemoryUsageReads = Json.reads[NonHeapMemoryUsage]
  private implicit val swapMemoryUsageReads = Json.reads[SwapMemoryUsage]
  private implicit val numberOfRequestsPerSecondReads = Json.reads[NumberOfRequestsPerSecond]

  private def convertToMegaBytes(bytes: Long): Long = bytes / 1000000

  private implicit val systemLoadAverageWrites = new Writes[SystemLoadAverage] {
    def writes(systemLoadAverage: SystemLoadAverage) = Json.obj(
      "hostname" -> systemLoadAverage.hostname,
      "date" -> systemLoadAverage.date.toString,
      "loadAverage" -> systemLoadAverage.loadAverage
    )
  }

  private implicit val heapMemoryUsageWrites = new Writes[HeapMemoryUsage] {
    def writes(heapMemoryUsage: HeapMemoryUsage) = Json.obj(
      "hostname" -> heapMemoryUsage.hostname,
      "date" -> heapMemoryUsage.date.toString,
      "init" -> convertToMegaBytes(heapMemoryUsage.memoryUsage.init),
      "used" -> convertToMegaBytes(heapMemoryUsage.memoryUsage.used),
      "committed" -> convertToMegaBytes(heapMemoryUsage.memoryUsage.committed),
      "max" -> convertToMegaBytes(heapMemoryUsage.memoryUsage.max)
    )
  }

  private implicit val nonHeapMemoryUsageWrites = new Writes[NonHeapMemoryUsage] {
    def writes(nonHeapMemoryUsage: NonHeapMemoryUsage) = Json.obj(
      "hostname" -> nonHeapMemoryUsage.hostname,
      "date" -> nonHeapMemoryUsage.date.toString,
      "init" -> convertToMegaBytes(nonHeapMemoryUsage.memoryUsage.init),
      "used" -> convertToMegaBytes(nonHeapMemoryUsage.memoryUsage.used),
      "committed" -> convertToMegaBytes(nonHeapMemoryUsage.memoryUsage.committed),
      "max" -> convertToMegaBytes(nonHeapMemoryUsage.memoryUsage.max)
    )
  }

  private implicit val swapMemoryUsageWrites = new Writes[SwapMemoryUsage] {
    def writes(swapMemoryUsage: SwapMemoryUsage) = Json.obj(
      "hostname" -> swapMemoryUsage.hostname,
      "date" -> swapMemoryUsage.date.toString,
      "free" -> convertToMegaBytes(swapMemoryUsage.free),
      "total" -> convertToMegaBytes(swapMemoryUsage.total),
      "used" -> convertToMegaBytes(swapMemoryUsage.used)
    )
  }

  private implicit val numberOfRequestsWrites = new Writes[NumberOfRequestsPerSecond] {
    def writes(numberOfRequests: NumberOfRequestsPerSecond) = Json.obj(
      "hostname" -> numberOfRequests.hostname,
      "date" -> numberOfRequests.date.toString,
      "numberOfRequests" -> numberOfRequests.numberOfRequests
    )
  }

  private class AuthenticatedRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)
  private object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      request.session.get("username").map { username =>
        block(new AuthenticatedRequest(username, request))
      } getOrElse {
        Future.successful(Redirect(routes.Admin.login))
      }
    }
  }

  private val MinUsernameLength = 4
  private val MaxUsernameLength = 12

  private val loginForm = Form(
    mapping(
      "username" -> nonEmptyText(minLength = MinUsernameLength, maxLength = MaxUsernameLength),
      "password" -> nonEmptyText
    )(AdminUser.apply)(AdminUser.unapply)
  )

  private def serverInfo: Map[String, String] = {
    import Play.current
    Map(
      "OS Name" -> Properties.osName,
      "Play mode" -> Play.mode.toString,
      "PlayVersion current" -> PlayVersion.current,
      "PlayVersion sbtVersion" -> PlayVersion.sbtVersion,
      "PlayVersion scalaVersion" -> PlayVersion.scalaVersion
    )
  }

  def index: Action[AnyContent] = AuthenticatedAction { request =>
    val jsonServerInfo = Json.stringify(Json.toJson(serverInfo))
    Ok(views.html.admin_index(jsonServerInfo))
  }

  def login: Action[AnyContent] = Action { request =>
    request.session.get("username").map { username =>
      Redirect(routes.Admin.index)
    }.getOrElse {
      Ok(views.html.admin_login())
    }
  }

  def logout: Action[AnyContent] = Action {
    Redirect(routes.Admin.index).withNewSession
  }

  def userIndex: Action[AnyContent] = Action {
    Redirect(routes.Admin.userList)
  }

  def userList: Action[AnyContent] = AuthenticatedAction.async { request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val cursor: Cursor[AdminUser] = userCollection.find(Json.obj()).cursor[AdminUser]
    val result: Future[List[AdminUser]] = cursor.collect[List]()
    result.map { users =>
      Ok(views.html.admin_user_list(Json.stringify(Json.toJson(users))))
    }
  }

  def doLogin: Action[AnyContent] = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        // FIXME: Create an error type instead of passing String.
        val result = BadRequest(views.html.admin_login("Invalid username"))
        Future.successful(result)
      },
      loginData => {
        import play.api.libs.concurrent.Execution.Implicits._

        val cursor: Cursor[AdminUser] = userCollection.find(Json.obj("username" -> loginData.username)).cursor[AdminUser]
        val result: Future[List[AdminUser]] = cursor.collect[List]()
        result.map {
          case List() => BadRequest(views.html.admin_login("No such username"))
          case adminUser :: _ =>
            if (adminUser.password == loginData.password) {
              Redirect(routes.Admin.index).withSession("username" -> loginData.username)
            } else {
              BadRequest(views.html.admin_login("Invalid password"))
            }
        }
      }
    )
  }

  def metricsIndex: Action[AnyContent] = Action {
    Ok(views.html.admin_metrics_main("") { null })
  }

  def systemLoadAverage: Action[AnyContent] = AuthenticatedAction.async { request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val cursor: Cursor[SystemLoadAverage] = metricCollection.find(Json.obj("tag" -> "SystemLoadAverage")).sort(Json.obj("date" -> -1)).cursor[SystemLoadAverage]
    val result: Future[List[SystemLoadAverage]] = cursor.collect[List]()

    result.map { metrics =>
      val headerJson: JsValue = Json.toJson("System Load Average")
      val dataJson: JsValue = Json.toJson(metrics)
      val columnsJson: JsValue = Json.parse("""
        [{
          "key": "hostname",
          "name": "Hostname"
        }, {
          "key": "date",
          "name": "Date"
        }, {
          "key": "loadAverage",
          "name": "LoadAverage"
        }]
      """)

      Ok(Json.obj("header" -> headerJson, "data" -> dataJson, "columns" -> columnsJson))
    }
  }

  def heapMemoryUsage: Action[AnyContent] = AuthenticatedAction.async { request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val cursor: Cursor[HeapMemoryUsage] = metricCollection.find(Json.obj("tag" -> "HeapMemoryUsage")).sort(Json.obj("date" -> -1)).cursor[HeapMemoryUsage]
    val result: Future[List[HeapMemoryUsage]] = cursor.collect[List]()

    result.map { metrics =>
      val headerJson: JsValue = Json.toJson("Heap Memory Usage")
      val dataJson: JsValue = Json.toJson(metrics)
      val columnsJson: JsValue = Json.parse("""
        [{
          "key": "hostname",
          "name": "Hostname"
        }, {
          "key": "date",
          "name": "Date"
        }, {
          "key": "init",
          "name": "Init (MB)"
        }, {
          "key": "used",
          "name": "Used (MB)"
        }, {
          "key": "committed",
          "name": "Committed (MB)"
        }, {
          "key": "max",
          "name": "Max (MB)"
        }]
      """)

      Ok(Json.obj("header" -> headerJson, "data" -> dataJson, "columns" -> columnsJson))
    }
  }

  def nonHeapMemoryUsage: Action[AnyContent] = AuthenticatedAction.async { request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val cursor: Cursor[NonHeapMemoryUsage] = metricCollection.find(Json.obj("tag" -> "NonHeapMemoryUsage")).sort(Json.obj("date" -> -1)).cursor[NonHeapMemoryUsage]
    val result: Future[List[NonHeapMemoryUsage]] = cursor.collect[List]()

    result.map { metrics =>
      val headerJson: JsValue = Json.toJson("Non Heap Memory Usage")
      val dataJson: JsValue = Json.toJson(metrics)
      val columnsJson: JsValue = Json.parse("""
        [{
          "key": "hostname",
          "name": "Hostname"
        }, {
          "key": "date",
          "name": "Date"
        }, {
          "key": "init",
          "name": "Init (MB)"
        }, {
          "key": "used",
          "name": "Used (MB)"
        }, {
          "key": "committed",
          "name": "Committed (MB)"
        }, {
          "key": "max",
          "name": "Max (MB)"
        }]
      """)

      Ok(Json.obj("header" -> headerJson, "data" -> dataJson, "columns" -> columnsJson))
    }
  }

  def swapMemoryUsage: Action[AnyContent] = AuthenticatedAction.async { request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val cursor: Cursor[SwapMemoryUsage] = metricCollection.find(Json.obj("tag" -> "SwapMemoryUsage")).sort(Json.obj("date" -> -1)).cursor[SwapMemoryUsage]
    val result: Future[List[SwapMemoryUsage]] = cursor.collect[List]()

    result.map { metrics =>
      val headerJson: JsValue = Json.toJson("Swap Memory Usage")
      val dataJson: JsValue = Json.toJson(metrics)
      val columnsJson: JsValue = Json.parse("""
        [{
          "key": "hostname",
          "name": "Hostname"
        }, {
          "key": "date",
          "name": "Date"
        }, {
          "key": "free",
          "name": "Free (MB)"
        }, {
          "key": "total",
          "name": "Total (MB)"
        }, {
          "key": "used",
          "name": "Used (MB)"
        }]
      """)

      Ok(Json.obj("header" -> headerJson, "data" -> dataJson, "columns" -> columnsJson))
    }
  }

  def numberOfRequests: Action[AnyContent] = AuthenticatedAction.async { request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val cursor: Cursor[NumberOfRequestsPerSecond] = metricCollection.find(Json.obj("tag" -> "NumberOfRequestsPerSecond")).sort(Json.obj("date" -> -1)).cursor[NumberOfRequestsPerSecond]
    val result: Future[List[NumberOfRequestsPerSecond]] = cursor.collect[List]()

    result.map { metrics =>
      val headerJson: JsValue = Json.toJson("Number Of Requests Per Second")
      val dataJson: JsValue = Json.toJson(metrics)
      val columnsJson: JsValue = Json.parse("""
        [{
          "key": "hostname",
          "name": "Hostname"
        }, {
          "key": "date",
          "name": "Date"
        }, {
          "key": "numOfRequests",
          "name": "NumberOfRequestsPerSecond"
        }]
      """)

      Ok(Json.obj("header" -> headerJson, "data" -> dataJson, "columns" -> columnsJson))
    }
  }
}
