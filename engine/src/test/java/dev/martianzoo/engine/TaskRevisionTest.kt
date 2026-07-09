package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.GameEvent
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Task.TaskId
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.tfm.canon.Canon
import kotlin.reflect.KClass
import kotlin.test.Test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class TaskRevisionTest {
  private val A = TaskId("A")
  private val game = Engine.newGame(Canon.SIMPLE_GAME)

  // Kinda gross
  private val tasks: TaskQueue = game.tasks
  private val events = game.events
  private val writer = game.gameplay(PLAYER1)
  private val start = game.timeline.checkpoint()

  @Test
  fun `initiating NoOp does nothing`() {
    val tasks = initiate("Ok")

    tasks.shouldBeEmpty()
    history().shouldBeEmpty()
    game.timeline.checkpoint() shouldBe start
  }

  @Test
  fun `initiating an abstract task works as expected`() {
    val task = initiate("2 Plant?").single()

    task shouldBe A
    tasks.extract { "${it.instruction}" }.shouldContainExactlyInAnyOrder("2 Plant<Player1>?")
    tasks.ids().shouldContainExactlyInAnyOrder(A)
    history().shouldHaveSize(1)
  }

  @Test
  fun `narrowing an instruction to itself has no effect`() {
    initiate("2 Plant?")
    val before = game.timeline.checkpoint()

    writer.reviseTask(A, "2 Plant?")
    tasks.ids().shouldContainExactlyInAnyOrder(A)
    events.entriesSince(before).shouldBeEmpty()
  }

  @Test
  fun `a normal case of narrowing works normally`() {
    initiate("2 Plant?")

    writer.reviseTask(A, "Plant!")
    history().shouldHaveSize(2)
    tasksAsText().shouldContainExactlyInAnyOrder("Plant<Player1>!")
  }

  @Test
  fun `an invalid narrowing fails, atomically`() {
    initiate("2 Plant?")
    history().shouldHaveSize(1)
    shouldThrow<NarrowingException> { writer.reviseTask(A, "3 Plant!") }
    history().shouldHaveSize(1)
  }

  @Test
  fun `repeated narrowing`() {
    initiate("3 StandardResource?")

    writer.reviseTask(A, "2 StandardResource?")
    writer.reviseTask(A, "2 Plant?")
    writer.reviseTask(A, "Plant?")
    writer.reviseTask(A, "Plant!")

    tasksAsText().shouldContainExactlyInAnyOrder("Plant<Player1>!")
  }

  @Test
  fun `narrowing an OR works normally`() {
    initiate("5 Plant OR 4 Heat")
    tasksAsText().shouldContainExactlyInAnyOrder("5 Plant<Player1>! OR 4 Heat<Player1>!")

    writer.reviseTask(A, "5 Plant")
    history().shouldHaveSize(2)
    tasksAsText().shouldContainExactlyInAnyOrder("5 Plant<Player1>!")
  }

  @Test
  fun `narrowing an OR can enqueue multiple instructions`() {
    initiate("5 Plant OR (4 Heat, 2 Energy)")

    writer.reviseTask(A, "4 Heat, 2 Energy")

    assertHistoryTypes(
        TaskAddedEvent::class, // full one
        TaskAddedEvent::class, // heat
        TaskAddedEvent::class, // energy
        TaskRemovedEvent::class, // -full one
    )
    tasksAsText().shouldContainExactlyInAnyOrder("4 Heat<Player1>!", "2 Energy<Player1>!")
  }

  @Test
  fun `narrowing to Ok automatically handles the task`() {
    initiate("2 Plant?")

    writer.reviseTask(A, "Ok")
    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
    tasks.isEmpty() shouldBe true
  }

  @Test
  fun `narrowing to something impossible is not prevented`() {
    initiate("-30 TerraformRating?")

    writer.reviseTask(A, "-21 TerraformRating!")

    history().shouldHaveSize(2)
    tasksAsText().shouldContainExactlyInAnyOrder("-21 TerraformRating<Player1>!")

    // Not the point of this test class, but incidentally, we're at a dead end
    shouldThrow<LimitsException> { writer.prepareTask(A) }
    shouldThrow<LimitsException> { writer.doTask(A) }
    shouldThrow<LimitsException> { game.gameplay(PLAYER1).autoExecNow() }
  }

  @Test
  fun `narrowing to NoOp enqueues the THEN instructions`() {
    val task = initiate("Plant? THEN (Steel, Heat)").single()
    tasks.extract { "${it.instruction}" }.shouldContainExactlyInAnyOrder("Plant<Player1>?")
    tasks.extract { "${it.then}" }.shouldContainExactlyInAnyOrder("Steel<Player1>!, Heat<Player1>!")

    writer.reviseTask(task, "Ok")
    tasksAsText().shouldContainExactly("Steel<Player1>!", "Heat<Player1>!")
    tasks.matching { it.then != null }.none() shouldBe true
  }

  @Test
  fun `a chain of 4 THEN clauses has the head sliced off one by one`() {
    val id = initiate("Plant? THEN Steel? THEN Heat? THEN Energy").single()

    writer.reviseTask(id, "Ok")

    val task1 = tasks.extract { it }.single()
    task1.instruction.toString() shouldBe "Steel<Player1>?"
    task1.then.toString() shouldBe "Heat<Player1>? THEN Energy<Player1>!"

    writer.reviseTask(task1.id, "Ok")
    val task2 = tasks.extract { it }.single()
    task2.instruction.toString() shouldBe "Heat<Player1>?"
    task2.then.toString() shouldBe "Energy<Player1>!"

    writer.reviseTask(task2.id, "Ok")
    val task3 = tasks.extract { it }.single()
    task3.instruction.toString() shouldBe "Energy<Player1>!"
    task3.then shouldBe null
  }

  fun initiate(ins: String) = writer.godMode().addTasks(ins)

  private operator fun Checkpoint.plus(increment: Int) = Checkpoint(ordinal + increment)

  private fun history(): List<GameEvent> = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) {
    history().map { it::class.simpleName!! } shouldBe c.map { it.simpleName!! }
  }

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
