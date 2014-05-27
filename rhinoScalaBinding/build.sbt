name := "rhinoScalaBinding"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.mozilla" % "rhino" % "1.7R4"
)

org.scalastyle.sbt.ScalastylePlugin.Settings

scalariformSettings
