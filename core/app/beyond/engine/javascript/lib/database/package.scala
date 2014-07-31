package beyond.engine.javascript.lib

import java.util.Date
import java.{ lang => jl }
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptRuntime
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONDateTime
import reactivemongo.bson.BSONDouble
import reactivemongo.bson.BSONHandler
import reactivemongo.bson.BSONInteger
import reactivemongo.bson.BSONJavaScript
import reactivemongo.bson.BSONLong
import reactivemongo.bson.BSONReader
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONValue
import reactivemongo.bson.BSONWriter

package object database {

  private[database] implicit object AnyRefBSONHandler extends BSONHandler[BSONValue, AnyRef] {
    override def write(value: AnyRef): BSONValue = value match {
      case s: String => BSONString(s)
      case i: jl.Integer => BSONInteger(i)
      case j: jl.Long => BSONLong(j)
      case z: jl.Boolean => BSONBoolean(z)
      case d: jl.Double => BSONDouble(d)
      case d: Date => BSONDateTime(d.getTime)
      case f: Function => BSONJavaScript(f.toString)
      case _ =>
        throw new IllegalArgumentException(s"$value(${value.getClass} cannot be a BSONValue")
    }

    override def read(bson: BSONValue): AnyRef = bson match {
      case BSONString(s) => s
      case BSONInteger(i) => Int.box(i)
      case BSONLong(j) => Long.box(j)
      case BSONBoolean(z) => Boolean.box(z)
      case BSONDouble(d) => Double.box(d)
      case BSONDateTime(d) => new Date(d)
      case _ =>
        throw new IllegalArgumentException(s"$bson cannot be converted")
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

  private[database] object AnyRefTypedBSONHandler {
    private def convertToFieldValue(field: Field, value: AnyRef): AnyRef = field match {
      case _: BooleanField =>
        Boolean.box(ScriptRuntime.toBoolean(value))
      case _: IntField =>
        Int.box(ScriptRuntime.toInt32(value))
      case _: StringField =>
        ScriptRuntime.toString(value)
      case _: DateField =>
        value match {
          case date: Date =>
            date
          case _ =>
            new Date(ScriptRuntime.toInteger(value).toLong)
        }
      case _: DoubleField =>
        Double.box(ScriptRuntime.toNumber(value))
      case _: LongField =>
        Long.box(ScriptRuntime.toInteger(value).toLong)
    }

    def write(field: Field, value: AnyRef)(implicit writer: BSONWriter[AnyRef, BSONValue]): BSONValue = {
      val fieldValue: AnyRef = convertToFieldValue(field, value)
      AnyRefBSONHandler.write(fieldValue)
    }

    def read(field: Field, bson: BSONValue)(implicit reader: BSONReader[BSONValue, AnyRef]): AnyRef = {
      val valueFromBSON: AnyRef = reader.read(bson)
      convertToFieldValue(field, valueFromBSON)
    }
  }
}
