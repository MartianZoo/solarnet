package dev.martianzoo.engine

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.AutoExecMode
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
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
  internal fun `a narrowing rejects distinct matching tasks`() {
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
  internal fun `a task id disambiguates distinct matching tasks`() {
    tasks.addTasks("Plant?")
    val general = tasks.addTasks("StandardResource?").single()

    agent.doTask("Plant!", general)

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
  internal fun `a different task id cannot override the selected task`() {
    val plant = tasks.addTasks("Plant?").single()
    val heat = tasks.addTasks("Heat?").single()
    agent.selectTask(heat)

    shouldThrow<TaskException> { agent.doTask("Heat!", plant) }

    agent.count("Heat") shouldBe 0
    game.tasks
        .extract { it.instruction.toString() }
        .shouldContainExactly(
            "Plant<Player1>?",
            "Heat<Player1>?",
        )
  }
}
