package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
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
          doTasks("PlayProject<Class<Research>>", "-11 MC", "Research FROM ProjectCard")
          selectTask("2! ProjectCard<Kim, Hand>")
          doTasks("-ProjectCard", "-ProjectCard")
          selectTask("ProjectCard<Kim, Hand>!")
          selectTask("ProjectCard<Kim, Hand>!")
        }
        .expect("ProjectCard")
  }

  @Test
  internal fun `Cannot exchange a card with an empty hand`() {
    kim.setToExMachina(1, "ProjectCard")
    kim.setToExMachina(8, "MC")

    kim.inTurn {
      doTasks("PlayProject<Class<MarsUniversity>>", "-8 MC")
      doTasks("MarsUniversity FROM ProjectCard")
      selectTask("(-ProjectCard<Kim, Hand>! THEN ProjectCard<Kim, Hand>!) OR Ok")
    }
  }

  @Test
  internal fun `May decline its discard with another project in hand`() {
    kim.exMachina("$MarsUniversity")
    kim.setToExMachina(2, "ProjectCard")
    kim.setToExMachina(3, "MC")

    kim.inTurn {
      doTasks("PlayProject<Class<SearchForLife>>", "-3 MC", "SearchForLife FROM ProjectCard")
      selectTask("(-ProjectCard<Kim, Hand>! THEN ProjectCard<Kim, Hand>!) OR Ok")
      doTask("Ok")
    }
  }
}
