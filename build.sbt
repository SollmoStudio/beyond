name := "beyond"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.akka23-SNAPSHOT",
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.akka23-SNAPSHOT"
)

lazy val root = project.in(file("."))
  .aggregate(beyondCore, beyondUser, beyondAdmin, rhinoScalaBinding)
  .dependsOn(beyondCore, beyondUser, beyondAdmin)
  .enablePlugins(PlayScala)

lazy val beyondCore = project.in(file("core"))
  .dependsOn(rhinoScalaBinding)
  .enablePlugins(PlayScala)

lazy val beyondAdmin: Project = project.in(file("modules/admin"))
  .enablePlugins(PlayScala)

lazy val beyondUser: Project = project.in(file("modules/user"))
  .dependsOn(beyondCore)
  .enablePlugins(PlayScala)

lazy val rhinoScalaBinding = project

org.scalastyle.sbt.ScalastylePlugin.Settings

// Create a default Scala style task to run with tests
lazy val testScalaStyle = taskKey[Unit]("testScalaStyle")

testScalaStyle := {
  org.scalastyle.sbt.PluginKeys.scalastyle.toTask("").value
}

(test in Test) <<= (test in Test) dependsOn testScalaStyle

scalariformSettings

Common.settings

scalacOptions ++= Seq(
  "-feature"
)
