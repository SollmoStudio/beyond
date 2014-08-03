package beyond

import beyond.UserActionActor.RequestWithUsername
import beyond.route.RouteAddress
import beyond.route.RoutingTableView
import beyond.route.RoutingTableView.HandleHere
import beyond.route.RoutingTableView.HandleIn
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import play.api.libs.json.JsArray
import play.api.mvc.ActionBuilder
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden
import play.api.mvc.Results.Status
import scala.concurrent.Future

object Authenticated extends ActionBuilder[RequestWithUsername] with Logging {
  private var routingTable: RoutingTableView = new RoutingTableView(BeyondConfiguration.currentServerRouteAddress)

  def syncRoutingTable(data: JsArray) {
    routingTable = new RoutingTableView(BeyondConfiguration.currentServerRouteAddress, data)
  }

  override def invokeBlock[A](request: Request[A], block: (RequestWithUsername[A]) => Future[Result]): Future[Result] =
    request.session.get("username").map { username =>
      val hash = username.##
      routingTable.queryServerToHandle(hash) match {
        case HandleHere =>
          block(new RequestWithUsername(username, request))
        case HandleIn(address: RouteAddress) =>
          val RedirectStatusCode = 310 // This code is just one of a redirection code not used by HTTP. It can be changed.
          logger.info(s"Request by $username is redirected to $address")
          Future.successful(new Status(RedirectStatusCode)(address))
      }
    } getOrElse {
      Future.successful(Forbidden)
    }
}
