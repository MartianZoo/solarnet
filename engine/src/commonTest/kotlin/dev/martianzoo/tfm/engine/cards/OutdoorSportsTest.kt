package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class OutdoorSportsTest : CardTest() {
  @Test
  fun `requires any city to be adjacent to an ocean`() {
    val game = newGame(Canon.fromOptionCodes("BMX", 2))
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    p1.phase("Action")
    p1.manual("100, 5 ProjectCard, CityTile<Player2, Tharsis_1_3>, OceanTile<Tharsis_1_5>")

    shouldThrow<RequirementException> { p1.playProject("OutdoorSports", 8) }

    p2.manual("OceanTile<Tharsis_1_2>")
    p1.playProject("OutdoorSports", 8).expect("PROD[2 Megacredit]")
  }
}
