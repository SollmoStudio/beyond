package beyond.engine.javascript.lib.http

import beyond.engine.javascript.BeyondContextFactory
import beyond.engine.javascript.JSArray
import beyond.engine.javascript.JSFunction
import beyond.engine.javascript.lib.ScriptableFuture
import com.beyondframework.rhino.ContextOps._
import java.io.File
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.annotations.{ JSFunction => JSFunctionAnnotation }
import org.mozilla.javascript.annotations.JSStaticFunction
import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future
import scalaz.syntax.std.boolean._

object ScriptableHttpClient {
  private[lib] def apply(context: Context, url: String, method: String): ScriptableHttpClient = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    context.newObject(scope, "HttpClient", url, method).asInstanceOf[ScriptableHttpClient]
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableHttpClient = {
    val url: String = args(0).asInstanceOf[String]
    val method: String = args(1).asInstanceOf[String]

    new ScriptableHttpClient(url, method)
  }

  private def convertScriptableToSeq(scriptable: Scriptable): Seq[(String, String)] =
    scriptable.getIds.map {
      case key: String =>
        key -> ScriptRuntime.toString(scriptable.get(key, scriptable))
    }

  @JSFunctionAnnotation
  def set(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient = {
    val thisHttpClient = thisObj.asInstanceOf[ScriptableHttpClient]
    val headers = args(0).asInstanceOf[Scriptable]

    thisHttpClient.setHeaders(convertScriptableToSeq(headers))
    thisHttpClient
  }

  @JSFunctionAnnotation
  def query(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient = {
    val thisHttpClient = thisObj.asInstanceOf[ScriptableHttpClient]
    val parameters = args(0).asInstanceOf[Scriptable]

    thisHttpClient.setQueryString(convertScriptableToSeq(parameters))
    thisHttpClient
  }

  private def convertScriptableToForm(scriptable: Scriptable): Map[String, Seq[String]] =
    scriptable.getIds.map {
      case key: String =>
        scriptable.get(key, scriptable) match {
          case value: String =>
            key -> Seq(value)
          case values: Scriptable =>
            key -> ScriptRuntime.getArrayElements(values).map(_.asInstanceOf[String]).toSeq
        }
    }.toMap

  @JSFunctionAnnotation
  def send(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient = {
    val thisHttpClient = thisObj.asInstanceOf[ScriptableHttpClient]
    val form = args(0).asInstanceOf[Scriptable]

    thisHttpClient.setFormBody(convertScriptableToForm(form))
    thisHttpClient
  }

  private def convertScriptableToJsObject(scriptable: Scriptable): JsObject = {
    val jsonSeq: Seq[(String, JsValue)] = scriptable.getIds.map {
      case key: String =>
        scriptable.get(key, scriptable) match {
          case number: Integer =>
            key -> JsNumber(number.toInt)
          case number: java.lang.Double =>
            key -> JsNumber(BigDecimal(number))
          case string: String =>
            key -> JsString(string)
          case obj: Scriptable =>
            key -> convertScriptableToJsObject(obj)
        }
    }

    JsObject(jsonSeq)
  }

  @JSFunctionAnnotation
  def json(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient = {
    val thisHttpClient = thisObj.asInstanceOf[ScriptableHttpClient]
    val json = args(0).asInstanceOf[Scriptable]

    thisHttpClient.setJsonBody(convertScriptableToJsObject(json))
    thisHttpClient
  }

  private val defaultAuthScheme: WSAuthScheme = WSAuthScheme.BASIC

  private def authSchemeForName(name: String): WSAuthScheme = name match {
    case "basic" => WSAuthScheme.BASIC
    case "digest" => WSAuthScheme.DIGEST
    case "kerberos" => WSAuthScheme.KERBEROS
    case "none" => WSAuthScheme.NONE
    case "ntlm" => WSAuthScheme.NTLM
    case "spnego" => WSAuthScheme.SPNEGO
  }

  @JSFunctionAnnotation
  def auth(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient = {
    val thisHttpClient = thisObj.asInstanceOf[ScriptableHttpClient]
    val username = args(0).asInstanceOf[String]
    val password = args(1).asInstanceOf[String]
    val scheme = args.isDefinedAt(2) ? authSchemeForName(args(2).asInstanceOf[String]) | defaultAuthScheme

    thisHttpClient.setAuth(username, password, scheme)
    thisHttpClient
  }

  @JSFunctionAnnotation
  def end(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture = {
    val thisHttpClient = thisObj.asInstanceOf[ScriptableHttpClient]

    import scala.concurrent.ExecutionContext.Implicits.global
    ScriptableFuture(context, thisHttpClient.execute().map(ScriptableHttpResult(context, _)))
  }

  @JSStaticFunction("get")
  def jsGet(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient =
    ScriptableHttpClient(context, args(0).asInstanceOf[String], "get")

  @JSStaticFunction
  def head(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient =
    ScriptableHttpClient(context, args(0).asInstanceOf[String], "head")

  @JSStaticFunction
  def post(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient =
    ScriptableHttpClient(context, args(0).asInstanceOf[String], "post")

  @JSStaticFunction("put")
  def jsPut(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient =
    ScriptableHttpClient(context, args(0).asInstanceOf[String], "put")

  @JSStaticFunction("delete")
  def jsDelete(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient =
    ScriptableHttpClient(context, args(0).asInstanceOf[String], "delete")

  @JSStaticFunction
  def options(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient =
    ScriptableHttpClient(context, args(0).asInstanceOf[String], "options")

  @JSStaticFunction
  def patch(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttpClient =
    ScriptableHttpClient(context, args(0).asInstanceOf[String], "patch")
}

class ScriptableHttpClient(url: String, method: String) extends ScriptableObject {
  import play.api.Play.current

  def this() = this("about:blank", "get")
  def this(url: String) = this(url, "get")

  override def getClassName: String = "HttpClient"

  private var holder: WSRequestHolder = WS.url(url).withMethod(method.toUpperCase)

  private def execute(): Future[WSResponse] = holder.execute()

  private def setHeaders(headers: Seq[(String, String)]) {
    holder = holder.withHeaders(headers: _*)
  }

  private def setQueryString(parameters: Seq[(String, String)]) {
    holder = holder.withQueryString(parameters: _*)
  }

  private def setFormBody(form: Map[String, Seq[String]]) {
    holder = holder.withBody(form)
  }

  private def setJsonBody(json: JsObject) {
    holder = holder.withBody(json)
  }

  private def setAuth(username: String, password: String, scheme: WSAuthScheme) {
    holder = holder.withAuth(username, password, scheme)
  }
}
