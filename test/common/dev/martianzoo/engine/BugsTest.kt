package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect engine behavior. */
internal class BugsTest {
  @Test
  internal fun `partial sequence narrowing incorrectly discards a shared refinement`() {
    val game = Engine.newGame(canonicalPremise())
    val player = game.testAgent(PLAYER1)
    player.runOperation("Heat")

    player.runOperation(
        "MC<Player(HAS Selected@StandardResource)> THEN Selected@StandardResource"
    ) {
      doTask("MC<Player1> THEN StandardResource")
      doTask("Steel")
    }

    // Steel was absent when the first-stage refinement was checked; selecting it should have
    // failed.
    player.count("Heat") shouldBe 1
    player.count("MC") shouldBe 1
    player.count("Steel") shouldBe 1
  }
}
