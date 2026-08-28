package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AtomicOperationScopeTest {
  @Test
  internal fun nestedOperationsAcrossActorsReportOnlyTheOutermostCompletion() {
    val game = Engine.newGame(testGamePremise(players = 2))
    val player1 = game.gameplay(PLAYER1).godMode()
    val player2 = game.gameplay(PLAYER2).godMode()
    var completions = 0
    game.onAtomicComplete = { completions++ }

    player1.manual("Ok") { player2.manual("Ok") }

    completions shouldBe 1

    player1.manual("Ok")

    completions shouldBe 2
  }

  @Test
  internal fun nestedGameplayCallsDoNotStartAutomaticAdvancement() {
    val game = Engine.newGame(testGamePremise(players = 2))
    val player1 = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    val player2 = game.gameplay(PLAYER2).godMode()

    player2.addTasks("Token")
    player1.manual("Ok") { player2.autoExecNow() }

    game.tasks.isEmpty() shouldBe false
    player2.autoExecNow()
    game.tasks.isEmpty() shouldBe true
  }
}
