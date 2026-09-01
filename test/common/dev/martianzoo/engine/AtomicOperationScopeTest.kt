package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AtomicOperationScopeTest {
  @Test
  internal fun nestedOperationsAcrossActorsReportOnlyTheOutermostCompletion() {
    val game = Engine.newGame(testGamePremise(players = 2))
    val player1 = game.agent(PLAYER1)
    val player2 = game.agent(PLAYER2)
    var completions = 0
    game.onAtomicComplete = { completions++ }

    player1.manual("Ok") { player2.manual("Ok") }

    completions shouldBe 1

    player1.manual("Ok")

    completions shouldBe 2
  }

  @Test
  internal fun nestedAgentCallsDoNotStartAutomaticAdvancement() {
    val game = Engine.newGame(testGamePremise(players = 2))
    val player1 = game.agent(PLAYER1).also { it.autoExecMode = NONE }
    val player2 = game.agent(PLAYER2)

    player2.addTasks("Token")
    player1.manual("Ok") { player2.autoExecNow() }

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
    val player = game.agent(PLAYER1).also { it.autoExecMode = NONE }
    var workflowPulses = 0
    game.onAtomicComplete = { if (game.tasks.isEmpty()) workflowPulses++ }

    player.addTasks("Blocker")
    player.manual("CleanupProbe")

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
    val player = game.agent(PLAYER1)
    var workflowPulses = 0
    game.onAtomicComplete = { if (game.tasks.isEmpty()) workflowPulses++ }

    player.manual("FirstCleanup")

    player.count("FirstCleanup") shouldBe 0
    player.count("SecondCleanup") shouldBe 0
    player.count("Done") shouldBe 1
    workflowPulses shouldBe 1
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
    val player = game.agent(PLAYER1).also { it.autoExecMode = NONE }

    shouldThrow<TaskException> { player.manual("CleanupProbe") }

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
    val player = game.agent(PLAYER1)

    shouldThrow<DeadEndException> { player.manual("CleanupProbe") }

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
    val player = game.agent(PLAYER1)

    player.sneak("CleanupProbe")

    player.count("CleanupProbe") shouldBe 0
    game.tasks.isEmpty() shouldBe true
    game.isIdle() shouldBe true
  }

  @Test
  internal fun directAgentMutationsReportAtomicCompletion() {
    val game = Engine.newGame(testGamePremise())
    val agent = game.agent(PLAYER1).also { it.autoExecMode = NONE }
    var completions = 0
    game.onAtomicComplete = { completions++ }

    agent.sneak("Token")
    val taskId = agent.addTasks("-Token?").single()
    val task = agent.tasks.getTaskData(taskId)
    agent.editTask(task.copy(whyPending = "waiting for test choice"))
    agent.dropTask(taskId)

    completions shouldBe 4
    agent.count("Token") shouldBe 1
    agent.tasks.isEmpty() shouldBe true
  }
}
