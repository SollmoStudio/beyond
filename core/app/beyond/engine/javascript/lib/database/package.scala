package beyond.engine.javascript.lib

import java.{ lang => jl }
import org.mozilla.javascript.Function
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONDouble
import reactivemongo.bson.BSONInteger
import reactivemongo.bson.BSONJavaScript
import reactivemongo.bson.BSONLong
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONValue
import reactivemongo.bson.BSONWriter

package object database {
  implicit object AnyRefBSONWriter extends BSONWriter[AnyRef, BSONValue] {
    override def write(value: AnyRef): BSONValue = value match {
      case s: String => BSONString(s)
      case i: jl.Integer => BSONInteger(i)
      case j: jl.Long => BSONLong(j)
      case z: jl.Boolean => BSONBoolean(z)
      case d: jl.Double => BSONDouble(d)
      case f: Function => BSONJavaScript(f.toString)
      case _ =>
        throw new IllegalArgumentException(s"$value(${value.getClass} cannot be a BSONValue")
    }
  }

  private[database] object Field {
    def apply(name: String, tpe: String): Field =
      tpe match {
        case "string" => StringField(name)
        case "int" => IntField(name)
        case "date" => DateField(name)
        case "long" => LongField(name)
        case "double" => DoubleField(name)
        case "boolean" => BooleanField(name)
      }
  }

  private[database] trait Field {
    val name: String
  }
  // FIXME: Support complex type(embedding, referencing, array).
  private[database] case class BooleanField(override val name: String) extends Field
  private[database] case class IntField(override val name: String) extends Field
  private[database] case class StringField(override val name: String) extends Field
  private[database] case class DateField(override val name: String) extends Field
  private[database] case class DoubleField(override val name: String) extends Field
  private[database] case class LongField(override val name: String) extends Field
}
