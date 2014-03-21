# InMemory Journal for Akka Persistence
A journal plugin for akka-persistence using simple TreeMaps.
This is a journal you should use for testing only. In tests it can be useful to avoid one failed test to cause other tests to fail as well.
Currently the journal will log if any messages were left unconfirmed when it closes down. For our internal tests we found it useful,
to let the journal exit the VM to signalize where unconfirmed messages occur. If there is enough interest we will add this as a configurable feature.

## Requirements
Akka 2.3.0 or higher

## Installation
There have no artifacts been published to a public repository, yet. To
use this journal you have to compile and publish it to your local
repository with `sbt publishLocal` and include it in your project:

    libraryDependencies += "io.github.michaelpisula" %% "akka-persistence-inmemory" % "0.1-SNAPSHOT"

## Configuration

Add to your application.conf

    akka.persistence.journal.plugin = "in-memory-journal"
