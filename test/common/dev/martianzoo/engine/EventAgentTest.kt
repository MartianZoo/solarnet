package dev.martianzoo.engine

import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent
import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent.Kind.DO_TASK
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EventAgentTest {
  @Test
  internal fun automaticPlayerInputRecordsNonIdentifyingAgentProvenance() {
    val game = Engine.newGame(testGamePremise("CLASS Token<Owner>"))
    val player = game.agent(PLAYER1)
    player.addTasks("Token<Player1>")
    val before = game.timeline.checkpoint()

    player.autoExecNow()

    val input = game.events.entriesSince(before).filterIsInstance<GameplayInputEvent>().single()
    input.operationStartOrdinal shouldBe before.ordinal
    input.actor shouldBe PLAYER1
    input.kind shouldBe DO_TASK
    input.source shouldBe "Token<Player1>!"
    input.taskNumber shouldBe 1
    input.agent shouldBe "autoexec FIRST"
    input shouldBe input.copy()
    input.toString() shouldBe
        "${input.ordinal}: DO_TASK 1 Token<Player1>! BY Player1 " +
            "(operation ${before.ordinal}) [agent: autoexec FIRST]"
  }
}
