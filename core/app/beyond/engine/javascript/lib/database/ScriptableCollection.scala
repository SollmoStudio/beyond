package beyond.engine.javascript.lib.database

import beyond.MongoMixin
import beyond.engine.javascript.BeyondContext
import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import beyond.engine.javascript.lib.ScriptableFuture
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSFunction => JSFunctionAnnotation }
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.commands.LastError
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ScriptableCollection {
  @JSFunctionAnnotation
  def insert(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val thisCollection = thisObj.asInstanceOf[ScriptableCollection]
    val dataToInsert = args(0).asInstanceOf[ScriptableObject]
    val insertQueryResult = thisCollection.insertInternal(dataToInsert)

    import com.beyondframework.rhino.RhinoConversions._
    val scriptableDocument = insertQueryResult.map { document =>
      beyondContextFactory.call { context: Context =>
        ScriptableDocument(context, thisCollection.fields, document)
      }
    }
    ScriptableFuture(context, scriptableDocument)
  }

  @JSFunctionAnnotation
  def find(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
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

  @JSFunctionAnnotation
  def findOne(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val thisCollection = thisObj.asInstanceOf[ScriptableCollection]
    val findQuery = args(0).asInstanceOf[ScriptableQuery]
    val findResult = thisCollection.findOneInternal(findQuery)

    import com.beyondframework.rhino.RhinoConversions._
    val convertedToScriptableDocumentResult = findResult.map {
      case None =>
        null
      case Some(document) =>
        beyondContextFactory.call { context: Context =>
          ScriptableDocument(context, thisCollection.fields, document)
        }
    }
    ScriptableFuture(context, convertedToScriptableDocumentResult)
  }

  @JSFunctionAnnotation
  def remove(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val thisCollection = thisObj.asInstanceOf[ScriptableCollection]
    val removeQuery = args(0).asInstanceOf[ScriptableQuery]
    val removeResult = thisCollection.removeInternal(removeQuery)
    ScriptableFuture(context, removeResult)
  }

  @JSFunctionAnnotation
  def removeOne(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val thisCollection = thisObj.asInstanceOf[ScriptableCollection]
    val removeQuery = args(0).asInstanceOf[ScriptableQuery]
    val removeResult = thisCollection.removeInternal(removeQuery, firstMatchOnly = true)
    ScriptableFuture(context, removeResult)
  }

  @JSFunctionAnnotation
  def save(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    implicit val executionContext = context.asInstanceOf[BeyondContext].executionContext
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val thisCollection = thisObj.asInstanceOf[ScriptableCollection]
    val dataToUpdate = args(0).asInstanceOf[ScriptableDocument]
    val saveQueryResult = thisCollection.saveInternal(dataToUpdate)

    import com.beyondframework.rhino.RhinoConversions._
    val scriptableDocument = saveQueryResult.map { document =>
      beyondContextFactory.call { context: Context =>
        ScriptableDocument(context, thisCollection.fields, document)
      }
    }
    ScriptableFuture(context, scriptableDocument)
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableCollection = {
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
  private def insertInternal(obj: ScriptableObject)(implicit ec: ExecutionContext): Future[BSONDocument] = {
    val dataToBeInserted: BSONDocument = convertScriptableObjectToBSONDocument(obj)(schema.fields)
    val objectId = Option(obj.get("objectId")).map {
      case objectId: ScriptableObjectId =>
        objectId.bson
      case somethingElse =>
        throw new IllegalArgumentException(s"objectId should be ObjectId. $somethingElse is not ObjectId.")
    }.getOrElse(BSONObjectID.generate)
    if (validateDocument(dataToBeInserted)) {
      val documentToBeInserted = BSONDocument("_id" -> objectId) ++ dataToBeInserted
      collection.insert(documentToBeInserted).map[BSONDocument] {
        case lastError if lastError.inError =>
          throw new Exception(lastError.message)
        case _ =>
          documentToBeInserted
      }
    } else {
      Future.failed(new IllegalArgumentException("Cannot pass validations."))
    }
  }

  // Cannot use name 'find', because static forwarder is not generated when the companion object and class have the same name method.
  private def findInternal(query: ScriptableQuery)(implicit ec: ExecutionContext): Future[Seq[BSONDocument]] =
    collection.find(query.query).cursor[BSONDocument].collect[Seq]()

  // Cannot use name 'findOne', because static forwarder is not generated when the companion object and class have the same name method.
  private def findOneInternal(query: ScriptableQuery)(implicit ec: ExecutionContext): Future[Option[BSONDocument]] =
    collection.find(query.query).one[BSONDocument]

  // Cannot use name 'remove', because static forwarder is not generated when the companion object and class have the same name method.
  private def removeInternal(query: ScriptableQuery, firstMatchOnly: Boolean = false)(implicit ec: ExecutionContext): Future[LastError] =
    collection.remove(query.query, firstMatchOnly = firstMatchOnly)

  // Cannot use name 'save', because static forwarder is not generated when the companion object and class have the same name method.
  private def saveInternal(dataToBeUpdated: ScriptableDocument)(implicit ec: ExecutionContext): Future[BSONDocument] = {
    val objectID = BSONDocument("_id" -> dataToBeUpdated.objectID)
    val modifier = BSONDocument("$set" -> dataToBeUpdated.modifier)
    if (modifier.isEmpty) {
      Future.successful(dataToBeUpdated.currentBSONDocument)
    } else if (validateDocument(dataToBeUpdated.currentBSONDocument)) {
      collection.update(objectID, modifier).map {
        case lastError if lastError.inError =>
          throw new Exception(lastError.message)
        case _ =>
          dataToBeUpdated.currentBSONDocument
      }
    } else {
      Future.failed(new IllegalArgumentException("Cannot pass validations."))
    }
  }

  private def validateDocument(document: BSONDocument): Boolean =
    fields.forall {
      case field: IntField =>
        document.getAs[Int](field.name).forall { value =>
          field.validations.forall(_.validate(value))
        }
      case field: DoubleField =>
        document.getAs[Double](field.name).forall { value =>
          field.validations.forall(_.validate(value))
        }
      case field: LongField =>
        document.getAs[Long](field.name).forall { value =>
          field.validations.forall(_.validate(value))
        }
      case _ =>
        true
    }
}
