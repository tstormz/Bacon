name := "Bacon"

version := "1.0"

scalaVersion := "2.11.1"

resolvers += "Akka Snapshot Repository" at "http://repo.akka..io/snapshots"

// Cassandra
libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-core" % "3.1.4"

// Akka Actors
libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.4.7"
libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "2.4.7"
libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.4.7"
libraryDependencies += "ch.megard" %% "akka-http-cors" % "0.1.2"

// JSON
libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.11"
libraryDependencies += "org.json4s" %% "json4s-ext" % "3.2.11"
libraryDependencies += "de.heikoseeberger" %% "akka-http-json4s" % "1.4.2"

// Testing
libraryDependencies += "info.cukes" % "cucumber-scala_2.11" % "1.2.4"
libraryDependencies += "info.cukes" % "cucumber-junit" % "1.2.4"
libraryDependencies += "junit" % "junit" % "4.12"