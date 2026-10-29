package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

internal class AgentCallCharacterizationTest {
  @Test
  fun failedAdminFollowUpRestoresPlayerRequestAndEarlierAdminWork() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        CLASS Foo { This: Reward BY Admin }
        CLASS Reward { This: Failure BY Admin }
        CLASS Failure { This:: Die! }
        """
            )
        )
    val agent = Agents(world) { NONE }[PLAYER1]
    val task = agent.addTasks("Foo!").single()
    val originalTask = world.tasks.getTaskData(task)
    val checkpoint = world.timeline.checkpoint()
    val events = world.events.nextOrdinal

    shouldThrow<DeadEndException> { agent.doTask(task) }

    agent.count("Foo") shouldBe 0
    agent.count("Reward") shouldBe 0
    agent.count("Failure") shouldBe 0
    world.tasks.extract { it.id } shouldBe listOf(task)
    world.tasks.getTaskData(task) shouldBe originalTask
    world.events.nextOrdinal shouldBe events
    world.timeline.checkpoint() shouldBe checkpoint
    world.timeline.positions.last() shouldBe checkpoint
  }

  @Test
  fun resultIncludesAdminHandlingAndNestedCommandsAcrossActors() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        CLASS Foo { This: Reward BY Admin }
        CLASS Reward
        CLASS Bar
        """,
                players = 2,
            )
        )
    val agents = Agents(world) { NONE }
    val result = agents[PLAYER1].runOperation("Foo!") { agents[PLAYER2].runOperation("Bar!") }

    agents[PLAYER1].count("Foo") shouldBe 1
    agents[PLAYER1].count("Reward") shouldBe 1
    agents[PLAYER1].count("Bar") shouldBe 1
    result.changes.size shouldBe 3
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  fun assertionFailureAfterNestedCallsRestoresGameAndAllowsAnotherCall() {
    val world = Engine.newGame(testGamePremise("CLASS Foo\nCLASS Bar", players = 2))
    val agents = Agents(world) { NONE }
    val checkpoint = world.timeline.checkpoint()
    val events = world.events.nextOrdinal
    val failure = AssertionError("fail after nested calls")

    val caught =
        shouldThrow<AssertionError> {
          agents[PLAYER1].runOperation("Foo!") {
            agents[PLAYER2].runOperation("Bar!")
            throw failure
          }
        }

    caught shouldBeSameInstanceAs failure
    agents[PLAYER1].count("Foo") shouldBe 0
    agents[PLAYER1].count("Bar") shouldBe 0
    world.tasks.isEmpty() shouldBe true
    world.events.nextOrdinal shouldBe events
    world.timeline.checkpoint() shouldBe checkpoint
    world.timeline.positions.last() shouldBe checkpoint

    agents[PLAYER1].runOperation("Foo!")
    agents[PLAYER1].count("Foo") shouldBe 1
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  fun abortRestoresTheOutermostCall() {
    val world = Engine.newGame(testGamePremise("CLASS Foo"))
    val agent = Agents(world)[PLAYER1]
    val checkpoint = world.timeline.checkpoint()

    val result = agent.runOperation("Foo!") { abort() }

    agent.count("Foo") shouldBe 0
    result.changes.isEmpty() shouldBe true
    world.tasks.isEmpty() shouldBe true
    world.timeline.checkpoint() shouldBe checkpoint
  }
}
