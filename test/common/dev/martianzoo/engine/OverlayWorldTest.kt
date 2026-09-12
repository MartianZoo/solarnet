package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class OverlayWorldTest {
  @Test
  internal fun hypotheticalExecutionLeavesTheWholeWorldUnchanged() {
    val whole =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Token
                CLASS Marker
                CLASS Watcher { Token:: Marker }
                """
            )
        ) as WholeWorld
    val wholePlayer = whole.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    wholePlayer.runOperation("Watcher")
    val pendingTask = wholePlayer.addTasks("Token?").single()
    val wholeCheckpoint = whole.timeline.checkpoint()
    val wholeRevision = whole.revision

    val overlay = Engine.overlay(whole)
    val overlayCheckpoint = overlay.timeline.checkpoint()
    val overlayPlayer = overlay.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

    (overlay is OverlayWorld) shouldBe true
    overlayPlayer.doTask("Token")

    overlayPlayer.count("Watcher") shouldBe 1
    overlayPlayer.count("Token") shouldBe 1
    overlayPlayer.count("Marker") shouldBe 1
    overlay.tasks.isEmpty() shouldBe true
    overlay.events
        .changesSince(overlayCheckpoint)
        .mapNotNull { it.change.gaining?.className?.toString() }
        .shouldContainExactly("Token", "Marker")

    wholePlayer.count("Watcher") shouldBe 1
    wholePlayer.count("Token") shouldBe 0
    wholePlayer.count("Marker") shouldBe 0
    whole.tasks.ids().shouldContainExactly(pendingTask)
    whole.timeline.checkpoint() shouldBe wholeCheckpoint
    whole.events.entriesSince(wholeCheckpoint).isEmpty() shouldBe true
    whole.revision shouldBe wholeRevision

    overlay.timeline.rollBack(overlayCheckpoint)
    overlayPlayer.count("Token") shouldBe 0
    overlay.tasks.ids().shouldContainExactly(pendingTask)
  }

  @Test
  internal fun rejectsReadsAfterItsBackingWorldChanges() {
    val whole = Engine.newGame(testGamePremise())
    val overlay = Engine.overlay(whole)

    whole.testAgent(PLAYER1).runOperation("Token")

    shouldThrow<IllegalStateException> { overlay.testAgent(PLAYER1).count("Token") }
  }

  @Test
  internal fun dependentRemovalUsesTheCombinedComponentGraph() {
    val whole =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Target { HAS MAX 1 This }
                CLASS Dependent<Target>
                """
            )
        )
    val wholePlayer = whole.testAgent(PLAYER1)
    wholePlayer.runOperation("Target")
    wholePlayer.runOperation("Dependent<Target>")
    val overlay = Engine.overlay(whole)
    val overlayPlayer = overlay.testAgent(PLAYER1)

    overlayPlayer.runOperation("-Target")

    overlayPlayer.count("Target") shouldBe 0
    overlayPlayer.count("Dependent<Target>") shouldBe 0
    wholePlayer.count("Target") shouldBe 1
    wholePlayer.count("Dependent<Target>") shouldBe 1
  }

  @Test
  internal fun rejectsEventReadsAfterItsBackingWorldRebranches() {
    val whole = Engine.newGame(testGamePremise("CLASS Token\nCLASS Marker"))
    val startingCheckpoint = whole.timeline.checkpoint()
    val wholePlayer = whole.testAgent(PLAYER1)
    wholePlayer.runOperation("Token")
    val overlay = Engine.overlay(whole)

    whole.timeline.rollBack(startingCheckpoint)
    wholePlayer.runOperation("Marker")

    shouldThrow<IllegalStateException> {
      overlay.events.changesSince(startingCheckpoint)
    }
  }
}
