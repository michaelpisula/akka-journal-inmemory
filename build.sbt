organization := "com.github.michaelpisula"

name := "akka-persistence-inmemory"

version := "0.2.0"

scalaVersion := "2.11.1"

javacOptions in (Compile) ++= Seq("-source", "1.7")

javacOptions in (Compile, compile) ++= Seq("-target", "1.7")

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath + "/.m2/repository")))

parallelExecution in Test := false

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

libraryDependencies += "com.github.krasserm" %% "akka-persistence-testkit" % "0.3.3" % "test" withSources() withJavadoc()

libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.4" % "compile" withSources() withJavadoc()

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test"

libraryDependencies += "commons-io" % "commons-io" % "2.4" % "test"

// publishing settings

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
<url>http://github.com/michaelpisula/akka-journal-inmemory</url>
<licenses>
  <license>
    <name>Apache 2 License</name>
    <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
    <distribution>repo</distribution>
  </license>
</licenses>
<scm>
  <url>git@github.com:michaelpisula/akka-journal-inmemory.git</url>
  <connection>scm:git:git@github.com:michaelpisula/akka-journal-inmemory.git</connection>
</scm>
<developers>
  <developer>
    <id>michaelpisula</id>
    <name>Michael Pisula</name>
  </developer>
</developers>
<parent>
  <groupId>org.sonatype.oss</groupId>
  <artifactId>oss-parent</artifactId>
  <version>7</version>
</parent>)
