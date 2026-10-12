package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SpliceTacticalGenomicsTest : TfmSandboxTest() {
  @Test
  internal fun `Can choose money for its own microbe tag`() {
    newTestGameBeforeCorporationPlay()

    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<SpliceTacticalGenomics>>",
          "SpliceTacticalGenomics FROM CorporationCard",
          "44 MC",
      )
      selectTask("SpliceTacticalGenomics_RequiredAction!")
      selectTask("2 MC!")
      doTasks("2 MC", "-15 MC", "5 ProjectCard<Hand FROM Selecting>")
    }
  }

  @Test
  internal fun `Another player's microbe tag can pay both players`() {
    newTestGameBeforeCorporationPlay()
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<SpliceTacticalGenomics>>",
          "SpliceTacticalGenomics FROM CorporationCard",
          "44 MC",
      )
      selectTask("SpliceTacticalGenomics_RequiredAction!")
      selectTask("2 MC!")
      doTasks("2 MC", "-15 MC", "5 ProjectCard<Hand FROM Selecting>")
    }
    advanceUntilActionPhase()
    stan.exMachina("3 OxygenStep")
    stan.setToExMachina(5, "MC")
    stan.setToExMachina(1, "ProjectCard")

    stan
        .inTurn {
          doTasks("PlayProject<Class<Decomposers>>", "-5 MC", "Decomposers FROM ProjectCard")
          selectTask("Microbe<Decomposers>.")
          selectTask("2 MC<Kim>!")
          kim.selectTask("2 MC!")
          doTask("2 MC")
        }
        .expect("-3 MC<Stan>, Microbe<$Decomposers<Stan>>")
  }

  @Test
  internal fun `The other player can take a microbe instead of money`() {
    newTestGameBeforeCorporationPlay()
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<SpliceTacticalGenomics>>",
          "SpliceTacticalGenomics FROM CorporationCard",
          "44 MC",
      )
      selectTask("SpliceTacticalGenomics_RequiredAction!")
      selectTask("2 MC!")
      doTasks("2 MC", "-15 MC", "5 ProjectCard<Hand FROM Selecting>")
    }
    advanceUntilActionPhase()
    stan.exMachina("3 OxygenStep")
    stan.setToExMachina(5, "MC")
    stan.setToExMachina(1, "ProjectCard")

    stan
        .inTurn {
          doTasks("PlayProject<Class<Decomposers>>", "-5 MC", "Decomposers FROM ProjectCard")
          selectTask("Microbe<Decomposers>.")
          selectTask("2 MC<Kim>!")
          kim.selectTask("2 MC!")
          doTask("Microbe<Decomposers>!")
        }
        .expect("-5 MC<Stan>, 2 Microbe<$Decomposers<Stan>>")
  }

  @Test
  internal fun `The microbe must go on the card just played`() {
    newTestGameBeforeCorporationPlay()
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<SpliceTacticalGenomics>>",
          "SpliceTacticalGenomics FROM CorporationCard",
          "44 MC",
      )
      selectTask("SpliceTacticalGenomics_RequiredAction!")
      selectTask("2 MC!")
      doTasks("2 MC", "-15 MC", "5 ProjectCard<Hand FROM Selecting>")
    }
    advanceUntilActionPhase()
    stan.exMachina("3 OxygenStep, $RegolithEaters")
    stan.setToExMachina(5, "MC")
    stan.setToExMachina(1, "ProjectCard")

    stan
        .inTurn {
          doTasks("PlayProject<Class<Decomposers>>", "-5 MC", "Decomposers FROM ProjectCard")
          selectTask("Microbe<Decomposers>.")
          selectTask("2 MC<Kim>")
          kim.doTask("2 MC")
          selectOnlyTask()
          shouldThrow<NarrowingException> { doTask("Microbe<$RegolithEaters>!") }
          doTask("Microbe<Decomposers>")
        }
        .expect("2 Microbe<$Decomposers<Stan>>, 0 Microbe<$RegolithEaters<Stan>>")
  }

  @Test
  internal fun `Can forgo money even when the played card cannot hold microbes`() {
    newTestGameBeforeCorporationPlay()
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<SpliceTacticalGenomics>>",
          "SpliceTacticalGenomics FROM CorporationCard",
          "44 MC",
      )
      selectTask("SpliceTacticalGenomics_RequiredAction!")
      selectTask("2 MC!")
      doTasks("2 MC", "-15 MC", "5 ProjectCard<Hand FROM Selecting>")
    }
    advanceUntilActionPhase()
    stan.setToExMachina(12, "MC")
    stan.setToExMachina(1, "ProjectCard")

    stan
        .inTurn {
          doTasks("PlayProject<Class<IndustrialMicrobes>>", "-12 MC")
          selectTask("-X Steel?")
          doTasks("Ok", "IndustrialMicrobes FROM ProjectCard", "PROD[Energy]", "PROD[Steel]")
          selectTask("2 MC<Kim>!")
          kim.selectTask("2 MC!")
          selectOnlyTask()
          doTask("Ok")
        }
        .expect("-12 MC<Stan>, 0 Microbe<Stan>")
  }
}
