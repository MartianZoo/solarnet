package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
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
    kim.doTasks("PlayProject<Class<$Supercapacitors>>")
    kim.selectTask("PassBaton") // Decline the second action.
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("-4 ProjectCard<Selecting>")
    stan.doTask("-4 ProjectCard<Selecting>")
    // Generation 1 produced an energy. Generation 2 starts with Stan.
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
  }

  @Test
  internal fun `Can let all existing energy become heat`() {
    kim.doTask("Ok") // Convert all existing energy to heat.
    kim.assertCounts(1 to "Energy", 3 to "Heat")
  }

  @Test
  internal fun `Can retain existing energy but cannot convert newly produced heat to energy`() {
    shouldThrow<NarrowingException> { kim.doTask("2! Energy FROM Heat") }
    kim.doTask("Energy FROM Heat!")
    kim.assertCounts(2 to "Energy", 2 to "Heat")
  }
}
