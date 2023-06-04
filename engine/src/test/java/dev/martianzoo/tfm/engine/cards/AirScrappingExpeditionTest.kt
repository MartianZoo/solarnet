package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TerraformingMarsApi
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AirScrappingExpeditionTest {
  @Test
  fun airScrappingExpedition() {
    val game = Engine.newGame(GameSetup(Canon, "CVERB", 2))
    val p1 = TerraformingMarsApi(game, PLAYER1)
    val ops = p1.game.operationLayer()

    with(p1) {
      ops.initiate("3 ProjectCard, ForcedPrecipitation")

      ops.initiate("AtmoCollectors") {
        doFirstTask("2 Floater<AtmoCollectors>")
      }

      assertCounts(2 to "Floater")

      assertThrows<NarrowingException>("1") {
        ops.initiate("AirScrappingExpedition") {
          doFirstTask("3 Floater<AtmoCollectors>")
        }
      }

      ops.initiate("AirScrappingExpedition") {
        doFirstTask("3 Floater<ForcedPrecipitation>")
      }
      assertCounts(5 to "Floater")
    }
  }
}
