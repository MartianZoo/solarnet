package dev.martianzoo.engine

import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.StateChange
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EventLogPrefixTest {
  @Test
  internal fun capturesAStartingSequenceAndAppendsLocally() {
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
  internal fun failedStateUpdateDoesNotAdvanceHistoryOrRevision() {
    val events = EventLog()
    val revision = events.revision
    val entry = gainEvent(events, "Energy")

    shouldThrow<IllegalStateException> { events.record(entry) { error("state update failed") } }

    events.size shouldBe 0
    events.revision shouldBe revision
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
