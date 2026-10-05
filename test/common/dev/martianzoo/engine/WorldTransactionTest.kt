package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldTransactionTest {
  @Test
  internal fun nestedOperationsAcrossActorsRecordOnlyTheOutermostResult() {
    val game = Engine.newGame(testGamePremise("CLASS Token : Owned<Player>", players = 2))
    val player1 = game.testAgent(PLAYER1)
    val player2 = game.testAgent(PLAYER2)

    player1.runOperation("Token") { player2.runOperation("Token") }

    val playback = game.recording().open()
    playback.positions.size shouldBe 2
    playback.seek(0)
    playback.world.reader.count(playback.world.reader.resolve(parse("Token<Anyone>"))) shouldBe 0
    playback.seek(1)
    playback.world.reader.count(playback.world.reader.resolve(parse("Token<Player1>"))) shouldBe 1
    playback.world.reader.count(playback.world.reader.resolve(parse("Token<Player2>"))) shouldBe 1

    player1.runOperation("Token")

    game.recording().open().positions.size shouldBe 3
  }

  @Test
  internal fun outerTransactionSettlesEveryAgentsPolicyAfterNestedCalls() {
    val game = Engine.newGame(testGamePremise(players = 2))
    val player1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val player2 = game.testAgent(PLAYER2)

    player2.addTasks("Token")
    player1.runOperation("Ok") { player2.autoExecNow() }

    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun idleCleanupCanCreateMoreWorkBeforeWorkflowAdvances() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS CleanupProbe : Owned<Player>, Temporary { -This: Followup }
                CLASS Followup : Owned<Player>
                CLASS Blocker
                CLASS Advance : Continuation { -This:: Advanced }
                CLASS Advanced
                """
            )
        )
    val player = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    player.addTasks("Blocker")
    player.runOperation("CleanupProbe, Advance")

    player.count("CleanupProbe") shouldBe 1
    player.count("Advanced") shouldBe 0

    player.doTask("Blocker")

    player.count("CleanupProbe") shouldBe 0
    player.count("Followup") shouldBe 0
    game.tasks.isEmpty() shouldBe false
    player.count("Advanced") shouldBe 0

    player.doTask("Followup")

    player.count("Followup") shouldBe 1
    game.tasks.isEmpty() shouldBe true
    player.count("Advanced") shouldBe 1
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

    player.runOperation("FirstCleanup")

    player.count("FirstCleanup") shouldBe 0
    player.count("SecondCleanup") shouldBe 0
    player.count("Done") shouldBe 1
  }

  @Test
  internal fun nestedScopesAndUnscopedSignalFinishFromTheInsideOut() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS GenerationScope : Scope, System { HAS MAX 1 This }
                CLASS PhaseScope<GenerationScope> : TemporaryScope<GenerationScope>, System {
                  -This:: PhaseDone
                }
                CLASS ActionScope<PhaseScope> : TemporaryScope<PhaseScope>, System {
                  -This: FinishAction
                }
                CLASS FinishAction : Signal, System
                CLASS PhaseDone : System
                CLASS Blocker : System
                """
            )
        )
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }

    admin.addTasks("Blocker")
    admin.sneak("GenerationScope")
    admin.sneak("PhaseScope")
    admin.sneak("ActionScope")

    admin.count("Scope") shouldBe 3

    admin.doTask("Blocker")

    admin.count("Scope") shouldBe 2
    admin.count("GenerationScope") shouldBe 1
    admin.count("PhaseScope") shouldBe 1
    admin.count("ActionScope") shouldBe 0
    admin.count("FinishAction") shouldBe 0
    game.tasks.isEmpty() shouldBe false

    admin.doTask("FinishAction")

    admin.count("Scope") shouldBe 1
    admin.count("GenerationScope") shouldBe 1
    admin.count("PhaseScope") shouldBe 0
    admin.count("FinishAction") shouldBe 0
    admin.count("PhaseDone") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun continuationWorkAlsoReceivesIdleCleanup() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS ContinueWork : Continuation { -This:: CleanupProbe }
                CLASS CleanupProbe : Temporary { -This:: Done }
                CLASS Done
                """
            )
        )
    val player = game.testAgent(PLAYER1)

    player.runOperation("ContinueWork")

    player.count("ContinueWork") shouldBe 0
    player.count("CleanupProbe") shouldBe 0
    player.count("Done") shouldBe 1
  }

  @Test
  internal fun completedManualOperationRejectsTaskCreatedByIdleCleanup() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS CleanupProbe : Owned<Player>, Temporary { -This: Followup }
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
  internal fun continuationStartsFollowUpOnlyAfterOperationValidation() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS ContinuePhase : Owned<Player>, Continuation {
                  -This: Followup<Me@>
                }
                CLASS Followup : Owned<Player>
                """
            )
        )
    val player = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    player.runOperation("ContinuePhase")

    player.count("ContinuePhase") shouldBe 0
    player.count("Followup") shouldBe 0
    game.tasks.isEmpty() shouldBe false

    player.doTask("Followup")

    player.count("Followup") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun continuationWaitsForDependentMandatoryCleanup() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS ContinuePhase : Continuation { HAS MAX 1 This }
                CLASS Unfinished<ContinuePhase> : MustCleanUp
                """
            )
        )
    val player = game.testAgent(PLAYER1)

    player.beginOperation("ContinuePhase, Unfinished")

    player.count("ContinuePhase") shouldBe 1
    player.count("Unfinished") shouldBe 1

    player.runOperation("-Unfinished")

    player.count("ContinuePhase") shouldBe 0
    player.count("Unfinished") shouldBe 0
  }

  @Test
  internal fun failingContinuationRollsBackTheOperationThatCreatedIt() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS ContinuePhase : Continuation { -This:: Die! }
                """
            )
        )
    val player = game.testAgent(PLAYER1)

    shouldThrow<DeadEndException> { player.runOperation("ContinuePhase") }

    player.count("ContinuePhase") shouldBe 0
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
  internal fun directAgentMutationsRecordSeparatePositions() {
    val game = Engine.newGame(testGamePremise())
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    agent.sneak("Token")
    val positionsBefore = game.recording().open().positions.size

    val taskId = agent.addTasks("-Token?").single()
    agent.selectTask(taskId)
    agent.narrowTask("-Token")
    agent.dropTask(agent.addTasks("Token?").single())

    game.recording().open().positions.size shouldBe positionsBefore + 5
    agent.count("Token") shouldBe 0
    agent.tasks.isEmpty() shouldBe true
  }
}
