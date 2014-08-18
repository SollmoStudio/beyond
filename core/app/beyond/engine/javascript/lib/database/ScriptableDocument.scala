package beyond.engine.javascript.lib.database

import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.IdScriptableObject
import org.mozilla.javascript.NativeJavaObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.annotations.{ JSFunction => JSFunctionAnnotation }
import org.mozilla.javascript.annotations.JSGetter
import reactivemongo.bson.BSONValue
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONUndefined
import scala.collection.mutable.{ Map => MutableMap }
import scalaz.syntax.std.boolean._

object ScriptableDocument {
  // This constructor is used internally. Users are not allowed to construct an instance directly.
  // A user must get a ScriptableDocument from either ScriptableCollection.find() or ScriptableCollection.findOne().
  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableDocument = {
    val fields = args(0).asInstanceOf[Seq[Field]]
    val currentValueInDB = args(1).asInstanceOf[BSONDocument]
    new ScriptableDocument(fields, currentValueInDB)
  }

  private[database] def apply(context: Context, fields: Seq[Field], document: BSONDocument): ScriptableDocument = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    val args: JSArray = Array(fields, document)
    context.newObject(scope, "Document", args).asInstanceOf[ScriptableDocument]
  }

  @JSFunctionAnnotation
  def toJSON(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): Scriptable =
    thisObj.asInstanceOf[ScriptableDocument].toScriptable(context)
}

class ScriptableDocument(fields: Seq[Field], currentValuesInDB: BSONDocument) extends IdScriptableObject {
  private type UpdatedValueTable = MutableMap[String, BSONValue]
  private val emptyUpdatedValueTable = MutableMap.empty[String, BSONValue]

  import ScriptableDocument._

  def this() = this(Seq.empty, BSONDocument.empty)

  override val getClassName: String = "Document"

  private val updatedValues: UpdatedValueTable = emptyUpdatedValueTable
  private val fieldsAccessors: MutableMap[Int, Callable] = MutableMap.empty[Int, Callable]

  def modifier: BSONDocument =
    BSONDocument(updatedValues)

  val objectIDOption: Option[BSONObjectID] =
    currentValuesInDB.getAs[BSONObjectID]("_id")

  def objectID: BSONObjectID =
    objectIDOption.getOrElse(throw new NoSuchElementException("ObjectID does not exist"))

  @JSGetter
  def getObjectId: String =
    objectID.stringify

  override val getMaxInstanceId: Int = fields.size

  override protected def getInstanceIdName(id: Int): String =
    (id > getMaxInstanceId) ? super.getInstanceIdName(id) | fieldByID(id).name

  override def findInstanceIdInfo(instanceName: String): Int = {
    val index = fields.indexWhere(_.name == instanceName)
    (index == -1) ? super.findInstanceIdInfo(instanceName) | index + 1 // IdScriptableObject index starts with 1.
  }

  override protected def getInstanceIdValue(id: Int): AnyRef =
    if (id > getMaxInstanceId) {
      super.getInstanceIdValue(id)
    } else {
      fieldsAccessors.getOrElseUpdate(id, newGetterFor(id))
    }

  private def newGetterFor(id: Int): Callable = new Callable() {
    val name = getInstanceIdName(id)
    override def call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: JSArray): AnyRef = {
      args match {
        case Array() =>
          currentJavaScriptValue(name)(cx, scope)
        case Array(arg) =>
          implicit val field = fieldByName(name)
          val scalaValue = convertJavaScriptToScalaWithField(arg)
          updatedValues.update(name, AnyRefBSONHandler.write(scalaValue))
          thisObj
        case _ =>
          throw new IllegalArgumentException(s"$name() method cannot get more than one argument.")
      }
    }
  }

  private def fieldByID(id: Int): Field = fields(id - 1)

  private def fieldByName(name: String): Field = fields.find(_.name == name).get

  private def currentJavaScriptValue(name: String)(implicit context: Context, scope: Scriptable): AnyRef = {
    val bsonValue = currentBSONValue(name)
    val scalaValue = AnyRefBSONHandler.read(bsonValue)
    convertScalaToJavaScript(scalaValue)(context, scope)
  }

  private def currentBSONValue(name: String): BSONValue =
    updatedValues.getOrElse(name, currentBSONValueInDB(name))

  private def currentBSONValueInDB(name: String): BSONValue =
    currentValuesInDB.getAs[BSONValue](name).getOrElse(BSONUndefined)

  private def toScriptable(context: Context): Scriptable = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global

    val obj = context.newObject(scope)
    objectIDOption.foreach { objectID =>
      obj.put("_id", obj, ObjectID(objectID))
    }
    fields.foreach { field =>
      val name = field.name
      val javaScriptValue = currentJavaScriptValue(name)(context, scope)
      obj.put(name, obj, javaScriptValue)
    }
    obj
  }

  def currentBSONDocument: BSONDocument = {
    val modifiedDocument = modifier
    val currentElements = currentValuesInDB.elements.filter {
      case (key, _) =>
        modifiedDocument.get(key) == None
    } ++ modifiedDocument.elements
    BSONDocument(currentElements)
  }
}
