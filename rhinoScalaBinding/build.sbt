name := "rhinoScalaBinding"

libraryDependencies ++= Seq(
  "org.mozilla" % "rhino" % "1.7R4"
)

unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "examples" / "scala"

org.scalastyle.sbt.ScalastylePlugin.Settings

org.scalastyle.sbt.PluginKeys.config := file("rhinoScalaBinding/scalastyle-config.xml")

scalariformSettings

Common.settings
