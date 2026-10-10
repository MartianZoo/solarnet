package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AwardsTest : TfmTest() {
  @Test
  internal fun multiplayerOnlyStandardActionsAreAbsentInSoloGames() {
    game = TfmEngine.newGame(canonicalPremise(players = 1))

    val award = game.classTable.getClass(cn("Award"))
    game.classTable.allSubclasses(award).filterNot { it.abstract }.shouldBeEmpty()
    game.classTable.isInhabited(cn("ClaimMilestone")) shouldBe false
    game.classTable.isInhabited(cn("FundAward")) shouldBe false
    (cn("ClaimMilestone") in game.classTable.allClassNames) shouldBe false
    (cn("FundAward") in game.classTable.allClassNames) shouldBe false
    admin.assertCounts(
        1 to "AquiferProject",
    )
  }

  @Test
  internal fun incorporatorCountsOnlyCheapActiveAndAutomatedProjects() {
    game =
        TfmEngine.newGame(
            canonicalPremise(
                Utopia,
                players = 2,
            )
        )
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)

    p1.sneak("Incorporator, $Ecoline, $InterplanetaryCinematics")
    p2.sneak("$MiningGuild, $Mine")

    admin.runOperation("End FROM Phase")

    p1.assertCounts(
        0 to "FirstPlace<Player1, Incorporator>",
        0 to "SecondPlace<Player1, Incorporator>",
    )
    p2.assertCounts(
        1 to "FirstPlace<Player2, Incorporator>",
        0 to "SecondPlace<Player2, Incorporator>",
    )
  }

  @Test
  internal fun customAwardMetricsAreCountedForEachPlayer() {
    game = TfmEngine.newGame(canonicalPremise(Cimmeria, players = 3))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val p3 = game.testTfm(PLAYER3)

    p1.sneak("Forecaster, $ArtificialLake")
    p2.sneak("$Birds, $Algae")
    p1.count("CardFront(HAS requirement)") shouldBe 1
    p2.count("CardFront(HAS requirement)") shouldBe 2

    admin.runOperation("End FROM Phase")

    p1.assertCounts(
        1 to "SecondPlace<Player1, Forecaster>",
    )
    p2.assertCounts(
        1 to "FirstPlace<Player2, Forecaster>",
    )
    p3.assertCounts(
        0 to "FirstPlace<Player3, Forecaster>",
        0 to "SecondPlace<Player3, Forecaster>",
    )
  }

  @Test
  internal fun fundingPriceProgressesAndOnlyThreeAwardsCanBeFunded() {
    game = TfmEngine.newGame(canonicalPremise(players = 2))
    val p1 = game.testTfm(PLAYER1)
    p1.sneak("100 MC")

    val first =
        p1.runOperation("FundAward<Class<Landlord>>") {
          doTask("-MC / Owed")
        }
    first.expect("-8 MC")
    p1.assertCounts(92 to "MC", 1 to "Landlord")

    shouldThrow<LimitsException> {
      p1.runOperation("FundAward<Class<Landlord>>") {
        doTask("-MC / Owed")
      }
    }
    p1.assertCounts(92 to "MC", 1 to "Landlord")

    val second =
        p1.runOperation("FundAward<Class<Scientist>>") {
          doTask("-MC / Owed")
        }
    second.expect("-14 MC")
    p1.assertCounts(78 to "MC", 1 to "Scientist")

    val third =
        p1.runOperation("FundAward<Class<Thermalist>>") {
          doTask("-MC / Owed")
        }
    third.expect("-20 MC")
    p1.assertCounts(58 to "MC", 1 to "Thermalist", 3 to "Award")

    shouldThrow<DeadEndException> {
      p1.runOperation("FundAward<Class<Miner>>") {
        doTask("-MC / Owed")
      }
    }
    p1.assertCounts(58 to "MC", 3 to "Award", 0 to "Miner")
  }

  @Test
  internal fun zeroScoresCanEarnFirstAndSecondWhileUnfundedAwardsAreIgnored() {
    game = TfmEngine.newGame(canonicalPremise(players = 3))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val p3 = game.testTfm(PLAYER3)

    p1.sneak("Thermalist, Miner, Heat")

    admin.runOperation("End FROM Phase")

    p1.assertCounts(
        1 to "FirstPlace<Player1, Thermalist>",
        1 to "FirstPlace<Player1, Miner>",
        0 to "FirstPlace<Player1, Scientist>",
        10 to "VictoryPoint",
    )
    p2.assertCounts(
        1 to "SecondPlace<Player2, Thermalist>",
        1 to "FirstPlace<Player2, Miner>",
        0 to "FirstPlace<Player2, Scientist>",
        7 to "VictoryPoint",
    )
    p3.assertCounts(
        1 to "SecondPlace<Player3, Thermalist>",
        1 to "FirstPlace<Player3, Miner>",
        0 to "FirstPlace<Player3, Scientist>",
        7 to "VictoryPoint",
    )
  }

  @Test
  internal fun negativeBankerProductionCanEarnFirstAndSecond() {
    game = TfmEngine.newGame(canonicalPremise(players = 3))
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    val p3 = game.testTfm(PLAYER3)

    p1.sneak("Banker, PROD[-4 MC]")
    p2.sneak("PROD[-5 MC]")
    p3.sneak("PROD[-5 MC]")
    p1.assertProds(-4 to "MC")
    p2.assertProds(-5 to "MC")
    p3.assertProds(-5 to "MC")

    admin.runOperation("End FROM Phase")

    p1.assertCounts(1 to "FirstPlace<Player1, Banker>", 5 to "VictoryPoint")
    p2.assertCounts(1 to "SecondPlace<Player2, Banker>", 2 to "VictoryPoint")
    p3.assertCounts(1 to "SecondPlace<Player3, Banker>", 2 to "VictoryPoint")
  }

  @Test
  internal fun adminDrivenEndReturnsOnlyAfterAllVictoryPointsSettle() {
    game = TfmEngine.newGame(canonicalPremise())
    val p1 = game.testTfm(PLAYER1)
    val p2 = game.testTfm(PLAYER2)
    p1.runOperation("3 VictoryPoint, TerraformRating")
    p2.runOperation("Banker, PROD[1 MC]")
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE

    admin.runOperation("End FROM Phase")

    p1.assertCounts(4 to "VictoryPoint<Player1>", 0 to "Victory<Player1>")
    p2.assertCounts(5 to "VictoryPoint<Player2>", 1 to "Victory<Player2>")
    admin.count("End") shouldBe 1
    admin.count("FinalScoringPending") shouldBe 0
    admin.count("MeasureAward<Banker>") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun adminScoresNegativeCardsAndPlayedEventsWithPlayerAutoexecutionOff() {
    game = TfmEngine.newGame(canonicalPremise())
    val p1 = game.testTfm(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = game.testTfm(PLAYER2).also { it.autoExecPolicy = NONE }
    p1.sneak("$CorporateStronghold, $EarthCatapult, 5 TerraformRating")
    p2.sneak("$EnergyTapping, PlayedEvent<Class<$LargeConvoy>>, 3 TerraformRating")

    admin.runOperation("End FROM Phase")

    p1.count("VictoryPoint") shouldBe 5
    p2.count("VictoryPoint") shouldBe 4
    game.isIdle() shouldBe true
  }
}
