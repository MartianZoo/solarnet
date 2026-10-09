package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.ProjectCardTest
import dev.martianzoo.tfm.tests.cards.cardnames.TitanShuttles
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class TitanShuttlesTest : ProjectCardTest() {
  @Test
  internal fun `Can convert floaters without Colonies or Venus Next`() {
    newTestGame(addOptions = "-ColoniesExpansion, -VenusNextExpansion, TitanShuttles")
    kim.exMachina("$TitanShuttles, 7 Floater<$TitanShuttles>")

    kim.cardAction2(TitanShuttles) { doTask("-5 Floater THEN 5 Titanium") }
        .expect("-5 Floater<$TitanShuttles>, 5 Titanium")
  }

  @Test
  internal fun `Can convert five floaters into five titanium with Colonies`() {
    newTestGame()
    kim.exMachina("$TitanShuttles, 7 Floater<$TitanShuttles>")

    kim.cardAction2(TitanShuttles) { doTask("-5 Floater THEN 5 Titanium") }
        .expect("-5 Floater<$TitanShuttles>, 5 Titanium")
  }

  @Test
  internal fun `Cannot underpay its floater cost`() {
    newTestGame()
    kim.exMachina("$TitanShuttles, 7 Floater<$TitanShuttles>")

    shouldThrow<NarrowingException> {
      kim.cardAction2(TitanShuttles) { doTask("-4 Floater THEN 5 Titanium") }
    }
  }
}
