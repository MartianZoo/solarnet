package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.BusinessContacts
import dev.martianzoo.tfm.tests.cards.cardnames.InventionContest
import kotlin.test.Test

internal class InspectAndKeepCardsTest : CardTest() {
  @Test
  internal fun `Business Contacts gains two project cards`() {
    newGame(CorporateEraExpansion)

    p1.runOperation("$BusinessContacts").expect("2 ProjectCard")
  }

  @Test
  internal fun `Invention Contest gains one project card`() {
    newGame(CorporateEraExpansion)

    p1.runOperation("$InventionContest").expect("ProjectCard")
  }
}
