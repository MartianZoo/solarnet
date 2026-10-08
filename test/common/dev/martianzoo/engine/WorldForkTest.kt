package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldForkTest {
  @Test
  internal fun forkCopiesLiveBehaviorAndThenDivergesIndependently() {
    val source =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Listener { Ping: Pong }
                CLASS Ping
                CLASS Pong
                CLASS Choice
                CLASS ForkOnly
                """
            )
        )
    val sourceAgent = source.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    sourceAgent.runOperation("Listener")
    val pendingChoice = sourceAgent.addTasks("Choice?").single()
    source.events.entriesSince(Checkpoint(0)).first().notes = "captured"
    val sourceChoiceCounts = mutableListOf<Int>()
    val sourceChoiceSubscription =
        source.components.listenToCount(
            sourceAgent.resolve("Choice"),
            source.reader,
            sourceChoiceCounts::add,
        )
    var sourceCompletions = 0
    source.onTransactionComplete = { sourceCompletions++ }

    val fork = Engine.fork(source)
    val forkAgent = fork.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    (fork.classTable === source.classTable) shouldBe true
    fork.events.entriesSince(Checkpoint(0)) shouldBe source.events.entriesSince(Checkpoint(0))
    fork.tasks.getTaskData(pendingChoice) shouldBe source.tasks.getTaskData(pendingChoice)
    fork.recording().positions shouldBe source.recording().positions
    fork.events.entriesSince(Checkpoint(0)).first().notes shouldBe "captured"

    source.events.entriesSince(Checkpoint(0)).first().notes = "source only"
    fork.events.entriesSince(Checkpoint(0)).first().notes shouldBe "captured"

    forkAgent.doTask("Choice")
    forkAgent.runOperation("Ping") { doTask("Pong") }

    forkAgent.count("Choice") shouldBe 1
    forkAgent.count("Pong") shouldBe 1
    sourceAgent.count("Choice") shouldBe 0
    sourceAgent.count("Pong") shouldBe 0
    (pendingChoice in source.tasks) shouldBe true
    sourceCompletions shouldBe 0
    sourceChoiceCounts shouldBe listOf(0)

    sourceAgent.doTask("Choice")
    sourceAgent.runOperation("Ping") { doTask("Pong") }

    fork.events.entriesSince(Checkpoint(0)) shouldBe source.events.entriesSince(Checkpoint(0))
    sourceChoiceCounts shouldBe listOf(0, 1)

    forkAgent.runOperation("ForkOnly")

    forkAgent.count("ForkOnly") shouldBe 1
    sourceAgent.count("ForkOnly") shouldBe 0
    sourceChoiceSubscription.cancel()
  }

  @Test
  internal fun forkCannotRollBackBeforeItsForkPoint() {
    val source = Engine.newGame(testGamePremise("CLASS Token\nCLASS Choice"))
    val sourceAgent = source.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    sourceAgent.runOperation("Token")
    sourceAgent.addTasks("Choice?")

    val fork = Engine.fork(source)
    val forkPoint = fork.timeline.checkpoint()
    val forkAgent = fork.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    shouldThrow<IllegalArgumentException> {
      fork.timeline.rollBack(Checkpoint(forkPoint.ordinal - 1))
    }
    forkAgent.doTask("Choice")
    fork.timeline.rollBack(forkPoint)

    fork.tasks.isEmpty() shouldBe false
    forkAgent.count("Choice") shouldBe 0
  }

  @Test
  internal fun forkRetainsStableOrderForIndependentQueuedEffects() {
    val source =
        Engine.newGame(
            testGamePremise(
                """
                CLASS FirstListener { Ping: First }
                CLASS SecondListener { Ping: Second }
                CLASS Ping
                CLASS First
                CLASS Second
                """
            )
        )
    val sourceAgent = source.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    sourceAgent.runOperation("SecondListener, FirstListener")
    val fork = Engine.fork(source)
    val forkAgent = fork.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    sourceAgent.runOperation("Ping") {
      doTask("First")
      doTask("Second")
    }
    forkAgent.runOperation("Ping") {
      doTask("First")
      doTask("Second")
    }

    fork.events.entriesSince(Checkpoint(0)) shouldBe source.events.entriesSince(Checkpoint(0))
  }

  @Test
  internal fun forkTracksMultipleEffectsThroughRemovalAndRollback() {
    val source =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Listener { Ping: Pong }
                CLASS Ping
                CLASS Pong : Atomized
                """
            )
        )
    val sourceAgent = source.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    sourceAgent.runOperation("2 Listener")
    val fork = Engine.fork(source)
    val forkPoint = fork.timeline.checkpoint()
    val forkAgent = fork.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    forkAgent.runOperation("-2 Listener")
    forkAgent.runOperation("Ping")
    fork.tasks.isEmpty() shouldBe true

    fork.timeline.rollBack(forkPoint)
    forkAgent.runOperation("Ping") {
      doTask("Pong")
      doTask("Pong")
    }

    forkAgent.count("Pong") shouldBe 2
  }

  @Test
  internal fun forkRejectsAnIncompleteGameplayPosition() {
    val source = Engine.newGame(testGamePremise("CLASS Token"))
    source.testAgent(PLAYER1).runOperation("Token")
    val completed = source.timeline.checkpoint()
    source.timeline.rollBack(Checkpoint(completed.ordinal - 1))

    shouldThrow<IllegalStateException> { Engine.fork(source) }
  }
}
