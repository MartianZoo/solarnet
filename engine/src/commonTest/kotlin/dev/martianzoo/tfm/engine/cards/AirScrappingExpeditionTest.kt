package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class AirScrappingExpeditionTest {
  @Test
  fun airScrappingExpedition() {
    val game = Engine.newGame(Canon.fromOptionCodes("CVERB", 2, testColonyTiles(2)))

    with(game.tfm(PLAYER1)) {
      godMode().manual("3 ProjectCard, ForcedPrecipitation")

      godMode().manual("AtmoCollectors") { doFirstTask("2 Floater<AtmoCollectors>") }

      assertCounts(2 to "Floater")

      shouldThrow<NarrowingException> {
        godMode().manual("AirScrappingExpedition") { doFirstTask("3 Floater<AtmoCollectors>") }
      }

      godMode().manual("AirScrappingExpedition") { doFirstTask("3 Floater<ForcedPrecipitation>") }
      assertCounts(5 to "Floater")
    }
  }
}
