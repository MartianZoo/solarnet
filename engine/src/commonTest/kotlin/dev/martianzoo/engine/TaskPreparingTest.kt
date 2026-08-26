package dev.martianzoo.engine

import dev.martianzoo.engine.Gameplay.TaskLayer
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
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

internal class TaskPreparingTest {
  private val game = setUpGame()
  private val tasks = game.tasks
  private val events = game.events
  private val start = game.timeline.checkpoint()
  private val gameplay = game.gameplay(PLAYER1)

  @Test
  internal fun `can prepare an abstract task`() {
    initiate("2 Plant?")
    gameplay.prepareTask("2 Plant?")

    val task = tasks.extract { it }.single()
    task.next shouldBe true
    "${task.instruction}" shouldBe "2 Plant<Player1>?"
    assertHistoryTypes(TaskAddedEvent::class, TaskEditedEvent::class)
  }

  @Test
  internal fun `preparing to NoOp automatically handles the task 1`() {
    initiate("-2 Plant?")
    gameplay.prepareTask("-2 Plant?").also { it shouldBe null }

    tasks.isEmpty() shouldBe true
    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
  }

  @Test
  internal fun `preparing to NoOp automatically handles the task 2`() {
    initiate("Plant / Heat")
    gameplay.prepareTask("Plant / Heat").also { it shouldBe null }

    tasks.isEmpty() shouldBe true
    assertHistoryTypes(TaskAddedEvent::class, TaskRemovedEvent::class)
  }

  @Test
  internal fun `preparing adjusts for limits 1`() {
    initiate("-30 TerraformRating?")
    gameplay.reviseTask("-30 TerraformRating?", "-25 TerraformRating?")
    gameplay.prepareTask("-25 TerraformRating?")
    tasksAsText().shouldContainExactlyInAnyOrder("-20 TerraformRating<Player1>?")
    gameplay.reviseTask("-20 TerraformRating?", "-15 TerraformRating!")
  }

  @Test
  internal fun `preparing adjusts for limits 2`() {
    initiate("-30 TerraformRating.")
    gameplay.prepareTask("-30 TerraformRating.")

    tasksAsText().shouldContainExactlyInAnyOrder("-20 TerraformRating<Player1>!")
  }

  @Test
  internal fun `preparing fails due to limit`() {
    initiate("-Plant!")
    history().shouldHaveSize(1)
    shouldThrow<LimitsException> { gameplay.prepareTask("-Plant!") }

    history().shouldHaveSize(1)
  }

  @Test
  internal fun `preparing then narrowing results in automatic re-preparing`() {
    initiate("PROD[-2 StandardResource]")
    gameplay.prepareTask("PROD[-2 StandardResource]")

    tasksAsText().shouldContainExactlyInAnyOrder("-2 Production<Player1, Class<MC>>!")
    shouldThrow<NarrowingException> {
      gameplay.reviseTask("PROD[-2 MC]", "PROD[-2 Plant]")
    }
    gameplay.reviseTask("PROD[-2 MC]", "PROD[-2 MC]")
  }

  @Test
  internal fun `preparing an OR prunes the options`() {
    initiate("-TR OR -Plant OR Heat OR Tharsis_5_5!")
    gameplay.prepareTask("-TR OR -Plant OR Heat OR Tharsis_5_5!")

    tasksAsText().shouldContainExactlyInAnyOrder("-TerraformRating<Player1>! OR Heat<Player1>!")
  }

  @Test
  internal fun `preparing to NoOp enqueues the THEN instructions`() {
    initiate("Plant / Heat THEN Steel / 2 OxygenStep THEN Heat")
    gameplay.prepareTask("Plant / Heat").also { it shouldBe null }

    tasksAsText().shouldContainExactlyInAnyOrder("Steel<Player1>! / 2 OxygenStep")
  }

  private fun initiate(ins: String) = (gameplay as TaskLayer).addTasks(ins)

  private fun history() = events.entriesSince(start)

  private fun assertHistoryTypes(vararg c: KClass<out GameEvent>) =
      history().map { it::class }.shouldContainExactly(*c)

  private fun tasksAsText() = tasks.extract { "${it.instruction}" }
}
