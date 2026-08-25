package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class RoboticWorkforceTest : CardTest() {
  @Test
  internal fun `Can copy Strip Mine's production box`() {
    newGame()
    p1.manual("PROD[4 Energy], $StripMine")
    p1.assertProds(2 to "Steel", 1 to "Titanium", 2 to "Energy")
    p1.manual("$RoboticWorkforce") { doTask("CopyProductionBox<$StripMine>") }
    p1.assertProds(4 to "Steel", 2 to "Titanium", 0 to "Energy")
  }

  @Test
  internal fun `Cannot copy a non-building card`() {
    newGame()
    p1.manual("PROD[Energy], $Mine, $MassConverter")
    p1.manual("$RoboticWorkforce") {
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<$MassConverter>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot copy another player's building card`() {
    newGame()
    val p2 = requireP2()
    p1.manual("$IndustrialMicrobes")
    p2.manual("$Mine")

    p1.manual("$RoboticWorkforce") {
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<$Mine<Player2>>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot copy a building card its player does not own`() {
    newGame()
    p1.manual("$IndustrialMicrobes")
    p1.manual("$RoboticWorkforce") {
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<$Mine>") }
      abort()
    }
  }
}
