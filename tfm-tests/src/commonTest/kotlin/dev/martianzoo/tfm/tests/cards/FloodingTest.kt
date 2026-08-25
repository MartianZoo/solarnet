package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class FloodingTest : CardTest() {
  @Test
  internal fun `Can charge either neighboring owner`() {
    playFlooding("Player2", "-4 Megacredit<Player2>")
    playFlooding("Player3", "-4 Megacredit<Player3>")
  }

  @Test
  internal fun `Can charge no one`() {
    playFlooding(null, null)
  }

  @Test
  internal fun `Cannot charge a non-neighboring owner`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
      shouldThrow<NarrowingException> {
        doTask("OceanTile<Tharsis_5_4> THEN -4 Megacredit<Player4>!")
      }
      abort()
    }
  }

  private fun playFlooding(owner: String?, expectedCharge: String?) {
    arrangeFlooding()
    val task =
        owner?.let { "OceanTile<Tharsis_5_4> THEN -4 Megacredit<$it>!" } ?: "OceanTile<Tharsis_5_4>"
    val expected = listOfNotNull("OceanTile<Tharsis_5_4>", expectedCharge).joinToString()

    p1.playProject(Flooding, 7) { doTask(task) }.expect(expected)
  }

  private fun arrangeFlooding() {
    val game = newGame(players = 4)
    val p2 = requireP2()
    val players = Player.players(4)
    val p3 = game.tfm(players[2])
    val p4 = game.tfm(players[3])
    engine.phase("Action")
    p1.manual("7, ProjectCard")
    p2.manual("10, CityTile<Tharsis_4_3>")
    p3.manual("10, CityTile<Tharsis_5_3>")
    p4.manual("10, CityTile<Tharsis_1_1>")
  }
}
