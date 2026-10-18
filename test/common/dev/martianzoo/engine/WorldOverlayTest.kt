package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.toComponent
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldOverlayTest {
  @Test
  internal fun agentsCanPlayOnAnOverlayAndMaterializeItsHistory() {
    val source =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Listener { Ping: Pong }
                CLASS Ping
                CLASS Pong
                CLASS Choice
                CLASS CopyOnly
                """
            )
        )
    val sourceAgents = Agents(source) { NONE }
    val sourceAgent = sourceAgents[PLAYER1]
    sourceAgent.runOperation("Listener")
    val pending = sourceAgent.addTasks("Choice?").single()
    val sourceCheckpoint = source.timeline.checkpoint()

    val imagined = sourceAgents.overlay()
    val imaginedAgent = imagined[PLAYER1]
    val originalNote = source.events.entriesSince(Checkpoint(0)).first().notes
    imagined.world.events.entriesSince(Checkpoint(0)).first().notes = "overlay only"
    source.events.entriesSince(Checkpoint(0)).first().notes shouldBe originalNote
    imaginedAgent.autoExecPolicy shouldBe NONE
    imagined.world.tasks.getTaskData(pending) shouldBe source.tasks.getTaskData(pending)
    imaginedAgent.runOperation("Ping") { doTask("Pong") }
    imaginedAgent.doTask("Choice")

    imaginedAgent.count("Pong") shouldBe 1
    imaginedAgent.count("Choice") shouldBe 1
    sourceAgent.count("Pong") shouldBe 0
    sourceAgent.count("Choice") shouldBe 0
    source.timeline.checkpoint() shouldBe sourceCheckpoint

    val copied = imagined.fork()
    copied.world.events.entriesSince(Checkpoint(0)) shouldBe
        imagined.world.events.entriesSince(Checkpoint(0))
    copied.world.events.entriesSince(Checkpoint(0)).first().notes shouldBe "overlay only"
    copied.world.timeline.positions shouldBe imagined.world.timeline.positions
    copied[PLAYER1].count("Pong") shouldBe 1
    copied[PLAYER1].runOperation("CopyOnly")
    imaginedAgent.count("CopyOnly") shouldBe 0
    sourceAgent.count("CopyOnly") shouldBe 0

    imagined.reset()
    imagined.world.timeline.checkpoint() shouldBe sourceCheckpoint
    imagined.world.tasks.getTaskData(pending) shouldBe source.tasks.getTaskData(pending)
    imagined.world.events.entriesSince(Checkpoint(0)).first().notes shouldBe originalNote
    imaginedAgent.count("Pong") shouldBe 0
    imaginedAgent.count("Choice") shouldBe 0
    copied[PLAYER1].count("Pong") shouldBe 1
  }

  @Test
  internal fun overlayResetRestoresItsEffectsWithoutTouchingTheSource() {
    val source =
        Engine.newGame(
            testGamePremise("CLASS Listener { Ping: Pong }\nCLASS Ping\nCLASS Pong\nCLASS Other")
        )
    val sourceAgent = Agents(source) { NONE }[PLAYER1]
    sourceAgent.runOperation("Listener")
    val imagined = Agents(source) { NONE }.overlay()
    val imaginedAgent = imagined[PLAYER1]
    val start = imagined.world.timeline.checkpoint()
    val retainedComponents = imagined.world.components
    val retainedEvents = imagined.world.events
    val observedPongCounts = mutableListOf<Int>()
    val subscription =
        retainedComponents.listenToCount(
            imaginedAgent.resolve("Pong"),
            imagined.world.reader,
            observedPongCounts::add,
        )

    imaginedAgent.runOperation("Ping") { doTask("Pong") }
    imagined.reset()
    imagined.world.timeline.checkpoint() shouldBe start
    retainedEvents.entriesSince(start) shouldBe emptyList()
    imaginedAgent.runOperation("Other")
    imagined.world.timeline.commit()
    imagined.reset()
    imagined.world.timeline.floor shouldBe start
    imaginedAgent.runOperation("Ping") { doTask("Pong") }
    imagined.world.timeline.stepBack() shouldBe true
    imagined.world.timeline.stepForward() shouldBe true

    imaginedAgent.count("Pong") shouldBe 1
    imaginedAgent.count("Other") shouldBe 0
    sourceAgent.count("Pong") shouldBe 0
    sourceAgent.count("Other") shouldBe 0
    observedPongCounts shouldBe listOf(0, 1, 0, 1, 0, 1)
    subscription.cancel()
  }

  @Test
  internal fun removingAnInheritedEffectStaysRemovedInTheBranchAndItsCopy() {
    val source =
        Engine.newGame(testGamePremise("CLASS Listener { Ping: Pong }\nCLASS Ping\nCLASS Pong"))
    val sourceAgent = Agents(source) { NONE }[PLAYER1]
    sourceAgent.runOperation("Listener")
    val imagined = Agents(source) { NONE }.overlay()
    val imaginedAgent = imagined[PLAYER1]

    imaginedAgent.runOperation("-Listener")
    imaginedAgent.runOperation("Ping")
    imaginedAgent.count("Pong") shouldBe 0

    val copied = imagined.fork()
    copied[PLAYER1].runOperation("Ping")
    copied[PLAYER1].count("Pong") shouldBe 0

    imagined.reset()
    imaginedAgent.runOperation("Ping") { doTask("Pong") }
    imaginedAgent.count("Pong") shouldBe 1
    sourceAgent.runOperation("Ping") { doTask("Pong") }
    sourceAgent.count("Pong") shouldBe 1
  }

  @Test
  internal fun aResetListenerCannotStartAnotherTrialDuringReset() {
    val source = Engine.newGame(testGamePremise("CLASS Token\nCLASS Other"))
    val imagined = Agents(source) { NONE }.overlay()
    val agent = imagined[PLAYER1]
    val start = imagined.world.timeline.checkpoint()
    var armed = false
    var rejected = false
    val subscription =
        imagined.world.components.listenToCount(
            agent.resolve("Token"),
            imagined.world.reader,
        ) { count ->
          if (armed && count == 0) {
            shouldThrow<IllegalStateException> { agent.runOperation("Other") }
            rejected = true
          }
        }

    agent.runOperation("Token")
    armed = true
    imagined.reset()

    rejected shouldBe true
    imagined.world.timeline.checkpoint() shouldBe start
    imagined.world.timeline.floor shouldBe start
    agent.count("Other") shouldBe 0
    agent.runOperation("Other")
    agent.count("Other") shouldBe 1
    subscription.cancel()
  }

  @Test
  internal fun sourceMutationInvalidatesAnOverlayEvenAfterSourceRollback() {
    val source = Engine.newGame(testGamePremise("CLASS Token"))
    val sourceAgent = Agents(source) { NONE }[PLAYER1]
    val before = source.timeline.checkpoint()
    val imagined = Engine.overlay(source)
    val retainedComponents = imagined.components
    val retainedEvents = imagined.events
    val retainedTasks = imagined.tasks

    sourceAgent.runOperation("Token")
    source.timeline.seek(before)
    source.timeline.truncate()

    val token = source.reader.resolve(parse<Expression>("Token")).toComponent()
    shouldThrow<IllegalStateException> { retainedComponents.countComponent(token) }
    shouldThrow<IllegalStateException> { retainedEvents.entriesSince(Checkpoint(0)) }
    shouldThrow<IllegalStateException> { retainedTasks.isEmpty() }
    shouldThrow<IllegalStateException> { Engine.fork(imagined) }
    shouldThrow<IllegalStateException> { imagined.reset() }
  }

  @Test
  internal fun sourceTaskProbesInvalidateAnOverlayEvenWhenTheyRestoreTheSource() {
    val source = Engine.newGame(testGamePremise("CLASS Choice"))
    val sourceAgents = Agents(source) { NONE }
    val agent = sourceAgents[PLAYER1]
    val task = agent.addTasks("Choice?").single()
    val before = source.timeline.checkpoint()

    val first = sourceAgents.overlay()
    agent.canSelectTask(task) shouldBe true
    source.timeline.checkpoint() shouldBe before
    shouldThrow<IllegalStateException> { first.world.timeline.checkpoint() }

    val second = sourceAgents.overlay()
    agent.canExecuteTask(task)
    source.timeline.checkpoint() shouldBe before
    shouldThrow<IllegalStateException> { second.world.timeline.checkpoint() }
  }

  @Test
  internal fun aSoughtSourceMustBeTruncatedBeforeOverlaying() {
    val source = Engine.newGame(testGamePremise("CLASS Token"))
    val agent = Agents(source) { NONE }[PLAYER1]
    val before = source.timeline.checkpoint()
    agent.runOperation("Token")
    source.timeline.seek(before)

    shouldThrow<IllegalStateException> { Engine.overlay(source) }
    source.timeline.truncate()
    Engine.overlay(source).reader.count(parse<Metric>("Token")) shouldBe 0
  }
}
