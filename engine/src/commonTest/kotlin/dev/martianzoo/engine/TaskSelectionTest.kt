package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Gameplay.TaskLayer
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskSelectionTest {
  private val game = setUpGame()
  private val gameplay = game.gameplay(PLAYER1).also { it.autoExecMode = AutoExecMode.NONE }
  private val tasks = gameplay.godMode() as TaskLayer

  @Test
  internal fun `an omitted task number rejects distinct matching tasks`() {
    tasks.addTasks("Plant?")
    tasks.addTasks("StandardResource?")

    shouldThrow<TaskException> { gameplay.doTask("Plant!") }

    game.tasks
        .extract { it.instruction.toString() }
        .shouldContainExactly(
            "Plant<Player1>?",
            "StandardResource<Player1>?",
        )
  }

  @Test
  internal fun `a task number selects by current queue order`() {
    tasks.addTasks("Plant?")
    tasks.addTasks("StandardResource?")

    gameplay.doTask("Plant!", 2)

    gameplay.count("Plant") shouldBe 1
    game.tasks.extract { it.instruction.toString() }.shouldContainExactly("Plant<Player1>?")
  }

  @Test
  internal fun `identical matching tasks are interchangeable`() {
    tasks.addTasks("3 TemperatureStep")

    gameplay.doTask("TemperatureStep!")

    gameplay.count("TemperatureStep") shouldBe 1
    game.tasks
        .extract { it.instruction.toString() }
        .filter { it.startsWith("TemperatureStep") }
        .shouldContainExactly("TemperatureStep.", "TemperatureStep.")
  }

  @Test
  internal fun `a prepared task wins even when a task number is supplied`() {
    tasks.addTasks("Plant?")
    tasks.addTasks("Heat?")
    gameplay.prepareTask("Heat?")

    gameplay.doTask("Heat!", 1)

    gameplay.count("Heat") shouldBe 1
    game.tasks.extract { it.instruction.toString() }.shouldContainExactly("Plant<Player1>?")
  }
}
