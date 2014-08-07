package beyond.engine.javascript.lib.database

import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.IdScriptableObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.annotations.JSGetter
import reactivemongo.bson.BSONValue
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID
import scala.collection.mutable.{ Map => MutableMap }
import scalaz.syntax.std.boolean._

object ScriptableDocument {
  private type UpdatedValueTable = MutableMap[String, AnyRef]
  private val emptyUpdatedValueTable = MutableMap.empty[String, AnyRef]

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
}

class ScriptableDocument(fields: Seq[Field], currentValuesInDB: BSONDocument) extends IdScriptableObject {
  import ScriptableDocument._

  def this() = this(Seq.empty, BSONDocument.empty)

  override val getClassName: String = "Document"

  private val updatedValues: UpdatedValueTable = emptyUpdatedValueTable
  private val fieldsAccessors: MutableMap[Int, Callable] = MutableMap.empty[Int, Callable]

  def modifier: BSONDocument =
    BSONDocument(updatedValues.map {
      case (name, value) =>
        val field = fieldByName(name)
        (name, AnyRefTypedBSONHandler.write(field, value))
    })

  def objectID: BSONObjectID =
    currentValuesInDB.getAs[BSONObjectID]("_id").getOrElse(throw new NoSuchElementException("ObjectID is not exists"))

  @JSGetter
  def getObjectID: String =
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
          currentValue(name)
        case Array(arg) =>
          updatedValues.update(name, arg)
          thisObj
        case _ =>
          throw new IllegalArgumentException(s"$name() method cannot get more than one argument.")
      }
    }
  }

  private def fieldByID(id: Int): Field = fields(id - 1)
  private def fieldByName(name: String): Field = fields.find(_.name == name).get

  private def currentValue(name: String): AnyRef =
    updatedValues.getOrElse(name, currentValueInDB(name))

  private def currentValueInDB(name: String): AnyRef =
    currentValuesInDB.getAs[BSONValue](name).map { bsonValue =>
      val field = fieldByName(name)
      AnyRefTypedBSONHandler.read(field, bsonValue)
    }.getOrElse(Undefined.instance)
}
