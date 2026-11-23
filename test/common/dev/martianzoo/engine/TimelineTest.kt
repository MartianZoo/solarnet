package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
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
    val agents = Agents(game) { EAGER }
    val agent = agents[PLAYER1].also { it.autoExecPolicy = NONE }
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
    val agents = Agents(game) { EAGER }
    val agent = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    val timeline = game.timeline
    val start = game.timeline.checkpoint()
    agent.runOperation("Listener")
    val listener = game.timeline.checkpoint()
    agent.runOperation("Ping") { doTask("Pong") }
    val end = game.timeline.checkpoint()
    val completeEvents = game.events.entriesSince(game.events.eventOrdinal(start))

    game.timeline.positions shouldBe (0..end.serial).map(::Checkpoint)
    shouldThrow<IllegalArgumentException> {
      game.timeline.seek(Checkpoint(end.serial + 1))
    }
    game.timeline.checkpoint() shouldBe end

    timeline.seek(listener)
    agent.count("Pong") shouldBe 0
    game.recording().positions shouldBe (0..listener.serial).map(::Checkpoint)
    shouldThrow<IllegalStateException> { agent.runOperation("Ping") }
    shouldThrow<IllegalStateException> {
      game.actorEngine(PLAYER1).addTasks(parse<InstructionGroup>("Choice"))
    }
    timeline.seek(start)
    agent.count("Listener") shouldBe 0
    timeline.seek(listener)
    agent.count("Pong") shouldBe 0
    timeline.seek(end)
    agent.count("Pong") shouldBe 1
    game.events.entriesSince(game.events.eventOrdinal(start)) shouldBe completeEvents

    game.timeline.seek(listener)
    game.timeline.truncate()
    game.timeline.positions shouldBe (0..listener.serial).map(::Checkpoint)
    shouldThrow<IllegalArgumentException> { timeline.seek(end) }
    agent.runOperation("Ping") { doTask("Pong") }
    agent.count("Pong") shouldBe 1
  }

  @Test
  internal fun positionsInsideAnOperationAreBetweenPlayerCalls() {
    val game =
        Engine.newGame(
            testGamePremise("CLASS ChoiceA\nCLASS ChoiceB\nCLASS ChoiceC\nCLASS ChoiceD")
        )
    val agents = Agents(game) { EAGER }
    val agent = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    val timeline = game.timeline
    val beforeOffer = timeline.checkpoint()
    agent.addTasks("ChoiceA OR ChoiceB, ChoiceC OR ChoiceD")
    val offered = timeline.checkpoint()

    agent.continueOperation {
      doTask("ChoiceA")
      val betweenCalls = timeline.checkpoint()
      (betweenCalls in game.timeline.positions) shouldBe true
      timeline.seek(offered)
      shouldThrow<IllegalStateException> { timeline.seek(beforeOffer) }
      timeline.seek(betweenCalls)
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
  internal fun directDoTasksRecordsNavigablePositions() {
    val game = Engine.newGame(testGamePremise("CLASS ChoiceA\nCLASS ChoiceB"))
    val agents = Agents(game) { EAGER }
    val agent = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    val timeline = game.timeline
    agent.addTasks("ChoiceA")
    val offered = timeline.checkpoint()
    agent.doTasks("ChoiceA")

    game.timeline.positions shouldBe (0..game.timeline.checkpoint().serial).map(::Checkpoint)
    agent.count("ChoiceA") shouldBe 1
    val done = game.timeline.checkpoint()
    timeline.seek(offered)
    agent.count("ChoiceA") shouldBe 0
    game.timeline.seek(done)
    agent.count("ChoiceA") shouldBe 1
  }

  @Test
  internal fun aRawTaskEditIsANavigableTransaction() {
    val game = Engine.newGame(testGamePremise("CLASS Choice"))
    val timeline = game.timeline
    val start = timeline.checkpoint()
    game.actorEngine(PLAYER1).addTasks(InstructionGroup.of(parse<InstructionTree>("Choice")))
    val offered = timeline.checkpoint()

    game.timeline.seek(start)
    game.tasks.isEmpty() shouldBe true
    timeline.seek(offered)
    game.tasks.isEmpty() shouldBe false
  }

  @Test
  internal fun failingEnclosingCallRemovesReturnedPositionsAndTruncatedHistory() {
    val game =
        Engine.newGame(
            testGamePremise("CLASS ChoiceA\nCLASS ChoiceB\nCLASS ChoiceC\nCLASS ChoiceD")
        )
    val agents = Agents(game) { EAGER }
    val agent = agents[PLAYER1].also { it.autoExecPolicy = NONE }
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
  internal fun nestedCorrectionsRecordEachPlayerReturn() {
    val game = Engine.newGame(testGamePremise("CLASS ChoiceA\nCLASS ChoiceB"))
    val agents = Agents(game) { EAGER }
    val agent = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    val start = game.timeline.checkpoint()

    game.timeline.atomic {
      agent.exMachina("ChoiceA")
      val afterFirstCall = game.timeline.checkpoint()
      game.timeline.positions shouldBe listOf(start, afterFirstCall)
      agent.exMachina("ChoiceB")
      game.timeline.positions shouldBe listOf(start, afterFirstCall, game.timeline.checkpoint())
    }

    game.timeline.positions.size shouldBe 3
  }
}
