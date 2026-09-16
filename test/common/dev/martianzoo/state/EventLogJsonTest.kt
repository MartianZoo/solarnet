package dev.martianzoo.state

import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.Player
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.Task.Selection
import dev.martianzoo.state.Task.TaskId
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EventLogJsonTest {
  private val premise = testGamePremise("CLASS Token\nCLASS Marker\nCLASS Ping : Signal")
  private val table = premise.classTable
  private val token = table.resolve(parse<Expression>("Token")).toComponent()
  private val marker = table.resolve(parse<Expression>("Marker")).toComponent()
  private val ping = table.resolve(parse<Expression>("Ping")).toComponent()

  @Test
  internal fun exactEventsRoundTripAndReconstructTheirWorld() {
    val player = Player(cn("Player1"))
    val task =
        Task(
            TaskId(1),
            ADMIN,
            player,
            instruction = parse<Instruction>("Token! BY Player1"),
            then = InstructionGroup(listOf(parse<Instruction>("Marker!"))),
            cause = Cause(parse("Token"), 0),
        )
    val selected = task.copy(selection = Selection.DELEGATED)
    val events =
        listOf(
            ChangeEvent(0, ADMIN, ComponentChange.Gain(2, token), null),
            TaskAddedEvent(1, task),
            TaskEditedEvent(2, task, selected),
            ChangeEvent(
                3,
                player,
                ComponentChange.Transmute(1, marker, token),
                Cause(parse("Token"), 0),
            ),
            TaskRemovedEvent(4, selected),
            ChangeEvent(
                5,
                player,
                ComponentChange.Transmute(1, gaining = ping, removing = ping),
                Cause(parse("Token"), 0),
            ),
        )
    events[3].notes = "line one\nline two\twith a tab"
    val original = GameWorld(premise, events)

    val text = EventLogJson.encode(events)
    val decoded = EventLogJson.decode(text, table)
    val reconstructed = GameWorld(premise, decoded)

    decoded shouldBe events
    decoded.map { it.notes } shouldBe events.map { it.notes }
    EventLogJson.encode(decoded) shouldBe text
    reconstructed.events.entriesSince(Checkpoint(0)) shouldBe
        original.events.entriesSince(Checkpoint(0))
    reconstructed.tasks.extract { it } shouldBe original.tasks.extract { it }
    reconstructed.components.getAll(table.componentClass.baseType, NoGameState).entries shouldBe
        original.components.getAll(table.componentClass.baseType, NoGameState).entries
  }
}
