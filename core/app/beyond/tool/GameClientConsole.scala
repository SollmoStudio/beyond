package beyond.tool

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.io.IO
import jline.console.ConsoleReader
import jline.console.completer.StringsCompleter
import scala.concurrent.duration._
import scala.io.Codec
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scalax.file.Path
import spray.can.Http
import spray.client.pipelining._
import spray.http.FormData
import spray.http.HttpCookie
import spray.http.HttpHeaders._
import spray.util._

object GameClientConsole extends App {
  import scala.collection.JavaConverters._

  implicit val system = ActorSystem("game-client-console")
  import system.dispatcher

  // FIXME: Show usage when -h option is given.
  val baseUrl = if (args.length == 0) {
    "http://localhost:9000"
  } else {
    args(0)
  }

  val prompt = "> "

  val consoleReader = new ConsoleReader

  val routes: Set[String] = {
    val routesPath = Path.fromString(System.getProperty("user.dir")) / "conf" / "routes"
    val lines = Source.fromFile(routesPath.jfile)(Codec.UTF8).getLines()
    lines.withFilter(!_.startsWith("#")).map(_.split("\\s+")).withFilter(_(0) == "POST").map(_(1)).toSet
  }

  consoleReader.addCompleter(new StringsCompleter(routes.asJava))

  login()

  def login() {
    val username = consoleReader.readLine("Username: ")
    val password = consoleReader.readLine("Password: ", new Character('*'))
    val data = Map("username" -> username, "password" -> password)

    val pipeline = sendReceive
    val responseFuture = pipeline {
      Post(baseUrl + "/user/login", FormData(data))
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
  case object InvalidCommand extends Command

  def parseActionCommand(input: String): ActionCommand = {
    // e.g. /user/login username=kseo password=kseopass
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
      val input = consoleReader.readLine(prompt)
      // ConsoleReader.readLine returns null if the end of the input stream has been reached.
      if (input == "exit" || input == null) {
        ExitCommand
      } else if (input.startsWith("/")) {
        parseActionCommand(input)
      } else {
        InvalidCommand
      }
    } catch {
      case _: Exception => InvalidCommand
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
      case InvalidCommand =>
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

