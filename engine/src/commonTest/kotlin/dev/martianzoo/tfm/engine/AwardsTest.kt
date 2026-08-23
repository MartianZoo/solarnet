package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.data.Player.Companion.PLAYER3
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.tfmAuthority
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AwardsTest : TfmTest() {
  @Test
  internal fun multiplayerOnlyStandardActionsAreAbsentInSoloGames() {
    game = Engine.newGame(canonicalPremise(players = 1))

    game.reader.tfmAuthority.awardDefinitions
        .filter { game.classTable.isActive(it.className) }
        .shouldBeEmpty()
    game.classTable.isActive(cn("ClaimMilestoneSA")) shouldBe false
    game.classTable.isActive(cn("FundAwardSA")) shouldBe false
    engine.assertCounts(
        1 to "PlayCardSA",
        1 to "AquiferSP",
    )
  }

  @Test
  internal fun incorporatorCountsOnlyCheapActiveAndAutomatedProjects() {
    game =
        Engine.newGame(
            canonicalPremise(
                Utopia,
                players = 2,
            )
        )
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    p1.godMode().sneak("Incorporator, $Ecoline, $InterplanetaryCinematics")
    p2.godMode().sneak("$MiningGuild, $Mine")

    engine.godMode().manual("EndPhase")

    p1.assertCounts(0 to "AwardTally<Player1, Incorporator>")
    p2.assertCounts(
        1 to "AwardTally<Player2, Incorporator>",
        1 to "FirstPlace<Player2, Incorporator>",
    )
  }

  @Test
  internal fun customAwardMetricsAreCountedForEachPlayer() {
    game = Engine.newGame(canonicalPremise(Cimmeria, players = 3))
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val p3 = game.tfm(PLAYER3)

    p1.godMode().sneak("Forecaster, $ArtificialLake")
    p2.godMode().sneak("$Birds, $Algae")
    p1.count("CardFront(HAS requirement)") shouldBe 1
    p2.count("CardFront(HAS requirement)") shouldBe 2

    engine.godMode().manual("EndPhase")

    p1.assertCounts(
        1 to "AwardTally<Player1, Forecaster>",
        1 to "SecondPlace<Player1, Forecaster>",
    )
    p2.assertCounts(
        2 to "AwardTally<Player2, Forecaster>",
        1 to "FirstPlace<Player2, Forecaster>",
    )
    p3.assertCounts(
        0 to "AwardTally<Player3, Forecaster>",
        0 to "FirstPlace<Player3, Forecaster>",
        0 to "SecondPlace<Player3, Forecaster>",
    )
  }

  @Test
  internal fun fundingPriceProgressesAndOnlyThreeAwardsCanBeFunded() {
    game = Engine.newGame(canonicalPremise(players = 2))
    val p1 = game.tfm(PLAYER1)
    p1.godMode().sneak("100 Megacredit")

    val first =
        p1.godMode().manual("UseAction<FundAwardSA, First>") {
          doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<>")
          doTask("Landlord")
        }
    first.expect("-8")
    p1.assertCounts(92 to "Megacredit", 1 to "Landlord")

    shouldThrow<LimitsException> {
      p1.godMode().manual("UseAction<FundAwardSA, First>") {
        doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<>")
        doTask("Landlord")
      }
    }
    p1.assertCounts(92 to "Megacredit", 1 to "Landlord")

    val second =
        p1.godMode().manual("UseAction<FundAwardSA, First>") {
          doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<>")
          doTask("Scientist")
        }
    second.expect("-14")
    p1.assertCounts(78 to "Megacredit", 1 to "Scientist")

    val third =
        p1.godMode().manual("UseAction<FundAwardSA, First>") {
          doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<>")
          doTask("Thermalist")
        }
    third.expect("-20")
    p1.assertCounts(58 to "Megacredit", 1 to "Thermalist", 3 to "Award")

    shouldThrow<NotNowException> {
      p1.godMode().manual("UseAction<FundAwardSA, First>") { doTask("Miner") }
    }
  }

  @Test
  internal fun zeroTalliesCanEarnFirstAndSecondWhileUnfundedAwardsAreIgnored() {
    game = Engine.newGame(canonicalPremise(players = 3))
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val p3 = game.tfm(PLAYER3)

    p1.godMode().sneak("Thermalist, Miner, Heat")

    engine.godMode().manual("EndPhase")

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
    game = Engine.newGame(canonicalPremise(players = 3))
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    val p3 = game.tfm(PLAYER3)

    p1.godMode().sneak("Banker, PROD[-4]")
    p2.godMode().sneak("PROD[-5]")
    p3.godMode().sneak("PROD[-5]")
    p1.assertProds(-4 to "Megacredit")
    p2.assertProds(-5 to "Megacredit")
    p3.assertProds(-5 to "Megacredit")

    engine.godMode().manual("EndPhase")

    p1.assertCounts(1 to "FirstPlace<Player1, Banker>", 5 to "VictoryPoint")
    p2.assertCounts(1 to "SecondPlace<Player2, Banker>", 2 to "VictoryPoint")
    p3.assertCounts(1 to "SecondPlace<Player3, Banker>", 2 to "VictoryPoint")
  }

  @Test
  internal fun awardPointsArePaidBeforeMultiplayerVictoryIsChecked() {
    game = Engine.newGame(canonicalPremise())
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)
    p1.godMode().sneak("3 VictoryPoint")
    p2.godMode().sneak("Banker, PROD[Megacredit]")

    engine.godMode().manual("EndPhase")

    p1.assertCounts(0 to "Victory<Player1>")
    p2.assertCounts(5 to "VictoryPoint<Player2>", 1 to "Victory<Player2>")
  }
}
