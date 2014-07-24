package db

import beyond.MongoMixin
import java.util.Date
import org.apache.commons.codec.digest.DigestUtils
import play.api.data.Form
import play.api.data.Forms.text
import play.api.data.Forms.tuple
import play.api.libs.json._
import play.modules.reactivemongo.json.collection.JSONCollection

trait UserMixin extends MongoMixin {
  val accountForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    )
  )

  case class Account(username: String, password: String, creationDate: Date)

  implicit val accountFormat = Json.format[Account]

  def collection: JSONCollection = db.collection[JSONCollection]("user.account")
  def encryptPassword(password: String): String = DigestUtils.sha1Hex(password)
  def createAccount(username: String, password: String): Account =
    Account(username, encryptPassword(password), new Date)
}
