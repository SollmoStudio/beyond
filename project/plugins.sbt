// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.4")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"
