package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldTransactionTest {
  @Test
  internal fun nestedOperationsAcrossActorsReportOnlyTheOutermostCompletion() {
    val game = Engine.newGame(testGamePremise(players = 2))
    val player1 = game.testAgent(PLAYER1)
    val player2 = game.testAgent(PLAYER2)
    var completions = 0
    game.onTransactionComplete = { completions++ }

    player1.runOperation("Ok") { player2.runOperation("Ok") }

    completions shouldBe 1

    player1.runOperation("Ok")

    completions shouldBe 2
  }

  @Test
  internal fun nestedAgentCallsDoNotStartAutomaticAdvancement() {
    val game = Engine.newGame(testGamePremise(players = 2))
    val player1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val player2 = game.testAgent(PLAYER2)

    player2.addTasks("Token")
    player1.runOperation("Ok") { player2.autoExecNow() }

    game.tasks.isEmpty() shouldBe false
    player2.autoExecNow()
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun idleCleanupCanCreateMoreWorkBeforeWorkflowAdvances() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS CleanupProbe : Owned<Player>, Temporary { -This: Followup<Owner> }
                CLASS Followup : Owned<Player>
                CLASS Blocker
                """
            )
        )
    val player = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    var workflowPulses = 0
    game.onTransactionComplete = { if (game.tasks.isEmpty()) workflowPulses++ }

    player.addTasks("Blocker")
    player.runOperation("CleanupProbe")

    player.count("CleanupProbe") shouldBe 1
    workflowPulses shouldBe 0

    player.doTask("Blocker")

    player.count("CleanupProbe") shouldBe 0
    player.count("Followup") shouldBe 0
    game.tasks.isEmpty() shouldBe false
    workflowPulses shouldBe 0

    player.doTask("Followup")

    player.count("Followup") shouldBe 1
    game.tasks.isEmpty() shouldBe true
    workflowPulses shouldBe 1
  }

  @Test
  internal fun idleCleanupRepeatsUntilNothingRemainsToRemove() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS FirstCleanup : Temporary { -This:: SecondCleanup }
                CLASS SecondCleanup : Temporary { -This:: Done }
                CLASS Done
                """
            )
        )
    val player = game.testAgent(PLAYER1)
    var workflowPulses = 0
    game.onTransactionComplete = { if (game.tasks.isEmpty()) workflowPulses++ }

    player.runOperation("FirstCleanup")

    player.count("FirstCleanup") shouldBe 0
    player.count("SecondCleanup") shouldBe 0
    player.count("Done") shouldBe 1
    workflowPulses shouldBe 1
  }

  @Test
  internal fun workStartedByCompletionCallbackAlsoReceivesIdleCleanup() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS CleanupProbe : Temporary { -This:: Done }
                CLASS Done
                """
            )
        )
    val player = game.testAgent(PLAYER1)
    var startFollowUp = true
    game.onTransactionComplete = {
      if (startFollowUp) {
        startFollowUp = false
        player.sneak("CleanupProbe")
      }
    }

    player.runOperation("Ok")

    player.count("CleanupProbe") shouldBe 0
    player.count("Done") shouldBe 1
  }

  @Test
  internal fun completedManualOperationRejectsTaskCreatedByIdleCleanup() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS CleanupProbe : Owned<Player>, Temporary { -This: Followup<Owner> }
                CLASS Followup : Owned<Player>
                """
            )
        )
    val player = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    shouldThrow<TaskException> { player.runOperation("CleanupProbe") }

    player.count("CleanupProbe") shouldBe 0
    player.count("Followup") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun completedManualOperationRejectsMustCleanUpCreatedByIdleCleanup() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS CleanupProbe : Temporary { -This:: Unfinished }
                CLASS Unfinished : MustCleanUp
                """
            )
        )
    val player = game.testAgent(PLAYER1)

    shouldThrow<DeadEndException> { player.runOperation("CleanupProbe") }

    player.count("CleanupProbe") shouldBe 0
    player.count("Unfinished") shouldBe 0
  }

  @Test
  internal fun directMutationPerformsIdleCleanupBeforeCompletion() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS CleanupProbe : Temporary
                """
            )
        )
    val player = game.testAgent(PLAYER1)

    player.sneak("CleanupProbe")

    player.count("CleanupProbe") shouldBe 0
    game.tasks.isEmpty() shouldBe true
    game.isIdle() shouldBe true
  }

  @Test
  internal fun directAgentMutationsReportAtomicCompletion() {
    val game = Engine.newGame(testGamePremise())
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    var completions = 0
    game.onTransactionComplete = { completions++ }

    agent.sneak("Token")
    val taskId = agent.addTasks("-Token?").single()
    agent.narrowTask(taskId, "-Token")
    agent.dropTask(taskId)

    completions shouldBe 4
    agent.count("Token") shouldBe 1
    agent.tasks.isEmpty() shouldBe true
  }
}
