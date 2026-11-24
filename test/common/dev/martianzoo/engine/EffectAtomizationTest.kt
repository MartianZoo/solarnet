package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.testsupport.count
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EffectAtomizationTest {
  @Test
  fun `automatic gains stay atomized after scaling by listeners and trigger count`() {
    val admin = Agents(Engine.newGame(premise))
    admin.runSystem("2 Producer, Listener")

    admin.runSystem("3 Trigger")

    admin.world.reader.count("Award") shouldBe 6
    admin.world.reader.count("Batch") shouldBe 6
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
