import sbt._
import Keys._

object Common {
  val settings: Seq[Setting[_]] = {
    organization := "com.beyondframework"
    version := "1.0-SNAPSHOT"
  }
}

