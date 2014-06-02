package beyond

import play.api.libs.functional.syntax._
import play.api.libs.json._

package object route {
  // RouteAddress is like "127.0.0.1:8080"
  type RouteAddress = String
  type RouteHash = Int

  object RouteAddress {
    def apply(ip: String, port: String): RouteAddress = ip + ":" + port

    def unapply(str: RouteAddress): Option[(String, String)] = {
      val parts = str split ":"
      if (parts.length == 2) Some(parts(0), parts(1)) else None
    }
  }

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
