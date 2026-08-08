package dev.martianzoo.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EventLogPrefixTest {
  @Test
  fun capturesAStartingSequenceAndAppendsLocally() {
    val parent = EventLog()
    recordGain(parent, "Energy")
    parent.markSetupStart()
    recordGain(parent, "Plant")

    val child = EventLog(parent)
    recordGain(parent, "Steel")
    recordGain(child, "Titanium")

    child.changesSince(Checkpoint(0)).map { it.change.gaining.toString() } shouldContainExactly
        listOf("Energy", "Plant", "Titanium")
    child.changesSinceSetup().map { it.change.gaining.toString() } shouldContainExactly
        listOf("Plant", "Titanium")
    child.changesSince(Checkpoint(0)).map { it.ordinal } shouldContainExactly listOf(0, 1, 2)
    shouldThrow<IllegalArgumentException> { child.rollBackTo(0) {} }
  }

  @Test
  fun failedStateUpdateDoesNotAdvanceHistoryOrRevision() {
    val events = EventLog()
    val revision = events.revision
    val entry = gainEvent(events, "Energy")

    shouldThrow<IllegalStateException> { events.record(entry) { error("state update failed") } }

    events.size shouldBe 0
    events.revision shouldBe revision
  }

  @Test
  fun revisesCommentsWithoutAdvancingHistoryOrRevision() {
    val parent = EventLog()
    recordGain(parent, "Energy")
    parent.markSetupStart()
    val child = EventLog(parent)
    val revision = child.revision

    child.reviseComment(0, "starting resource")

    child.entriesSince(Checkpoint(0)).single().comment shouldBe "starting resource"
    parent.entriesSince(Checkpoint(0)).single().comment shouldBe "starting resource"
    child.size shouldBe 1
    child.revision shouldBe revision
  }

  private fun recordGain(events: EventLog, type: String) {
    events.record(gainEvent(events, type)) {}
  }

  private fun gainEvent(events: EventLog, type: String): ChangeEvent =
      ChangeEvent(
          ordinal = events.nextOrdinal,
          actor = ENGINE,
          change = StateChange(gaining = parse<Expression>(type)),
          cause = null,
      )
}
