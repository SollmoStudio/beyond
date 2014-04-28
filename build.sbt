name := "beyond"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.reactivemongo" %% "reactivemongo" % "0.10.0"
)     

play.Project.playScalaSettings

org.scalastyle.sbt.ScalastylePlugin.Settings
