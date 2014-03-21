package akka.persistence.journal.inmemory

import com.typesafe.config.ConfigFactory
import akka.persistence.journal.JournalSpec

class InMemoryJournalSpec extends JournalSpec {
  lazy val config = ConfigFactory.parseString( """
                                                 |akka.persistence.journal.plugin = "in-memory-journal"
                                                 |akka.persistence.snapshot-store.local.dir = "target/snapshots"
                                               """.stripMargin)

}
