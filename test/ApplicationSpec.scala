import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

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

    "login" in new WithApplication {
      val login = route(FakeRequest(POST, "/session/login")
        .withFormUrlEncodedBody("username" -> "myname", "password" -> "mypass")).get

      status(login) must equalTo(OK)
      contentType(login) must beSome.which(_ == "text/plain")
      contentAsString(login) must equalTo("Hello myname")
    }

    "logout when username exists in session" in new WithApplication {
      val logout = route(FakeRequest(POST, "/session/logout").withSession("username" -> "myname")).get

      status(logout) must equalTo(OK)
      contentType(logout) must beSome.which(_ == "text/plain")
      contentAsString(logout) must equalTo("Goodbye myname")
      session(logout).isEmpty must equalTo(true)
    }

    "logout without login" in new WithApplication {
      val login = route(FakeRequest(POST, "/session/logout")).get

      status(login) must equalTo(FORBIDDEN)
    }
  }
}
