package beyond.tool

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.io.IO
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import spray.can.Http
import spray.client.pipelining._
import spray.http.FormData
import spray.http.HttpCookie
import spray.http.HttpHeaders._
import spray.util._

object TestGameClient extends App {
  implicit val system = ActorSystem("test-game-client")
  import system.dispatcher

  // FIXME: Don't hardcode the base URL. Take this as an optional argument.
  val baseUrl = "http://localhost:9000"
  val prompt = "> "

  login()

  def login() {
    val username = Console.readLine("Username: ")
    val password = Console.readLine("Password: ") // FIXME: Don't show plain password texts.
    val data = Map("username" -> username, "password" -> password)

    val pipeline = sendReceive
    val responseFuture = pipeline {
      Post(baseUrl + "/session/login", FormData(data))
    }

    responseFuture.onComplete {
      case Success(response) =>
        val status = response.entity.asString
        val cookies: List[HttpCookie] = response.headers.collect { case `Set-Cookie`(hc) => hc }
        Console.println(status)
        if (status == "Hello " + username) {
          Console.println("Login succeeded")
          actionLoop(cookies)
        } else {
          Console.println("Login failed")
          login()
        }
      case Failure(error) =>
        Console.println("Login request failed")
        shutdown()
    }
  }

  sealed trait Command
  case class ActionCommand(action: String, params: Map[String, String]) extends Command
  case object ExitCommand extends Command
  case object ErrorCommand extends Command

  def parseActionCommand(input: String): ActionCommand = {
    // e.g. /session/login username=kseo password=kseopass
    val words = input.split("\\s+")
    val action = words(0)
    val params = words.drop(1).map(param => {
      val keyAndValue = param.split("=")
      keyAndValue(0) -> keyAndValue(1)
    })

    ActionCommand(action, params.toMap)
  }

  def readCommand(): Command = {
    try {
      val input = Console.readLine(prompt)
      // Console.readLine returns null if the end of the input stream has been reached.
      if (input == "exit" || input == null) {
        ExitCommand
      } else if (input.startsWith("/")) {
        parseActionCommand(input)
      } else {
        ErrorCommand
      }
    } catch {
      case _: Exception => ErrorCommand
    }
  }

  def actionLoop(cookies: List[HttpCookie]) {
    val command = readCommand()
    command match {
      case ActionCommand(action, params) =>
        val pipeline = sendReceive
        val responseFuture = pipeline {
          Post(baseUrl + action, FormData(params)).withHeaders(Cookie(cookies))
        }
        responseFuture.onComplete {
          case Success(response) =>
            val result = response.entity.asString
            val statusCode = response.status
            Console.println("Status Code: " + statusCode)
            Console.println("Result: ")
            Console.println(result)
            actionLoop(cookies)
          case Failure(error) =>
            Console.println("Action request failed")
            shutdown()
        }
      case ErrorCommand =>
        Console.println("Invalid command")
        actionLoop(cookies)
      case ExitCommand =>
        shutdown()
    }
  }

  def shutdown() {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }
}

