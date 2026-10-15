package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SelectOnlyTaskTest {
  @Test
  internal fun `Selecting the sole task leaves its choice to the caller`() {
    val player = player()
    player.addTasks("Coin OR Marker")
    player.selectOnlyTask()
    player.count("Coin") shouldBe 0
    player.count("Marker") shouldBe 0
    player.narrowTask("Coin")
    player.count("Coin") shouldBe 1
    player.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun `Selecting requires exactly a single task without changing the queue`() {
    val player = player()
    shouldThrow<TaskException> { player.selectOnlyTask() }
    player.addTasks("Coin, Marker")
    val pending = player.tasks.extract { it }
    shouldThrow<TaskException> { player.selectOnlyTask() }
    player.tasks.extract { it } shouldBe pending
    player.count("Coin") shouldBe 0
    player.count("Marker") shouldBe 0
  }

  @Test
  internal fun `Scope selection resolves a concrete task and exposes its continuation`() {
    val player = player()
    player.addTasks("Coin THEN Marker")
    player.completeOperation {
      selectOnlyTask()
      selectOnlyTask()
    }
    player.count("Coin") shouldBe 1
    player.count("Marker") shouldBe 1
    player.tasks.isEmpty() shouldBe true
  }

  private fun player(): Agent =
      Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker")))[PLAYER1].also {
        it.autoExecPolicy = NONE
      }

  @Test
  internal fun `Choosing X also binds the deferred continuation`() {
    val player = player()
    player.addTasks("X Coin THEN X Marker")
    player.completeOperation {
      selectOnlyTask(x = 3)
      selectOnlyTask()
    }
    player.count("Coin") shouldBe 3
    player.count("Marker") shouldBe 3
  }
}
