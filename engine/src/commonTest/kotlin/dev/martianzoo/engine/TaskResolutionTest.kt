package dev.martianzoo.engine

import dev.martianzoo.engine.Gameplay.TaskLayer
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.GameEvent
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
  private val gameplay = game.gameplay(PLAYER1).also { it.autoExecMode = AutoExecMode.NONE }

  @Test
  internal fun `selecting resolves an abstract task and takes the select-lock`() {
    initiate("2 Plant?")
    gameplay.selectTask("2 Plant?")

    val task = tasks.extract { it }.single()
    task.selected shouldBe true
    "${task.instruction}" shouldBe "2 Plant<Player1>?"
    assertHistoryTypes(TaskAddedEvent::class, TaskEditedEvent::class)
  }

  @Test
  internal fun `selection that resolves to NoOp completes the task`() {
    initiate("-2 Plant?")
    gameplay.selectTask("-2 Plant?")

    tasks.isEmpty() shouldBe true
    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
  }

  @Test
  internal fun `selection executes a concrete task immediately`() {
    initiate("Plant!")
    gameplay.selectTask("Plant!")

    tasks.isEmpty() shouldBe true
    assertHistoryTypes(
        TaskAddedEvent::class,
        TaskEditedEvent::class,
        GameEvent.ChangeEvent::class,
        TaskRemovedEvent::class,
    )
    gameplay.count("Plant") shouldBe 1
  }

  @Test
  internal fun `selection resolves limits before narrowing`() {
    initiate("-30 TerraformRating?")
    gameplay.selectTask("-30 TerraformRating?")
    tasksAsText().shouldContainExactlyInAnyOrder("-20 TerraformRating<Player1>?")
    tasks.extract { it.selected }.shouldContainExactly(true)
  }

  @Test
  internal fun `selection failure is atomic`() {
    initiate("-Plant!")
    history().shouldHaveSize(1)
    shouldThrow<LimitsException> { gameplay.selectTask("-Plant!") }

    history().shouldHaveSize(1)
    tasks.extract { it.selected }.shouldContainExactly(false)
  }

  @Test
  internal fun `the select-lock rejects a second selection`() {
    initiate("Plant?, Heat?")
    val (plant, heat) = tasks.ids().toList()

    gameplay.selectTask(plant)

    shouldThrow<TaskException> { gameplay.selectTask(heat) }
    tasks.selectedTask() shouldBe plant
  }

  @Test
  internal fun `selection resolves an OR by pruning impossible options`() {
    initiate("-TR OR -Plant OR Heat OR Tharsis_5_5!")
    gameplay.selectTask("-TR OR -Plant OR Heat OR Tharsis_5_5!")

    tasksAsText().shouldContainExactlyInAnyOrder("-TerraformRating<Player1>! OR Heat<Player1>!")
  }

  private fun initiate(ins: String) = (gameplay as TaskLayer).addTasks(ins)

  private fun history() = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) =
      history().map { it::class }.shouldContainExactly(*c)

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
