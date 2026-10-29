package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CorrectionTest {
  @Test
  internal fun correctionPreservesPartialChoicesAndDoesNotExecuteSelectedGroups() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        ABSTRACT CLASS Color
        CLASS Red : Color
        CLASS Blue : Color
        CLASS Coin
        CLASS Marker
    """
            )
        )
    val agent = Agents(world) { NONE }[PLAYER1]
    val task = agent.addTasks("(X Color, Coin) OR Marker").single()
    agent.selectTask(task)
    agent.narrowTask("X Color, Coin")
    agent.narrowTask("X Red, Coin")
    val selected = agent.tasks.getTaskData(task)

    agent.exMachina("Marker")

    agent.tasks.getTaskData(task) shouldBe selected
    agent.count("Red") shouldBe 0
    agent.count("Coin") shouldBe 0
    agent.doTask("2 Red, Coin", task)
    agent.count("Red") shouldBe 2
    agent.count("Blue") shouldBe 0
    agent.count("Coin") shouldBe 1
    agent.count("Marker") shouldBe 1
  }

  @Test
  internal fun correctionKeepsTheSelectedCalculatedAmount() {
    val world = Engine.newGame(testGamePremise("CLASS Coin\nCLASS Foo"))
    val agent = Agents(world) { NONE }[PLAYER1]
    agent.exMachina("3 Foo")
    val task = agent.addTasks("Coin / Foo").single()
    agent.selectTask(task)
    val selected = agent.tasks.getTaskData(task)

    agent.exMachina("Foo")

    agent.tasks.getTaskData(task) shouldBe selected
    agent.doTask(task)
    agent.count("Coin") shouldBe 3
    agent.count("Foo") shouldBe 4
  }

  @Test
  internal fun systemCorrectionIncludesImmediateEffectsAndSuppressesQueuedEffects() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        CLASS Switch : System { This :: Marker; This : Coin }
        CLASS Marker
        CLASS Coin
    """
            )
        )
    val agents = Agents(world) { NONE }
    for (actor in listOf(ADMIN, PLAYER1)) {
      val agent = agents[actor]
      val before = world.events.nextOrdinal
      val markers = agent.count("Marker")

      val result = agent.exMachina("Switch")

      agent.count("Switch") shouldBe 1
      agent.count("Marker") shouldBe markers + 1
      agent.count("Coin") shouldBe 0
      world.tasks.isEmpty() shouldBe true
      result.changes shouldBe world.events.changesSince(before)
      agent.exMachina("-Switch")
      agent.count("Switch") shouldBe 0
    }
  }

  @Test
  internal fun newUnfinishedWorkRollsBackTheEntireCorrectionIncludingAudit() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        CLASS Pending : MustCleanUp
        CLASS Coin
        CLASS StartsWork { This :: Pending }
    """
            )
        )
    val agents = Agents(world) { NONE }
    val agent = agents[PLAYER1]
    agents.taskLog.start()
    agents.taskLog.gameStarted()
    val before = world.events.nextOrdinal
    val inputs = agents.taskLog.inputs()

    for (change in listOf("Coin, Pending", "Coin, StartsWork")) {
      shouldThrow<NotNowException> { agent.exMachina(change) }
      world.events.nextOrdinal shouldBe before
      agents.taskLog.inputs() shouldBe inputs
      agent.count("Coin") shouldBe 0
      agent.count("Pending") shouldBe 0
      agent.count("StartsWork") shouldBe 0
    }
  }

  @Test
  internal fun existingUnfinishedWorkMayBeRemovedButNotReplacedByDifferentWork() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        CLASS Pending : MustCleanUp
        CLASS Different : MustCleanUp
    """
            )
        )
    val agent = Agents(world) { NONE }[PLAYER1]
    agent.beginOperation("Pending")
    val before = world.events.nextOrdinal

    shouldThrow<NotNowException> { agent.exMachina("Different FROM Pending") }
    world.events.nextOrdinal shouldBe before
    agent.count("Pending") shouldBe 1
    agent.exMachina("-Pending")
    agent.count("MustCleanUp") shouldBe 0
  }
}
