package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.TaskMatcher
import dev.martianzoo.engine.recording
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TaskPreparationTest {
  private fun agents(extra: String = "") =
      Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker\nCLASS Other\n$extra"))) {
        NONE
      }

  @Test
  fun opensOnlyTheEachNeededByTheRequest() {
    val a = agents()
    val p = a[PLAYER1]
    p.addTasks("EACH Player { Coin, Marker }, EACH Player { Other }")
    p.doTask("Coin")
    p.count("Coin") shouldBe 1
    p.count("Marker") shouldBe 0
    p.count("Other") shouldBe 0
    p.doTasks("Marker", "Other")
    p.count("Marker") shouldBe 1
    p.count("Other") shouldBe 1
    p.tasks.isEmpty() shouldBe true
  }

  @Test
  fun choosesTheUniqueOrGroupAndLeavesItsOtherWorkPending() {
    val p = agents()[PLAYER1]
    p.addTasks("(Coin, Marker) OR Other")
    p.doTask("Coin")
    p.count("Coin") shouldBe 1
    p.count("Marker") shouldBe 0
    p.count("Other") shouldBe 0
    p.doTask("Marker")
    p.tasks.isEmpty() shouldBe true
  }

  @Test
  fun distinctPreparationsDoNotBecomeEquivalentBecauseTheFirstGainMatches() {
    for (offered in
        listOf(
            "Coin, EACH Player { Coin }",
            "(EACH Player { Coin, Marker }) OR (EACH Player { Coin, Other })",
            "(Coin THEN Marker) OR (Coin THEN Other)",
        )) {
      val a = agents()
      val p = a[PLAYER1]
      p.addTasks(offered)
      val before = a.world.events.nextOrdinal
      shouldThrow<TaskException> { p.doTask("Coin") }
      a.world.events.nextOrdinal shouldBe before
      p.count("Coin") shouldBe 0
    }
  }

  @Test
  fun satisfiedGatesDoNotDistinguishOtherwiseIdenticalArms() {
    val p = agents()[PLAYER1]
    p.exMachina("Marker")
    p.addTasks("Coin OR (Marker: Coin)")
    p.doTask("Coin")
    p.count("Coin") shouldBe 1
    p.tasks.isEmpty() shouldBe true
  }

  @Test
  fun anExplicitEachDoesNotBecomeTheHeadOfAnUnrelatedThen() {
    val p = agents()[PLAYER1]
    p.addTasks("EACH Player { Other }, X Coin THEN X Marker")
    p.doTask("EACH Player { Other }")
    p.doTask("Other")
    p.count("Other") shouldBe 1
    p.count("Coin") shouldBe 0
  }

  @Test
  fun aTotalCannotHideAnAlternativePreparation() {
    for (alternative in listOf("EACH Player { 2 Coin }", "2 Coin OR Marker")) {
      val a = agents()
      val p = a[PLAYER1]
      p.addTasks("Coin, Coin, $alternative")
      val before = a.world.events.nextOrdinal
      shouldThrow<TaskException> { p.doTask("2 Coin") }
      a.world.events.nextOrdinal shouldBe before
      p.count("Coin") shouldBe 0
    }
  }

  @Test
  fun sourceQualificationUsesTheSameMatcherForTheContainerAndItsChild() {
    val p = agents("CLASS Box { This: EACH Player { Coin } }")[PLAYER1]
    p.addTasks("Box")
    p.doTask("Box")
    p.addTasks("Coin")
    p.doTask("Coin", cn("Box"))
    p.count("Coin") shouldBe 1
    p.doTask("Coin")
    p.count("Coin") shouldBe 2
  }

  @Test
  fun failureRestoresTheUnexpandedParentAndAllChanges() {
    val a = agents("CLASS Trouble { This:: -Marker }")
    val p = a[PLAYER1]
    p.addTasks("EACH Player { Trouble }")
    val before = a.world.events.nextOrdinal
    shouldThrow<GameplayException> { p.doTask("Trouble") }
    a.world.events.nextOrdinal shouldBe before
    p.count("Trouble") shouldBe 0
    p.doTask("EACH")
  }

  @Test
  fun doesNotOpenDeferredWorkOrBypassASelection() {
    val a = agents()
    val p = a[PLAYER1]
    p.addTasks("Marker, LATER { EACH Player { Coin } }")
    shouldThrow<TaskException> { p.doTask("Coin") }
    p.selectTask("Marker")
    shouldThrow<NarrowingException> { p.doTask("Coin") }
    p.doTasks("Marker", "Coin")
    p.count("Coin") shouldBe 1
  }

  @Test
  fun doesNotMoveARequestedGainAheadOfNewAdminWork() {
    val a = agents("CLASS Gift : System { This:: Marker }")
    val p = a[PLAYER1]
    p.addTasks("EACH Player { Gift, Coin }")
    val before = a.world.events.nextOrdinal
    shouldThrow<TaskException> { p.doTask("Coin") }
    a.world.events.nextOrdinal shouldBe before
    p.count("Coin") shouldBe 0
    p.count("Marker") shouldBe 0
    p.doTask("EACH")
    p.count("Marker") shouldBe 1
    p.doTask("Coin")
  }

  @Test
  fun theExporterCanAskTheSameQuestionWithoutChangingTheRecording() {
    val a = agents()
    a[PLAYER1].addTasks("EACH Player { Coin, Marker }")
    val world = a.world.recording().open().world
    val before = world.nextOrdinal
    val match = requireNotNull(TaskMatcher(world).match(PLAYER1, "Coin"))
    match.preparations.size shouldBe 1
    world.nextOrdinal shouldBe before
    world.tasks.extract { it }.size shouldBe 1
  }
}
