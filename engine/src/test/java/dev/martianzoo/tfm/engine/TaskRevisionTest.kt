package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameEvent
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Timeline.Checkpoint
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import kotlin.reflect.KClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TaskRevisionTest {
  private val A = TaskId("A")
  private val NO_CHANGE = TaskResult()
  private val game = Engine.newGame(Canon.SIMPLE_GAME)

  // Kinda gross
  private val tasks = game.tasks
  private val events = game.events
  private val writer = game.writer(PLAYER1)
  private val start = game.timeline.checkpoint()

  @Test
  fun `initiating NoOp does nothing`() {
    val tasks = initiate("Ok")

    assertThat(tasks).isEmpty()
    assertThat(history()).isEmpty()
    assertThat(game.timeline.checkpoint()).isEqualTo(start)
  }

  @Test
  fun `initiating an abstract task works as expected`() {
    val task = initiate("2 Plant?").single()

    assertThat(task).isEqualTo(A)
    assertThat(tasks.extract { "${it.instruction}" }).containsExactly("2 Plant<Player1>?")
    assertThat(tasks.ids()).containsExactly(A)
    assertThat(history()).hasSize(1)
  }

  @Test
  fun `narrowing an instruction to itself has no effect`() {
    initiate("2 Plant?")
    val before = game.timeline.checkpoint()

    val result = writer.reviseTask(A, parse("2 Plant?"))

    assertThat(result).isEqualTo(NO_CHANGE)
    assertThat(tasks.ids()).containsExactly(A)
    assertThat(events.entriesSince(before)).isEmpty()
  }

  @Test
  fun `a normal case of narrowing works normally`() {
    initiate("2 Plant?")

    val result = writer.reviseTask(A, parse("Plant!"))

    assertThat(result).isEqualTo(NO_CHANGE)
    assertThat(history()).hasSize(2)
    assertThat(tasksAsText()).containsExactly("Plant<Player1>!")
  }

  @Test
  fun `an invalid narrowing fails, atomically`() {
    initiate("2 Plant?")
    assertThat(history()).hasSize(1)
    assertThrows<NarrowingException> { writer.reviseTask(A, parse("3 Plant!")) }
    assertThat(history()).hasSize(1)
  }

  @Test
  fun `repeated narrowing`() {
    initiate("3 StandardResource?")

    writer.reviseTask(A, parse("2 StandardResource?"))
    writer.reviseTask(A, parse("2 Plant?"))
    writer.reviseTask(A, parse("Plant?"))
    writer.reviseTask(A, parse("Plant!"))

    assertThat(tasksAsText()).containsExactly("Plant<Player1>!")
  }

  @Test
  fun `narrowing an OR works normally`() {
    initiate("5 Plant OR 4 Heat")
    assertThat(tasksAsText()).containsExactly("5 Plant<Player1>! OR 4 Heat<Player1>!")

    val result = writer.reviseTask(A, parse("5 Plant"))

    assertThat(result).isEqualTo(NO_CHANGE)
    assertThat(history()).hasSize(2)
    assertThat(tasksAsText()).containsExactly("5 Plant<Player1>!")
  }

  @Test
  fun `narrowing an OR can enqueue multiple instructions`() {
    initiate("5 Plant OR (4 Heat, 2 Energy)")

    writer.reviseTask(A, parse("4 Heat, 2 Energy"))

    assertHistoryTypes(
        TaskAddedEvent::class, // full one
        TaskAddedEvent::class, // heat
        TaskAddedEvent::class, // energy
        TaskRemovedEvent::class, // -full one
    )
    assertThat(tasksAsText()).containsExactly("4 Heat<Player1>!", "2 Energy<Player1>!")
  }

  @Test
  fun `narrowing to NoOp automatically handles the task`() {
    initiate("2 Plant?")

    val result = writer.reviseTask(A, NoOp)
    assertThat(result).isEqualTo(NO_CHANGE)

    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
    assertThat(tasks.isEmpty()).isTrue()
  }

  @Test
  fun `narrowing to something impossible is not prevented`() {
    initiate("-30 TerraformRating?")

    writer.reviseTask(A, parse("-21 TerraformRating!"))

    assertThat(history()).hasSize(2)
    assertThat(tasksAsText()).containsExactly("-21 TerraformRating<Player1>!")

    // Not the point of this test class, but incidentally, we're at a dead end
    assertThrows<LimitsException> { writer.prepareTask(A) }
    assertThrows<LimitsException> { writer.executeTask(A) }
    assertThrows<LimitsException> { game.gameplay(PLAYER1).autoExecNow() }
  }

  @Test
  fun `narrowing to NoOp enqueues the THEN instructions`() {
    val task = initiate("Plant? THEN (Steel, Heat)").single()
    assertThat(tasks.extract { "${it.instruction}" }).containsExactly("Plant<Player1>?")
    assertThat(tasks.extract { "${it.then}" }).containsExactly("Steel<Player1>!, Heat<Player1>!")

    val result = writer.reviseTask(task, NoOp)
    assertThat(result.tasksSpawned).hasSize(2)
    assertThat(tasksAsText()).containsExactly("Steel<Player1>!", "Heat<Player1>!").inOrder()
    assertThat(tasks.matching { it.then != null }.none())
  }

  @Test
  fun `a chain of 4 THEN clauses has the head sliced off one by one`() {
    val id = initiate("Plant? THEN Steel? THEN Heat? THEN Energy").single()

    val result = writer.reviseTask(id, NoOp)
    assertThat(result.tasksSpawned).hasSize(1)

    val task1 = tasks.extract { it }.single()
    assertThat(task1.instruction.toString()).isEqualTo("Steel<Player1>?")
    assertThat(task1.then.toString()).isEqualTo("Heat<Player1>? THEN Energy<Player1>!")

    writer.reviseTask(task1.id, NoOp)
    val task2 = tasks.extract { it }.single()
    assertThat(task2.instruction.toString()).isEqualTo("Heat<Player1>?")
    assertThat(task2.then.toString()).isEqualTo("Energy<Player1>!")

    writer.reviseTask(task2.id, NoOp)
    val task3 = tasks.extract { it }.single()
    assertThat(task3.instruction.toString()).isEqualTo("Energy<Player1>!")
    assertThat(task3.then).isNull()
  }

  fun initiate(ins: String) = writer.addTasks(parse(ins)).tasksSpawned

  private operator fun Checkpoint.plus(increment: Int) = Checkpoint(ordinal + increment)

  private fun history(): List<GameEvent> = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) {
    assertThat(history().map { it::class.simpleName!! })
        .containsExactlyElementsIn(c.map { it.simpleName!! })
        .inOrder()
  }

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
