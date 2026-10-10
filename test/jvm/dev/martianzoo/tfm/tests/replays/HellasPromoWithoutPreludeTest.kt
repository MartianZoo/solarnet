package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class HellasPromoWithoutPreludeTest : AbstractFullGameTest() {
  override val config = GameConfig("HellasMap, PromoCardPack", "Player1", "Player2")

  @Test
  internal fun earlyGameWithNoPrelude() {
    TfmWorkflow.Automatic(agents).launch()
    p1.keepStartingProjects(7)
    p2.keepStartingProjects(5)
    p1.playCorp(InterplanetaryCinematics)
    p2.playCorp(PharmacyUnion) {
      doTask("Disease<$PharmacyUnion>")
      doTask("Disease<$PharmacyUnion>")
    }

    p1.turn {
      playProject(MediaGroup, 6)
      playProject(Sabotage, 1) { doTask("-7 MC<Player2>") }
    }

    p2.turn { playProject(Research, 11) }
  }
}
