package dev.martianzoo.state

import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExistingDependentsException
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.Task.Selection
import dev.martianzoo.state.Task.TaskId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameWorldTest {
  private val premise = testGamePremise("CLASS Token\nCLASS Holder<Token>", players = 0)
  private val table = premise.classTable
  private val token = table.resolve(parse<Expression>("Token")).toComponent()
  private val holder = table.resolve(parse<Expression>("Holder<Token>")).toComponent()

  @Test
  internal fun appliesAndReversesOnlyExactConcreteChanges() {
    val world = GameWorld(premise)
    val tokenGain = changeEvent(world, ComponentChange.Gain(component = token))
    val holderGain = changeEvent(world, ComponentChange.Gain(component = holder))

    shouldThrow<DependencyException> { world.apply(holderGain) }
    world.apply(tokenGain)
    world.apply(changeEvent(world, holderGain.change))

    world.components.count(token.type, NoGameState) shouldBe 1
    world.components.count(holder.type, NoGameState) shouldBe 1
    shouldThrow<ExistingDependentsException> {
      world.apply(changeEvent(world, ComponentChange.Remove(component = token)))
    }

    world.rollBackTo(0)
    world.components.count(token.type, NoGameState) shouldBe 0
    world.components.count(holder.type, NoGameState) shouldBe 0
    world.events.entriesSince(Checkpoint(0)) shouldBe emptyList()
  }

  @Test
  internal fun exactTaskEventsKeepHistoryAndPendingProjectionTogether() {
    val world = GameWorld(premise)
    val task =
        Task(
            id = TaskId(0),
            controller = ADMIN,
            instruction = parse<Instruction>("Token!"),
            cause = null,
        )

    world.apply(TaskAddedEvent(0, task))
    val selected = task.copy(selection = Selection.SELECTED)

    shouldThrow<IllegalArgumentException> {
      world.apply(TaskEditedEvent(1, oldTask = selected, task = task))
    }
    world.tasks.getTaskData(task.id) shouldBe task
    world.events.entriesSince(Checkpoint(0)).map { it.ordinal } shouldBe listOf(0)

    world.apply(TaskEditedEvent(1, oldTask = task, task = selected))

    world.tasks.getTaskData(task.id) shouldBe selected
    world.events.entriesSince(Checkpoint(0)).map { it.ordinal } shouldBe listOf(0, 1)

    world.rollBackTo(0)
    world.tasks.isEmpty() shouldBe true
    world.events.entriesSince(Checkpoint(0)) shouldBe emptyList()
  }

  @Test
  internal fun rejectedOrdinalDoesNotAdvanceStateHistoryOrRevision() {
    val world = GameWorld(premise)
    val revision = world.revision

    shouldThrow<IllegalArgumentException> {
      world.apply(ChangeEvent(1, ADMIN, ComponentChange.Gain(component = token), cause = null))
    }

    world.components.countComponent(token) shouldBe 0
    world.events.entriesSince(Checkpoint(0)) shouldBe emptyList()
    world.revision shouldBe revision
  }

  @Test
  internal fun separateWorldsReplayTheSameTaskValueAndThenDiverge() {
    val first = GameWorld(premise)
    val second = GameWorld(premise)
    val task =
        Task(
            id = TaskId(0),
            controller = ADMIN,
            instruction = parse<Instruction>("Token!"),
            cause = null,
        )
    val added = TaskAddedEvent(0, task)

    first.apply(added)
    second.apply(added)
    second.apply(TaskEditedEvent(1, task, task.copy(selection = Selection.SELECTED)))

    first.tasks.getTaskData(task.id).selection shouldBe Selection.UNSELECTED
    second.tasks.getTaskData(task.id).selection shouldBe Selection.SELECTED
  }

  private fun changeEvent(world: GameWorld, change: ComponentChange): ChangeEvent =
      ChangeEvent(world.nextOrdinal, ADMIN, change, cause = null)
}
