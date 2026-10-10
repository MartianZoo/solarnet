package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SupercapacitorsTest : TfmGameplayTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame(addOptions = "-VenusNextExpansion", playerCount = 2)
    kim.turn { playProject(Supercapacitors, 4) }
    stan.pass()
    kim.pass()
    kim.buyCards(0)
    stan.buyCards(0)
    // Generation 1 produced an energy. Generation 2 starts with Stan.
    stan.pass()
    kim.pass()
  }

  @Test
  internal fun `Can let all existing energy become heat`() {
    kim.declineTask()
    kim.assertCounts(1 to "Energy", 3 to "Heat")
  }

  @Test
  internal fun `Can retain existing energy but cannot convert newly produced heat to energy`() {
    shouldThrow<NarrowingException> { kim.doTask("2 Energy FROM Heat!") }
    kim.doTask("Energy FROM Heat!")
    kim.assertCounts(2 to "Energy", 2 to "Heat")
  }
}
