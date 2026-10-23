package dev.martianzoo.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameEvent.TaskAddedEvent
import dev.martianzoo.state.GameEvent.TaskEditedEvent
import dev.martianzoo.state.GameEvent.TaskRemovedEvent
import dev.martianzoo.testsupport.PLAYER1
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
  private val start = game.events.nextOrdinal
  private val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = AutoExecPolicy.NONE }

  @Test
  internal fun `selecting resolves an abstract task and records the selection`() {
    initiate("2? Plant")
    agent.selectTask("2? Plant")

    val task = tasks.extract { it }.single()
    task.selected shouldBe true
    "${task.instruction}" shouldBe "2? Plant<Player1>"
    assertHistoryTypes(
        TaskAddedEvent::class,
        TaskEditedEvent::class,
    )
  }

  @Test
  internal fun `selection that resolves to zero completes the task`() {
    initiate("-2? Plant")
    agent.selectTask("-2? Plant")

    tasks.isEmpty() shouldBe true
    assertHistoryTypes(
        TaskAddedEvent::class,
        TaskEditedEvent::class,
        TaskRemovedEvent::class,
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
    )
    agent.count("Plant") shouldBe 1
  }

  @Test
  internal fun `selection resolves limits before narrowing`() {
    initiate("-30? TerraformRating")
    agent.selectTask("-30? TerraformRating")
    tasksAsText().shouldContainExactlyInAnyOrder("-20? TerraformRating<Player1>")
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
  internal fun `a selected task excludes a second selection`() {
    val plant = initiate("Plant?").single()
    val heat = initiate("Heat?").single()

    agent.selectTask(plant)

    shouldThrow<TaskException> { agent.selectTask(heat) }
    tasks.selectedTask() shouldBe plant
  }

  @Test
  internal fun `selection checks prerequisites but leaves count dead ends available`() {
    initiate("-TerraformRating OR -Plant OR Heat OR Tharsis_5_5!")
    agent.selectTask("-TerraformRating OR -Plant OR Heat OR Tharsis_5_5!")

    tasksAsText()
        .shouldContainExactlyInAnyOrder(
            "-TerraformRating<Player1>! OR Heat<Player1>! OR Tharsis_5_5!"
        )
    val before = game.events.nextOrdinal
    shouldThrow<LimitsException> { agent.doTask("Tharsis_5_5!") }
    game.events.nextOrdinal shouldBe before
    agent.doTask("Heat!")
    agent.count("Heat") shouldBe 1
  }

  @Test
  internal fun `doing a gated action names the action without its gate`() {
    initiate("MAX 0 Heat: StandardResource")

    agent.doTask("Plant")

    agent.count("Plant") shouldBe 1
    tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `doing a gated action still checks its gate`() {
    agent.sneak("Heat")
    initiate("MAX 0 Heat: Plant")
    val before = game.events.nextOrdinal

    shouldThrow<RequirementException> { agent.doTask("Plant") }

    agent.count("Plant") shouldBe 0
    game.events.nextOrdinal shouldBe before
  }

  @Test
  internal fun `an unavailable gated task does not hide an ordinary task`() {
    agent.sneak("Heat")
    val unavailable = initiate("MAX 0 Heat: Plant").single()
    initiate("Plant")

    agent.doTask("Plant")

    agent.count("Plant") shouldBe 1
    tasks.ids().shouldContainExactly(unavailable)
  }

  @Test
  internal fun `an available gated task takes precedence over an unavailable gated task`() {
    agent.sneak("Heat")
    val unavailable = initiate("MAX 0 Heat: Plant").single()
    initiate("Heat: Plant")

    agent.doTask("Plant")

    agent.count("Plant") shouldBe 1
    tasks.ids().shouldContainExactly(unavailable)
  }

  @Test
  internal fun `doing a scaled action names the calculated amount without its metric`() {
    agent.sneak("2 Heat")
    initiate("Plant / Heat")

    agent.doTask("2 Plant")

    agent.count("Plant") shouldBe 2
    tasks.isEmpty() shouldBe true
  }

  private fun initiate(ins: String) = (agent as Agent).addTasks(ins)

  private fun history() = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) =
      history().map { it::class }.shouldContainExactly(*c)

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
