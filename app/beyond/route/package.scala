package beyond

import play.api.libs.functional.syntax._
import play.api.libs.json._

package object route {
  // RouteAddress is like "127.0.0.1:8080"
  type RouteAddress = String
  type RouteHash = Int

  implicit val routeWrites: Writes[RouteHashAndAddress] = (
    (__ \ "hash").write[RouteHash] and
    (__ \ "address").write[RouteAddress]
  )(unlift(RouteHashAndAddress.unapply))

  implicit val routeReads: Reads[RouteHashAndAddress] = (
    (__ \ "hash").read[RouteHash] and
    (__ \ "address").read[RouteAddress]
  )(RouteHashAndAddress)

  implicit val routeFormat: Format[RouteHashAndAddress] =
    Format(routeReads, routeWrites)

  case class RouteHashAndAddress(hash: RouteHash, address: RouteAddress)
}
