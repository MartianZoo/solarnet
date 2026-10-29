package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agenttestsupport.withTestSoloSetup
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class StartTokenTest : TfmGameplayTest() {
  @Test
  internal fun `Start token begins with the first player and passes after each research phase`() {
    newTestGame(playerCount = 3)

    admin.assertCounts(
        3 to "NextPlayer",
        1 to "NextPlayer<Kim, Stan>",
        1 to "NextPlayer<Stan, Rob>",
        1 to "NextPlayer<Rob, Kim>",
    )
    admin.assertCounts(1 to "StartToken<Kim>", 0 to "StartToken<Stan>")

    finishGeneration(kim, stan, rob)
    admin.assertCounts(0 to "StartToken<Kim>", 1 to "StartToken<Stan>")

    finishGeneration(stan, rob, kim)
    admin.assertCounts(0 to "StartToken<Stan>", 1 to "StartToken<Rob>")

    finishGeneration(rob, kim, stan)
    admin.assertCounts(1 to "StartToken<Kim>", 0 to "StartToken<Rob>")
    admin.assertCounts(1 to "StartToken")
  }

  @Test
  internal fun `Start token stays with the only player in a solo game`() {
    newTestGame(playerCount = 1)

    finishGeneration(kim)

    admin.assertCounts(1 to "StartToken<Kim>", 0 to "NextPlayer")
  }

  /**
   * Every player passes in turn order, the start player chooses World Government's step, and no
   * player buys research cards.
   */
  private fun finishGeneration(vararg inTurnOrder: Agent) {
    inTurnOrder.forEach { it.doTask("Pass") }
    players.forEach { it.autoExecNow(EAGER) }
    inTurnOrder.first().doTask("VenusStep! BY Admin")

    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
  }

  internal class Setup : TfmTest() {
    @Test
    internal fun `solo setup links each greenery to its own city`() {
      // The second greenery neighbors the first city, but not the second city.
      shouldThrow<IllegalArgumentException> {
            withTestSoloSetup(
                canonicalPremise(players = 1),
                listOf("Tharsis_4_1", "Tharsis_5_1", "Tharsis_5_8", "Tharsis_3_1").map(::cn),
            )
          }
          .message
          .shouldContain("greenery must be adjacent")
    }

    @Test
    internal fun `Turn order reads the start token holder`() {
      val setup = canonicalPremise(Hellas, PromoCards, players = 2)
      val game = TfmEngine.newGame(setup)
      val admin = game.testAgent(ADMIN)
      val p1 = game.testAgent(PLAYER1)
      val p2 = game.testAgent(PLAYER2)

      admin.exMachina("StartToken<Player2> FROM StartToken<Player1>")
      admin.beginOperation("SetupPhase FROM BootstrapPhase")
      admin.count("SetupPhase") shouldBe 1
      admin.count("Baton") shouldBe 0
      admin.count("ActionScope") shouldBe 0
      p1.doTask("-3 ProjectCard<Selecting>")
      p2.doTask("-5 ProjectCard<Selecting>")
      p2.doTasks("PlayCard<Class<CorporationCard>, Class<CrediCor>>")
      p1.doTasks("PlayCard<Class<CorporationCard>, Class<InterplanetaryCinematics>>")

      admin.count("ActionPhase") shouldBe 1
      game.tasks.extract { it }.filterNot { it.deferred }.map { it.assignee } shouldContainExactly
          listOf(PLAYER2)
    }

    @Test
    internal fun `Research passes the token according to the explicit seat order`() {
      game = setUpGame(players = 3)
      admin.exMachina(
          "NextPlayer<Player1, Player3> FROM NextPlayer<Player1, Player2>, " +
              "NextPlayer<Player3, Player2> FROM NextPlayer<Player3, Player1>, " +
              "NextPlayer<Player2, Player1> FROM NextPlayer<Player2, Player3>"
      )
      admin.beginOperation("ResearchPhase FROM SetupPhase")
      admin.assertCounts(0 to "StartToken<Player1>", 1 to "StartToken<Player3>")
    }

    @Test
    internal fun `Seat order and start token reject invalid corrections`() {
      game = setUpGame(players = 3)
      shouldThrow<LimitsException> { admin.exMachina("-StartToken<Player1>") }
      shouldThrow<LimitsException> { admin.exMachina("NextPlayer<Player1, Player3>") }
      shouldThrow<LimitsException> { admin.exMachina("NextPlayer<Player3, Player2>") }
    }
  }
}
