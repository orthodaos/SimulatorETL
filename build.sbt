name := "SimulatorETL"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

val akkaVersion = "2.4.2"


libraryDependencies +=  "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"


libraryDependencies += "com.typesafe" % "config" % "1.3.0"


libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"
libraryDependencies += "ch.qos.logback" % "logback-core" % "1.1.3"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"

resourceDirectory in Compile := sourceDirectory.value / "main/config"

resourceDirectory in Test := sourceDirectory.value / "test/config"