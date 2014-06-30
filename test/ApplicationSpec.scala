import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._

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

    "create an account" in new WithApplication {
      val create = route(FakeRequest(POST, "/user/create")
        .withFormUrlEncodedBody("username" -> "myname", "password" -> "mypass")).get

      status(create) must equalTo(OK)
      contentType(create) must beSome.which(_ == "application/json")
      val resJson = Json.parse(contentAsString(create))
      (resJson \ "result").asOpt[String].get must contain("OK")
      (resJson \ "message").asOpt[String].get must contain("Account created")
      (resJson \ "username").asOpt[String].get must contain("myname")
    }

    "already exists account" in new WithApplication {
      val create = route(FakeRequest(POST, "/user/create")
        .withFormUrlEncodedBody("username" -> "myname", "password" -> "mypass")).get

      status(create) must equalTo(OK)
      contentType(create) must beSome.which(_ == "application/json")
      val resJson = Json.parse(contentAsString(create))
      (resJson \ "result").asOpt[String].get must contain("Error")
      (resJson \ "message").asOpt[String].get must contain("Already exists account")
    }

    "login when access to invalid username" in new WithApplication {
      val login = route(FakeRequest(POST, "/session/login")
        .withFormUrlEncodedBody("username" -> "invalidname", "password" -> "invalidpass")).get

      status(login) must equalTo(OK)
      contentType(login) must beSome.which(_ == "application/json")
      val resJson = Json.parse(contentAsString(login))
      (resJson \ "result").asOpt[String].get must contain("Error")
      (resJson \ "message").asOpt[String].get must contain("Cannot find an account")
      (resJson \ "username").asOpt[String].get must contain("invalidname")
    }

    "login when access to invalid password" in new WithApplication {
      val login = route(FakeRequest(POST, "/session/login")
        .withFormUrlEncodedBody("username" -> "myname", "password" -> "invalidpassword")).get

      status(login) must equalTo(OK)
      contentType(login) must beSome.which(_ == "application/json")
      val resJson = Json.parse(contentAsString(login))
      (resJson \ "result").asOpt[String].get must contain("Error")
      (resJson \ "message").asOpt[String].get must contain("Invalid password")
    }

    "login" in new WithApplication {
      val login = route(FakeRequest(POST, "/user/login")
        .withFormUrlEncodedBody("username" -> "myname", "password" -> "mypass")).get

      status(login) must equalTo(OK)
      contentType(login) must beSome.which(_ == "application/json")
      val resJson = Json.parse(contentAsString(login))
      (resJson \ "result").asOpt[String].get must contain("OK")
      (resJson \ "message").asOpt[String].get must contain("Hello myname")
    }

    "logout when username exists in session" in new WithApplication {
      val logout = route(FakeRequest(POST, "/user/logout").withSession("username" -> "myname")).get

      status(logout) must equalTo(OK)
      contentType(logout) must beSome.which(_ == "text/plain")
      contentAsString(logout) must equalTo("Goodbye myname")
      session(logout).isEmpty must equalTo(true)
    }

    "logout without login" in new WithApplication {
      val login = route(FakeRequest(POST, "/user/logout")).get

      status(login) must equalTo(FORBIDDEN)
    }
  }
}
