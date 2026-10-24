package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MarsUniversityTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Research can alternate discard and draw starting with one other card in hand`() {
    kim.exMachina("$MarsUniversity")
    kim.setToExMachina(2, "ProjectCard")
    kim.setToExMachina(11, "MC")

    kim.inTurn {
          doTasks(
              *playCardTasks("Research"),
              "11 Pay<> FROM MC",
              "Research FROM ProjectCard",
          )
          doTask("2 ProjectCard<Kim, Hand>")
          doTasks("-ProjectCard", "-ProjectCard")
          doTask("ProjectCard<Kim, Hand>")
          doTask("ProjectCard<Kim, Hand>")
        }
        .expect("ProjectCard")
  }

  @Test
  internal fun `Cannot exchange a card with an empty hand`() {
    kim.setToExMachina(1, "ProjectCard")
    kim.setToExMachina(8, "MC")

    kim.inTurn {
      doTasks(*playCardTasks("MarsUniversity"), "8 Pay<> FROM MC")
      doTasks("MarsUniversity FROM ProjectCard")
      // Resolve the exchange offer: the empty hand leaves no card to discard.
      selectTask("(-ProjectCard<Kim, Hand>! THEN ProjectCard<Kim, Hand>!) OR Ok")
    }
  }

  @Test
  internal fun `May decline its discard with another project in hand`() {
    kim.exMachina("$MarsUniversity")
    kim.setToExMachina(2, "ProjectCard")
    kim.setToExMachina(3, "MC")

    kim.inTurn {
      doTasks(
          *playCardTasks("SearchForLife"),
          "3 Pay<> FROM MC",
          "SearchForLife FROM ProjectCard",
      )
      doTask("Ok")
    }
  }
}
