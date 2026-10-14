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
          doTasks(
              "ImportedGhg FROM ProjectCard",
              "PROD[Heat]",
              "3 Heat",
          )
          selectTask("ProjectCard<Kim, Hand>!")
          doTask("PlayedEvent<Class<ImportedGhg>> FROM ImportedGhg")
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
          "$TechnologyDemonstration FROM ProjectCard",
          "2 ProjectCard",
      )
      kim.doTask("ProjectCard")
      doTask("PlayedEvent<Class<$TechnologyDemonstration>> FROM $TechnologyDemonstration")
    }
  }
}
