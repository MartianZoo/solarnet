package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
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
    val wholePlayer = whole.agent(PLAYER1).also { it.autoExecMode = NONE }
    wholePlayer.manual("Watcher")
    val pendingTask = wholePlayer.addTasks("Token?").single()
    val wholeCheckpoint = whole.timeline.checkpoint()
    val wholeRevision = whole.revision

    val overlay = Engine.overlay(whole)
    val overlayCheckpoint = overlay.timeline.checkpoint()

    (overlay is OverlayWorld) shouldBe true
    overlay.agent(PLAYER1).autoExecMode shouldBe NONE
    overlay.agent(PLAYER1).doTask("Token")

    overlay.agent(PLAYER1).count("Watcher") shouldBe 1
    overlay.agent(PLAYER1).count("Token") shouldBe 1
    overlay.agent(PLAYER1).count("Marker") shouldBe 1
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
    overlay.agent(PLAYER1).count("Token") shouldBe 0
    overlay.tasks.ids().shouldContainExactly(pendingTask)
  }

  @Test
  internal fun rejectsReadsAfterItsBackingWorldChanges() {
    val whole = Engine.newGame(testGamePremise())
    val overlay = Engine.overlay(whole)

    whole.agent(PLAYER1).manual("Token")

    shouldThrow<IllegalStateException> { overlay.agent(PLAYER1).count("Token") }
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
    val wholePlayer = whole.agent(PLAYER1)
    wholePlayer.manual("Target")
    wholePlayer.manual("Dependent<Target>")
    val overlay = Engine.overlay(whole)

    overlay.agent(PLAYER1).manual("-Target")

    overlay.agent(PLAYER1).count("Target") shouldBe 0
    overlay.agent(PLAYER1).count("Dependent<Target>") shouldBe 0
    wholePlayer.count("Target") shouldBe 1
    wholePlayer.count("Dependent<Target>") shouldBe 1
  }

  @Test
  internal fun rejectsEventReadsAfterItsBackingWorldRebranches() {
    val whole = Engine.newGame(testGamePremise("CLASS Token\nCLASS Marker"))
    val startingCheckpoint = whole.timeline.checkpoint()
    whole.agent(PLAYER1).manual("Token")
    val overlay = Engine.overlay(whole)

    whole.timeline.rollBack(startingCheckpoint)
    whole.agent(PLAYER1).manual("Marker")

    shouldThrow<IllegalStateException> {
      overlay.events.changesSince(startingCheckpoint)
    }
  }
}
