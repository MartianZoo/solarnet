package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class CrediCorTest : TfmSandboxTest() {
  @Test
  internal fun `Cannot fund a City standard project with its own rebate`() {
    newTestGame(kimCorporation = CrediCor)
    kim.setToExMachina(21, "MC")

    kim.inTurn {
      doTasks(*standardProjectTasks("CityProject"))
      shouldThrow<TaskException> { doTask("4 MC") }
      shouldThrow<TaskException> { doTask("25 Pay<> FROM MC") }
      abort()
    }

    kim.assertCounts(21 to "MC", 0 to "CityTile", 0 to "Owed", 0 to "PaymentScope")
  }

  @Test
  internal fun `Does not rebate a standard project costing less than twenty MC`() {
    newTestGame(kimCorporation = CrediCor)

    kim.inTurn {
          doTasks(
              *standardProjectTasks("PowerPlantProject"),
              "11 Pay<> FROM MC",
              "PROD[Energy]",
          )
        }
        .expect("-11 MC, PROD[Energy]")
  }

  @Test
  internal fun `Uses printed cost for its rebate even when discounts bring payment below twenty MC`() {
    newTestGame(kimCorporation = CrediCor)

    kim.exMachina("$EarthOffice, $ResearchOutpost")

    kim.inTurn {
      doTasks(
          *playCardTasks("EarthCatapult"),
          "19 Pay<> FROM MC",
          "EarthCatapult FROM ProjectCard",
          "4 MC",
      )
    }
  }

  @Test
  internal fun `Stacks its standard-project rebate with Standard Technology`() {
    newTestGame(kimCorporation = CrediCor)

    kim.exMachina("$StandardTechnology")

    kim.inTurn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Tharsis_2_1>",
          "PROD[MC]",
          "4 MC",
          "3 MC",
      )
    }
  }
}
