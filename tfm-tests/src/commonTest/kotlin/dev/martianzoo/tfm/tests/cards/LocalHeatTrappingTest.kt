package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class LocalHeatTrappingTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
  }

  @Test
  internal fun `Can take plants when enough heat is available`() {
    p1.manual("6 Heat, $Pets")
    p1.manual("$LocalHeatTrapping") {
          doTask("4 Plant")
        }
        .expect("-5 Heat, 4 Plant")
  }

  @Test
  internal fun `Can add animals to Pets`() {
    p1.manual("6 Heat, $Pets")
    p1.manual("$LocalHeatTrapping") { addCardResources(Pets) }.expect("-5 Heat, 2 Animal")
  }

  @Test
  internal fun `Cannot choose abstract Animal instead of an eligible card`() {
    p1.manual("6 Heat, $Pets")
    p1.manual("$LocalHeatTrapping") {
      shouldThrow<AbstractException> { doTask("2 Animal") }
      abort()
    }
  }

  @Test
  internal fun `Can choose animals without a holder and gain nothing`() {
    p1.manual("6 Heat")

    p1.manual("$LocalHeatTrapping") {
          // Decline gaining plants by choosing animals when no animal holder exists.
          declineTask()
        }
        .expect("-5 Heat, 0 Plant")
  }

  @Test
  internal fun `Cannot evade an eligible holder by selecting an absent holder`() {
    p1.manual("6 Heat, $Pets")
    p1.manual("$LocalHeatTrapping") {
          shouldThrow<NarrowingException> { doTask("2 Animal<$Fish>") }
          addCardResources(Pets)
        }
        .expect("-5 Heat, 2 Animal<$Pets>")
  }

  @Test
  internal fun `Cannot be played without enough heat`() {
    p1.manual("4 Heat, ProjectCard, $Pets, 1")
    p1.assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")

    engine.phase("Action")

    p1.playProject(LocalHeatTrapping, 1) {
      p1.assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")
      abort()
    }
  }
}
