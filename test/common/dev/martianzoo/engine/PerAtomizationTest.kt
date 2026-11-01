package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.state.Actor.Companion.ADMIN
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PerAtomizationTest {
  @Test
  fun `computed atomized gains fire a separate reaction for each step`() {
    val admin = Engine.newGame(premise).testAgent(ADMIN)
    admin.runOperation("Listener, 3 Counter")

    admin.runOperation("CountedSignal / Counter")

    admin.count("Award") shouldBe 3
    admin.count("Batch") shouldBe 3
  }

  private companion object {
    val premise =
        testGamePremise(
            """
            CLASS Counter
            CLASS CountedSignal : Signal, Atomized { This:: Award }
            CLASS Listener { X CountedSignal:: Batch }
            CLASS Award
            CLASS Batch
            """,
            players = 0,
        )
  }
}
