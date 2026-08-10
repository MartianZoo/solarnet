package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

class StartTokenTest {
  @Test
  fun startsWithPlayer1AndPassesLeftEachGeneration() {
    val engine = setUpGame(players = 3).tfm(ENGINE)

    engine.assertCounts(1 to "StartToken<Player1>", 0 to "StartToken<Player2>")

    engine.godMode().manual("Generation")
    engine.assertCounts(0 to "StartToken<Player1>", 1 to "StartToken<Player2>")

    engine.godMode().manual("Generation")
    engine.assertCounts(0 to "StartToken<Player2>", 1 to "StartToken<Player3>")

    engine.godMode().manual("Generation")
    engine.assertCounts(1 to "StartToken<Player1>", 0 to "StartToken<Player3>")
    engine.assertCounts(1 to "StartToken")
  }

  @Test
  fun staysWithPlayer1InAnActualOnePlayerSetup() {
    val game = setUpGame(players = 1)
    val engine = game.tfm(ENGINE)

    engine.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    engine.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    engine.godMode().manual("Generation")

    engine.assertCounts(1 to "StartToken<Player1>")
  }

  @Test
  fun `solo setup links each greenery to its own city`() {
    val engine = setUpGame(players = 1).tfm(ENGINE)

    engine.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    engine.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    engine.doTask("CityTile<Tharsis_5_8, SoloOpponent>")

    // This area neighbors the first city, but not the selected second city.
    shouldThrow<NarrowingException> {
      engine.doTask("GreeneryTile<Tharsis_3_1, SoloOpponent>")
    }
  }

  @Test
  fun autoWorkflowReadsTheTokenOwner() {
    val setup = canonicalPremise(HellasMapOption, PromoCardPack, players = 2)
    val game = Engine.newGame(setup)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    val workflow = TfmWorkflow.Auto(game).launch()

    p1.playCorp("InterplanetaryCinematics", 7)
    engine.godMode().sneak("StartToken<Player2> FROM StartToken<Player1>")
    p2.playCorp("PharmacyUnion", 5)

    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    workflow.shutdown()
  }
}
