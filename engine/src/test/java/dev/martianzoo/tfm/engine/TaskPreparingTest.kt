package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Gameplay.TaskLayer
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEditedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Task.TaskId
import kotlin.reflect.KClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TaskPreparingTest {
  private val A = TaskId("A")
  private val game = Engine.newGame(Canon.SIMPLE_GAME)
  private val tasks = game.tasks
  private val events = game.events
  private val start = game.timeline.checkpoint()
  private val gameplay = game.gameplay(PLAYER1)

  @Test
  fun `can prepare an abstract task`() {
    initiate("2 Plant?").also { assertThat(it).containsExactly(A) }
    gameplay.prepareTask(A).also { assertThat(it).isEqualTo(A) }

    val task = tasks.extract { it }.single()
    assertThat(task.next)
    assertThat("${task.instruction}").isEqualTo("2 Plant<Player1>?")
    assertHistoryTypes(TaskAddedEvent::class, TaskEditedEvent::class)
  }

  @Test
  fun `preparing to NoOp automatically handles the task 1`() {
    initiate("-2 Plant?").also { assertThat(it).containsExactly(A) }
    gameplay.prepareTask(A).also { assertThat(it).isNull() }

    assertThat(tasks.isEmpty()).isTrue()
    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
  }

  @Test
  fun `preparing to NoOp automatically handles the task 2`() {
    initiate("Plant / Heat").also { assertThat(it).containsExactly(A) }
    gameplay.prepareTask(A).also { assertThat(it).isNull() }

    assertThat(tasks.isEmpty()).isTrue()
    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
  }

  @Test
  fun `preparing adjusts for limits 1`() {
    initiate("-30 TerraformRating?").also { assertThat(it).containsExactly(A) }
    gameplay.reviseTask(A, "-25 TerraformRating?")
    gameplay.prepareTask(A).also { assertThat(it).isEqualTo(A) }
    assertThat(tasksAsText()).containsExactly("-20 TerraformRating<Player1>?")
    gameplay.reviseTask(A, "-15 TerraformRating!")
  }

  @Test
  fun `preparing adjusts for limits 2`() {
    initiate("-30 TerraformRating.").also { assertThat(it).containsExactly(A) }
    gameplay.prepareTask(A).also { assertThat(it).isEqualTo(A) }

    assertThat(tasksAsText()).containsExactly("-20 TerraformRating<Player1>!")
  }

  @Test
  fun `preparing fails due to limit`() {
    initiate("-Plant!").also { assertThat(it).containsExactly(A) }
    assertThat(history()).hasSize(1)
    assertThrows<LimitsException> { gameplay.prepareTask(A) }

    assertThat(history()).hasSize(1)
  }

  @Test
  fun `preparing then narrowing results in automatic re-preparing`() {
    initiate("PROD[-2 StandardResource]").also { assertThat(it).containsExactly(A) }
    gameplay.prepareTask(A).also { assertThat(it).isEqualTo(A) }

    assertThat(tasksAsText()).containsExactly("-2 Production<Player1>!")
    assertThrows<LimitsException> { gameplay.reviseTask(A, "PROD[-2 Plant]") }
    gameplay.reviseTask(A, "PROD[-2]")
  }

  @Test
  fun `preparing an OR prunes the options`() {
    initiate("-TR OR -Plant OR Heat OR Tharsis_5_5!").also { assertThat(it).containsExactly(A) }
    gameplay.prepareTask(A).also { assertThat(it).isEqualTo(A) }

    assertThat(tasksAsText()).containsExactly("-TerraformRating<Player1>! OR Heat<Player1>!")
  }

  @Test
  fun `preparing to NoOp enqueues the THEN instructions`() {
    initiate("Plant / Heat THEN Steel / 2 OxygenStep THEN Heat").also {
      assertThat(it).containsExactly(A)
    }
    gameplay.prepareTask(A).also { assertThat(it).isNull() }

    assertThat(tasksAsText()).containsExactly("Steel<Player1>! / 2 OxygenStep")
  }

  fun initiate(ins: String) = (gameplay as TaskLayer).addTasks(ins)

  private fun history() = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) =
      assertThat(history().map { it::class }).containsExactly(*c).inOrder()

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
