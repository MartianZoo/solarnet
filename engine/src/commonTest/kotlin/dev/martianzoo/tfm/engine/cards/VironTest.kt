package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TfmGameplay
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class VironTest : CardTest() {
  @Test
  fun `after using Atmo Collectors, uses it again through Viron`() {
    initializeGame()
    p1.cardAction1("AtmoCollectors")
    p1.cardAction1("Viron") { doTask("UseAction1<AtmoCollectors>") }.expect("Floater")
    p1.assertOneActionMarkerOnEachCard()
  }

  @Test
  fun `after using Atmo Collectors, chooses its other action through Viron`() {
    initializeGame()

    p1.cardAction1("AtmoCollectors")

    p1.cardAction1("Viron") {
          doTask("UseAction2<AtmoCollectors>")
          doTask("2 Titanium")
        }
        .expect("-Floater")

    p1.assertOneActionMarkerOnEachCard()
  }

  @Test
  fun `after using another card, tries to choose Viron through itself`() {
    initializeGame()
    p1.cardAction1("AtmoCollectors")
    p1.cardAction1("Viron") {
      shouldThrow<NarrowingException> { doTask("UseAction1<Viron>") }
      abort()
    }
  }

  @Test
  fun `with an unused action card, tries to choose it through Viron`() {
    initializeGame()
    p1.manual("ExtractorBalloons")
    p1.cardAction1("AtmoCollectors")

    p1.cardAction1("Viron") {
      shouldThrow<NarrowingException> { doTask("UseAction1<ExtractorBalloons>") }
      abort()
    }
  }

  @Test
  fun `after p2 uses a card, p1 tries to choose it through Viron`() {
    newGame(
        "VenusNextExpansion,ColoniesExpansion",
        colonyTiles = testColonyTiles(2),
    )
    val p2 = requireP2()
    engine.phase("Action")
    p1.manual("Viron, ExtractorBalloons")
    p2.manual("AtmoCollectors") { doTask("2 Floater<AtmoCollectors>") }
    p1.cardAction1("ExtractorBalloons")
    p2.cardAction1("AtmoCollectors")

    p1.cardAction1("Viron") {
      shouldThrow<NarrowingException> { doTask("UseAction1<AtmoCollectors<Player2>>") }
      abort()
    }

    p2.assertCounts(1 to "ActionUsedMarker<AtmoCollectors>")
  }

  private fun initializeGame() {
    newGame(
        "VenusNextExpansion,ColoniesExpansion",
        colonyTiles = testColonyTiles(2),
    )
    engine.phase("Action")
    p1.manual("Viron, AtmoCollectors") { doTask("2 Floater<AtmoCollectors>") }
  }

  private fun TfmGameplay.assertOneActionMarkerOnEachCard() {
    assertCounts(
        1 to "ActionUsedMarker<AtmoCollectors>",
        1 to "ActionUsedMarker<Viron>",
        2 to "ActionUsedMarker",
    )
  }
}
