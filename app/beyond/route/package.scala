package beyond

import play.api.libs.functional.syntax._
import play.api.libs.json._

package object route {
  type Address = String
  type Hash = Int

  implicit val serverWrites: Writes[Server] = (
    (__ \ "hash").write[Hash] and
    (__ \ "address").write[Address]
  )(unlift(Server.unapply))

  implicit val serverReads: Reads[Server] = (
    (__ \ "hash").read[Hash] and
    (__ \ "address").read[Address]
  )(Server)

  implicit val locationFormat: Format[Server] =
    Format(serverReads, serverWrites)

  case class Server(hash: Hash, address: Address)
}
