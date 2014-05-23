name := "beyond"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.apache.curator" % "curator-recipes" % "2.4.2",
  "org.apache.zookeeper" % "zookeeper" % "3.4.6",
  "org.mozilla" % "rhino" % "1.7R4",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",
  "org.reactivemongo" %% "reactivemongo" % "0.10.0"
).map(_.exclude("org.slf4j", "slf4j-log4j12"))

val toolsJar = if (System.getProperty("os.name") != "Mac OS X") {
  Seq(Attributed.blank(file(System.getProperty("java.home").dropRight(3) + "lib/tools.jar")))
} else {
  Nil
}

// adding the tools.jar to the unmanaged-jars seq
unmanagedJars in Compile ~= (toolsJar ++ _)

play.Project.playScalaSettings

org.scalastyle.sbt.ScalastylePlugin.Settings

scalariformSettings

