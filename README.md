# InMemory Journal for Akka Persistence
A journal plugin for akka-persistence using simple TreeMaps.
This is a journal you should use for testing only. In tests it can be useful to avoid one failed test to cause other tests to fail as well.
Currently the journal will log if any messages were left unconfirmed when it closes down. For our internal tests we found it useful,
to let the journal exit the VM to signalize where unconfirmed messages occur. If there is enough interest we will add this as a configurable feature.

## Requirements
Akka 2.3.0 or higher

## Installation
The artifact has been published to Maven Central. Include it in your build.sbt like this:

    libraryDependencies += "com.github.michaelpisula" %% "akka-persistence-inmemory" % "0.1"

## Configuration

Add to your application.conf

    akka.persistence.journal.plugin = "in-memory-journal"
