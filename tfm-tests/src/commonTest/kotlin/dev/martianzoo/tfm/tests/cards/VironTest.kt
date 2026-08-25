package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VironTest : CardTest() {
  @Test
  internal fun `Can repeat an action used earlier in the generation`() {
    initializeGame()
    p1.cardAction1(AtmoCollectors)
    p1.cardAction1(Viron) { doTask("UseAction<$AtmoCollectors, First>") }.expect("Floater")
  }

  @Test
  internal fun `Can choose a different action on the previously used card`() {
    initializeGame()

    p1.cardAction1(AtmoCollectors)

    p1.cardAction1(Viron) {
          doTask("UseAction<$AtmoCollectors, Second>")
          doTask("2 Titanium")
        }
        .expect("-Floater")
  }

  @Test
  internal fun `Cannot use Viron to repeat Viron's own action`() {
    initializeGame()
    p1.cardAction1(AtmoCollectors)
    p1.cardAction1(Viron) {
      shouldThrow<NarrowingException> { doTask("UseAction<$Viron, First>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot choose an action card that has not been used`() {
    initializeGame()
    p1.manual("$ExtractorBalloons")
    p1.cardAction1(AtmoCollectors)

    p1.cardAction1(Viron) {
      shouldThrow<NarrowingException> { doTask("UseAction<$ExtractorBalloons, First>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot repeat another player's action`() {
    newGame(
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    val p2 = requireP2()
    engine.phase("Action")
    p1.manual("$Viron, $ExtractorBalloons")
    p2.manual("$AtmoCollectors") { addCardResources(AtmoCollectors) }
    p1.cardAction1(ExtractorBalloons)
    p2.cardAction1(AtmoCollectors)

    p1.cardAction1(Viron) {
      shouldThrow<NarrowingException> { doTask("UseAction<$AtmoCollectors<Player2>, First>") }
      abort()
    }
  }

  @Test
  internal fun `Repeats an action on another corporation`() {
    newGame(VenusNextExpansion)
    engine.phase("Action")
    p1.manual("$Viron, $Celestic")
    p1.stdAction("HandleMandates").expect("2 ProjectCard")
    p1.cardAction1(Celestic) { addCardResources(Celestic) }

    p1.cardAction1(Viron) {
      doTask("UseAction<$Celestic, First>")
      addCardResources(Celestic)
    }
    p1.count("Floater<$Celestic>") shouldBe 2
  }

  private fun initializeGame() {
    newGame(
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    engine.phase("Action")
    p1.manual("$Viron, $AtmoCollectors") { addCardResources(AtmoCollectors) }
  }
}
