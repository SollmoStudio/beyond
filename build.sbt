name := "beyond"

lazy val beyond = project.in(file("."))
  .aggregate(beyondCore, beyondAdmin, rhinoScalaBinding)
  .dependsOn(beyondCore, beyondAdmin)

lazy val beyondCore = project.in(file("core"))
  .dependsOn(rhinoScalaBinding, beyondAdmin)

lazy val beyondAdmin = project

lazy val rhinoScalaBinding = project

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",
  "org.reactivemongo" %% "reactivemongo" % "0.10.0"
)

play.Project.playScalaSettings

org.scalastyle.sbt.ScalastylePlugin.Settings

// Create a default Scala style task to run with tests
lazy val testScalaStyle = taskKey[Unit]("testScalaStyle")

testScalaStyle := {
  org.scalastyle.sbt.PluginKeys.scalastyle.toTask("").value
}

(test in Test) <<= (test in Test) dependsOn testScalaStyle

scalariformSettings

Common.settings

