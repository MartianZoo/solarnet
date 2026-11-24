package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.recording
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ExMachinaDropTaskTest {
  private fun agents(extra: String = "", players: Int = 1) =
      Agents(
          Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker\nCLASS Other\n$extra", players))
      ) {
        NONE
      }

  @Test
  fun omissionRunsNeitherImmediateNorQueuedEffects() {
    val p = agents("CLASS Gift { This:: Marker; This: Other }")[PLAYER1]
    p.addTasks("Gift THEN Coin")
    p.exMachinaDropTask("Gift")
    p.count("Gift") shouldBe 0
    p.count("Marker") shouldBe 0
    p.count("Other") shouldBe 0
    p.doTask("Coin")
    p.count("Coin") shouldBe 1
    p.tasks.isEmpty() shouldBe true
  }

  @Test
  fun skippedCostRetainsItsBoundReward() {
    val p = agents()[PLAYER1]
    p.exMachina("6 Coin")
    p.addTasks("-2X Coin THEN 3X Marker")
    p.exMachinaDropTask("-4 Coin")
    p.count("Coin") shouldBe 6
    p.doTask("6 Marker")
    p.count("Marker") shouldBe 6
    p.tasks.isEmpty() shouldBe true
  }

  @Test
  fun sourceQualifiedOmissionOpensOnlyItsContainer() {
    val p = agents("CLASS Box { This: EACH Player { Coin, Marker } }")[PLAYER1]
    p.addTasks("Box")
    p.doTask("Box")
    p.addTasks("Coin")
    p.exMachinaDropTask("Coin", cn("Box"))
    p.count("Coin") shouldBe 0
    p.doTasks("Coin", "Marker")
    p.count("Coin") shouldBe 1
    p.count("Marker") shouldBe 1
    p.tasks.isEmpty() shouldBe true
  }

  @Test
  fun completeTotalSkipsAllMatchingAmounts() {
    val p = agents()[PLAYER1]
    p.addTasks("2 Coin, 3 Coin, Marker")
    p.exMachinaDropTask("5 Coin")
    p.count("Coin") shouldBe 0
    p.doTask("Marker")
    p.tasks.isEmpty() shouldBe true
  }

  @Test
  fun missingAmbiguousAndPartialRequestsLeaveHistoryUnchanged() {
    for (offered in listOf("Other", "(Coin THEN Marker) OR (Coin THEN Other)", "2 Coin")) {
      val a = agents()
      val p = a[PLAYER1]
      p.addTasks(offered)
      val before = a.world.recording().events
      shouldThrow<GameplayException> { p.exMachinaDropTask("Coin") }
      a.world.recording().events shouldBe before
    }
  }

  @Test
  fun failureInMandatoryFollowingWorkRestoresTheOmittedTask() {
    val a = agents("CLASS Trouble : System { This:: -Marker }")
    val p = a[PLAYER1]
    p.addTasks("Coin THEN Trouble")
    val before = a.world.recording().events
    shouldThrow<GameplayException> { p.exMachinaDropTask("Coin") }
    a.world.recording().events shouldBe before
    p.exMachina("Marker")
    p.exMachinaDropTask("Coin")
    p.count("Coin") shouldBe 0
    p.count("Marker") shouldBe 0
    p.count("Trouble") shouldBe 1
    p.tasks.isEmpty() shouldBe true
  }

  @Test
  fun recipientCanOmitReleasedWorkWithoutConsumingOtherReleases() {
    val a = agents(players = 2)
    val sender = a[PLAYER1]
    val recipient = a[PLAYER2]
    sender.addTasks("NOW Player2 { 2 Coin }, NOW Player2 { Marker }")
    recipient.addTasks("3 Coin")
    recipient.exMachinaDropTask("5 Coin", releasedBy = sender)
    recipient.count("Coin") shouldBe 0
    recipient.doTask("Marker", releasedBy = sender)
    recipient.count("Marker") shouldBe 1
    sender.tasks.isEmpty() shouldBe true
    recipient.tasks.isEmpty() shouldBe true
  }

  @Test
  fun priorityAndSelectedWorkStillApply() {
    val a = agents()
    val p = a[PLAYER1]
    p.addTasks("Coin, LATER { Marker }")
    val before = a.world.recording().events
    shouldThrow<TaskException> { p.exMachinaDropTask("Marker") }
    a.world.recording().events shouldBe before
    p.selectTask("Coin")
    shouldThrow<NarrowingException> { p.exMachinaDropTask("Other") }
    p.exMachinaDropTask("Coin")
    p.exMachinaDropTask("Marker")
    p.tasks.isEmpty() shouldBe true
  }
}
