package dev.martianzoo.tfm.canon

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.RoutineReplayEncoder.Entry.Call
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent
import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent.Kind.SELECT_TASK
import dev.martianzoo.pets.data.Player
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TerraformingMarsReplayEncoderTest {
  @Test
  internal fun standaloneTaskSelectionIsRetained() {
    val world = Engine.newGame(Canon.gamePremise(GameConfig("UtopiaMap", "Dad")))
    val actor = world.actors.filterIsInstance<Player>().single()
    val preludeStarted =
        ChangeEvent(
            ordinal = 1,
            actor = ENGINE,
            change = StateChange(gaining = cn("PreludePhase").expression),
            cause = null,
        )
    val selection =
        GameplayInputEvent(
            ordinal = 2,
            operationStartOrdinal = 2,
            actor = actor,
            kind = SELECT_TASK,
            source = "ignored",
            taskNumber = 2,
        )

    TerraformingMarsReplayEncoder.encode(world, listOf(preludeStarted, selection)) shouldBe
        listOf(Call(actor, "tasks", listOf("2 select")))
  }
}
