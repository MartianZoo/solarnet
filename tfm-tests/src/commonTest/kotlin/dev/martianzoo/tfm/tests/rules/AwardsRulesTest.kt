package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.data.Player.Companion.PLAYER3
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AwardsRulesTest : CardTest() {
  @Test
  internal fun `Tied players receive the appropriate first and second place award points`() {
    newGame(players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p1.manual("Thermalist, Miner, 3 Heat, 3 Steel")
    p2.manual("2 Heat, 3 Steel")
    p3.manual("2 Heat, 2 Steel")

    TfmWorkflow.Manual(game).endPhase()

    p1.count("VictoryPoint") shouldBe 30
    p2.count("VictoryPoint") shouldBe 27
    p3.count("VictoryPoint") shouldBe 22
  }

  @Test
  internal fun `A two-player game awards no second-place points`() {
    newGame()
    val p2 = requireP2()
    p1.manual("Thermalist, Heat")

    TfmWorkflow.Manual(game).endPhase()

    p1.count("VictoryPoint") shouldBe 25
    p2.count("VictoryPoint") shouldBe 20
  }

  @Test
  internal fun `MC break a multiplayer victory-point tie`() {
    newGame()
    val p2 = requireP2()
    p1.manual("2 VictoryPoint, 5 MC")
    p2.manual("2 VictoryPoint, 4 MC")

    TfmWorkflow.Manual(game).endPhase()

    p1.count("Victory") shouldBe 1
    p2.count("Victory") shouldBe 0
  }
}
