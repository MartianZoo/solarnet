package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.testsupport.count
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PerAtomizationTest {
  @Test
  fun `computed atomized gains fire a separate reaction for each step`() {
    val admin = Agents(Engine.newGame(premise))
    admin.runSystem("Listener, 3 Counter")

    admin.runSystem("CountedSignal / Counter")

    admin.world.reader.count("Award") shouldBe 3
    admin.world.reader.count("Batch") shouldBe 3
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
