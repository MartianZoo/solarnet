package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.GameEvent
import dev.martianzoo.pets.data.GameEvent.TaskAddedEvent
import dev.martianzoo.pets.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass
import kotlin.test.Test

internal class TaskRevisionTest {
  private val game = Engine.newGame(canonicalPremise(), inputOnlySynonyms = TEST_CLASS_SYNONYMS)

  // Kinda gross
  private val tasks: TaskQueue = game.tasks
  private val events = game.events
  private val writer = game.gameplay(PLAYER1)
  private val start = game.timeline.checkpoint()

  @Test
  internal fun `initiating NoOp does nothing`() {
    val tasks = initiate("Ok")

    tasks.shouldBeEmpty()
    history().shouldBeEmpty()
    game.timeline.checkpoint() shouldBe start
  }

  @Test
  internal fun `initiating an abstract task works as expected`() {
    initiate("2 Plant?")

    tasks.extract { "${it.instruction}" }.shouldContainExactlyInAnyOrder("2 Plant<Player1>?")
    history().shouldHaveSize(1)
  }

  @Test
  internal fun `narrowing an instruction to itself has no effect`() {
    initiate("2 Plant?")
    val before = game.timeline.checkpoint()

    writer.reviseTask("2 Plant?", "2 Plant?")
    tasksAsText().shouldContainExactlyInAnyOrder("2 Plant<Player1>?")
    events.entriesSince(before).shouldBeEmpty()
  }

  @Test
  internal fun `a normal case of narrowing works normally`() {
    val originalId = initiate("2 Plant?").single()

    writer.reviseTask("2 Plant?", "Plant!")
    history().shouldHaveSize(2)
    tasksAsText().shouldContainExactlyInAnyOrder("Plant<Player1>!")
    tasks.ids().shouldContainExactly(originalId)
    originalId.ordinal shouldBe (history().single { it is TaskAddedEvent }).ordinal
  }

  @Test
  internal fun `an invalid narrowing fails, atomically`() {
    initiate("2 Plant?")
    history().shouldHaveSize(1)
    shouldThrow<NarrowingException> { writer.reviseTask("2 Plant?", "3 Plant!") }
    history().shouldHaveSize(1)
  }

  @Test
  internal fun `repeated narrowing`() {
    initiate("3 StandardResource?")

    writer.reviseTask("3 StandardResource?", "2 StandardResource?")
    writer.reviseTask("2 StandardResource?", "2 Plant?")
    writer.reviseTask("2 Plant?", "Plant?")
    writer.reviseTask("Plant?", "Plant!")

    tasksAsText().shouldContainExactlyInAnyOrder("Plant<Player1>!")
  }

  @Test
  internal fun `narrowing an OR works normally`() {
    initiate("5 Plant OR 4 Heat")
    tasksAsText().shouldContainExactlyInAnyOrder("5 Plant<Player1>! OR 4 Heat<Player1>!")

    writer.reviseTask("5 Plant OR 4 Heat", "5 Plant")
    history().shouldHaveSize(2)
    tasksAsText().shouldContainExactlyInAnyOrder("5 Plant<Player1>!")
  }

  @Test
  internal fun `narrowing an OR can enqueue multiple instructions`() {
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
  internal fun `an OR with only one live grouped arm starts each grouped task`() {
    initiate("(4 Heat, 2 Energy) OR Die")

    tasksAsText().shouldContainExactlyInAnyOrder("4 Heat<Player1>!", "2 Energy<Player1>!")
  }

  @Test
  internal fun `doing a task can select a grouped arm`() {
    initiate("5 Plant OR (4 Heat, 2 Energy)")

    writer.doTask("4 Heat, 2 Energy")

    writer.count("Heat") shouldBe 4
    writer.count("Energy") shouldBe 2
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `doing only one instruction from a grouped arm is rejected atomically`() {
    initiate("5 Plant OR (4 Heat, 2 Energy)")

    shouldThrow<TaskException> { writer.doTask("4 Heat") }

    writer.count("Heat") shouldBe 0
    writer.count("Energy") shouldBe 0
    tasksAsText()
        .shouldContainExactly("5 Plant<Player1>! OR (4 Heat<Player1>!, 2 Energy<Player1>!)")
  }

  @Test
  internal fun `trying a task can select a grouped arm`() {
    initiate("5 Plant OR (4 Heat, 2 Energy)")

    writer.tryTask("4 Heat, 2 Energy")

    writer.count("Heat") shouldBe 4
    writer.count("Energy") shouldBe 2
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `narrowing an OR can narrow each instruction in a grouped arm`() {
    initiate("5 Plant OR (4 StandardResource, 2 StandardResource)")

    writer.reviseTask(
        "5 Plant OR (4 StandardResource, 2 StandardResource)",
        "4 Heat, 2 Energy",
    )

    tasksAsText().shouldContainExactlyInAnyOrder("4 Heat<Player1>!", "2 Energy<Player1>!")
  }

  @Test
  internal fun `narrowing to the first stage selects a THEN arm of an OR`() {
    initiate("(-ProjectCard THEN ProjectCard) OR Ok")

    writer.reviseTask("(-ProjectCard THEN ProjectCard) OR Ok", "-ProjectCard")

    val discard = tasks.extract { it }.single()
    discard.instruction.toString() shouldBe "-ProjectCard<Player1>!"
    discard.then.toString() shouldBe "ProjectCard<Player1>!"
  }

  @Test
  internal fun `doing an entire THEN instruction at once is rejected`() {
    initiate("Plant THEN Heat")

    shouldThrow<TaskException> { writer.doTask("Plant THEN Heat") }

    writer.count("Plant") shouldBe 0
    writer.count("Heat") shouldBe 0
    tasksAsText().shouldContainExactly("Plant<Player1>!")
  }

  @Test
  internal fun `narrowing a gated instruction to Ok throws NarrowingException`() {
    initiate("10 TR: Plant")

    shouldThrow<NarrowingException> { writer.reviseTask("10 TR: Plant", "Ok") }

    tasksAsText().shouldContainExactly("10 TerraformRating<Player1>: Plant<Player1>!")
  }

  @Test
  internal fun `changing a grouped instruction is a narrowing failure`() {
    initiate("TR: (Plant, Heat)")

    shouldThrow<NarrowingException> {
      writer.reviseTask("TR: (Plant, Heat)", "TR: (Plant, Steel)")
    }
  }

  @Test
  internal fun `narrowing to Ok automatically handles the task`() {
    initiate("2 Plant?")

    writer.reviseTask("2 Plant?", "Ok")
    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `narrowing to something impossible is not prevented`() {
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
  internal fun `selecting an AMAP target early locks its domain and rejects a zero target`() {
    writer.godMode().manual("OceanTile<Tharsis_1_2>")
    initiate("OceanTile<>")

    shouldThrow<NarrowingException> {
      writer.reviseTask("OceanTile<>", "OceanTile<Tharsis_1_2>")
    }
    writer.reviseTask("OceanTile<>", "OceanTile<Tharsis_1_4>")

    tasks.extract { it.next }.shouldContainExactly(true)
    tasksAsText().shouldContainExactly("OceanTile<Tharsis_1_4>!")
  }

  @Test
  internal fun `an omitted selection intensity preserves a stronger pending intensity`() {
    initiate("OceanTile<LandArea>!")

    shouldThrow<TaskException> { writer.doTask("OceanTile<Tharsis_2_3>.") }
    writer.doTask("OceanTile<Tharsis_2_3>")

    writer.count("OceanTile<Tharsis_2_3>") shouldBe 1
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `an omitted selection intensity inherits from the selected OR arm`() {
    initiate("OceanTile<LandArea>! OR Plant!")

    writer.doTask("OceanTile<Tharsis_2_3>")

    writer.count("OceanTile<Tharsis_2_3>") shouldBe 1
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `selecting a PER-wrapped AMAP target early locks the evaluated instruction`() {
    writer.godMode().manual("Plant")
    initiate("OceanTile<> / Plant")

    writer.reviseTask("OceanTile<> / Plant", "OceanTile<Tharsis_1_4> / Plant")

    tasks.extract { it.next }.shouldContainExactly(true)
    tasksAsText().shouldContainExactly("OceanTile<Tharsis_1_4>!")
  }

  @Test
  internal fun `selecting a PER-wrapped AMAP target with a zero metric resolves to NoOp`() {
    initiate("OceanTile<> / Steel")

    writer.reviseTask("OceanTile<> / Steel", "OceanTile<Tharsis_1_4> / Steel")

    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `doing a task evaluates a PER revision before matching`() {
    writer.godMode().manual("3 Heat")
    initiate("X Plant?")

    writer.doTask("Plant / Heat")

    writer.count("Plant") shouldBe 3
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `narrowing to NoOp enqueues the THEN instructions`() {
    initiate("Plant? THEN (Steel, Heat)")
    tasks.extract { "${it.instruction}" }.shouldContainExactlyInAnyOrder("Plant<Player1>?")
    tasks.extract { "${it.then}" }.shouldContainExactlyInAnyOrder("Steel<Player1>!, Heat<Player1>!")

    writer.reviseTask("Plant?", "Ok")
    tasksAsText().shouldContainExactly("Steel<Player1>!", "Heat<Player1>!")
    tasks.matching { it.then != null }.none() shouldBe true
  }

  @Test
  internal fun `a chain of 4 THEN clauses has the head sliced off one by one`() {
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
  internal fun `a lone X does not keep otherwise independent THEN stages together`() {
    initiate("Plant? THEN X StandardResource?")

    val task = tasks.extract { it }.single()
    task.instruction.toString() shouldBe "Plant<Player1>?"
    task.then.toString() shouldBe "X StandardResource<Player1>?"
  }

  @Test
  internal fun `selecting a THEN head carries its X into the continuation`() {
    initiate("X Plant? THEN X Heat?")

    writer.doTask("3 Plant")

    tasksAsText().shouldContainExactly("3 Heat<Player1>?")
  }

  @Test
  internal fun `executing a THEN head creates independent abstract tail tasks`() {
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

  @Test
  internal fun `autoexec leaves an AMAP choice that binds a later stage to the player`() {
    game.gameplay(PLAYER2).godMode().manual("3 Megacredit")
    initiate("3 Megacredit FROM Megacredit<Player>. THEN Plant<Player>")

    writer.autoExecNow()

    tasksAsText()
        .shouldContainExactly("3 Megacredit<Player1> FROM Megacredit<Player>. THEN Plant<Player>!")
    game.gameplay(PLAYER2).count("Megacredit") shouldBe 3
  }

  @Test
  internal fun `autoexec does not infer an abstract AMAP actor from the sole existing component`() {
    game.gameplay(PLAYER2).godMode().manual("3 Megacredit")
    initiate("3 Megacredit FROM Megacredit<Player>.")

    writer.autoExecNow()

    tasksAsText().shouldContainExactly("3 Megacredit<Player1> FROM Megacredit<Player>.")
    game.gameplay(PLAYER2).count("Megacredit") shouldBe 3
  }

  @Test
  internal fun `selecting a zero-count AMAP actor after autoexec still binds the continuation`() {
    writer.godMode().manual("3 Megacredit")
    initiate("3 Megacredit FROM Megacredit<Player>. THEN Plant<Player>")
    writer.autoExecNow()
    writer.autoExecMode = NONE

    writer.doTask("3 Megacredit FROM Megacredit<Player2>.")

    tasksAsText().shouldContainExactly("Plant<Player2>!")
    writer.count("Megacredit") shouldBe 3
    game.gameplay(PLAYER2).count("Plant") shouldBe 0
  }

  @Test
  internal fun `selecting an AMAP source binds the later stage before preparation`() {
    game.gameplay(PLAYER2).godMode().manual("3 Megacredit")
    writer.autoExecMode = NONE
    initiate("3 Megacredit FROM Megacredit<Player>. THEN Plant<Player>")

    writer.doTask("3 Megacredit FROM Megacredit<Player2>.")

    tasksAsText().shouldContainExactly("Plant<Player2>!")
    writer.count("Megacredit") shouldBe 3
    game.gameplay(PLAYER2).count("Megacredit") shouldBe 0
  }

  private fun initiate(ins: String) = writer.godMode().addTasks(ins)

  private operator fun Checkpoint.plus(increment: Int) = Checkpoint(ordinal + increment)

  private fun history(): List<GameEvent> = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) {
    history().map { it::class.simpleName!! } shouldBe c.map { it.simpleName!! }
  }

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
