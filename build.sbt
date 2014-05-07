name := "beyond"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.apache.zookeeper" % "zookeeper" % "3.4.6",
  "org.mozilla" % "rhino" % "1.7R4",
  "org.reactivemongo" %% "reactivemongo" % "0.10.0"
)     

play.Project.playScalaSettings

org.scalastyle.sbt.ScalastylePlugin.Settings
