package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.TaskForm.Decision.Kind.ALTERNATIVE
import dev.martianzoo.agent.TaskForm.Decision.Kind.AMOUNT
import dev.martianzoo.agent.TaskForm.Decision.Kind.TARGET
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
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
  private val game = Engine.newGame(canonicalPremise())

  // Kinda gross
  private val tasks: TaskQueue = game.tasks
  private val events = game.events
  private val writer = game.testAgent(PLAYER1)
  private val start = game.timeline.checkpoint()

  init {
    writer.autoExecPolicy = NONE
  }

  @Test
  internal fun `initiating NoOp does nothing`() {
    val tasks = initiate("Ok")

    tasks.isEmpty() shouldBe true
    history().shouldBeEmpty()
    game.timeline.checkpoint() shouldBe start
  }

  @Test
  internal fun `nonmandatory Die is the empty zero-capacity change`() {
    initiate("Die?")
    initiate("Die.")

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
  internal fun `narrowing an instruction to itself adds no history after selection`() {
    initiate("2 Plant?")
    writer.selectTask("2 Plant?")
    val before = game.timeline.checkpoint()

    writer.narrowTask("2 Plant?")
    tasksAsText().shouldContainExactlyInAnyOrder("2 Plant<Player1>?")
    events.entriesSince(before).shouldBeEmpty()
  }

  @Test
  internal fun `narrowing requires a selected task`() {
    initiate("StandardResource?")

    shouldThrow<TaskException> { writer.narrowTask("Plant?") }

    tasksAsText().shouldContainExactly("StandardResource<Player1>?")
  }

  @Test
  internal fun `an unselected task can be filled in progressively without changing the World`() {
    val id = initiate("3 StandardResource?").single()
    val before = game.timeline.checkpoint()
    val form = writer.fillInTask(id)

    form.narrow("2 StandardResource?")
    form.narrow("Plant!")

    events.entriesSince(before).shouldBeEmpty()
    tasks.selectedTask() shouldBe null
    tasksAsText().shouldContainExactly("3 StandardResource<Player1>?")
    form.instruction.toString() shouldBe "Plant<Player1>!"

    form.commit()
    writer.count("Plant") shouldBe 1
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `a form chooses an OR arm before its optional amount`() {
    val id = initiate("Plant? OR Heat?").single()
    val form = writer.fillInTask(id)
    val before = game.timeline.checkpoint()

    val arm = form.decisions().single()
    arm.kind shouldBe ALTERNATIVE
    form.options(arm).toSet() shouldBe setOf("Plant<Player1>?", "Heat<Player1>?")
    form.choose(arm, "Plant<Player1>?")

    val amount = form.decisions().single()
    amount.kind shouldBe AMOUNT
    form.options(amount).toList() shouldBe listOf("Ok", "1")
    events.entriesSince(before).shouldBeEmpty()
    tasksAsText().shouldContainExactly("Plant<Player1>? OR Heat<Player1>?")
  }

  @Test
  internal fun `broad options retain an OR arm rejected by current resolution`() {
    val id = initiate("-Plant! OR Heat!").single()
    val form = writer.fillInTask(id)

    form.decisions().single().kind shouldBe ALTERNATIVE
    form.options(form.decisions().single()).toList() shouldBe
        listOf("-Plant<Player1>!", "Heat<Player1>!")
    form.choose(form.decisions().single(), "-Plant<Player1>!")
    form.instruction.toString() shouldBe "-Plant<Player1>!"
  }

  @Test
  internal fun `a form lists concrete targets and bounded shared X amounts`() {
    val targetId = initiate("StandardResource?").single()
    val target = writer.fillInTask(targetId)
    val decision = target.decisions().single()
    decision.kind shouldBe TARGET
    target.options(decision).first() shouldBe "Ok"
    target.choose(decision, "Plant")
    target.decisions().single().kind shouldBe AMOUNT

    writer.sneak("2 Plant")
    val xId = initiate("-X Plant! THEN X Heat!").single()
    val x = writer.fillInTask(xId)
    val amount = x.decisions().single()
    amount.kind shouldBe AMOUNT
    x.options(amount).toList() shouldBe listOf("1", "2")
  }

  @Test
  internal fun `a form chooses one type reason at a time without combining branches and dependencies`() {
    val fixture =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Site {
                  ABSTRACT CLASS Region {
                    CLASS One { HAS MAX 1 This }
                    CLASS Two { HAS MAX 1 This }
                  }
                }
                ABSTRACT CLASS Shade {
                  CLASS Red { HAS MAX 1 This }
                  CLASS Blue { HAS MAX 1 This }
                }
                ABSTRACT CLASS Choice {
                  CLASS Plain { HAS MAX 1 This }
                  ABSTRACT CLASS Routed {
                    CLASS Tagged<Site, Shade> { HAS MAX 1 This }
                  }
                }
                """
                    .trimIndent(),
                players = 0,
            )
        )
    val agent = fixture.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    agent.runOperation("One, Two, Red, Blue")
    val form = agent.fillInTask(agent.addTasks("Choice.").single())

    val root = form.decisions().single()
    root.kind shouldBe TARGET
    root.focus.toString() shouldBe "Choice"
    form.options(root).toSet() shouldBe setOf("Plain", "Routed")
    form.choose(root, "Routed")

    val branch = form.decisions().single()
    branch.focus.toString() shouldBe "Routed"
    form.options(branch).toList() shouldBe listOf("Tagged")
    form.choose(branch, "Tagged")

    val dependencyRoot = form.decisions().single()
    dependencyRoot.focus.toString() shouldBe "Site"
    form.options(dependencyRoot).toSet() shouldBe setOf("One", "Two")
    form.choose(dependencyRoot, "One")

    val shade = form.decisions().single()
    shade.focus.toString() shouldBe "Shade"
    form.options(shade).toSet() shouldBe setOf("Red", "Blue")
    form.choose(shade, "Red")
    form.decisions().shouldBeEmpty()
  }

  @Test
  internal fun `dependency options use existing holders belonging to the chosen player`() {
    val fixture =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Holder<Player> {
                  HAS MAX 1 This
                  ABSTRACT CLASS Storage {
                    CLASS Available
                    CLASS OtherPlayers
                    CLASS Absent
                  }
                }
                CLASS Token<Holder>
                """
                    .trimIndent(),
                players = 2,
            )
        )
    val agent = fixture.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    agent.sneak("Available<Player1>, OtherPlayers<Player2>")
    val form = agent.fillInTask(agent.addTasks("Token<Holder<Player1>>.").single())
    val before = fixture.timeline.checkpoint()
    val holder = form.decisions().single()
    holder.focus.toString() shouldBe "Holder"
    form.options(holder).toList() shouldBe listOf("Available")
    fixture.timeline.checkpoint() shouldBe before
    form.choose(holder, "Available")
    form.decisions().shouldBeEmpty()
    form.commit()
    agent.count("Token<Available<Player1>>") shouldBe 1

    // An optional gain can choose an absent holder and then choose zero, so retain those paths.
    val optional = agent.fillInTask(agent.addTasks("Token<Holder<Player1>>?").single())
    optional.options(optional.decisions().single()).toSet() shouldBe
        setOf("Ok", "Available", "OtherPlayers", "Absent")
    optional.choose(optional.decisions().single(), "Absent")
    optional.options(optional.decisions().single()).toList() shouldBe listOf("Ok")
    optional.choose(optional.decisions().single(), "Ok")
    optional.commit()
    agent.count("Token") shouldBe 1
  }

  @Test
  internal fun `a class literal chooses represented classes without instance dependencies`() {
    val form = writer.fillInTask(initiate("Class<StandardResource>.").single())

    val representedClass = form.decisions().single()
    representedClass.kind shouldBe TARGET
    representedClass.focus.toString() shouldBe "StandardResource"
    form.options(representedClass).toSet() shouldBe setOf("MC", "Metal", "Plant", "Energy", "Heat")
    form.choose(representedClass, "Plant")
    form.decisions().shouldBeEmpty()
  }

  @Test
  internal fun `a form chooses a source before the destination that excludes it`() {
    writer.sneak("Steel")
    val form =
        writer.fillInTask(
            initiate("StandardResource(NOT Source@StandardResource) FROM Source@StandardResource!")
                .single()
        )
    form.choose(form.decisions().single(), "Metal")
    form.choose(form.decisions().single(), "Steel")
    val destination = form.decisions().single()
    form.options(destination).toList().contains("Metal") shouldBe true
    form.choose(destination, "Metal")
    form.options(form.decisions().single()).toList() shouldBe listOf("Titanium")
    form.choose(form.decisions().single(), "Titanium")
    form.commit()
    writer.count("Steel") shouldBe 0
    writer.count("Titanium") shouldBe 1
  }

  @Test
  internal fun `a refinement alone is not a target choice`() {
    val fixture =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Spot { HAS MAX 1 This }
                CLASS Mark<Spot> { HAS MAX 1 This }
                """
                    .trimIndent(),
                players = 0,
            )
        )
    val agent = fixture.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val form = agent.fillInTask(agent.addTasks("Spot(HAS MAX 0 Mark).").single())

    form.decisions().shouldBeEmpty()
  }

  @Test
  internal fun `a form can choose a shared X target before its amount`() {
    writer.sneak("Plant, Heat")
    val id = initiate("-X StandardResource! THEN X MC!").single()
    val form = writer.fillInTask(id)
    val target = form.decisions().single()

    target.kind shouldBe TARGET
    form.choose(target, "Plant")

    form.decisions().single().kind shouldBe AMOUNT
    form.options(form.decisions().single()).toList() shouldBe listOf("1")
  }

  @Test
  internal fun `a PER metric bounds an X choice after scaling`() {
    writer.sneak("2 Heat, 3 Plant")
    val id = initiate("-X Plant! / Heat").single()
    val form = writer.fillInTask(id)

    form.decisions().single().kind shouldBe AMOUNT
    form.options(form.decisions().single()).toList() shouldBe listOf("1")

    writer.sneak("2 Plant")
    val doubled = writer.fillInTask(initiate("-2X Plant! / Heat").single())
    doubled.options(doubled.decisions().single()).toList() shouldBe listOf("1")
  }

  @Test
  internal fun `an uncapped X remains a lazy choice sequence`() {
    val id = initiate("X Plant!").single()
    val form = writer.fillInTask(id)

    form.options(form.decisions().single()).take(3).toList() shouldBe listOf("1", "2", "3")
    form.choose(form.decisions().single(), "1000000")
    form.instruction.toString() shouldBe "1000000 Plant<Player1>!"
  }

  @Test
  internal fun `a form reports optional PER amount choices as unsupported`() {
    writer.sneak("Heat")
    val id = initiate("Plant? / Heat").single()

    shouldThrow<UnsupportedOperationException> { writer.fillInTask(id).decisions() }
  }

  @Test
  internal fun `a form preserves a gate while choosing its optional change`() {
    val id = initiate("MAX 0 Heat: Plant?").single()
    val form = writer.fillInTask(id)

    form.decisions().single().kind shouldBe AMOUNT
    val options = form.options(form.decisions().single()).toList()
    options shouldBe listOf("Ok", "1")
  }

  @Test
  internal fun `a form can choose a transmutation source before its optional count`() {
    writer.sneak("Plant, Heat")
    val id = initiate("3 Plant FROM StandardResource?").single()
    val form = writer.fillInTask(id)

    form.decisions().single().kind shouldBe TARGET
    form.choose(form.decisions().single(), "Heat")

    form.decisions().single().kind shouldBe AMOUNT
    form.options(form.decisions().single()).toList() shouldBe listOf("Ok", "1")
  }

  @Test
  internal fun `an AMAP form keeps a zero-capacity target beside a useful one`() {
    val fixture =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Choice {
                  CLASS Full { HAS MAX 1 This }
                  CLASS Open { HAS MAX 1 This }
                }
                """
                    .trimIndent(),
                players = 0,
            )
        )
    val agent = fixture.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    agent.runOperation("Full")
    val id = agent.addTasks("Choice.").single()
    val form = agent.fillInTask(id)

    form.decisions().single().kind shouldBe TARGET
    form.options(form.decisions().single()).toSet() shouldBe setOf("Full", "Open")
    form.choose(form.decisions().single(), "Full")
    form.commit()
    agent.count("Full") shouldBe 1
    agent.count("Open") shouldBe 0
  }

  @Test
  internal fun `a form cannot change an earlier voluntary choice`() {
    val id = initiate("StandardResource?").single()
    val form = writer.fillInTask(id)
    form.narrow("Steel?")

    shouldThrow<NarrowingException> { form.narrow("Plant!") }

    form.instruction.toString() shouldBe "Steel<Player1>?"
    tasksAsText().shouldContainExactly("StandardResource<Player1>?")
  }

  @Test
  internal fun `committing a partial form selects the task and leaves it pending`() {
    val id = initiate("StandardResource?").single()
    val form = writer.fillInTask(id)
    form.narrow("Steel?")

    form.commit()

    tasks.selectedTask() shouldBe id
    tasksAsText().shouldContainExactly("Steel<Player1>?")
    writer.count("Steel") shouldBe 0
  }

  @Test
  internal fun `a form cannot be used after its task disappears`() {
    val id = initiate("StandardResource?").single()
    val form = writer.fillInTask(id)
    form.narrow("Steel?")

    writer.dropTask(id)

    shouldThrow<TaskException> { form.commit() }
  }

  @Test
  internal fun `a selected task can keep a form private until it is committed`() {
    val id = initiate("StandardResource?").single()
    writer.selectTask(id)
    val before = game.timeline.checkpoint()
    val form = writer.fillInTask(id)

    form.narrow("Steel!")

    events.entriesSince(before).shouldBeEmpty()
    tasksAsText().shouldContainExactly("StandardResource<Player1>?")
    form.commit()
    writer.count("Steel") shouldBe 1
  }

  @Test
  internal fun `a form of a THEN first stage retains its continuation`() {
    val id = initiate("Chosen@StandardResource THEN Chosen@StandardResource").single()
    val before = tasks.getTaskData(id).instruction
    val form = writer.fillInTask(id)
    form.choose(form.decisions().single(), "Metal")
    form.instruction.toString() shouldBe "Metal<Player1>! THEN Metal<Player1>!"
    form.choose(form.decisions().single(), "Steel")
    form.instruction.toString() shouldBe "Steel<Player1>! THEN Steel<Player1>!"

    tasks.getTaskData(id).instruction shouldBe before
    form.commit()

    writer.count("Steel") shouldBe 1
    writer.doTask("Steel")
    writer.count("Steel") shouldBe 2
  }

  @Test
  internal fun `independent forms of one task do not share choices`() {
    val id = initiate("StandardResource?").single()
    val steel = writer.fillInTask(id)
    val plant = writer.fillInTask(id)

    steel.narrow("Steel?")
    plant.narrow("Plant?")

    steel.instruction.toString() shouldBe "Steel<Player1>?"
    plant.instruction.toString() shouldBe "Plant<Player1>?"
    tasksAsText().shouldContainExactly("StandardResource<Player1>?")
  }

  @Test
  internal fun `a competing form is rechecked when another form commits`() {
    val id = initiate("StandardResource?").single()
    val steel = writer.fillInTask(id)
    val plant = writer.fillInTask(id)
    steel.narrow("Steel?")
    plant.narrow("Plant?")

    steel.commit()

    shouldThrow<NarrowingException> { plant.commit() }
    tasksAsText().shouldContainExactly("Steel<Player1>?")
    writer.count("Plant") shouldBe 0
  }

  @Test
  internal fun `id-based narrowing of a selected task executes when concrete`() {
    val taskId = initiate("2 Plant?").single()
    writer.selectTask(taskId)

    writer.narrowTask("Plant!")

    writer.count("Plant") shouldBe 1
    tasks.isEmpty() shouldBe true
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
    history().shouldHaveSize(2)
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

    selectAndNarrow(
        "5 Plant OR (4 StandardResource, 2 StandardResource)",
        "4 Heat, 2 Energy",
    )

    tasksAsText().shouldContainExactlyInAnyOrder("4 Heat<Player1>!", "2 Energy<Player1>!")
  }

  @Test
  internal fun `narrowing to the first stage executes it and admits its THEN continuation`() {
    writer.runOperation("ProjectCard")
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
    initiate("10 TerraformRating: Plant")

    shouldThrow<dev.martianzoo.pets.api.Exceptions.RequirementException> {
      writer.selectTask("10 TerraformRating: Plant")
    }

    tasksAsText().shouldContainExactly("10 TerraformRating<Player1>: Plant<Player1>!")
  }

  @Test
  internal fun `resolution that produces siblings completes the selected structural task`() {
    writer.runOperation("Plant")
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
        TaskRemovedEvent::class,
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
  internal fun `ocean target refinement rejects an occupied area`() {
    writer.autoExecPolicy = AutoExecPolicy.EAGER
    writer.runOperation("OceanTile<Tharsis_1_2>")
    writer.autoExecPolicy = NONE
    initiate("OceanTile<>")

    writer.selectTask("OceanTile<>")
    shouldThrow<NarrowingException> { writer.narrowTask("OceanTile<Tharsis_1_2>") }
    writer.narrowTask("OceanTile<Tharsis_1_4>")

    tasks.selectedTask() shouldBe null
    writer.count("OceanTile<Tharsis_1_4>") shouldBe 1
  }

  @Test
  internal fun `selecting a task resolves a city already identified by area`() {
    val game = Engine.newGame(canonicalPremise(cn("PromoCardPack")))
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    p1.autoExecPolicy = NONE
    p2.runOperation("CityTile<Player2, Tharsis_4_2>")
    p1.addTasks("Cathedral<CityTile<Anyone, Tharsis_4_2>>")

    p1.selectTask("Cathedral<CityTile<Anyone, Tharsis_4_2>>")

    p1.count("Cathedral<Player1, NormalCityTile<Player2, Tharsis_4_2>>") shouldBe 1
  }

  @Test
  internal fun `AMAP saturates only the selected target`() {
    game.testAgent(PLAYER2).runOperation("3 MC")
    initiate("-5 MC<Player>.")

    selectAndNarrow("-5 MC<Player>.", "-5 MC<Player1>.")

    tasks.isEmpty() shouldBe true
    writer.count("MC") shouldBe 0
    game.testAgent(PLAYER2).count("MC") shouldBe 3
  }

  @Test
  internal fun `AMAP gain may select a maxed target while another target has capacity`() {
    writer.autoExecPolicy = AutoExecPolicy.EAGER
    writer.runOperation("14 OxygenStep!")
    writer.autoExecPolicy = NONE
    initiate("GlobalParameter.")

    selectAndNarrow("GlobalParameter.", "OxygenStep.")

    tasks.isEmpty() shouldBe true
    writer.count("OxygenStep") shouldBe 14
    writer.count("TemperatureStep") shouldBe 1
  }

  @Test
  internal fun `an omitted selection quantifier preserves a stronger pending quantifier`() {
    initiate("OceanTile<LandArea>!")

    shouldThrow<TaskException> { writer.doTask("OceanTile<Tharsis_2_3>.") }
    writer.doTask("OceanTile<Tharsis_2_3>")

    writer.count("OceanTile<Tharsis_2_3>") shouldBe 1
    tasks.matching { "OceanTile" in it.instruction.toString() }.none() shouldBe true
  }

  @Test
  internal fun `an omitted selection quantifier inherits from the selected OR arm`() {
    initiate("OceanTile<LandArea>! OR Plant!")

    writer.doTask("OceanTile<Tharsis_2_3>")

    writer.count("OceanTile<Tharsis_2_3>") shouldBe 1
    tasks.matching { "OceanTile" in it.instruction.toString() }.none() shouldBe true
  }

  @Test
  internal fun `selection resolves PER before its AMAP target is narrowed`() {
    writer.runOperation("Plant")
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
    writer.runOperation("3 Heat")
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
  internal fun `a later stage can exclude the type selected by the first stage`() {
    writer.runOperation("Plant, Heat")
    initiate("Selected@StandardResource THEN -StandardResource(NOT Selected@StandardResource)")

    writer.doTask("Steel")

    tasksAsText().shouldContainExactly("-StandardResource<Player1>(NOT Steel<Player1>)!")
    writer.doTask("-Plant")
    writer.count("Steel") shouldBe 1
    writer.count("Plant") shouldBe 0
    writer.count("Heat") shouldBe 1
  }

  @Test
  internal fun `a first-stage gate waits for its shared choice`() {
    writer.runOperation("Plant")
    initiate(
        "(Selected@StandardResource: Selected@StandardResource) THEN Selected@StandardResource"
    )

    shouldThrow<TaskException> { writer.doTask("Heat") }
    writer.doTask("Plant")

    writer.count("Plant") shouldBe 2
    tasksAsText().shouldContainExactly("Plant<Player1>!")
  }

  @Test
  internal fun `a transmutation can exclude its selected source from the destination`() {
    writer.runOperation("Plant")
    initiate("StandardResource(NOT Source@StandardResource) FROM Source@StandardResource")

    writer.doTask("Steel FROM Plant")

    writer.count("Steel") shouldBe 1
    writer.count("Plant") shouldBe 0
  }

  @Test
  internal fun `narrowing a variable-sharing THEN to a concrete sequence splits its first stage`() {
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
    game.testAgent(PLAYER2).runOperation("3 MC")
    initiate("3 MC FROM MC<@Player>. THEN Plant<@Player>")

    writer.autoExecNow()

    tasksAsText().shouldContainExactly("3 MC<Player1> FROM MC<Player>. THEN Plant<Player>!")
    game.testAgent(PLAYER2).count("MC") shouldBe 3
  }

  @Test
  internal fun `autoexec does not infer an abstract AMAP actor from the sole existing component`() {
    game.testAgent(PLAYER2).runOperation("3 MC")
    initiate("3 MC FROM MC<Player>.")

    writer.autoExecNow()

    tasksAsText().shouldContainExactly("3 MC<Player1> FROM MC<Player>.")
    game.testAgent(PLAYER2).count("MC") shouldBe 3
  }

  @Test
  internal fun `selecting a zero-count AMAP actor after autoexec still binds the continuation`() {
    writer.runOperation("3 MC")
    initiate("3 MC FROM MC<@Player>. THEN Plant<@Player>")
    writer.autoExecNow()
    writer.autoExecPolicy = NONE

    writer.doTask("3 MC FROM MC<Player2>.")

    tasksAsText().shouldContainExactly("Plant<Player2>!")
    writer.count("MC") shouldBe 3
    game.testAgent(PLAYER2).count("Plant") shouldBe 0
  }

  @Test
  internal fun `selecting an AMAP source binds the later stage before resolution`() {
    game.testAgent(PLAYER2).runOperation("3 MC")
    writer.autoExecPolicy = NONE
    initiate("3 MC FROM MC<@Player>. THEN Plant<@Player>")

    writer.doTask("3 MC FROM MC<Player2>.")

    tasksAsText().shouldContainExactly("Plant<Player2>!")
    writer.count("MC") shouldBe 3
    game.testAgent(PLAYER2).count("MC") shouldBe 0
  }

  @Test
  internal fun `selecting a gated mandatory source binds the later stage before resolution`() {
    game.testAgent(PLAYER2).runOperation("Plant, 3 MC")
    initiate("(Plant<@Player>: 3 MC FROM MC<@Player>) THEN Heat<@Player>")

    writer.doTask("3 MC FROM MC<Player2>")

    tasksAsText().shouldContainExactly("Heat<Player2>!")
    writer.count("MC") shouldBe 3
    game.testAgent(PLAYER2).count("MC") shouldBe 0
  }

  @Test
  internal fun `a gated source must satisfy the gate for the selected player`() {
    writer.runOperation("3 MC")
    game.testAgent(PLAYER2).runOperation("Plant")
    initiate("(Plant<@Player>: 3 MC<Player2> FROM MC<@Player>) THEN Heat<@Player>")

    shouldThrow<TaskException> { writer.doTask("3 MC<Player2> FROM MC<Player1>") }

    writer.count("MC") shouldBe 3
    tasks.extract { it }.shouldHaveSize(1)
  }

  @Test
  internal fun `Partial narrowing cannot discard a shared observing refinement`() {
    writer.runOperation("Heat")
    val id =
        initiate("MC<Player(HAS Selected@StandardResource)> THEN Selected@StandardResource")
            .single()
    writer.selectTask(id)

    shouldThrow<NarrowingException> {
      writer.narrowTask("MC<Player1> THEN StandardResource")
    }
    shouldThrow<NarrowingException> { writer.narrowTask("MC<Player1>") }
    shouldThrow<NarrowingException> { writer.narrowTask("MC<Player1> THEN Steel") }
    writer.narrowTask("MC<Player1> THEN Heat")
    writer.doTask("Heat")

    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 2
    writer.count("Steel") shouldBe 0
  }

  @Test
  internal fun `Selecting a THEN head through OR cannot discard its shared observer`() {
    writer.runOperation("Heat")
    val id =
        initiate(
                "(MC<Player(HAS Selected@StandardResource)> THEN Selected@StandardResource) OR Energy"
            )
            .single()
    writer.selectTask(id)

    shouldThrow<NarrowingException> { writer.narrowTask("MC<Player1>") }
    writer.narrowTask("MC<Player1> THEN Heat")
    writer.doTask("Heat")

    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 2
    writer.count("Energy") shouldBe 0
  }

  @Test
  internal fun `Resolving the same predicate in another scope preserves its shared choice`() {
    writer.runOperation("Heat")
    val first =
        initiate("MC<Player(HAS Selected@StandardResource)> THEN Selected@StandardResource")
            .single()
    writer.selectTask(first)
    writer.narrowTask("MC<Player1> THEN Heat")
    writer.doTask("Heat")

    val second =
        initiate("MC<Player(HAS Selected@StandardResource)> THEN Selected@StandardResource")
            .single()
    writer.selectTask(second)
    shouldThrow<NarrowingException> { writer.narrowTask("MC<Player1> THEN Steel") }
    writer.narrowTask("MC<Player1> THEN Heat")
    writer.doTask("Heat")

    writer.count("MC") shouldBe 2
    writer.count("Heat") shouldBe 3
    writer.count("Steel") shouldBe 0
  }

  @Test
  internal fun `Declining a supplier still works after an earlier NoOp is normalized away`() {
    writer.runOperation("Heat")
    val id =
        initiate(
                "Die? THEN MC<Player(HAS Selected@StandardResource)> THEN Selected@StandardResource?"
            )
            .single()
    writer.selectTask(id)
    writer.narrowTask("MC<Player1> THEN Ok")

    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 1
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `An outer gate preserves the shared choice when selecting its sequence`() {
    writer.runOperation("Heat")
    val id =
        initiate("Heat: (MC<Player(HAS Selected@StandardResource)> THEN Selected@StandardResource)")
            .single()
    writer.selectTask(id)
    shouldThrow<NarrowingException> { writer.narrowTask("MC<Player1> THEN Steel") }
    writer.narrowTask("MC<Player1> THEN Heat")
    writer.doTask("Heat")

    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 2
    writer.count("Steel") shouldBe 0
  }

  @Test
  internal fun `An unselected gated task cannot erase its unresolved shared choice`() {
    writer.runOperation("Heat")
    val id =
        initiate("Heat: (MC<Player(HAS Selected@StandardResource)> THEN Selected@StandardResource)")
            .single()

    shouldThrow<TaskException> {
      writer.doTask("Heat: (MC<Player1> THEN StandardResource)")
    }
    writer.count("MC") shouldBe 0
    writer.count("Heat") shouldBe 1
    tasks.extract { it }.shouldHaveSize(1)

    writer.selectTask(id)
    writer.narrowTask("MC<Player1> THEN Heat")
    writer.doTask("Heat")
    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 2
  }

  @Test
  internal fun `An unrelated first stage runs while later shared choices stay correlated`() {
    val id = initiate("Heat THEN Chosen@StandardResource THEN Chosen@StandardResource").single()
    writer.selectTask(id)
    writer.count("Heat") shouldBe 1

    writer.selectTask(tasks.ids().single())
    shouldThrow<NarrowingException> { writer.narrowTask("Steel THEN Plant") }
    writer.narrowTask("Steel THEN Steel")
    writer.doTask("Steel")
    writer.count("Steel") shouldBe 2
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `A concrete shared choice can retain an abstract existence query in a later gate`() {
    writer.runOperation("Heat")
    val id = initiate("MC<Chosen@Player> THEN (StandardResource: Heat<Chosen@Player>)").single()
    writer.selectTask(id)
    writer.narrowTask("MC<Player1> THEN (StandardResource: Heat<Player1>)")
    writer.doTask("Heat")

    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 2
  }

  @Test
  internal fun `Binding an outer variable cannot discard an inner shared predicate`() {
    writer.runOperation("Heat")
    val id =
        initiate(
                "MC<Chosen@Player(HAS Selected@StandardResource)> " +
                    "THEN Selected@StandardResource THEN Heat<Chosen@Player>"
            )
            .single()
    writer.selectTask(id)
    shouldThrow<NarrowingException> {
      writer.narrowTask("MC<Player1> THEN Steel THEN Heat<Player1>")
    }
    writer.narrowTask("MC<Player1> THEN Heat THEN Heat<Player1>")
    writer.doTask("Heat")
    writer.doTask("Heat")

    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 3
    writer.count("Steel") shouldBe 0
  }

  @Test
  internal fun `A selected shared owner leaves an independent resource choice open`() {
    val id = initiate("Heat<Chosen@Player> THEN StandardResource<Chosen@Player>").single()
    writer.selectTask(id)
    writer.narrowTask("Heat<Player1> THEN StandardResource<Player1>")
    writer.count("Heat") shouldBe 1
    writer.doTask("Steel<Player1>")
    writer.count("Steel") shouldBe 1
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `A later OR supplier must satisfy its earlier observer`() {
    writer.runOperation("Heat")
    val id =
        initiate(
                "MC<Player(HAS Selected@StandardResource)> THEN (Selected@StandardResource OR 2 Selected@StandardResource)"
            )
            .single()
    writer.selectTask(id)
    shouldThrow<NarrowingException> { writer.narrowTask("MC<Player1> THEN Steel") }
    writer.narrowTask("MC<Player1> THEN Heat")
    writer.doTask("Heat")
    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 2
    writer.count("Steel") shouldBe 0
  }

  @Test
  internal fun `An unmarked abstract placement cannot erase its current HAS condition`() {
    val fixture =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Place {
                  CLASS First { HAS MAX 1 This }
                  CLASS Second { HAS MAX 1 This }
                }
                CLASS Marker<Place>
                CLASS Tile<Place>
                """
                    .trimIndent(),
                players = 0,
            )
        )
    val agent = fixture.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    agent.runOperation("First, Second, Marker<First>")
    val id = agent.addTasks("Tile<Place(HAS Marker)>").single()
    agent.selectTask(id)
    shouldThrow<NarrowingException> { agent.narrowTask("Tile<Place>") }
    shouldThrow<NarrowingException> { agent.narrowTask("Tile<Second>") }
    agent.narrowTask("Tile<First>")
    agent.count("Tile<First>") shouldBe 1
    agent.count("Tile<Second>") shouldBe 0
  }

  @Test
  internal fun `Conflicting repeated trigger captures are a non-match`() {
    val fixture =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Place {
                  CLASS First { HAS MAX 1 This }
                  CLASS Second { HAS MAX 1 This }
                }
                CLASS Pair<Place, Place>
                CLASS Notice<Place>
                CLASS Watcher { Pair<Chosen@Place, Chosen@Place>: Notice<Chosen@Place> }
                """
                    .trimIndent(),
                players = 0,
            )
        )
    val agent = fixture.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    agent.runOperation("First, Second, Watcher")
    agent.runOperation("Pair<First, Second>")
    agent.count("Notice") shouldBe 0
    agent.runOperation("Pair<First, First>") { doTask("Notice<First>") }
    agent.count("Notice<First>") shouldBe 1
    agent.count("Notice<Second>") shouldBe 0
  }

  @Test
  internal fun `A normalized partial proposal can retain an unsettled observer`() {
    writer.runOperation("Heat")
    val id =
        initiate("MC<Player(HAS Selected@StandardResource)>? THEN Selected@StandardResource")
            .single()
    writer.selectTask(id)
    writer.narrowTask("MC<Player(HAS Selected@StandardResource)>! THEN Selected@StandardResource")
    writer.count("MC") shouldBe 0
    shouldThrow<NarrowingException> { writer.narrowTask("MC<Player1> THEN Steel") }
    writer.narrowTask("MC<Player1> THEN Heat")
    writer.doTask("Heat")
    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 2
  }

  @Test
  internal fun `Declining the only supplier leaves an aggregate observer`() {
    writer.runOperation("Heat")
    initiate("Chosen@StandardResource? THEN MC<Player(HAS Chosen@StandardResource)>")
    writer.doTask("Ok")
    writer.doTask("MC<Player1>")
    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 1
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `A first-stage gate can observe a value supplied by a later stage`() {
    writer.runOperation("Heat")
    val id = initiate("(Selected@StandardResource: MC) THEN Selected@StandardResource").single()
    writer.selectTask(id)
    writer.count("MC") shouldBe 0
    writer.narrowTask("(Heat: MC) THEN Heat")
    writer.doTask("Heat")
    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 2
  }

  @Test
  internal fun `A first-stage alias captures a uniquely determined concrete subtype`() {
    val fixture =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Kind {
                  ABSTRACT CLASS Shape { CLASS Only { HAS MAX 1 This } }
                  CLASS Other { HAS MAX 1 This }
                }
                CLASS Notice<Kind>
                """
                    .trimIndent(),
                players = 0,
            )
        )
    val agent = fixture.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    agent.runOperation("Other")
    agent.addTasks("Chosen@Shape THEN Notice<Chosen@Shape>")
    agent.doTask("Shape")
    agent.count("Only") shouldBe 1
    shouldThrow<TaskException> { agent.doTask("Notice<Other>") }
    agent.doTask("Notice<Only>")
    agent.count("Notice<Only>") shouldBe 1
    agent.count("Notice<Other>") shouldBe 0
  }

  @Test
  internal fun `A feasible removal still checks its shared predicate before execution`() {
    writer.runOperation("2 MC, Heat")
    val id =
        initiate("-MC<Player(HAS Selected@StandardResource)> THEN Selected@StandardResource")
            .single()
    writer.selectTask(id)

    shouldThrow<NarrowingException> { writer.narrowTask("-MC<Player1> THEN Steel") }
    writer.count("MC") shouldBe 2
    writer.count("Steel") shouldBe 0

    writer.narrowTask("-MC<Player1> THEN Heat")
    writer.doTask("Heat")
    writer.count("MC") shouldBe 1
    writer.count("Heat") shouldBe 2
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
