package beyond.engine.javascript.lib.database

import beyond.engine.javascript.BeyondContextFactory
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
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
  // This constructor is used internally. Users are not allowed to construct an instance directly.
  // A user must get a ScriptableDocument from either ScriptableCollection.find() or ScriptableCollection.findOne().
  def jsConstructor(context: Context, args: Array[AnyRef], constructor: Function, inNewExpr: Boolean): ScriptableDocument = {
    val fields = args(0).asInstanceOf[Seq[Field]]
    val currentValueInDB = args(1).asInstanceOf[BSONDocument]
    new ScriptableDocument(fields, currentValueInDB)
  }

  private[database] def apply(context: Context, fields: Seq[Field], document: BSONDocument): ScriptableDocument = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    val args: Array[AnyRef] = Array(fields, document)
    context.newObject(scope, "Document", args).asInstanceOf[ScriptableDocument]
  }
}

class ScriptableDocument(fields: Seq[Field], currentValueInDB: BSONDocument) extends IdScriptableObject {
  def this() = this(Seq.empty, BSONDocument.empty)

  override val getClassName: String = "Document"

  private val fieldsAccessors: MutableMap[Int, Callable] = MutableMap.empty[Int, Callable]

  @JSGetter
  def getObjectID: String =
    currentValueInDB.getAs[BSONObjectID]("_id")
      .getOrElse(throw new NoSuchElementException("ObjectID is not exists"))
      .stringify

  override val getMaxInstanceId: Int = fields.size

  override protected def getInstanceIdName(id: Int): String =
    (id > getMaxInstanceId) ? super.getInstanceIdName(id) | fieldInfo(id).name

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
    override def call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array[AnyRef]): AnyRef = {
      currentValueInDB.getAs[BSONValue](name).map { bsonValue =>
        val field = fieldInfo(id)
        AnyRefTypedBSONHandler.read(field, bsonValue)
      }.getOrElse(Undefined.instance)
    }
  }

  private def fieldInfo(id: Int): Field = fields(id - 1)
}
