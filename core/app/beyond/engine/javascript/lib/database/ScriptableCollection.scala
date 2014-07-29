package beyond.engine.javascript.lib.database

import beyond.MongoMixin
import beyond.engine.javascript.BeyondContext
import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.lib.ScriptableFuture
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.JSFunction
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONValue
import reactivemongo.core.commands.LastError
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ScriptableCollection {
  @JSFunction
  def insert(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val thisCollection = thisObj.asInstanceOf[ScriptableCollection]
    val dataToInsert = args(0).asInstanceOf[ScriptableObject]
    val insertQueryResult = thisCollection.insertInternal(dataToInsert)
    ScriptableFuture(context, insertQueryResult)
  }

  @JSFunction
  def find(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val thisCollection = thisObj.asInstanceOf[ScriptableCollection]
    val findQuery = args(0).asInstanceOf[ScriptableQuery]
    val queryResultFuture = thisCollection.findInternal(findQuery)

    import com.beyondframework.rhino.RhinoConversions._
    val convertedToScriptableDocumentFuture = queryResultFuture.map { documents =>
      beyondContextFactory.call { context: Context =>
        documents.map(ScriptableDocument(context, thisCollection.fields, _)).toArray
      }
    }
    ScriptableFuture(context, convertedToScriptableDocumentFuture)
  }

  @JSFunction
  def findOne(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture =
    ???

  @JSFunction
  def remove(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture =
    ???

  @JSFunction
  def removeOne(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableFuture =
    ???

  @JSFunction
  def save(context: Context, thisObj: Scriptable, args: Array[AnyRef], function: Function): ScriptableDocument =
    ???

  def jsConstructor(context: Context, args: Array[AnyRef], constructor: Function, inNewExpr: Boolean): ScriptableCollection = {
    val name = args(0).asInstanceOf[String]
    val schema = args(1).asInstanceOf[ScriptableSchema]
    new ScriptableCollection(name, schema)
  }
}

class ScriptableCollection(name: String, schema: ScriptableSchema) extends ScriptableObject with MongoMixin {
  def this() = this(null, null)

  override val getClassName: String = "Collection"
  private def collection: BSONCollection = db.collection[BSONCollection](name)

  private def fields: Seq[Field] = schema.fields

  // Cannot use name 'insert', because static forwarder is not generated when the companion object and class have the same name method.
  private def insertInternal(obj: ScriptableObject)(implicit ec: ExecutionContext): Future[LastError] = {
    val dataToBeInserted: Seq[(String, BSONValue)] = schema.fields.map { field =>
      // FIXME: Handle default and optional value.
      field.name -> AnyRefBSONWriter.write(obj.get(field.name))
    }
    val convertedToBSONDocumentToBeInserted = BSONDocument(dataToBeInserted)
    collection.insert(convertedToBSONDocumentToBeInserted)
  }

  // Cannot use name 'find', because static forwarder is not generated when the companion object and class have the same name method.
  private def findInternal(query: ScriptableQuery)(implicit ec: ExecutionContext): Future[Seq[BSONDocument]] =
    collection.find(query.query).cursor[BSONDocument].collect[Seq]()
}
