package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
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
import kotlin.test.Test

internal class StartTokenTest : TfmGameplayTest() {
  @Test
  internal fun `Start token begins with the first player and passes after each research phase`() {
    newTestGame(playerCount = 3)

    admin.assertCounts(
        3 to "AfterMe",
        1 to "AfterMe<Kim, Stan>",
        1 to "AfterMe<Stan, Rob>",
        1 to "AfterMe<Rob, Kim>",
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

    admin.assertCounts(1 to "StartToken<Kim>", 0 to "AfterMe")
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
      val admin = setUpGame(players = 1).testAgent(ADMIN)

      admin.doTask("CityTile<Tharsis_4_1>")
      admin.doTask("GreeneryTile<Tharsis_5_1>")
      admin.doTask("CityTile<Tharsis_5_8>")

      // This area neighbors the first city, but not the selected second city.
      shouldThrow<NarrowingException> { admin.doTask("GreeneryTile<Tharsis_3_1>") }
    }

    @Test
    internal fun `Turn order reads the start token holder`() {
      val setup = canonicalPremise(Hellas, PromoCards, players = 2)
      val game = TfmEngine.newGame(setup)
      val admin = game.testAgent(ADMIN)
      val p1 = game.testAgent(PLAYER1)
      val p2 = game.testAgent(PLAYER2)

      admin.beginOperation("SetupPhase FROM BootstrapPhase")
      p1.doTask("-3 ProjectCard<Selecting>")
      p2.doTask("-5 ProjectCard<Selecting>")
      p1.doTasks("PlayCard<Class<CorporationCard>, Class<InterplanetaryCinematics>>")
      admin.sneak("StartToken<Player2> FROM StartToken<Player1>")
      p2.doTasks("PlayCard<Class<CorporationCard>, Class<CrediCor>>")

      admin.count("ActionPhase") shouldBe 1
      game.tasks.extract { it }.filterNot { it.deferred }.map { it.assignee } shouldContainExactly
          listOf(PLAYER2)
    }

    @Test
    internal fun `Research passes the token according to the explicit seat order`() {
      game = setUpGame(players = 3)
      admin.sneak(
          "AfterMe<Player1, Player3> FROM AfterMe<Player1, Player2>, " +
              "AfterMe<Player3, Player2> FROM AfterMe<Player3, Player1>, " +
              "AfterMe<Player2, Player1> FROM AfterMe<Player2, Player3>"
      )
      admin.beginOperation("ResearchPhase FROM SetupPhase")
      admin.assertCounts(0 to "StartToken<Player1>", 1 to "StartToken<Player3>")
    }

    @Test
    internal fun `Seat order and start token reject invalid corrections`() {
      game = setUpGame(players = 3)
      shouldThrow<LimitsException> { admin.sneak("-StartToken<Player1>") }
      shouldThrow<LimitsException> { admin.sneak("AfterMe<Player1, Player3>") }
      shouldThrow<LimitsException> { admin.sneak("AfterMe<Player3, Player2>") }
    }
  }
}
