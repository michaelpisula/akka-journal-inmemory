organization := "io.github.michaelpisula"

name := "akka-persistence-inmemory"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.3"

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath + "/.m2/repository")))

parallelExecution in Test := false

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

libraryDependencies += "com.github.krasserm" %% "akka-persistence-testkit" % "0.2" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.0" % "compile"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.0" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test"

libraryDependencies += "commons-io" % "commons-io" % "2.4" % "test"
