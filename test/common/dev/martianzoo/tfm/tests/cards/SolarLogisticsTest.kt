package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SolarLogisticsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Draws a card for a space event played by its owner`() {
    kim.exMachina("$SolarLogistics")
    // The drawn card replaces the played event in hand.
    kim.inTurn {
          doTasks("PlayProject<Class<ImportedGhg>>", "-5 MC")
          selectTask("-X? Titanium")
          doTasks("Ok", "ImportedGhg FROM ProjectCard", "PROD[Heat]", "3 Heat")
          selectTask("ProjectCard<Kim, Hand>!")
        }
        .expect("0 ProjectCard")
  }

  @Test
  internal fun `Draws a card for a space event played by an opponent`() {
    kim.exMachina("$SolarLogistics")

    stan.inTurn {
      doTasks(
          "PlayProject<Class<$TechnologyDemonstration>>",
          "-5 MC",
          "Ok", // No titanium paid.
          "$TechnologyDemonstration FROM ProjectCard",
          "2 ProjectCard",
      )
      stan.selectTask("ProjectCard<Kim>") // Hand the reaction to Solar Logistics' owner.
      kim.doTask("ProjectCard")
    }
  }
}
