name := "admin"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",
  "org.reactivemongo" %% "reactivemongo" % "0.10.0"
)

org.scalastyle.sbt.ScalastylePlugin.Settings

scalariformSettings

Common.settings
