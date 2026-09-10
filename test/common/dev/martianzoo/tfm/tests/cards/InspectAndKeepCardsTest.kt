package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.BusinessContacts
import dev.martianzoo.tfm.tests.cards.cardnames.InventionContest
import kotlin.test.Test

internal class InspectAndKeepCardsTest : CardTest() {
  @Test
  internal fun `Business Contacts inspects four project cards and keeps two`() {
    newGame(CorporateEraExpansion)
    p1.autoExecPolicy = NONE

    p1.runOperation("$BusinessContacts") {
      doTask("4 ProjectCard<Selecting>")
      p1.assertCounts(4 to "ProjectCard<Selecting>", 0 to "ProjectCard<Hand>")
      doTask("2 ProjectCard<Hand FROM Selecting>")
    }

    p1.assertCounts(0 to "ProjectCard<Selecting>", 2 to "ProjectCard<Hand>")
  }

  @Test
  internal fun `Invention Contest inspects three project cards and keeps one`() {
    newGame(CorporateEraExpansion)
    p1.autoExecPolicy = NONE

    p1.runOperation("$InventionContest") {
      doTask("3 ProjectCard<Selecting>")
      p1.assertCounts(3 to "ProjectCard<Selecting>", 0 to "ProjectCard<Hand>")
      doTask("ProjectCard<Hand FROM Selecting>")
    }

    p1.assertCounts(0 to "ProjectCard<Selecting>", 1 to "ProjectCard<Hand>")
  }
}
