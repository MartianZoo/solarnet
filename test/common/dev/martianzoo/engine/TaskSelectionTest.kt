package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskSelectionTest {
  private val game = setUpGame()
  private val agent = game.agent(PLAYER1).also { it.autoExecMode = AutoExecMode.NONE }
  private val tasks = agent as Agent

  @Test
  internal fun `an omitted task number rejects distinct matching tasks`() {
    tasks.addTasks("Plant?")
    tasks.addTasks("StandardResource?")

    shouldThrow<TaskException> { agent.doTask("Plant!") }

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

    agent.doTask("Plant!", 2)

    agent.count("Plant") shouldBe 1
    game.tasks.extract { it.instruction.toString() }.shouldContainExactly("Plant<Player1>?")
  }

  @Test
  internal fun `identical matching tasks are interchangeable`() {
    tasks.addTasks("3 TemperatureStep")

    agent.doTask("TemperatureStep!")

    agent.count("TemperatureStep") shouldBe 1
    game.tasks
        .extract { it.instruction.toString() }
        .filter { it.startsWith("TemperatureStep") }
        .shouldContainExactly("TemperatureStep.", "TemperatureStep.")
  }

  @Test
  internal fun `a selected task wins even when a task number is supplied`() {
    tasks.addTasks("Plant?")
    tasks.addTasks("Heat?")
    agent.selectTask("Heat?")

    agent.doTask("Heat!", 1)

    agent.count("Heat") shouldBe 1
    game.tasks.extract { it.instruction.toString() }.shouldContainExactly("Plant<Player1>?")
  }
}
