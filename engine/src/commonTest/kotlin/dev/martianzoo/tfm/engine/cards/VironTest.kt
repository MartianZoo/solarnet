package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class VironTest : CardTest() {
  @Test
  fun `Can repeat an action used earlier in the generation`() {
    initializeGame()
    p1.cardAction1(AtmoCollectors)
    p1.cardAction1(Viron) { doTask("UseAction1<$AtmoCollectors>") }.expect("Floater")
  }

  @Test
  fun `Can choose a different action on the previously used card`() {
    initializeGame()

    p1.cardAction1(AtmoCollectors)

    p1.cardAction1(Viron) {
          doTask("UseAction2<$AtmoCollectors>")
          doTask("2 Titanium")
        }
        .expect("-Floater")
  }

  @Test
  fun `Cannot use Viron to repeat Viron's own action`() {
    initializeGame()
    p1.cardAction1(AtmoCollectors)
    p1.cardAction1(Viron) {
      shouldThrow<NarrowingException> { doTask("UseAction1<$Viron>") }
      abort()
    }
  }

  @Test
  fun `Cannot choose an action card that has not been used`() {
    initializeGame()
    p1.manual("$ExtractorBalloons")
    p1.cardAction1(AtmoCollectors)

    p1.cardAction1(Viron) {
      shouldThrow<NarrowingException> { doTask("UseAction1<$ExtractorBalloons>") }
      abort()
    }
  }

  @Test
  fun `Cannot repeat another player's action`() {
    newGame(
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    val p2 = requireP2()
    engine.phase("Action")
    p1.manual("$Viron, $ExtractorBalloons")
    p2.manual("$AtmoCollectors") { doTask("2 Floater<$AtmoCollectors>") }
    p1.cardAction1(ExtractorBalloons)
    p2.cardAction1(AtmoCollectors)

    p1.cardAction1(Viron) {
      shouldThrow<NarrowingException> { doTask("UseAction1<$AtmoCollectors<Player2>>") }
      abort()
    }
  }

  @Test
  fun `Repeats an action on another corporation`() {
    newGame(VenusNextExpansion)
    engine.phase("Action")
    p1.manual("$Viron, $Celestic")
    p1.stdAction("HandleMandates").expect("2 ProjectCard")
    p1.cardAction1(Celestic) { doTask("Floater<$Celestic>") }

    p1.cardAction1(Viron) {
      doTask("UseAction1<$Celestic>")
      doTask("Floater<$Celestic>")
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
    p1.manual("$Viron, $AtmoCollectors") { doTask("2 Floater<$AtmoCollectors>") }
  }
}
