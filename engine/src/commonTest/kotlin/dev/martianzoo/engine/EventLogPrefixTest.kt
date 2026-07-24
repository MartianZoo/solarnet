package dev.martianzoo.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

internal class EventLogPrefixTest {
  @Test
  fun capturesAStartingSequenceAndAppendsLocally() {
    val parent = WritableEventLog()
    parent.addChangeEvent(StateChange(1, parse<Expression>("Energy"), null), ENGINE, null)
    parent.setStartPoint()
    parent.addChangeEvent(StateChange(1, parse<Expression>("Plant"), null), ENGINE, null)

    val child = WritableEventLog(parent)
    parent.addChangeEvent(StateChange(1, parse<Expression>("Steel"), null), ENGINE, null)
    child.addChangeEvent(StateChange(1, parse<Expression>("Titanium"), null), ENGINE, null)

    child.changesSince(Checkpoint(0)).map { it.change.gaining.toString() } shouldContainExactly
        listOf("Energy", "Plant", "Titanium")
    child.changesSinceSetup().map { it.change.gaining.toString() } shouldContainExactly
        listOf("Plant", "Titanium")
    child.changesSince(Checkpoint(0)).map { it.ordinal } shouldContainExactly listOf(0, 1, 2)
    shouldThrow<IllegalArgumentException> { child.eventsToRollBack(0) }
  }
}
