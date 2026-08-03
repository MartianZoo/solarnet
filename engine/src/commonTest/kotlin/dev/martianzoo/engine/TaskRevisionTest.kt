package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.GameEvent
import dev.martianzoo.data.GameEvent.TaskAddedEvent
import dev.martianzoo.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.tfm.engine.canonicalPremise
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import kotlin.test.Test

class TaskRevisionTest {
  private val game = Engine.newGame(canonicalPremise())

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
    initiate("2 Plant?")

    tasks.extract { "${it.instruction}" }.shouldContainExactlyInAnyOrder("2 Plant<Player1>?")
    history().shouldHaveSize(1)
  }

  @Test
  fun `narrowing an instruction to itself has no effect`() {
    initiate("2 Plant?")
    val before = game.timeline.checkpoint()

    writer.reviseTask("2 Plant?", "2 Plant?")
    tasksAsText().shouldContainExactlyInAnyOrder("2 Plant<Player1>?")
    events.entriesSince(before).shouldBeEmpty()
  }

  @Test
  fun `a normal case of narrowing works normally`() {
    initiate("2 Plant?")

    writer.reviseTask("2 Plant?", "Plant!")
    history().shouldHaveSize(2)
    tasksAsText().shouldContainExactlyInAnyOrder("Plant<Player1>!")
  }

  @Test
  fun `an invalid narrowing fails, atomically`() {
    initiate("2 Plant?")
    history().shouldHaveSize(1)
    shouldThrow<NarrowingException> { writer.reviseTask("2 Plant?", "3 Plant!") }
    history().shouldHaveSize(1)
  }

  @Test
  fun `repeated narrowing`() {
    initiate("3 StandardResource?")

    writer.reviseTask("3 StandardResource?", "2 StandardResource?")
    writer.reviseTask("2 StandardResource?", "2 Plant?")
    writer.reviseTask("2 Plant?", "Plant?")
    writer.reviseTask("Plant?", "Plant!")

    tasksAsText().shouldContainExactlyInAnyOrder("Plant<Player1>!")
  }

  @Test
  fun `narrowing an OR works normally`() {
    initiate("5 Plant OR 4 Heat")
    tasksAsText().shouldContainExactlyInAnyOrder("5 Plant<Player1>! OR 4 Heat<Player1>!")

    writer.reviseTask("5 Plant OR 4 Heat", "5 Plant")
    history().shouldHaveSize(2)
    tasksAsText().shouldContainExactlyInAnyOrder("5 Plant<Player1>!")
  }

  @Test
  fun `narrowing an OR can enqueue multiple instructions`() {
    initiate("5 Plant OR (4 Heat, 2 Energy)")

    writer.reviseTask("5 Plant OR (4 Heat, 2 Energy)", "4 Heat, 2 Energy")

    assertHistoryTypes(
        TaskAddedEvent::class, // full one
        TaskAddedEvent::class, // heat
        TaskAddedEvent::class, // energy
        TaskRemovedEvent::class, // -full one
    )
    tasksAsText().shouldContainExactlyInAnyOrder("4 Heat<Player1>!", "2 Energy<Player1>!")
  }

  @Test
  fun `narrowing an OR can narrow each instruction in a grouped arm`() {
    initiate("5 Plant OR (4 StandardResource, 2 StandardResource)")

    writer.reviseTask(
        "5 Plant OR (4 StandardResource, 2 StandardResource)",
        "4 Heat, 2 Energy",
    )

    tasksAsText().shouldContainExactlyInAnyOrder("4 Heat<Player1>!", "2 Energy<Player1>!")
  }

  @Test
  fun `changing a grouped instruction is a narrowing failure`() {
    initiate("TR: (Plant, Heat)")

    shouldThrow<NarrowingException> {
      writer.reviseTask("TR: (Plant, Heat)", "TR: (Plant, Steel)")
    }
  }

  @Test
  fun `narrowing to Ok automatically handles the task`() {
    initiate("2 Plant?")

    writer.reviseTask("2 Plant?", "Ok")
    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
    tasks.isEmpty() shouldBe true
  }

  @Test
  fun `narrowing to something impossible is not prevented`() {
    initiate("-30 TerraformRating?")

    writer.reviseTask("-30 TerraformRating?", "-21 TerraformRating!")

    history().shouldHaveSize(2)
    tasksAsText().shouldContainExactlyInAnyOrder("-21 TerraformRating<Player1>!")

    // Not the point of this test class, but incidentally, we're at a dead end
    shouldThrow<LimitsException> { writer.prepareTask("-21 TerraformRating!") }
    shouldThrow<LimitsException> { writer.doTask("-21 TerraformRating!") }
    shouldThrow<LimitsException> { game.gameplay(PLAYER1).autoExecNow() }
  }

  @Test
  fun `narrowing to NoOp enqueues the THEN instructions`() {
    initiate("Plant? THEN (Steel, Heat)")
    tasks.extract { "${it.instruction}" }.shouldContainExactlyInAnyOrder("Plant<Player1>?")
    tasks.extract { "${it.then}" }.shouldContainExactlyInAnyOrder("Steel<Player1>!, Heat<Player1>!")

    writer.reviseTask("Plant?", "Ok")
    tasksAsText().shouldContainExactly("Steel<Player1>!", "Heat<Player1>!")
    tasks.matching { it.then != null }.none() shouldBe true
  }

  @Test
  fun `a chain of 4 THEN clauses has the head sliced off one by one`() {
    initiate("Plant? THEN Steel? THEN Heat? THEN Energy")

    writer.reviseTask("Plant?", "Ok")

    val task1 = tasks.extract { it }.single()
    task1.instruction.toString() shouldBe "Steel<Player1>?"
    task1.then.toString() shouldBe "Heat<Player1>? THEN Energy<Player1>!"

    writer.reviseTask("Steel?", "Ok")
    val task2 = tasks.extract { it }.single()
    task2.instruction.toString() shouldBe "Heat<Player1>?"
    task2.then.toString() shouldBe "Energy<Player1>!"

    writer.reviseTask("Heat?", "Ok")
    val task3 = tasks.extract { it }.single()
    task3.instruction.toString() shouldBe "Energy<Player1>!"
    task3.then shouldBe null
  }

  @Test
  fun `executing a THEN head creates independent abstract tail tasks`() {
    initiate("Plant! THEN (Steel?, Heat?)")

    writer.doTask("Plant!")

    tasksAsText().shouldContainExactlyInAnyOrder("Steel<Player1>?", "Heat<Player1>?")
    tasks.matching { it.then != null }.shouldBeEmpty()

    writer.reviseTask("Heat?", "Heat!")
    writer.doTask("Heat!")

    writer.reviseTask("Steel?", "Steel!")
    writer.doTask("Steel!")

    tasks.isEmpty() shouldBe true
  }

  private fun initiate(ins: String) = writer.godMode().addTasks(ins)

  private operator fun Checkpoint.plus(increment: Int) = Checkpoint(ordinal + increment)

  private fun history(): List<GameEvent> = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) {
    history().map { it::class.simpleName!! } shouldBe c.map { it.simpleName!! }
  }

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
