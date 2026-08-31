package dev.martianzoo.engine

import dev.martianzoo.engine.Agent.TaskLayer
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.GameEvent
import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent
import dev.martianzoo.pets.data.GameEvent.TaskAddedEvent
import dev.martianzoo.pets.data.GameEvent.TaskEditedEvent
import dev.martianzoo.pets.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
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
  private val agent = game.agent(PLAYER1).also { it.autoExecMode = AutoExecMode.NONE }

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
        GameplayInputEvent::class,
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
        GameplayInputEvent::class,
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
        GameplayInputEvent::class,
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
    initiate("Plant?, Heat?")
    val (plant, heat) = tasks.ids().toList()

    agent.selectTask(plant)

    shouldThrow<TaskException> { agent.selectTask(heat) }
    tasks.selectedTask() shouldBe plant
  }

  @Test
  internal fun `selection resolves an OR by pruning impossible options`() {
    initiate("-TR OR -Plant OR Heat OR Tharsis_5_5!")
    agent.selectTask("-TR OR -Plant OR Heat OR Tharsis_5_5!")

    tasksAsText().shouldContainExactlyInAnyOrder("-TerraformRating<Player1>! OR Heat<Player1>!")
  }

  private fun initiate(ins: String) = (agent as TaskLayer).addTasks(ins)

  private fun history() = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) =
      history().map { it::class }.shouldContainExactly(*c)

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
