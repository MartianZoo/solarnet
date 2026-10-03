package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
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

  @Test
  fun `queued self effects allow separate choices after scaling by trigger count`() {
    val admin = Engine.newGame(premise).testAgent(ADMIN)
    admin.autoExecPolicy = NONE

    admin.runOperation("3 ChoiceProducer") {
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
            CLASS Trigger : Signal
            CLASS Producer { Trigger:: CountedSignal }
            CLASS CountedSignal : Signal, Atomized { This:: Award }
            CLASS Listener { X CountedSignal:: Batch }
            CLASS Award
            CLASS Batch
            CLASS ChoiceProducer { This: Item }
            ABSTRACT CLASS Item : Atomized
            CLASS RedItem : Item
            CLASS BlueItem : Item
            """,
            players = 0,
        )
  }
}
