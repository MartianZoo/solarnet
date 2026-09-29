package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
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

  @Test
  fun `computed atomized gains allow a separate choice for each item`() {
    val admin = Engine.newGame(premise).testAgent(ADMIN)
    admin.runOperation("3 Counter")
    admin.autoExecPolicy = NONE

    admin.runOperation("Item / Counter") {
      doTask("RedItem")
      doTask("BlueItem")
      doTask("RedItem")
    }

    admin.count("RedItem") shouldBe 2
    admin.count("BlueItem") shouldBe 1
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
            ABSTRACT CLASS Item : Atomized
            CLASS RedItem : Item
            CLASS BlueItem : Item
            """,
            players = 0,
        )
  }
}
