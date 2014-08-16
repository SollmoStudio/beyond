package beyond.engine.javascript.lib.database

import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import com.beyondframework.rhino.ContextOps._
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSFunction => JSFunctionAnnotation }
import reactivemongo.bson.BSONArray
import reactivemongo.bson.BSONDocument

object ScriptableQuery {
  // Cannot use name eq because static forwarder is not generated
  // when the same name method exists in the class and companion object.
  @JSFunctionAnnotation("eq")
  def jsEq(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableQuery = {
    val currentQuery: BSONDocument = thisObj.asInstanceOf[ScriptableQuery].query
    val field = args(0).asInstanceOf[String]
    val value = args(1)
    val newQuery: BSONDocument = currentQuery.add(field -> value)
    ScriptableQuery(context, newQuery)
  }

  @JSFunctionAnnotation
  def neq(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableQuery = {
    val currentQuery: BSONDocument = thisObj.asInstanceOf[ScriptableQuery].query
    val field = args(0).asInstanceOf[String]
    val value = args(1)
    val newQuery: BSONDocument = currentQuery.add(field -> BSONDocument("$ne" -> value))
    ScriptableQuery(context, newQuery)
  }

  @JSFunctionAnnotation
  def lt(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableQuery = {
    val currentQuery: BSONDocument = thisObj.asInstanceOf[ScriptableQuery].query
    val field = args(0).asInstanceOf[String]
    val value = args(1)
    val newQuery: BSONDocument = currentQuery.add(field -> BSONDocument("$lt" -> value))
    ScriptableQuery(context, newQuery)
  }

  @JSFunctionAnnotation
  def lte(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableQuery = {
    val currentQuery: BSONDocument = thisObj.asInstanceOf[ScriptableQuery].query
    val field = args(0).asInstanceOf[String]
    val value = args(1)
    val newQuery: BSONDocument = currentQuery.add(field -> BSONDocument("$lte" -> value))
    ScriptableQuery(context, newQuery)
  }

  @JSFunctionAnnotation
  def gt(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableQuery = {
    val currentQuery: BSONDocument = thisObj.asInstanceOf[ScriptableQuery].query
    val field = args(0).asInstanceOf[String]
    val value = args(1)
    val newQuery: BSONDocument = currentQuery.add(field -> BSONDocument("$gt" -> value))
    ScriptableQuery(context, newQuery)
  }

  @JSFunctionAnnotation
  def gte(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableQuery = {
    val currentQuery: BSONDocument = thisObj.asInstanceOf[ScriptableQuery].query
    val field = args(0).asInstanceOf[String]
    val value = args(1)
    val newQuery: BSONDocument = currentQuery.add(field -> BSONDocument("$gte" -> value))
    ScriptableQuery(context, newQuery)
  }

  @JSFunctionAnnotation
  def where(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableQuery =
    ???

  @JSFunctionAnnotation
  def or(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableQuery = {
    val currentQuery: BSONDocument = thisObj.asInstanceOf[ScriptableQuery].query
    val orQuery = BSONArray(args.map(_.asInstanceOf[ScriptableQuery].query).+:(currentQuery))
    val newQuery: BSONDocument = BSONDocument("$or" -> orQuery)
    ScriptableQuery(context, newQuery)
  }

  @JSFunctionAnnotation
  def and(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableQuery = {
    val currentQuery: BSONDocument = thisObj.asInstanceOf[ScriptableQuery].query
    val andQuery: BSONDocument = args.map(_.asInstanceOf[ScriptableQuery].query).fold(currentQuery) {
      case (currentDocument, newDocument) => currentDocument.add(newDocument)
    }
    ScriptableQuery(context, andQuery)
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableQuery =
    args match {
      case Array() => new ScriptableQuery
      case Array(bson: BSONDocument) => new ScriptableQuery(bson)
      case _ => throw new IllegalArgumentException
    }

  private[database] def apply(context: Context, bson: BSONDocument): ScriptableQuery = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    context.newObject(scope, "Query", bson).asInstanceOf[ScriptableQuery]
  }
}

class ScriptableQuery(val query: BSONDocument) extends ScriptableObject {
  def this() = this(BSONDocument.empty)

  override val getClassName: String = "Query"
}
