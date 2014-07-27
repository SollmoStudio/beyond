import beyond.JsonResponse
import beyond.MongoMixin

import org.specs2.execute._
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner.RunWith

import play.api.libs.json._
import play.api.mvc.Session
import play.api.test._
import play.api.test.Helpers._

import reactivemongo.core.commands.DropDatabase

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication {
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("Your new application is ready.")
    }

    "respond to ping with pong" in new WithApplication {
      val ping = route(FakeRequest(POST, "/ping")).get

      status(ping) must equalTo(OK)
      contentType(ping) must beSome.which(_ == "text/plain")
      contentAsString(ping) must equalTo("pong")
    }

    abstract class WithEmptyDatabase extends WithApplication with MongoMixin {
      override def around[T: AsResult](t: => T): Result = super.around {
        clearDatabase
        t
      }

      def clearDatabase() {
        import scala.concurrent.ExecutionContext.Implicits.global
        val result = db.command(new DropDatabase)
        Await.result(result, Duration(10, "seconds"))
      }
    }

    "create an account" in new WithEmptyDatabase {
      val create = route(FakeRequest(POST, "/user/create")
        .withFormUrlEncodedBody("username" -> "testuser", "password" -> "testpassword")).get

      status(create) must equalTo(OK)
      contentType(create) must beSome.which(_ == "application/json")
      val jsonResult = Json.parse(contentAsString(create))
      (jsonResult \ "status").asOpt[String].get must equalTo(JsonResponse.ok.status)
      (jsonResult \ "message").asOpt[String].get must equalTo("User Created")
      (jsonResult \ "username").asOpt[String].get must equalTo("testuser")
    }

    "create an account with a username existing already" in new WithEmptyDatabase {
      val create = route(FakeRequest(POST, "/user/create")
        .withFormUrlEncodedBody("username" -> "testuser", "password" -> "testpassword")).get
      status(create) must equalTo(OK)

      val duplicatedCreate = route(FakeRequest(POST, "/user/create")
        .withFormUrlEncodedBody("username" -> "testuser", "password" -> "testpassword")).get

      status(duplicatedCreate) must equalTo(OK)
      contentType(duplicatedCreate) must beSome.which(_ == "application/json")
      val jsonResult = Json.parse(contentAsString(duplicatedCreate))
      (jsonResult \ "status").asOpt[String].get must equalTo(JsonResponse.badRequest.status)
      (jsonResult \ "message").asOpt[String].get must equalTo("Username Duplicated")
    }

    abstract class WithAccountCreated(username: String, password: String) extends WithEmptyDatabase {
      override def around[T: AsResult](t: => T): Result = super.around {
        createAccount(username, password)
        t
      }

      def createAccount(username: String, password: String) {
        val create = route(FakeRequest(POST, "/user/create")
          .withFormUrlEncodedBody("username" -> username, "password" -> password)).get
        Await.result(create, Duration(10, "seconds"))
      }
    }

    "login" in new WithAccountCreated("testuser", "testpassword") {
      val login = route(FakeRequest(POST, "/user/login")
        .withFormUrlEncodedBody("username" -> "testuser", "password" -> "testpassword")).get

      status(login) must equalTo(OK)
      contentType(login) must beSome.which(_ == "application/json")
      val jsonResult = Json.parse(contentAsString(login))
      (jsonResult \ "status").asOpt[String].get must equalTo(JsonResponse.ok.status)
      (jsonResult \ "message").asOpt[String].get must equalTo("User Login")
      (jsonResult \ "username").asOpt[String].get must equalTo("testuser")

      val loginSession = session(login)
      loginSession("username") must equalTo("testuser")
    }

    "login with wrong username" in new WithAccountCreated("testuser", "testpassword") {
      val login = route(FakeRequest(POST, "/user/login")
        .withFormUrlEncodedBody("username" -> "wronguser", "password" -> "testpassword")).get

      status(login) must equalTo(OK)
      contentType(login) must beSome.which(_ == "application/json")
      val jsonResult = Json.parse(contentAsString(login))
      (jsonResult \ "status").asOpt[String].get must equalTo(JsonResponse.unauthorized.status)
      (jsonResult \ "message").asOpt[String].get must equalTo("Invalid Username Or Password")

      session(login).isEmpty must equalTo(true)
    }

    "login with wrong password" in new WithAccountCreated("testuser", "testpassword") {
      val login = route(FakeRequest(POST, "/user/login")
        .withFormUrlEncodedBody("username" -> "testuser", "password" -> "wrongpassword")).get

      status(login) must equalTo(OK)
      contentType(login) must beSome.which(_ == "application/json")
      val jsonResult = Json.parse(contentAsString(login))
      (jsonResult \ "status").asOpt[String].get must equalTo(JsonResponse.unauthorized.status)
      (jsonResult \ "message").asOpt[String].get must equalTo("Invalid Username Or Password")

      session(login).isEmpty must equalTo(true)
    }

    class FakeRequestWithLoginSession(username: String, password: String) {
      def loginSession: Session = {
        val login = route(FakeRequest(POST, "/user/login")
          .withFormUrlEncodedBody("username" -> username, "password" -> password)).get
        session(login)
      }

      def apply(method: String, path: String) =
        FakeRequest(method, path).withSession(loginSession.data.toSeq: _*)
    }

    object FakeRequestWithLoginSession {
      def apply(username: String, password: String) =
        new FakeRequestWithLoginSession(username, password)
    }

    "logout" in new WithAccountCreated("testuser", "testpassword") {
      val request = FakeRequestWithLoginSession("testuser", "testpassword")
      val logout = route(request(POST, "/user/logout")).get

      status(logout) must equalTo(OK)
      contentType(logout) must beSome.which(_ == "application/json")
      val jsonResult = Json.parse(contentAsString(logout))
      (jsonResult \ "status").asOpt[String].get must equalTo(JsonResponse.ok.status)
      (jsonResult \ "message").asOpt[String].get must equalTo("User Logout")
      (jsonResult \ "username").asOpt[String].get must equalTo("testuser")

      session(logout).isEmpty must equalTo(true)
    }

    "logout without login" in new WithApplication {
      val logout = route(FakeRequest(POST, "/user/logout")).get

      status(logout) must equalTo(FORBIDDEN)
    }
  }
}
