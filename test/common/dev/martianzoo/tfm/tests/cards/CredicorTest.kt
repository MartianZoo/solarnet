package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CrediCorTest : TfmSandboxTest() {
  @Test
  internal fun `Uses printed cost for its rebate even when discounts bring payment below twenty MC`() {
    newTestGame(kimCorporation = CrediCor, startAtCorporation = true)
    kim.inTurn {
      doTask("PlayCard<Class<CorporationCard>, Class<CrediCor>>")
      doTask("CrediCor FROM CorporationCard")
      doTask("57 MC")
      doTask("-30 MC")
      doTask("10 ProjectCard<Hand FROM Selecting>")
    }
    advanceUntilActionPhase()

    kim.exMachina("$EarthOffice, $ResearchOutpost")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<EarthCatapult>>",
          "-19 MC",
          "EarthCatapult FROM ProjectCard",
          "4 MC",
      )
    }
  }

  @Test
  internal fun `Stacks its standard-project rebate with Standard Technology`() {
    newTestGame(kimCorporation = CrediCor, startAtCorporation = true)
    kim.inTurn {
      doTask("PlayCard<Class<CorporationCard>, Class<CrediCor>>")
      doTask("CrediCor FROM CorporationCard")
      doTask("57 MC")
      doTask("-30 MC")
      doTask("10 ProjectCard<Hand FROM Selecting>")
    }
    advanceUntilActionPhase()

    kim.exMachina("$StandardTechnology")

    kim.inTurn {
      doTasks(
          "UseStandardProject<CityProject>",
          "-25 MC",
          "CityTile<Tharsis_2_1>",
          "PROD[MC]",
          "4 MC",
          "3 MC",
      )
    }
  }
}
