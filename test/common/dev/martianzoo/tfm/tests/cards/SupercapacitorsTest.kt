package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SupercapacitorsTest : TfmGameplayTest(NONE) {
  @BeforeTest
  fun initializeGame() {
    newTestGame(addOptions = "-VenusNextExpansion", playerCount = 2)
    kim.doTasks(
        *playCardTasks("$Supercapacitors"),
        "4 Pay<> FROM MC",
        "$Supercapacitors FROM ProjectCard",
        "PROD[MC]",
    )
    kim.doTask("PassBaton") // Decline the second action.
    stan.doTask("Pass")
    kim.doTask("Pass")
    kim.doTasks("22 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    stan.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    players.forEach { it.doTask("4 ProjectCard<Selecting>") }
    kim.doTask("-4 ProjectCard<Selecting>")
    stan.doTask("-4 ProjectCard<Selecting>")
    // Generation 1 produced an energy. Generation 2 starts with Stan.
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTask("Heat FROM Energy") }
    kim.doTasks("22 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    stan.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    // Kim's Supercapacitors choice about the old energy remains for each test.
  }

  @Test
  internal fun `Can let all existing energy become heat`() {
    kim.doTask("Ok") // Convert all existing energy to heat.
    kim.assertCounts(1 to "Energy", 3 to "Heat")
  }

  @Test
  internal fun `Can retain existing energy but cannot convert newly produced heat to energy`() {
    shouldThrow<TaskException> { kim.doTask("2! Energy FROM Heat") }
    kim.doTask("Energy FROM Heat!")
    kim.assertCounts(2 to "Energy", 2 to "Heat")
  }
}
