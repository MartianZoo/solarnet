package dev.martianzoo.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.data.GameEvent
import dev.martianzoo.pets.data.GameEvent.TaskAddedEvent
import dev.martianzoo.pets.data.GameEvent.TaskEditedEvent
import dev.martianzoo.pets.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import kotlin.test.Test

internal class TaskResolutionTest {
  private val game = setUpGame()
  private val tasks = game.tasks
  private val events = game.events
  private val start = game.timeline.checkpoint()
  private val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = AutoExecPolicy.NONE }

  @Test
  internal fun `selecting resolves an abstract task and takes the select-lock`() {
    initiate("2 Plant?")
    agent.selectTask("2 Plant?")

    val task = tasks.extract { it }.single()
    task.selected shouldBe true
    "${task.instruction}" shouldBe "2 Plant<Player1>?"
    assertHistoryTypes(
        TaskAddedEvent::class,
        TaskEditedEvent::class,
    )
  }

  @Test
  internal fun `selection that resolves to NoOp completes the task`() {
    initiate("-2 Plant?")
    agent.selectTask("-2 Plant?")

    tasks.isEmpty() shouldBe true
    assertHistoryTypes(
        TaskAddedEvent::class,
        TaskRemovedEvent::class,
    )
  }

  @Test
  internal fun `selection executes a concrete task immediately`() {
    initiate("Plant!")
    agent.selectTask("Plant!")

    tasks.isEmpty() shouldBe true
    assertHistoryTypes(
        TaskAddedEvent::class,
        TaskEditedEvent::class,
        GameEvent.ChangeEvent::class,
        TaskRemovedEvent::class,
    )
    agent.count("Plant") shouldBe 1
  }

  @Test
  internal fun `selection resolves limits before narrowing`() {
    initiate("-30 TerraformRating?")
    agent.selectTask("-30 TerraformRating?")
    tasksAsText().shouldContainExactlyInAnyOrder("-20 TerraformRating<Player1>?")
    tasks.extract { it.selected }.shouldContainExactly(true)
  }

  @Test
  internal fun `selection failure is atomic`() {
    initiate("-Plant!")
    history().shouldHaveSize(1)
    shouldThrow<LimitsException> { agent.selectTask("-Plant!") }

    history().shouldHaveSize(1)
    tasks.extract { it.selected }.shouldContainExactly(false)
  }

  @Test
  internal fun `the select-lock rejects a second selection`() {
    val plant = initiate("Plant?").single()
    val heat = initiate("Heat?").single()

    agent.selectTask(plant)

    shouldThrow<TaskException> { agent.selectTask(heat) }
    tasks.selectedTask() shouldBe plant
  }

  @Test
  internal fun `selection resolves an OR by pruning impossible options`() {
    initiate("-TerraformRating OR -Plant OR Heat OR Tharsis_5_5!")
    agent.selectTask("-TerraformRating OR -Plant OR Heat OR Tharsis_5_5!")

    tasksAsText().shouldContainExactlyInAnyOrder("-TerraformRating<Player1>! OR Heat<Player1>!")
  }

  @Test
  internal fun `selection preserves a transmutation's shared type choice`() {
    agent.runOperation("Steel, Titanium")
    val taskId = initiate("Production<Class<StandardResource>> FROM StandardResource").single()

    agent.selectTask(taskId)
    val selected = tasks.getTaskData(taskId).instruction as Transmute
    selected.typeVariables.isEmpty shouldBe false

    shouldThrow<NarrowingException> {
      agent.narrowTask("Production<Class<Steel>> FROM Titanium")
    }
    agent.narrowTask("Production<Class<Steel>> FROM Steel")
    agent.count("Steel") shouldBe 0
    agent.count("Production<Class<Steel>>") shouldBe 1
  }

  private fun initiate(ins: String) = (agent as Agent).addTasks(ins)

  private fun history() = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) =
      history().map { it::class }.shouldContainExactly(*c)

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
