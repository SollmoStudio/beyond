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
import play.{ api => playApi }
import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.Future
import scalaz.syntax.std.boolean._

object ScriptableHttp {
  private[lib] def apply(context: Context, url: String): ScriptableHttp = {
    val beyondContextFactory = context.getFactory.asInstanceOf[BeyondContextFactory]
    val scope = beyondContextFactory.global
    context.newObject(scope, "Http", url).asInstanceOf[ScriptableHttp]
  }

  def jsConstructor(context: Context, args: JSArray, constructor: JSFunction, inNewExpr: Boolean): ScriptableHttp = {
    val url: String = args(0).asInstanceOf[String]

    inNewExpr ? new ScriptableHttp(url) | apply(context, url)
  }

  private def execute(context: Context, http: ScriptableHttp, method: String): ScriptableFuture = {
    import scala.concurrent.ExecutionContext.Implicits.global
    ScriptableFuture(context, http.execute(method).map(ScriptableHttpResult(context, _)))
  }

  private def convertScriptableToSeq(scriptable: Scriptable): Seq[(String, String)] =
    scriptable.getIds.map {
      case key: String =>
        key -> ScriptRuntime.toString(scriptable.get(key, scriptable))
    }

  @JSFunctionAnnotation
  def withHeaders(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttp = {
    val thisHttp = thisObj.asInstanceOf[ScriptableHttp]
    val headers = args(0).asInstanceOf[Scriptable]

    thisHttp.withHeadersInternal(convertScriptableToSeq(headers))
    thisHttp
  }

  @JSFunctionAnnotation
  def withQueryString(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttp = {
    val thisHttp = thisObj.asInstanceOf[ScriptableHttp]
    val parameters = args(0).asInstanceOf[Scriptable]

    thisHttp.withQueryStringInternal(convertScriptableToSeq(parameters))
    thisHttp
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
  def withForm(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttp = {
    val thisHttp = thisObj.asInstanceOf[ScriptableHttp]
    val form = args(0).asInstanceOf[Scriptable]

    thisHttp.withFormInternal(convertScriptableToForm(form))
    thisHttp
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

  @JSFunctionAnnotation("withJSON") // Naming convention different between JS and Scala
  def withJson(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttp = {
    val thisHttp = thisObj.asInstanceOf[ScriptableHttp]
    val json = args(0).asInstanceOf[Scriptable]

    thisHttp.withJsonInternal(convertScriptableToJsObject(json))
    thisHttp
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
  def withAuth(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableHttp = {
    val thisHttp = thisObj.asInstanceOf[ScriptableHttp]
    val username = args(0).asInstanceOf[String]
    val password = args(1).asInstanceOf[String]
    val scheme = args.isDefinedAt(2) ? authSchemeForName(args(2).asInstanceOf[String]) | defaultAuthScheme

    thisHttp.withAuthInternal(username, password, scheme)
    thisHttp
  }

  @JSFunctionAnnotation("get")
  def jsGet(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture =
    execute(context, thisObj.asInstanceOf[ScriptableHttp], "get")

  @JSFunctionAnnotation
  def head(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture =
    execute(context, thisObj.asInstanceOf[ScriptableHttp], "head")

  @JSFunctionAnnotation
  def post(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture =
    execute(context, thisObj.asInstanceOf[ScriptableHttp], "post")

  @JSFunctionAnnotation("put")
  def jsPut(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture =
    execute(context, thisObj.asInstanceOf[ScriptableHttp], "put")

  @JSFunctionAnnotation("delete")
  def jsDelete(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture =
    execute(context, thisObj.asInstanceOf[ScriptableHttp], "delete")

  @JSFunctionAnnotation
  def options(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture =
    execute(context, thisObj.asInstanceOf[ScriptableHttp], "options")

  @JSFunctionAnnotation
  def patch(context: Context, thisObj: Scriptable, args: JSArray, function: JSFunction): ScriptableFuture =
    execute(context, thisObj.asInstanceOf[ScriptableHttp], "patch")

  // If there's no running application, create one.
  // Mainly for test and console
  lazy private val testApp =
    new playApi.DefaultApplication(new File("."), this.getClass.getClassLoader, None, playApi.Mode.Test)
}

class ScriptableHttp(val url: String) extends ScriptableObject {
  implicit private val app = playApi.Play.maybeApplication.getOrElse(ScriptableHttp.testApp)

  def this() = this(null)

  override def getClassName: String = "Http"

  private var holder: WSRequestHolder = WS.url(url)

  private def execute(method: String): Future[WSResponse] = holder.withMethod(method.toUpperCase).execute()

  private def withHeadersInternal(headers: Seq[(String, String)]) {
    holder = holder.withHeaders(headers: _*)
  }

  private def withQueryStringInternal(parameters: Seq[(String, String)]) {
    holder = holder.withQueryString(parameters: _*)
  }

  private def withFormInternal(form: Map[String, Seq[String]]) {
    holder = holder.withBody(form)
  }

  private def withJsonInternal(json: JsObject) {
    holder = holder.withBody(json)
  }

  private def withAuthInternal(username: String, password: String, scheme: WSAuthScheme) {
    holder = holder.withAuth(username, password, scheme)
  }
}
