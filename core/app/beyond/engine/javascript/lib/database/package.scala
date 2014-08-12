package beyond.engine.javascript.lib

import java.util.Date
import java.{ lang => jl }
import beyond.engine.javascript.JSArray
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import reactivemongo.bson.BSONArray
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONDateTime
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDouble
import reactivemongo.bson.BSONHandler
import reactivemongo.bson.BSONInteger
import reactivemongo.bson.BSONJavaScript
import reactivemongo.bson.BSONLong
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONUndefined
import reactivemongo.bson.BSONValue

package object database {

  private[database] implicit object AnyRefBSONHandler extends BSONHandler[BSONValue, AnyRef] {
    override def write(value: AnyRef): BSONValue = value match {
      case i: jl.Integer => BSONInteger(i)
      case j: jl.Long => BSONLong(j)
      case d: jl.Double => BSONDouble(d)
      case s: String => BSONString(s)
      case z: jl.Boolean => BSONBoolean(z)
      case date: Date => BSONDateTime(date.getTime)
      case f: Function => BSONJavaScript(f.toString)
      case map: Map[String, AnyRef] =>
        BSONDocument(map.map {
          case (k, v) =>
            k -> write(v)
        })
      case seq: Seq[AnyRef] =>
        BSONArray(seq.map(write))
      case _: Undefined => BSONUndefined
      case ObjectID(id) =>
        id
      case native: NativeJavaObject =>
        write(native.unwrap())
      case _ =>
        throw new IllegalArgumentException(s"$value(${value.getClass} cannot be a BSONValue")
    }

    override def read(bson: BSONValue): AnyRef = bson match {
      case BSONInteger(i) => Int.box(i)
      case BSONLong(j) => Long.box(j)
      case BSONDouble(d) => Double.box(d)
      case BSONString(s) => s
      case BSONBoolean(z) => Boolean.box(z)
      case BSONDateTime(d) => new Date(d)
      case BSONJavaScript(js) => js
      case document: BSONDocument =>
        document.elements.map {
          case (k, v) =>
            k -> read(v)
        }.toMap
      case array: BSONArray =>
        array.values.map(read)
      case BSONUndefined => Undefined.instance
      case id: BSONObjectID => ObjectID(id)
      case _ =>
        throw new IllegalArgumentException(s"$bson cannot be a scala object")
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

  private[database] def convertScalaToJavaScript(value: AnyRef)(implicit context: Context, scope: Scriptable): Scriptable = value match {
    case i: jl.Integer =>
      val args: JSArray = Array(Double.box(i.toDouble))
      context.newObject(scope, "Number", args)
    case j: jl.Long =>
      val args: JSArray = Array(Double.box(j.toDouble))
      context.newObject(scope, "Number", args)
    case d: jl.Double =>
      val args: JSArray = Array(Double.box(d))
      context.newObject(scope, "Number", args)
    case str: String =>
      val args: JSArray = Array(str)
      context.newObject(scope, "String", args)
    case b: jl.Boolean =>
      val args: JSArray = Array(b)
      context.newObject(scope, "Boolean", args)
    case date: Date =>
      val args: JSArray = Array(Long.box(date.getTime))
      context.newObject(scope, "Date", args)
    case map: Map[String, AnyRef] =>
      val obj = context.newObject(scope)
      map.foreach {
        case (name, nativeValue) =>
          val javaScriptValue = convertScalaToJavaScript(nativeValue)
          obj.put(name, obj, javaScriptValue)
      }
      obj
    case seq: Seq[AnyRef] =>
      val args: JSArray = seq.map(convertScalaToJavaScript).toArray
      context.newObject(scope, "Array", args)
    case _ =>
      throw new IllegalArgumentException(s"$value(${value.getClass} cannot be a JavaScript Object")
  }

  // FIXME: Handle default and optional value.
  private[database] def convertJavaScriptToScalaWithField(value: AnyRef)(implicit field: Field): AnyRef = {
    (value, field) match {
      case (native: NativeJavaObject, field: Field) =>
        convertJavaScriptToScalaWithField(native.unwrap())(field)
      case (_, IntField(_)) =>
        Int.box(ScriptRuntime.toInt32(value))
      case (_, DoubleField(_)) =>
        Double.box(ScriptRuntime.toNumber(value))
      case (_, LongField(_)) =>
        Long.box(ScriptRuntime.toInteger(value).toLong)
      case (_, StringField(_)) =>
        ScriptRuntime.toString(value)
      case (_, BooleanField(_)) =>
        Boolean.box(ScriptRuntime.toBoolean(value))
      case (date: Date, DateField(_)) =>
        date
      case (_, DateField(_)) =>
        new Date(ScriptRuntime.toInteger(value).toLong)
      case _ =>
        throw new IllegalArgumentException(s"$value(${value.getClass} cannot be a Scala object with $field type")
    }
  }

  private[database] def convertJavaScriptObjectToScalaWithField(obj: ScriptableObject)(implicit fields: Seq[Field]): Map[String, AnyRef] =
    fields.map { implicit field =>
      val name = field.name
      val value = convertJavaScriptToScalaWithField(obj.get(name))
      name -> value
    }.toMap

  private[database] def convertScriptableObjectToBSONDocument(obj: ScriptableObject)(implicit fields: Seq[Field]): BSONDocument =
    AnyRefBSONHandler.write(convertJavaScriptObjectToScalaWithField(obj)).asInstanceOf[BSONDocument]
}
