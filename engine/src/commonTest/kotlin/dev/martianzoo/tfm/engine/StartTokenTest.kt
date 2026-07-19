package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

class StartTokenTest {
  @Test
  fun startsWithPlayer1AndPassesLeftEachGeneration() {
    val engine = Engine.newGame(GameSetup(Canon, "BRM", 3)).tfm(ENGINE)

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
    val engine = Engine.newGame(GameSetup(Canon, "BRM", 1)).tfm(ENGINE)

    engine.godMode().manual("Generation")

    engine.assertCounts(1 to "StartToken<Player1>")
  }

  @Test
  fun passLeftPreservesAnotherDependency() {
    val engine = Engine.newGame(GameSetup(Canon, "BRMC", 3, setOf(cn("Luna")))).tfm(ENGINE)

    engine.godMode().sneak("Colony<Luna, Player1>")
    engine.godMode().manual("PassLeft<Colony<Luna, Player1>>")

    engine.assertCounts(
        0 to "Colony<Luna, Player1>",
        1 to "Colony<Luna, Player2>",
        0 to "Colony<Player1>",
        1 to "Colony<Player2>",
    )
  }

  @Test
  fun autoWorkflowReadsTheTokenOwner() {
    val setup = GameSetup(Canon, "BRHX", 2)
    val game = Engine.newGame(setup)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    engine.godMode().sneak("StartToken<Player2> FROM StartToken<Player1>")
    val workflow = TfmWorkflow.Auto(game, setup).launch()

    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    workflow.shutdown()
  }
}
