name := "Bacon"

version := "1.0"

scalaVersion := "2.11.1"

resolvers += "Akka Snapshot Repository" at "http://repo.akka..io/snapshots"

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.4"
libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "2.4.7"
libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7"

libraryDependencies += "info.cukes" % "cucumber-scala_2.11" % "1.2.4"
libraryDependencies += "info.cukes" % "cucumber-junit" % "1.2.4"
libraryDependencies += "junit" % "junit" % "4.12"