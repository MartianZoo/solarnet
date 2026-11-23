package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class InterplanetaryTradeTest : TfmSandboxTest() {
  @Test
  internal fun `Ignores the tags of a previously played event`() {
    newTestGame()
    kim.exMachina("$Ecoline, $Mine, $SearchForLife")
    kim.setToExMachina(43, "MC")
    kim.inTurn {
      doTasks(
          *playCardTasks("$ImportedHydrogen"),
          "16 Pay<> FROM MC",
          "3 Plant",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
          "2 Steel",
      )
    }

    kim.inTurn {
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "27 Pay<> FROM MC",
          "EACH @WildTagElection { Tag<@WildTagElection>? }",
          "PROD[4 MC]",
      )
    }
  }

  @Test
  internal fun `Counts all eleven tag types with a wild Event tag and no Venus`() {
    interplanetaryTradeWithWildEvent(false)
  }

  @Test
  internal fun `Counts all twelve tag types with a wild Event tag and Venus`() {
    interplanetaryTradeWithWildEvent(true)
  }

  private fun interplanetaryTradeWithWildEvent(venus: Boolean) {
    newTestGame("$ResearchNetwork" + if (venus) "" else ", -VenusNextExpansion")
    // Establish the supporting tableau; only Interplanetary Trade's tag count is under test.
    kim.exMachina(
        "$IoMiningIndustries, $Lichen, $Tardigrades, $Research, $ResearchNetwork, $Pets, $PowerPlant, $ImmigrantCity"
    )
    if (venus) kim.exMachina("$FloatingHabs")
    kim.inTurn {
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "27 Pay<> FROM MC",
          "EACH",
          "EventTag<WildTagElection<WildTag<$ResearchNetwork>>>",
          "PROD[${if (venus) 12 else 11} MC]",
      )
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }
}
