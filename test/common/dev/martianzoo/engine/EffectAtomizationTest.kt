package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.state.Actor.Companion.ADMIN
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EffectAtomizationTest {
  @Test
  fun `automatic gains stay atomized after scaling by listeners and trigger count`() {
    val admin = Engine.newGame(premise).testAgent(ADMIN)
    admin.runOperation("2 Producer, Listener")

    admin.runOperation("3 Trigger")

    admin.count("Award") shouldBe 6
    admin.count("Batch") shouldBe 6
  }

  private companion object {
    val premise =
        testGamePremise(
            """
            CLASS Trigger : Signal
            CLASS Producer { Trigger:: CountedSignal }
            CLASS CountedSignal : Signal, Atomized { This:: Award }
            CLASS Listener { X CountedSignal:: Batch }
            CLASS Award
            CLASS Batch
            """,
            players = 0,
        )
  }
}
