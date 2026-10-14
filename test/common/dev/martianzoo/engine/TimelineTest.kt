package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TimelineTest {
  @Test
  internal fun selectionAndExecutionAreSeparateCompletedPositions() {
    val game = Engine.newGame(testGamePremise("CLASS ChoiceA\nCLASS ChoiceB"))
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val task = agent.addTasks("ChoiceA OR ChoiceB").single()
    val offered = game.timeline.checkpoint()

    agent.selectTask(task)
    val selected = game.timeline.checkpoint()
    game.tasks.getTaskData(task).selected shouldBe true

    agent.doTask("ChoiceA")
    val done = game.timeline.checkpoint()
    agent.count("ChoiceA") shouldBe 1

    game.timeline.seek(offered)
    game.tasks.getTaskData(task).selected shouldBe false
    shouldThrow<IllegalStateException> { game.actorEngine(PLAYER1).canSelectTask(task) }
    shouldThrow<IllegalStateException> { game.actorEngine(PLAYER1).canExecuteTask(task) }
    game.timeline.seek(selected)
    game.tasks.getTaskData(task).selected shouldBe true
    game.timeline.seek(done)
    game.tasks.isEmpty() shouldBe true
    agent.count("ChoiceA") shouldBe 1
  }

  @Test
  internal fun seekingReplaysCompletedPositionsAndRequiresTruncationBeforeNewPlay() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Listener { Ping: Pong }
                CLASS Ping
                CLASS Pong
                CLASS Choice
                """
            )
        )
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val start = game.timeline.checkpoint()
    agent.runOperation("Listener")
    val listener = game.timeline.checkpoint()
    agent.runOperation("Ping") { doTask("Pong") }
    val end = game.timeline.checkpoint()
    val completeEvents = game.events.entriesSince(start)

    game.timeline.positions shouldBe listOf(start, listener, end)
    shouldThrow<IllegalArgumentException> {
      game.timeline.seek(Checkpoint(listener.ordinal + 1))
    }
    game.timeline.checkpoint() shouldBe end

    game.timeline.stepBack() shouldBe true
    game.timeline.checkpoint() shouldBe listener
    agent.count("Pong") shouldBe 0
    game.recording().positions shouldBe listOf(start, listener)
    shouldThrow<IllegalStateException> { agent.runOperation("Ping") }
    shouldThrow<IllegalStateException> {
      game.actorEngine(PLAYER1).addTasks(parse<InstructionGroup>("Choice"))
    }
    game.timeline.stepBack() shouldBe true
    game.timeline.checkpoint() shouldBe start
    agent.count("Listener") shouldBe 0
    game.timeline.stepBack() shouldBe false
    game.timeline.stepForward() shouldBe true
    game.timeline.checkpoint() shouldBe listener
    game.timeline.stepForward() shouldBe true
    game.timeline.checkpoint() shouldBe end
    agent.count("Pong") shouldBe 1
    game.events.entriesSince(start) shouldBe completeEvents

    game.timeline.seek(listener)
    game.timeline.truncate()
    game.timeline.positions shouldBe listOf(start, listener)
    game.timeline.stepForward() shouldBe false
    agent.runOperation("Ping") { doTask("Pong") }
    agent.count("Pong") shouldBe 1
  }

  @Test
  internal fun positionsInsideAnOperationAreBetweenPlayerCalls() {
    val game =
        Engine.newGame(
            testGamePremise("CLASS ChoiceA\nCLASS ChoiceB\nCLASS ChoiceC\nCLASS ChoiceD")
        )
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    agent.addTasks("ChoiceA OR ChoiceB, ChoiceC OR ChoiceD")
    lateinit var betweenCalls: Checkpoint

    agent.continueOperation {
      doTask("ChoiceA")
      betweenCalls = game.timeline.checkpoint()
      (betweenCalls in game.timeline.positions) shouldBe true
      game.timeline.stepBack() shouldBe true
      game.timeline.stepBack() shouldBe false
      game.timeline.stepForward() shouldBe true
      doTask("ChoiceC")
      game.timeline.seek(betweenCalls)
      game.timeline.truncate()
      doTask("ChoiceD")
    }

    agent.count("ChoiceA") shouldBe 1
    agent.count("ChoiceC") shouldBe 0
    agent.count("ChoiceD") shouldBe 1
  }

  @Test
  internal fun directDoTasksRecordsAfterItsCompletionCallback() {
    val game = Engine.newGame(testGamePremise("CLASS ChoiceA\nCLASS ChoiceB"))
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    agent.addTasks("ChoiceA")
    val before = game.timeline.positions
    var callbackStarted = false
    game.onTransactionComplete = {
      if (!callbackStarted) {
        callbackStarted = true
        (game.timeline.checkpoint() in game.timeline.positions) shouldBe false
        agent.runOperation("ChoiceB")
      }
    }

    agent.doTasks("ChoiceA")

    game.timeline.positions shouldBe before + game.timeline.checkpoint()
    agent.count("ChoiceA") shouldBe 1
    agent.count("ChoiceB") shouldBe 1
  }

  @Test
  internal fun anIncompleteRawPositionCannotSeekToAnEarlierPlayerReturn() {
    val game = Engine.newGame(testGamePremise("CLASS Choice"))
    val start = game.timeline.checkpoint()
    game.actorEngine(PLAYER1).addTasks(InstructionGroup.of(parse<InstructionTree>("Choice")))

    shouldThrow<IllegalStateException> { game.timeline.seek(start) }
  }

  @Test
  internal fun failingEnclosingCallRemovesReturnedPositionsAndTruncatedHistory() {
    val game =
        Engine.newGame(
            testGamePremise("CLASS ChoiceA\nCLASS ChoiceB\nCLASS ChoiceC\nCLASS ChoiceD")
        )
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    agent.addTasks("ChoiceA OR ChoiceB, ChoiceC OR ChoiceD")
    val before = game.timeline.checkpoint()

    shouldThrow<IllegalStateException> {
      agent.continueOperation {
        doTask("ChoiceA")
        val between = game.timeline.checkpoint()
        doTask("ChoiceC")
        game.timeline.seek(between)
        game.timeline.truncate()
        error("reject the enclosing call")
      }
    }

    game.timeline.checkpoint() shouldBe before
    game.timeline.positions.last() shouldBe before
    agent.count("ChoiceA") shouldBe 0
    agent.count("ChoiceC") shouldBe 0
  }

  @Test
  internal fun completionCallbackCanSeekAfterANestedPlayerCallReturns() {
    val game = Engine.newGame(testGamePremise("CLASS ChoiceA\nCLASS ChoiceB"))
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val start = game.timeline.checkpoint()
    var inCallback = false
    game.onTransactionComplete = {
      if (!inCallback) {
        inCallback = true
        shouldThrow<IllegalStateException> { game.timeline.seek(start) }
        shouldThrow<IllegalStateException> { game.timeline.truncate() }
        agent.runOperation("ChoiceB")
        val afterNestedCall = game.timeline.checkpoint()
        (afterNestedCall in game.timeline.positions) shouldBe true
        game.timeline.seek(start)
        game.timeline.truncate()
      }
    }

    agent.runOperation("ChoiceA")
    agent.count("ChoiceA") shouldBe 0
    agent.count("ChoiceB") shouldBe 0
    game.timeline.positions shouldBe listOf(start)
  }

  @Test
  internal fun completionCallbackMayLeaveTheWorldAtARecordedPastPosition() {
    val game = Engine.newGame(testGamePremise("CLASS ChoiceA\nCLASS ChoiceB"))
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val start = game.timeline.checkpoint()
    var callbackStarted = false
    game.onTransactionComplete = {
      if (!callbackStarted) {
        callbackStarted = true
        agent.runOperation("ChoiceB")
        game.timeline.seek(start)
      }
    }

    agent.runOperation("ChoiceA")

    game.timeline.checkpoint() shouldBe start
    shouldThrow<IllegalStateException> { agent.runOperation("ChoiceA") }
    game.timeline.stepForward() shouldBe true
    agent.count("ChoiceA") shouldBe 1
    agent.count("ChoiceB") shouldBe 1
  }

  @Test
  internal fun nestedCorrectionsRecordEachPlayerReturn() {
    val game = Engine.newGame(testGamePremise("CLASS ChoiceA\nCLASS ChoiceB"))
    val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val start = game.timeline.checkpoint()

    game.timeline.atomic {
      agent.sneak("ChoiceA")
      val afterFirstCall = game.timeline.checkpoint()
      game.timeline.positions shouldBe listOf(start, afterFirstCall)
      agent.sneak("ChoiceB")
      game.timeline.positions shouldBe listOf(start, afterFirstCall, game.timeline.checkpoint())
    }

    game.timeline.positions.size shouldBe 3
  }
}
