package dev.martianzoo.state

import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.ExistingDependentsException
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.Task.TaskId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameWorldTest {
  private val premise =
      testGamePremise("CLASS Token\nCLASS Holder<Token>\nCLASS Moment : Signal", players = 0)
  private val table = premise.classTable
  private val token = table.resolve(parse<Expression>("Token")).toComponent()
  private val holder = table.resolve(parse<Expression>("Holder")).toComponent()
  private val moment = table.resolve(parse<Expression>("Moment")).toComponent()

  @Test
  internal fun rejectsSelfTransmutationInPassiveState() {
    val world = GameWorld(premise)
    world.apply(changeEvent(world, ComponentChange.Gain(component = token)))
    shouldThrow<ExpressionException> {
      world.apply(changeEvent(world, ComponentChange.Transmute(1, token, token)))
    }
    world.components.count(token.type, NoGameState) shouldBe 1
  }

  @Test
  internal fun componentExistenceFollowsLiveRefinementsAndRollback() {
    val world =
        GameWorld(
            testGamePremise(
                """
                ABSTRACT CLASS Space {
                  CLASS Left
                  CLASS Right
                }
                CLASS Occupant<Space>
                """
                    .trimIndent(),
                players = 0,
            )
        )
    val left = world.reader.resolve(parse("Left")).toComponent()
    val right = world.reader.resolve(parse("Right")).toComponent()
    val occupant = world.reader.resolve(parse("Occupant<Right>")).toComponent()
    val occupied = world.reader.resolve(parse("Space(HAS Occupant)"))
    world.components.containsAny(right.type, world.reader) shouldBe false

    world.apply(changeEvent(world, ComponentChange.Gain(component = left)))
    world.apply(changeEvent(world, ComponentChange.Gain(component = right)))

    world.components.containsAny(right.type, world.reader) shouldBe true
    world.components.containsAny(occupant.type, world.reader) shouldBe false
    world.components.containsAny(occupied, world.reader) shouldBe false

    world.apply(changeEvent(world, ComponentChange.Gain(component = occupant)))

    world.components.containsAny(occupied, world.reader) shouldBe true
    world.rollBackTo(2)
    world.components.containsAny(occupied, world.reader) shouldBe false
    world.components.containsAny(right.type, world.reader) shouldBe true
    world.rollBackTo(0)
    world.components.containsAny(right.type, world.reader) shouldBe false
  }

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
  internal fun selfTransmutationNeverChangesTheComponentProjection() {
    val world = GameWorld(premise)
    val observedCounts = mutableListOf<Int>()
    world.components.listenToCount(moment.type, world.reader, observedCounts::add)

    world.apply(changeEvent(world, ComponentChange.Transmute(gaining = moment, removing = moment)))

    world.components.countComponent(moment) shouldBe 0
    observedCounts shouldBe listOf(0)
    world.rollBackTo(0)
    world.components.countComponent(moment) shouldBe 0
    observedCounts shouldBe listOf(0)
  }

  @Test
  internal fun ordinaryTransmutationCanGainASignal() {
    val world = GameWorld(premise)
    world.apply(changeEvent(world, ComponentChange.Gain(component = token)))

    world.apply(changeEvent(world, ComponentChange.Transmute(gaining = moment, removing = token)))

    world.components.countComponent(moment) shouldBe 1
    world.components.countComponent(token) shouldBe 0
    world.rollBackTo(1)
    world.components.countComponent(moment) shouldBe 0
    world.components.countComponent(token) shouldBe 1
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
    val selected = task.copy(selected = true)

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
  internal fun rejectedOrdinalDoesNotAdvanceStateHistory() {
    val world = GameWorld(premise)

    shouldThrow<IllegalArgumentException> {
      world.apply(ChangeEvent(1, ADMIN, ComponentChange.Gain(component = token), cause = null))
    }

    world.components.countComponent(token) shouldBe 0
    world.events.entriesSince(Checkpoint(0)) shouldBe emptyList()
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
    second.apply(TaskEditedEvent(1, task, task.copy(selected = true)))

    first.tasks.getTaskData(task.id).selected shouldBe false
    second.tasks.getTaskData(task.id).selected shouldBe true
  }

  @Test
  internal fun forkCopiesDependencyStateWithoutCopyingListeners() {
    val source = GameWorld(premise)
    source.apply(changeEvent(source, ComponentChange.Gain(component = token)))
    source.apply(changeEvent(source, ComponentChange.Gain(component = holder)))
    val sourceTokenCounts = mutableListOf<Int>()
    val subscription =
        source.components.listenToCount(token.type, source.reader, sourceTokenCounts::add)

    val fork = source.fork()

    shouldThrow<ExistingDependentsException> {
      fork.apply(changeEvent(fork, ComponentChange.Remove(component = token)))
    }
    fork.apply(changeEvent(fork, ComponentChange.Remove(component = holder)))
    fork.apply(changeEvent(fork, ComponentChange.Remove(component = token)))

    fork.components.countComponent(token) shouldBe 0
    source.components.countComponent(token) shouldBe 1
    source.components.countComponent(holder) shouldBe 1
    sourceTokenCounts shouldBe listOf(1)
    subscription.cancel()
  }

  private fun changeEvent(world: GameWorld, change: ComponentChange): ChangeEvent =
      ChangeEvent(world.nextOrdinal, ADMIN, change, cause = null)
}
