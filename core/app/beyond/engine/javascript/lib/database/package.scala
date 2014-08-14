package beyond.engine.javascript.lib

import java.util.Date
import java.{ lang => jl }
import beyond.engine.javascript.JSArray
import com.beyondframework.rhino.ContextOps._
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeArray
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
      case ObjectId(id) =>
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
      case id: BSONObjectID => ObjectId(id)
      case _ =>
        throw new IllegalArgumentException(s"$bson cannot be a scala object")
    }
  }

  private[database] object Field {
    private val NoName: String = ""
    def apply(name: String, fieldOption: ScriptableObject): Field = {
      val fieldType = fieldOption.get("type").toString
      Field(name, fieldType, fieldOption)
    }

    private def apply(name: String, tpe: String, option: ScriptableObject): Field =
      tpe match {
        case "string" => StringField(name)
        case "int" => IntField(name)
        case "date" => DateField(name)
        case "long" => LongField(name)
        case "double" => DoubleField(name)
        case "boolean" => BooleanField(name)
        case "reference" =>
          val collection = Option(option.get("collection")).asInstanceOf[Option[ScriptableCollection]]
          ReferenceField(name, collection.getOrElse(throw new IllegalArgumentException("The reference type must have collection")))
        case "embedding" =>
          val schema = Option(option.get("schema")).asInstanceOf[Option[ScriptableSchema]]
          EmbeddingField(name, schema.getOrElse(throw new IllegalArgumentException("The embedding type must have schema")))
        case "array" =>
          val elementType = Option(option.get("elementType"))
            .getOrElse(throw new IllegalArgumentException("The array type must have elementType"))
            .asInstanceOf[ScriptableObject]
          val elementField = Field(NoName, elementType)
          ArrayField(name, elementField)
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
  private[database] case class ReferenceField(override val name: String, collection: ScriptableCollection) extends Field
  private[database] case class EmbeddingField(override val name: String, schema: ScriptableSchema) extends Field
  private[database] case class ArrayField(override val name: String, elementType: Field) extends Field

  private[database] def convertScalaToJavaScript(value: AnyRef)(implicit context: Context, scope: Scriptable): AnyRef = value match {
    case number: jl.Number =>
      number
    case str: String =>
      str
    case boolean: jl.Boolean =>
      boolean
    case date: Date =>
      context.newObject(scope, "Date", Long.box(date.getTime))
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
    case objectID: ObjectId =>
      context.getWrapFactory.wrapNewObject(context, scope, objectID)
    case _ =>
      throw new IllegalArgumentException(s"$value(${value.getClass} cannot be a JavaScript Object")
  }
  // FIXME: Handle default and optional value.
  private[database] def convertJavaScriptToScalaWithField(value: AnyRef)(implicit field: Field): AnyRef = (value, field) match {
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
    case (obj: ScriptableDocument, ReferenceField(_, _)) =>
      ObjectId(obj.objectID)
    case (objectID: ObjectId, ReferenceField(_, _)) =>
      objectID
    case (value: ScriptableObject, EmbeddingField(_, schema)) =>
      convertJavaScriptObjectToScalaWithField(value)(schema.fields)
    case (array: NativeArray, ArrayField(_, elementType)) =>
      array.toArray.map(convertJavaScriptToScalaWithField(_)(elementType)).toSeq
    case (_, _) =>
      throw new IllegalArgumentException(s"$value(${value.getClass} cannot be a Scala object with $field type")
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
