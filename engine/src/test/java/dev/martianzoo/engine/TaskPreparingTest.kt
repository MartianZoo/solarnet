package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.data.GameEvent
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskEditedEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.engine.Gameplay.TaskLayer
import dev.martianzoo.tfm.canon.Canon
import kotlin.reflect.KClass
import kotlin.test.Test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class TaskPreparingTest {
  private val A = TaskId("A")
  private val game = Engine.newGame(Canon.SIMPLE_GAME)
  private val tasks = game.tasks
  private val events = game.events
  private val start = game.timeline.checkpoint()
  private val gameplay = game.gameplay(PLAYER1)

  @Test
  fun `can prepare an abstract task`() {
    initiate("2 Plant?").also { it.shouldContainExactlyInAnyOrder(A) }
    gameplay.prepareTask(A).also { it shouldBe A }

    val task = tasks.extract { it }.single()
    task.next shouldBe true
    "${task.instruction}" shouldBe "2 Plant<Player1>?"
    assertHistoryTypes(TaskAddedEvent::class, TaskEditedEvent::class)
  }

  @Test
  fun `preparing to NoOp automatically handles the task 1`() {
    initiate("-2 Plant?").also { it.shouldContainExactlyInAnyOrder(A) }
    gameplay.prepareTask(A).also { it shouldBe null }

    tasks.isEmpty() shouldBe true
    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
  }

  @Test
  fun `preparing to NoOp automatically handles the task 2`() {
    initiate("Plant / Heat").also { it.shouldContainExactlyInAnyOrder(A) }
    gameplay.prepareTask(A).also { it shouldBe null }

    tasks.isEmpty() shouldBe true
    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
  }

  @Test
  fun `preparing adjusts for limits 1`() {
    initiate("-30 TerraformRating?").also { it.shouldContainExactlyInAnyOrder(A) }
    gameplay.reviseTask(A, "-25 TerraformRating?")
    gameplay.prepareTask(A).also { it shouldBe A }
    tasksAsText().shouldContainExactlyInAnyOrder("-20 TerraformRating<Player1>?")
    gameplay.reviseTask(A, "-15 TerraformRating!")
  }

  @Test
  fun `preparing adjusts for limits 2`() {
    initiate("-30 TerraformRating.").also { it.shouldContainExactlyInAnyOrder(A) }
    gameplay.prepareTask(A).also { it shouldBe A }

    tasksAsText().shouldContainExactlyInAnyOrder("-20 TerraformRating<Player1>!")
  }

  @Test
  fun `preparing fails due to limit`() {
    initiate("-Plant!").also { it.shouldContainExactlyInAnyOrder(A) }
    history().shouldHaveSize(1)
    shouldThrow<LimitsException> { gameplay.prepareTask(A) }

    history().shouldHaveSize(1)
  }

  @Test
  fun `preparing then narrowing results in automatic re-preparing`() {
    initiate("PROD[-2 StandardResource]").also { it.shouldContainExactlyInAnyOrder(A) }
    gameplay.prepareTask(A).also { it shouldBe A }

    tasksAsText().shouldContainExactlyInAnyOrder("-2 Production<Player1>!")
    shouldThrow<LimitsException> { gameplay.reviseTask(A, "PROD[-2 Plant]") }
    gameplay.reviseTask(A, "PROD[-2]")
  }

  @Test
  fun `preparing an OR prunes the options`() {
    initiate("-TR OR -Plant OR Heat OR Tharsis_5_5!").also { it.shouldContainExactlyInAnyOrder(A) }
    gameplay.prepareTask(A).also { it shouldBe A }

    tasksAsText().shouldContainExactlyInAnyOrder("-TerraformRating<Player1>! OR Heat<Player1>!")
  }

  @Test
  fun `preparing to NoOp enqueues the THEN instructions`() {
    initiate("Plant / Heat THEN Steel / 2 OxygenStep THEN Heat").also {
      it.shouldContainExactlyInAnyOrder(A)
    }
    gameplay.prepareTask(A).also { it shouldBe null }

    tasksAsText().shouldContainExactlyInAnyOrder("Steel<Player1>! / 2 OxygenStep")
  }

  fun initiate(ins: String) = (gameplay as TaskLayer).addTasks(ins)

  private fun history() = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) =
      history().map { it::class }.shouldContainExactly(*c)

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
