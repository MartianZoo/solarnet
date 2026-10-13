package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.testTfm
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
  internal fun startsWithTheFirstPlayerAndPassesAfterEachResearchPhase() {
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
    shouldThrow<LimitsException> { admin.sneak("-StartToken<Kim>") }
    shouldThrow<LimitsException> { admin.sneak("AfterMe<Kim, Rob>") }
    shouldThrow<LimitsException> { admin.sneak("AfterMe<Rob, Stan>") }
  }

  @Test
  internal fun passesAccordingToTheExplicitAfterMeRelation() {
    newTestGame(playerCount = 3)
    // Reverse the whole ring in one correction, preserving one predecessor and successor each.
    admin.sneak(
        "AfterMe<Kim, Rob> FROM AfterMe<Kim, Stan>, " +
            "AfterMe<Rob, Stan> FROM AfterMe<Rob, Kim>, " +
            "AfterMe<Stan, Kim> FROM AfterMe<Stan, Rob>"
    )

    finishGeneration(kim, rob, stan)

    admin.assertCounts(0 to "StartToken<Kim>", 1 to "StartToken<Rob>")
  }

  @Test
  internal fun staysWithTheOnlyPlayerInAnActualOnePlayerGame() {
    newTestGame(playerCount = 1)

    finishGeneration(kim)

    admin.assertCounts(1 to "StartToken<Kim>", 0 to "AfterMe")
  }

  @Test
  internal fun `solo setup links each greenery to its own city`() {
    val admin = setUpGame(players = 1).testTfm(ADMIN)

    admin.doTask("CityTile<Tharsis_4_1>")
    admin.doTask("GreeneryTile<Tharsis_5_1>")
    admin.doTask("CityTile<Tharsis_5_8>")

    // This area neighbors the first city, but not the selected second city.
    shouldThrow<NarrowingException> { admin.doTask("GreeneryTile<Tharsis_3_1>") }
  }

  @Test
  internal fun turnOrderReadsTheTokenHolder() {
    val setup = canonicalPremise(Hellas, PromoCards, players = 2)
    val game = TfmEngine.newGame(setup)
    val admin = game.testTfm(ADMIN)
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)

    admin.startGame()
    p1.keepStartingProjects(7)
    p2.keepStartingProjects(5)
    p1.playCorp(InterplanetaryCinematics)
    admin.sneak("StartToken<Player2> FROM StartToken<Player1>")
    p2.playCorp(CrediCor)

    admin.count("ActionPhase") shouldBe 1
    game.tasks.extract { it }.filterNot { it.deferred }.map { it.assignee } shouldContainExactly
        listOf(PLAYER2)
  }

  /**
   * Every player passes in turn order, the start player chooses World Government's step, and no
   * player buys research cards.
   */
  private fun finishGeneration(vararg inTurnOrder: TfmGameplay) {
    inTurnOrder.forEach { it.pass() }
    inTurnOrder.first().wgt("VenusStep")
    players.forEach { it.buyCards(0) }
  }
}
