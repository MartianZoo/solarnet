package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
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
    // The old energy and heat are preparation; exercise their conversion through real production.
    kim.exMachina("$Supercapacitors, Energy, Heat")
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("Heat FROM Energy", "21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
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
