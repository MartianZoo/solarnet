package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MarsUniversityTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Research can alternate discard and draw starting with one other card in hand`() {
    kim.exMachina("$MarsUniversity")
    kim.setToExMachina(2, "ProjectCard")
    kim.setToExMachina(11, "MC")

    kim.playProject(Research, 11) {
          kim.doTask("-ProjectCard")
          kim.doTask("-ProjectCard")
        }
        .expect("ProjectCard")
  }

  @Test
  internal fun `Cannot exchange a card with an empty hand`() {
    kim.setToExMachina(1, "ProjectCard")
    kim.setToExMachina(8, "MC")

    kim.playProject(MarsUniversity, 8).expect("-ProjectCard")
  }

  @Test
  internal fun `May decline its discard with another project in hand`() {
    kim.exMachina("$MarsUniversity")
    kim.setToExMachina(2, "ProjectCard")
    kim.setToExMachina(3, "MC")

    kim.playProject(SearchForLife, 3) { declineTask() }.expect("-ProjectCard")
  }
}
