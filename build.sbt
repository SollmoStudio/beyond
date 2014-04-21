name := "beyond"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)     

play.Project.playScalaSettings

org.scalastyle.sbt.ScalastylePlugin.Settings
