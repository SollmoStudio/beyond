package beyond.engine.javascript.lib

import beyond.engine.javascript.JSArray
import com.beyondframework.rhino.ContextOps._
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import java.util.Date
import java.{ lang => jl }
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import reactivemongo.bson.BSONArray
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONDateTime
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDouble
import reactivemongo.bson.BSONHandler
import reactivemongo.bson.BSONInteger
import reactivemongo.bson.BSONJavaScript
import reactivemongo.bson.BSONLong
import reactivemongo.bson.BSONNull
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONValue

package object database extends Logging {

  private[database] implicit object AnyRefBSONHandler extends BSONHandler[BSONValue, AnyRef] {
    override def write(value: AnyRef): BSONValue = value match { // scalastyle:ignore cyclomatic.complexity
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
      case ObjectId(id) =>
        id
      case native: NativeJavaObject =>
        write(native.unwrap())
      case null =>
        BSONNull
      case _ =>
        throw new IllegalArgumentException(s"$value(${value.getClass} cannot be a BSONValue")
    }

    override def read(bson: BSONValue): AnyRef = bson match { // scalastyle:ignore cyclomatic.complexity
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
      case id: BSONObjectID => ObjectId(id)
      case BSONNull =>
        null
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

    private def apply(name: String, tpe: String, option: ScriptableObject): Field = {
      // ScriptRuntime treats null as false.
      val isOptional = ScriptRuntime.toBoolean(option.get("optional"))
      val isNullable = if (isOptional) {
        logger.warn(s"You are using 'optional' for a $name field, but 'optional' is deprecated. Use 'nullable' instead of 'optional'.")
        true
      } else {
        ScriptRuntime.toBoolean(option.get("nullable"))
      }
      val defaultValue = Option(option.get("default"))
      tpe match {
        case "string" => StringField(name, isNullable, defaultValue)
        case "int" =>
          val validations: Seq[Validation[Int]] = (
            Option(option.get("min")).map(ScriptRuntime.toInt32).map(MinValidation[Int]) ++
            Option(option.get("max")).map(ScriptRuntime.toInt32).map(MaxValidation[Int])).toSeq
          IntField(name, isNullable, defaultValue, validations)
        case "date" => DateField(name, isNullable, defaultValue)
        case "long" =>
          val validations: Seq[Validation[Long]] = (
            Option(option.get("min")).map(ScriptRuntime.toUint32).map(MinValidation[Long]) ++
            Option(option.get("max")).map(ScriptRuntime.toUint32).map(MaxValidation[Long])).toSeq
          LongField(name, isNullable, defaultValue, validations)
        case "double" =>
          val validations: Seq[Validation[Double]] = (
            Option(option.get("min")).map(ScriptRuntime.toNumber).map(MinValidation[Double]) ++
            Option(option.get("max")).map(ScriptRuntime.toNumber).map(MaxValidation[Double])).toSeq
          DoubleField(name, isNullable, defaultValue, validations)
        case "boolean" =>
          BooleanField(name, isNullable, defaultValue)
        case "reference" =>
          val collection = Option(option.get("collection")).asInstanceOf[Option[ScriptableCollection]]
          ReferenceField(name, collection, isNullable, defaultValue)
        case "embedding" =>
          val schema = Option(option.get("schema")).asInstanceOf[Option[ScriptableSchema]]
          EmbeddingField(name, schema.getOrElse(throw new IllegalArgumentException("The embedding type must have schema")), isNullable, defaultValue)
        case "array" =>
          val elementType = Option(option.get("elementType"))
            .getOrElse(throw new IllegalArgumentException("The array type must have elementType"))
            .asInstanceOf[ScriptableObject]
          val elementField = Field(NoName, elementType)
          ArrayField(name, elementField, isNullable, defaultValue)
      }
    }
  }

  private[database] trait Validation[T] {
    def validate(input: T): Boolean
  }
  private[database] case class MinValidation[T <% Ordered[T]](min: T) extends Validation[T] {
    override def validate(input: T): Boolean = min <= input
  }
  private[database] case class MaxValidation[T <% Ordered[T]](max: T) extends Validation[T] {
    override def validate(input: T): Boolean = input <= max
  }

  private[database] trait Field {
    val name: String
    val isNullable: Boolean
    val defaultValue: Option[AnyRef]
  }

  private[database] case class BooleanField(override val name: String, override val isNullable: Boolean,
    override val defaultValue: Option[AnyRef]) extends Field
  private[database] case class IntField(override val name: String, override val isNullable: Boolean, override val defaultValue: Option[AnyRef],
    validations: Seq[Validation[Int]]) extends Field
  private[database] case class StringField(override val name: String, override val isNullable: Boolean, override val defaultValue: Option[AnyRef]) extends Field
  private[database] case class DateField(override val name: String, override val isNullable: Boolean, override val defaultValue: Option[AnyRef]) extends Field
  private[database] case class DoubleField(override val name: String, override val isNullable: Boolean, override val defaultValue: Option[AnyRef],
    validations: Seq[Validation[Double]]) extends Field
  private[database] case class LongField(override val name: String, override val isNullable: Boolean, override val defaultValue: Option[AnyRef],
    validations: Seq[Validation[Long]]) extends Field
  private[database] case class ReferenceField(override val name: String, collection: Option[ScriptableCollection], override val isNullable: Boolean,
    override val defaultValue: Option[AnyRef]) extends Field
  private[database] case class EmbeddingField(override val name: String, schema: ScriptableSchema, override val isNullable: Boolean,
    override val defaultValue: Option[AnyRef]) extends Field
  private[database] case class ArrayField(override val name: String, elementType: Field, override val isNullable: Boolean,
    override val defaultValue: Option[AnyRef]) extends Field

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
      context.newObject(scope, "ObjectId", objectID.toString)
    case null =>
      null
    case _ =>
      throw new IllegalArgumentException(s"$value(${value.getClass} cannot be a JavaScript Object")
  }

  private[database] def convertJavaScriptToScalaWithField(value: AnyRef)(implicit field: Field): AnyRef = (value, field) match {
    case (null, field: Field) if field.defaultValue.isDefined =>
      null
    case (null, field: Field) if field.isNullable =>
      null
    case (null, field: Field) =>
      throw new IllegalArgumentException(s"$field is not optional field.")
    case (native: NativeJavaObject, field: Field) =>
      convertJavaScriptToScalaWithField(native.unwrap())(field)
    case (_, _: IntField) =>
      Int.box(ScriptRuntime.toInt32(value))
    case (_, _: DoubleField) =>
      Double.box(ScriptRuntime.toNumber(value))
    case (_, _: LongField) =>
      Long.box(ScriptRuntime.toInteger(value).toLong)
    case (_, _: StringField) =>
      ScriptRuntime.toString(value)
    case (_, _: BooleanField) =>
      Boolean.box(ScriptRuntime.toBoolean(value))
    case (date: Date, _: DateField) =>
      date
    case (_, _: DateField) =>
      new Date(ScriptRuntime.toInteger(value).toLong)
    case (obj: ScriptableDocument, _: ReferenceField) =>
      ObjectId(obj.objectID)
    case (objectID: ScriptableObjectId, _: ReferenceField) =>
      ObjectId(objectID.bson)
    case (value: ScriptableObject, embeddingField: EmbeddingField) =>
      convertJavaScriptObjectToScalaWithField(value)(embeddingField.schema.fields)
    case (array: NativeArray, arrayField: ArrayField) =>
      array.toArray.map(convertJavaScriptToScalaWithField(_)(arrayField.elementType)).toSeq
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
