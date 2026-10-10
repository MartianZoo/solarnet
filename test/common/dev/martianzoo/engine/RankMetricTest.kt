package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.testsupport.PLAYER3
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RankMetricTest {
  @Test
  internal fun highestFirstCompetitionRankPreservesTiesAndSkipsPlaces() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Score : Owned<Player>
                CLASS Cash : Owned<Player>
                CLASS Prize : Owned<Player>
                CLASS GlobalPrize : Owned<Player>
                CLASS TieBreakPrize : Owned<Player>
                CLASS InversePrize : Owned<Player>
                CLASS DifferencePrize : Owned<Player>
                """,
                players = 3,
            )
        )
    game.testAgent(PLAYER1).runOperation("3 Score<Player1>")
    game.testAgent(PLAYER2).runOperation("2 Score<Player2>")
    game.testAgent(PLAYER3).runOperation("2 Score<Player3>, Cash<Player3>")

    game
        .testAgent(ADMIN)
        .runOperation("EACH @Player(HAS =2 (RANK Me@Player { OWN[Score] })) { Prize<@Player> }")

    game.testAgent(PLAYER1).count("Prize<Player1>") shouldBe 0
    game.testAgent(PLAYER2).count("Prize<Player2>") shouldBe 1
    game.testAgent(PLAYER3).count("Prize<Player3>") shouldBe 1

    game
        .testAgent(ADMIN)
        .runOperation("EACH @Player(HAS =1 (RANK Player { Score })) { GlobalPrize<@Player> }")
    game.testAgent(PLAYER1).count("GlobalPrize<Player1>") shouldBe 1
    game.testAgent(PLAYER2).count("GlobalPrize<Player2>") shouldBe 1
    game.testAgent(PLAYER3).count("GlobalPrize<Player3>") shouldBe 1

    game
        .testAgent(ADMIN)
        .runOperation(
            "EACH @Player(HAS =2 (RANK Me@Player { OWN[Score], OWN[Cash] })) { TieBreakPrize<@Player> }"
        )
    game.testAgent(PLAYER1).count("TieBreakPrize<Player1>") shouldBe 0
    game.testAgent(PLAYER2).count("TieBreakPrize<Player2>") shouldBe 0
    game.testAgent(PLAYER3).count("TieBreakPrize<Player3>") shouldBe 1

    game
        .testAgent(ADMIN)
        .runOperation(
            "EACH @Player(HAS =3 (RANK Me@Player { 99 - OWN[Score] })) { InversePrize<@Player> }"
        )
    game.testAgent(PLAYER1).count("InversePrize<Player1>") shouldBe 1
    game.testAgent(PLAYER2).count("InversePrize<Player2>") shouldBe 0
    game.testAgent(PLAYER3).count("InversePrize<Player3>") shouldBe 0

    game
        .testAgent(ADMIN)
        .runOperation(
            "EACH @Player(HAS =1 (RANK @Player { Score<Anyone(NOT @Player)> })) { DifferencePrize<@Player> }"
        )
    game.testAgent(PLAYER1).count("DifferencePrize<Player1>") shouldBe 0
    game.testAgent(PLAYER2).count("DifferencePrize<Player2>") shouldBe 1
    game.testAgent(PLAYER3).count("DifferencePrize<Player3>") shouldBe 1
  }

  @Test
  internal fun refinementDomainCanSupplyAnOmittedRankSelector() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Score : Owned<Player>
                CLASS Prize : Owned<Player>
                """,
                players = 3,
            )
        )
    game.testAgent(PLAYER1).runOperation("3 Score<Player1>")
    game.testAgent(PLAYER2).runOperation("2 Score<Player2>")
    game.testAgent(PLAYER3).runOperation("Score<Player3>")

    game
        .testAgent(ADMIN)
        .runOperation("EACH @Player(HAS =2 (RANK Me@Player { OWN[Score] })) { Prize<@Player> }")

    game.testAgent(PLAYER1).count("Prize<Player1>") shouldBe 0
    game.testAgent(PLAYER2).count("Prize<Player2>") shouldBe 1
    game.testAgent(PLAYER3).count("Prize<Player3>") shouldBe 0
  }

  @Test
  internal fun ownedCandidatesSupplyTheirHoldersToRankMetrics() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Score : Owned<Player>
                CLASS Candidate : Owned<Player>
                """,
                players = 2,
            )
        )
    val admin = game.testAgent(ADMIN)
    val p1 = game.testAgent(PLAYER1)
    val p2 = game.testAgent(PLAYER2)
    p1.runOperation("Score, Candidate")
    p2.runOperation("2 Score, Candidate")

    admin.runOperation(
        "EACH Me@Player(HAS =1 (RANK Me@Player { OWN[Score] })) { -Candidate<Me@Player> }"
    )

    p1.count("Candidate") shouldBe 1
    p2.count("Candidate") shouldBe 0
  }

  @Test
  internal fun rankKeepsItsCandidateScopeInsideARepresentedClassRefinement() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Kind {
                  CLASS FirstKind
                  CLASS SecondKind
                }
                CLASS Score<Class<Kind>>
                CLASS Prize<Class<Kind>>
                """
            )
        )
    val admin = game.testAgent(ADMIN)
    admin.runOperation("3 Score<Class<FirstKind>>, Score<Class<SecondKind>>")

    admin.runOperation(
        "Prize<Class<Kind>(HAS =1 (RANK @Class<Kind> { Score<Class<Kind>(NOT @Class)> }))>"
    )

    admin.count("Prize<Class<FirstKind>>") shouldBe 0
    admin.count("Prize<Class<SecondKind>>") shouldBe 1
  }
}
