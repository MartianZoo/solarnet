package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class LocalHeatTrappingTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
  }

  @Test
  fun `with enough heat, chooses plants from Local Heat Trapping`() {
    p1.manual("6 Heat, $Pets")
    p1.manual("$LocalHeatTrapping") {
          doTask("4 Plant")
        }
        .expect("-5 Heat, 4 Plant")
  }

  @Test
  fun `with Pets in play, chooses animals from Local Heat Trapping`() {
    p1.manual("6 Heat, $Pets")
    p1.manual("$LocalHeatTrapping") { doTask("2 Animal<$Pets>") }.expect("-5 Heat, 2 Animal")
  }

  @Test
  fun `with Pets in play, tries an abstract animal choice from Local Heat Trapping`() {
    p1.manual("6 Heat, $Pets")
    p1.manual("$LocalHeatTrapping") {
      shouldThrow<AbstractException> { doTask("2 Animal") }
      abort()
    }
  }

  @Test
  fun `without Fish in play, its optional animals narrow to nothing`() {
    p1.manual("6 Heat, $Pets")
    p1.manual("$LocalHeatTrapping") { doTask("2 Animal<$Fish>") }.expect("-5 Heat")
  }

  @Test
  fun `without enough heat, tries to resolve Local Heat Trapping`() {
    p1.manual("4 Heat, ProjectCard, $Pets, 1")
    p1.assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")

    engine.phase("Action")

    p1.playProject(LocalHeatTrapping, 1) {
      p1.assertCounts(0 to "Plant", 4 to "Heat", 1 to "Animal")
      abort()
    }
  }
}
