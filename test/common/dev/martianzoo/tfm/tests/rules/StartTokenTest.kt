package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

internal class StartTokenTest {
  @Test
  internal fun startsWithPlayer1AndPassesAfterEachResearchPhase() {
    val admin = setUpGame(players = 3).tfm(ADMIN)

    admin.assertCounts(
        3 to "AfterMe",
        1 to "AfterMe<Player1, Player2>",
        1 to "AfterMe<Player2, Player3>",
        1 to "AfterMe<Player3, Player1>",
    )
    admin.assertCounts(1 to "StartToken<Player1>", 0 to "StartToken<Player2>")

    admin.nextGeneration(0, 0, 0)
    admin.assertCounts(0 to "StartToken<Player1>", 1 to "StartToken<Player2>")

    admin.nextGeneration(0, 0, 0)
    admin.assertCounts(0 to "StartToken<Player2>", 1 to "StartToken<Player3>")

    admin.nextGeneration(0, 0, 0)
    admin.assertCounts(1 to "StartToken<Player1>", 0 to "StartToken<Player3>")
    admin.assertCounts(1 to "StartToken")
    shouldThrow<LimitsException> { admin.manual("-StartToken<Player1>") }
    shouldThrow<LimitsException> { admin.manual("AfterMe<Player1, Player3>") }
    shouldThrow<LimitsException> { admin.manual("AfterMe<Player3, Player2>") }
  }

  @Test
  internal fun passesAccordingToTheExplicitAfterMeRelation() {
    val admin = setUpGame(players = 3).tfm(ADMIN)
    admin.sneak("AfterMe<Player1, Player3> FROM AfterMe<Player1, Player2>")

    admin.nextGeneration(0, 0, 0)

    admin.assertCounts(0 to "StartToken<Player1>", 1 to "StartToken<Player3>")
  }

  @Test
  internal fun staysWithPlayer1InAnActualOnePlayerSetup() {
    val game = setUpGame(players = 1)
    val admin = game.tfm(ADMIN)

    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_2_2, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_2_3, SoloOpponent>")
    admin.nextGeneration(0)

    admin.assertCounts(
        1 to "StartToken<Player1>",
        1 to "AfterMe<Player1, Player1>",
        1 to "AfterMe",
    )
  }

  @Test
  internal fun `solo setup links each greenery to its own city`() {
    val admin = setUpGame(players = 1).tfm(ADMIN)

    admin.doTask("CityTile<Tharsis_4_1, SoloOpponent>")
    admin.doTask("GreeneryTile<Tharsis_5_1, SoloOpponent>")
    admin.doTask("CityTile<Tharsis_5_8, SoloOpponent>")

    // This area neighbors the first city, but not the selected second city.
    shouldThrow<NarrowingException> { admin.doTask("GreeneryTile<Tharsis_3_1, SoloOpponent>") }
  }

  @Test
  internal fun autoWorkflowReadsTheTokenOwner() {
    val setup = canonicalPremise(Hellas, PromoCardPack, players = 2)
    val game = Engine.newGame(setup)
    val admin = game.tfm(ADMIN)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    val workflow = TfmWorkflow.Auto(game).launch()
    game.retainStartingProjects(7, 5)

    p1.playCorp(InterplanetaryCinematics, 7)
    admin.sneak("StartToken<Player2> FROM StartToken<Player1>")
    p2.playCorp(PharmacyUnion, 5)

    game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    workflow.shutdown()
  }
}
