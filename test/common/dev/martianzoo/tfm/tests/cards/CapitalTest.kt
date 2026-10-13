package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CapitalTest : TfmSandboxTest() {
  @Test
  internal fun `Scores adjacent oceans but not distant oceans`() {
    newTestGame()
    kim.exMachina(
        "$Capital, CapitalTile<$Capital, Tharsis_3_3>, OceanTile<Tharsis_3_2>, OceanTile<Tharsis_4_3>, OceanTile<Tharsis_6_8>, OceanTile<Tharsis_9_9>"
    )

    victoryPoints() shouldBe listOf(22, 20, 20)
  }

  @Test
  internal fun `Counts as a special tile toward the Manager milestone`() {
    newTestGame(addOptions = "UtopiaMap")
    kim.exMachina(
        "$Capital, CapitalTile<$Capital, Utopia_1_1>, EcologicalZone_SpecialTile<Utopia_2_2>, NaturalPreserve_SpecialTile<Utopia_3_3>"
    )

    kim.inTurn {
      doTasks("ClaimMilestone<Class<Manager>>", "-MC / Owed<Class<MC>>")
      selectOnlyTask()
    }
  }

  @Test
  internal fun `Counts as a neighboring special tile for the Founder award`() {
    newTestGame(addOptions = "CimmeriaMap", playerCount = 2)
    kim.exMachina("$Capital, CapitalTile<$Capital, Cimmeria_3_3>")
    stan.exMachina("GreeneryTile<Stan, Cimmeria_2_2>")
    stan.inTurn {
      doTasks("FundAward<Class<Founder>>", "-8 MC")
      selectOnlyTask()
    }

    victoryPoints() shouldBe listOf(21, 26)
  }

  @Test
  internal fun `Triggers Tharsis Republic's city production benefit`() {
    newTestGame(kimCorporation = TharsisRepublic)
    kim.inTurn {
      doTasks("RequiredActionsSignal", "CityTile<Tharsis_1_1>", "PROD[MC]", "3 MC", "2 Steel")
    }
    stan.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>"
    )
    stan.setToExMachina(2, "PROD[Energy]")

    stan
        .inTurn {
          doTasks("PlayProject<Class<Capital>>", "-26 MC")
          selectTask("-X? Steel")
          doTasks(
              "Ok",
              "Capital FROM ProjectCard",
              "PROD[-2 Energy]",
              "PROD[5 MC]",
              "CapitalTile<Tharsis_3_3>",
          )
          selectTask("Production<Kim, Class<MC>>!")
          kim.selectTask("PROD[MC]")
        }
        .expect("PROD[MC<Kim>]")
  }
}
