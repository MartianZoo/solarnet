package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.tests.cards.cardnames.Flooding
import kotlin.test.Test

/** Passing characterizations of incorrect Flooding victim restrictions. */
internal class FloodingBugsTest : CardTest() {
  @Test
  internal fun `Flooding incorrectly charges a non-neighboring owner`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player4>!")
        }
        .expect("0 MC<Player2>, 0 MC<Player3>, -4 MC<Player4>")
  }

  @Test
  internal fun `Flooding incorrectly qualifies the victim through a different ocean area`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_1_2>! THEN -4 MC<Player2>!")
        }
        .expect("-4 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `partial narrowing of Flooding incorrectly leaves the victim unrestricted`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          p1.selectTask(tasks.ids().single())
          p1.narrowTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Anyone>?")
          doTask("-4 MC<Player4>!")
        }
        .expect("0 MC<Player2>, 0 MC<Player3>, -4 MC<Player4>")
  }

  private fun arrangeFlooding() {
    val game = newGame(players = 4)
    val players = Player.players(4)
    admin.phase("Action")
    p1.runOperation("7 MC, ProjectCard")
    requireP2().runOperation("10 MC, CityTile<Tharsis_4_3>")
    game.testTfm(players[2]).runOperation("10 MC, CityTile<Tharsis_5_3>")
    game.testTfm(players[3]).runOperation("10 MC, CityTile<Tharsis_1_1>")
  }
}
