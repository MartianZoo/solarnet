package dev.martianzoo.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.canonicalPremise
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AtomicOperationBoundaryTest {
  @Test
  fun nestedOperationsAcrossActorsReportOnlyTheOutermostCompletion() {
    val game = Engine.newGame(canonicalPremise(players = 2))
    val player1 = game.gameplay(PLAYER1).godMode()
    val player2 = game.gameplay(PLAYER2).godMode()
    var completions = 0
    game.onAtomicComplete = { completions++ }

    player1.manual("Ok") { player2.manual("Ok") }

    completions shouldBe 1

    player1.manual("Ok")

    completions shouldBe 2
  }
}
