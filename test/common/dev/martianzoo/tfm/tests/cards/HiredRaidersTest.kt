package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.data.Player.Companion.PLAYER3
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.HiredRaiders
import kotlin.test.Test

internal class HiredRaidersTest : CardTest() {
  @Test
  internal fun `May steal less than the offered maximum`() {
    newGame(CorporateEraExpansion, players = 3)
    engine.phase("Action")
    p1.autoExecMode = NONE
    p1.manual("2 MC, ProjectCard")
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p2.manual("2 Steel")
    p3.manual("2 Steel")

    p1.playProject(HiredRaiders, 1) {
          doTask("Steel<Player1> FROM Steel<Player3>")
        }
        .expect("Steel<Player1>, -Steel<Player3>")

    p2.assertCounts(2 to "Steel")
  }
}
