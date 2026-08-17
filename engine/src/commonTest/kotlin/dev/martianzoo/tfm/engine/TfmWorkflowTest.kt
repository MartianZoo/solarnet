package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.HellasMapOption
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class TfmWorkflowTest {
  @Test
  fun turnDeclinesAnUnusedSecondAction() {
    val game = Engine.newGame(canonicalPremise(HellasMapOption, PromoCardPack, players = 2))
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(InterplanetaryCinematics, 7)
    p2.playCorp(PharmacyUnion, 5)

    p1.turn { sellPatents(1) }
    p2.pass()
    p1.pass()

    engine.assertCounts(2 to "Generation", 1 to "ResearchPhase")
    workflow.shutdown()
  }

  @Test
  fun soleRemainingPlayerDoesNotReceiveSecondActions() {
    val game = Engine.newGame(canonicalPremise(HellasMapOption, PromoCardPack, players = 2))
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp(InterplanetaryCinematics, 7)
    p2.playCorp(PharmacyUnion, 5)

    p1.pass()
    p2.turn {
      sellPatents(1)
      sellPatents(1)
      pass()
    }

    engine.assertCounts(2 to "Generation", 1 to "ResearchPhase")
    workflow.shutdown()
  }
}
