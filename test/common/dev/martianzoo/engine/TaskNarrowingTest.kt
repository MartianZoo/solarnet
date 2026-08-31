package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.GameEvent
import dev.martianzoo.pets.data.GameEvent.GameplayInputEvent
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

internal class TaskNarrowingTest {
  private val game = Engine.newGame(canonicalPremise(), inputOnlySynonyms = TEST_CLASS_SYNONYMS)

  // Kinda gross
  private val tasks: TaskQueue = game.tasks
  private val events = game.events
  private val writer = game.agent(PLAYER1)
  private val start = game.timeline.checkpoint()

  init {
    writer.autoExecMode = NONE
  }

  @Test
  internal fun `initiating NoOp does nothing`() {
    val tasks = initiate("Ok")

    tasks.isEmpty() shouldBe true
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
  internal fun `narrowing an instruction to itself adds only input provenance after selection`() {
    initiate("2 Plant?")
    writer.selectTask("2 Plant?")
    val before = game.timeline.checkpoint()

    writer.narrowTask("2 Plant?")
    tasksAsText().shouldContainExactlyInAnyOrder("2 Plant<Player1>?")
    events.entriesSince(before).map { it::class }.shouldContainExactly(GameplayInputEvent::class)
  }

  @Test
  internal fun `a concrete narrowing executes immediately`() {
    initiate("2 Plant?")

    selectAndNarrow("2 Plant?", "Plant!")
    writer.count("Plant") shouldBe 1
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `an invalid narrowing fails, atomically`() {
    initiate("2 Plant?")
    history().shouldHaveSize(1)
    shouldThrow<NarrowingException> { selectAndNarrow("2 Plant?", "3 Plant!") }
    history().shouldHaveSize(3)
    tasks.extract { it.selected }.shouldContainExactly(true)
  }

  @Test
  internal fun `repeated narrowing`() {
    initiate("3 StandardResource?")

    selectAndNarrow("3 StandardResource?", "2 StandardResource?")
    selectAndNarrow("2 StandardResource?", "2 Plant?")
    selectAndNarrow("2 Plant?", "Plant?")
    selectAndNarrow("Plant?", "Plant!")

    writer.count("Plant") shouldBe 1
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `narrowing an OR works normally`() {
    initiate("5 Plant OR 4 Heat")
    tasksAsText().shouldContainExactlyInAnyOrder("5 Plant<Player1>! OR 4 Heat<Player1>!")

    selectAndNarrow("5 Plant OR 4 Heat", "5 Plant")
    writer.count("Plant") shouldBe 5
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `narrowing an OR can enqueue multiple instructions`() {
    initiate("5 Plant OR (4 Heat, 2 Energy)")

    selectAndNarrow("5 Plant OR (4 Heat, 2 Energy)", "4 Heat, 2 Energy")

    assertHistoryTypes(
        TaskAddedEvent::class, // full one
        GameEvent.TaskEditedEvent::class, // selected
        GameplayInputEvent::class, // selection
        TaskAddedEvent::class, // heat
        TaskAddedEvent::class, // energy
        TaskRemovedEvent::class, // -full one
        GameplayInputEvent::class, // narrowing
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

    selectAndNarrow(
        "5 Plant OR (4 StandardResource, 2 StandardResource)",
        "4 Heat, 2 Energy",
    )

    tasksAsText().shouldContainExactlyInAnyOrder("4 Heat<Player1>!", "2 Energy<Player1>!")
  }

  @Test
  internal fun `narrowing to the first stage executes it and admits its THEN continuation`() {
    writer.manual("ProjectCard")
    initiate("(-ProjectCard THEN ProjectCard) OR Ok")

    selectAndNarrow("(-ProjectCard THEN ProjectCard) OR Ok", "-ProjectCard")

    tasksAsText().shouldContainExactly("ProjectCard<Player1, Hand>!")
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
  internal fun `an unmet gate prevents selection before narrowing`() {
    initiate("10 TR: Plant")

    shouldThrow<dev.martianzoo.pets.api.Exceptions.RequirementException> {
      writer.selectTask("10 TR: Plant")
    }

    tasksAsText().shouldContainExactly("10 TerraformRating<Player1>: Plant<Player1>!")
  }

  @Test
  internal fun `resolution that produces siblings completes the selected structural task`() {
    writer.manual("Plant")
    val original = initiate("Plant: (Steel?, Heat?)").single()

    writer.selectTask(original)

    (original in tasks) shouldBe false
    tasksAsText().shouldContainExactlyInAnyOrder("Steel<Player1>?", "Heat<Player1>?")
    tasks.extract { it.selected }.shouldContainExactly(false, false)
  }

  @Test
  internal fun `narrowing to Ok automatically handles the task`() {
    initiate("2 Plant?")

    selectAndNarrow("2 Plant?", "Ok")
    assertHistoryTypes(
        TaskAddedEvent::class,
        GameEvent.TaskEditedEvent::class,
        GameplayInputEvent::class,
        TaskRemovedEvent::class,
        GameplayInputEvent::class,
    )
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `selection can resolve a limited task directly to completion`() {
    initiate("-30 TerraformRating?")

    writer.selectTask("-30 TerraformRating?")

    writer.count("TerraformRating") shouldBe 0
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `narrowing a selected AMAP target rejects an occupied area`() {
    writer.autoExecMode = AutoExecMode.FIRST
    writer.manual("OceanTile<Tharsis_1_2>")
    writer.autoExecMode = NONE
    initiate("OceanTile<>")

    writer.selectTask("OceanTile<>")
    shouldThrow<NarrowingException> { writer.narrowTask("OceanTile<Tharsis_1_2>") }
    writer.narrowTask("OceanTile<Tharsis_1_4>")

    tasks.selectedTask() shouldBe null
    writer.count("OceanTile<Tharsis_1_4>") shouldBe 1
  }

  @Test
  internal fun `an omitted selection intensity preserves a stronger pending intensity`() {
    initiate("OceanTile<LandArea>!")

    shouldThrow<TaskException> { writer.doTask("OceanTile<Tharsis_2_3>.") }
    writer.doTask("OceanTile<Tharsis_2_3>")

    writer.count("OceanTile<Tharsis_2_3>") shouldBe 1
    tasks.matching { "OceanTile" in it.instruction.toString() }.none() shouldBe true
  }

  @Test
  internal fun `an omitted selection intensity inherits from the selected OR arm`() {
    initiate("OceanTile<LandArea>! OR Plant!")

    writer.doTask("OceanTile<Tharsis_2_3>")

    writer.count("OceanTile<Tharsis_2_3>") shouldBe 1
    tasks.matching { "OceanTile" in it.instruction.toString() }.none() shouldBe true
  }

  @Test
  internal fun `selection resolves PER before its AMAP target is narrowed`() {
    writer.manual("Plant")
    initiate("OceanTile<> / Plant")

    writer.selectTask("OceanTile<> / Plant")
    writer.narrowTask("OceanTile<Tharsis_1_4>")

    tasks.selectedTask() shouldBe null
    writer.count("OceanTile<Tharsis_1_4>") shouldBe 1
  }

  @Test
  internal fun `selecting a PER-wrapped AMAP target with a zero metric resolves to NoOp`() {
    initiate("OceanTile<> / Steel")

    writer.selectTask("OceanTile<> / Steel")

    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `doing a task evaluates a PER narrowing before matching`() {
    writer.manual("3 Heat")
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

    selectAndNarrow("Plant?", "Ok")
    tasksAsText().shouldContainExactly("Steel<Player1>!", "Heat<Player1>!")
    tasks.matching { it.then != null }.none() shouldBe true
  }

  @Test
  internal fun `a chain of 4 THEN clauses has the head sliced off one by one`() {
    initiate("Plant? THEN Steel? THEN Heat? THEN Energy")

    selectAndNarrow("Plant?", "Ok")

    val task1 = tasks.extract { it }.single()
    task1.instruction.toString() shouldBe "Steel<Player1>?"
    task1.then.toString() shouldBe "Heat<Player1>? THEN Energy<Player1>!"

    selectAndNarrow("Steel?", "Ok")
    val task2 = tasks.extract { it }.single()
    task2.instruction.toString() shouldBe "Heat<Player1>?"
    task2.then.toString() shouldBe "Energy<Player1>!"

    selectAndNarrow("Heat?", "Ok")
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
  internal fun `narrowing a linked THEN to a concrete sequence splits its first stage`() {
    initiate("X Plant? THEN X Heat?")

    selectAndNarrow("X Plant? THEN X Heat?", "3 Plant THEN 3 Heat")

    val task = tasks.extract { it }.single()
    task.instruction.toString() shouldBe "3 Heat<Player1>!"
    task.then shouldBe null
    writer.count("Plant") shouldBe 3
  }

  @Test
  internal fun `executing a THEN head creates independent abstract tail tasks`() {
    initiate("Plant! THEN (Steel?, Heat?)")

    writer.doTask("Plant!")

    tasksAsText().shouldContainExactlyInAnyOrder("Steel<Player1>?", "Heat<Player1>?")
    tasks.matching { it.then != null }.shouldBeEmpty()

    selectAndNarrow("Heat?", "Heat!")

    selectAndNarrow("Steel?", "Steel!")

    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `autoexec leaves an AMAP choice that binds a later stage to the player`() {
    game.agent(PLAYER2).manual("3 MC")
    initiate("3 MC FROM MC<Player>. THEN Plant<Player>")

    writer.autoExecNow()

    tasksAsText().shouldContainExactly("3 MC<Player1> FROM MC<Player>. THEN Plant<Player>!")
    game.agent(PLAYER2).count("MC") shouldBe 3
  }

  @Test
  internal fun `autoexec does not infer an abstract AMAP actor from the sole existing component`() {
    game.agent(PLAYER2).manual("3 MC")
    initiate("3 MC FROM MC<Player>.")

    writer.autoExecNow()

    tasksAsText().shouldContainExactly("3 MC<Player1> FROM MC<Player>.")
    game.agent(PLAYER2).count("MC") shouldBe 3
  }

  @Test
  internal fun `selecting a zero-count AMAP actor after autoexec still binds the continuation`() {
    writer.manual("3 MC")
    initiate("3 MC FROM MC<Player>. THEN Plant<Player>")
    writer.autoExecNow()
    writer.autoExecMode = NONE

    writer.doTask("3 MC FROM MC<Player2>.")

    tasksAsText().shouldContainExactly("Plant<Player2>!")
    writer.count("MC") shouldBe 3
    game.agent(PLAYER2).count("Plant") shouldBe 0
  }

  @Test
  internal fun `selecting an AMAP source binds the later stage before resolution`() {
    game.agent(PLAYER2).manual("3 MC")
    writer.autoExecMode = NONE
    initiate("3 MC FROM MC<Player>. THEN Plant<Player>")

    writer.doTask("3 MC FROM MC<Player2>.")

    tasksAsText().shouldContainExactly("Plant<Player2>!")
    writer.count("MC") shouldBe 3
    game.agent(PLAYER2).count("MC") shouldBe 0
  }

  private fun initiate(ins: String) = writer.addTasks(ins)

  private fun selectAndNarrow(current: String, narrowing: String) {
    writer.selectTask(current)
    writer.narrowTask(narrowing)
  }

  private operator fun Checkpoint.plus(increment: Int) = Checkpoint(ordinal + increment)

  private fun history(): List<GameEvent> = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) {
    history().map { it::class.simpleName!! } shouldBe c.map { it.simpleName!! }
  }

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
