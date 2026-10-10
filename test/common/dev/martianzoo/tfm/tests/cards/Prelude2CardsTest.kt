package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Player
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class Prelude2CardsTest : CardTest() {
  // https://boardgamegeek.com/thread/3412262/i-bit-confused-on-combining-this-and-prelude-1-int
  @Test
  internal fun `Prelude and Prelude 2 share one setup and phase`() {
    newGame(PreludeExpansion, Prelude2CardPack)

    admin.phase("Prelude")

    admin.count("PreludePhase") shouldBe 1
    p1.count("PreludeCard") shouldBe 2
    requireP2().count("PreludeCard") shouldBe 2
  }

  @Test
  internal fun `Early Colonization advances every active track twice and ignores inactive tracks`() {
    val colonyTiles = testColonyTiles(2, "Luna")
    newGame(PreludeExpansion, Prelude2CardPack, ColoniesExpansion, colonyTiles = colonyTiles)
    admin.runOperation("2 ColonyProduction<Luna>")

    p1.runOperation("$EarlyColonization") { doTask("Colony<Luna>") }

    colonyTiles.forEach { tile ->
      admin.count("ColonyProduction<$tile>") shouldBe if (tile == cn("Luna")) 5 else 3
    }
    p1.count("Energy") shouldBe 3

    admin.phase("Production")
    with(TfmWorkflow.Stepwise(agents)) {
      solarPhase()
      coloniesSolarPhase()
    }
    colonyTiles.forEach { tile ->
      admin.count("ColonyProduction<$tile>") shouldBe if (tile == cn("Luna")) 6 else 4
    }
    admin.assertCounts(
        0 to "ColonyProduction<Miranda>",
        0 to "ColonyProduction<Titan>",
        0 to "ColonyProduction<Enceladus>",
    )
  }

  @Test
  internal fun `Early Colonization is unplayable when its owner has no legal colony`() {
    val colonyTiles = testColonyTiles(2)
    newGame(PreludeExpansion, Prelude2CardPack, ColoniesExpansion, colonyTiles = colonyTiles)
    admin.phase("Prelude")
    p1.runOperation(
        "PreludeCard, Colony<Luna>, Colony<Ceres>, Colony<Triton>, " +
            "Colony<Ganymede>, Colony<Callisto>"
    )

    shouldThrow<DependencyException> { p1.playPrelude(EarlyColonization) }

    p1.count("$EarlyColonization") shouldBe 0
    p1.count("Energy") shouldBe 0
  }

  @Test
  internal fun `Early Colonization fizzles when an active colony is at position five`() {
    earlyColonizationAtPosition(5)
  }

  @Test
  internal fun `Early Colonization fizzles when an active colony is at position six`() {
    earlyColonizationAtPosition(6)
  }

  private fun earlyColonizationAtPosition(position: Int) {
    newGame(PreludeExpansion, Prelude2CardPack, ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(CrediCor, 0)
    admin.phase("Prelude")
    p1.playPrelude(BoardOfDirectors)
    p1.playPrelude(Donation)
    admin.phase("Action")
    repeat(position - 1) {
      admin.phase("ColoniesSolar")
      admin.phase("Action")
    }
    shouldThrow<LimitsException> {
      p1.cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        p1.playPrelude(EarlyColonization) { doTask("Colony<Ceres>") }
      }
    }
    p1.count("Energy") shouldBe 0
    admin.count("ColonyProduction<Luna>") shouldBe position
    p1.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          doTask("-PreludeCard")
        }
        .expect("3 MC")
    p1.assertCounts(0 to "$EarlyColonization", 0 to "Colony<Ceres>")
  }

  @Test
  internal fun `Double Down copies Industrial Complex's direct benefit`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack)
    admin.phase("Prelude")
    p1.runOperation("36 MC, PROD[-5 MC], 2 PreludeCard")
    p1.playPrelude(IndustrialComplex)
    p1.runOperation("PROD[-Steel]")

    p1.playPrelude(DoubleDown) { doTask("CopyPrelude<$IndustrialComplex>") }
        .expect("-18 MC, PROD[Steel]")
  }

  @Test
  internal fun `Recession applies each opponent loss as much as possible`() {
    newGame(PreludeExpansion, Prelude2CardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.testTfm(PLAYER3)
    p2.runOperation("4 MC, PROD[-4 MC]")
    p3.runOperation("5 MC, PROD[2 MC]")
    admin.phase("Prelude")

    p1.playPrelude(Recession)

    p1.count("$Recession") shouldBe 1
    p1.count("MC") shouldBe 10
    p2.count("MC") shouldBe 0
    p3.count("MC") shouldBe 0
    p2.assertProds(-5 to "MC")
    p3.assertProds(1 to "MC")
  }

  @Test
  internal fun `Recession is unplayable when an opponent is at minimum mc production`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    val p2 = requireP2()
    admin.phase("Prelude")
    p1.playPrelude(Donation)
    p2.playPrelude(Loan)
    p1.playPrelude(BoardOfDirectors)
    p2.playPrelude(Biolab)
    admin.phase("Action")
    p1.runOperation("ProjectCard")
    p1.sellPatents(1)
    p2.playProject(BlackPolarDust, 15) { placeTile(2, 6) }
    p2.playProject(PeroxidePower, 7)
    p2.assertProds(-5 to "MC")
    val p1MoneyBefore = p1.count("MC")
    val directorsBefore = p1.count("Director<$BoardOfDirectors>")

    shouldThrow<LimitsException> {
      p1.cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        p1.playPrelude(Recession)
      }
    }

    p1.count("$Recession") shouldBe 0
    p1.count("MC") shouldBe p1MoneyBefore
    p1.count("Director<$BoardOfDirectors>") shouldBe directorsBefore
    p2.count("MC") shouldBe 8
    p2.assertProds(-5 to "MC")
  }

  @Test
  internal fun `Recession fizzles when an opponent has minimum money production`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack)
    p1.playCorp(MonsInsurance, 0) {
      p1.selectTask(
          "EACH Other@Player(NOT Player1) { " +
              "-2 Production<Other@Player, Class<MC>>! BY Other@Player }"
      )
      autoExecNow()
    }
    val p2 = requireP2()
    p2.playCorp(CrediCor, 0)
    admin.phase("Prelude")
    p2.playPrelude(Loan)
    p2.playPrelude(BiosphereSupport)
    p2.assertProds(-5 to "MC")
    shouldThrow<LimitsException> { p1.playPrelude(Recession) }

    p1.startTurn()
    p1.doTask("-PreludeCard").expect("15 MC, 0 MC<Player2>, PROD[0 MC<Player2>]")
    p2.assertProds(-5 to "MC")
  }

  @Test
  internal fun `Recession ordering determines which victim receives partial Mons compensation`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, players = 5)
    val playerActors = Player.players(5)
    val players = playerActors.map { game.testTfm(it) }
    val mons = players[1]
    val victims = players.drop(2)
    val victimActors = playerActors.drop(2)
    mons.runOperation("$MonsInsurance")
    mons.runOperation("-27 MC")
    victims.forEach { it.runOperation("5 MC") }
    admin.phase("Prelude")
    players.drop(1).forEach { it.autoExecPolicy = NONE }
    p1.autoExecPolicy = CONCRETE

    fun OperationScope.settle(
        victim: Player,
        secondPayout: Int = 3,
    ) {
      doTask("-5 MC<$victim>")
      p1.selectTask("3 MC<$victim FROM Player2>.")
      mons.doTask("3 MC<$victim> FROM MC<Player2>")
      doTask("PROD[-1 MC<$victim>]")
      p1.selectTask("3 MC<$victim FROM Player2>.")
      mons.doTask("$secondPayout MC<$victim> FROM MC<Player2>")
    }

    p1.playPrelude(Recession) {
      p1.autoExecPolicy = NONE
      doTask("EACH Other@Player(NOT Player1) { -5 MC<Other@Player>., PROD[-1 MC<Other@Player>] }")
      doTask("-5 MC<Player2>")
      doTask("PROD[-1 MC<Player2>]")
      settle(victimActors[0])
      settle(victimActors[2])
      settle(victimActors[1], secondPayout = 1)
      doTask("10 MC<Player1>")
    }

    // https://boardgamegeek.com/thread/3334230/article/44565901#44565901
    mons.count("MC") shouldBe 0
    victims.map { it.count("MC") } shouldBe listOf(6, 4, 6)
    mons.count("PreludeCard") shouldBe 2

    mons.autoExecPolicy = CONCRETE
    shouldThrow<LimitsException> { mons.playPrelude(MainBeltAsteroids) }
    shouldThrow<LimitsException> { mons.playPrelude(BusinessEmpire) }

    mons.startTurn()
    mons.doTask("-PreludeCard")
    mons.autoExecPolicy = EAGER
    mons.playPrelude(BusinessEmpire)

    mons.count("MC") shouldBe 9
    mons.count("$BusinessEmpire") shouldBe 1
    mons.count("PreludeCard") shouldBe 0
  }

  @Test
  internal fun `Planetary Alliance makes both tagged searches`() {
    newGame(PreludeExpansion, Prelude2CardPack, VenusNextExpansion)
    admin.phase("Prelude")
    p1.playPrelude(PlanetaryAlliance)

    p1.count("ProjectCard") shouldBe 2
    p1.count("TerraformRating") shouldBe 22
  }

  // https://boardgamegeek.com/thread/3154781/do-event-tags-count-for-sagitta
  @Test
  internal fun `Sagitta treats the event icon as an additional printed tag`() {
    newGame(
        GameConfig(
            "PreludeExpansion, Prelude2CardPack, CorporateEraExpansion, " +
                "ColoniesExpansion, PromoCardPack, SagittaFrontierServices, " +
                testColonyTiles(2).joinToString(),
            "Player1",
            "Player2",
        )
    )
    val p2 = requireP2()

    p1.runOperation("$SagittaFrontierServices")
    p1.count("MC") shouldBe 35

    p1.runOperation("$AtmoCollectors") { addCardResources(AtmoCollectors) }
    p1.count("MC") shouldBe 39

    p2.runOperation("7 MC")
    p1.runOperation("$Sabotage") { doTask("-7 MC<Player2>") }
    p1.count("MC") shouldBe 40

    p1.runOperation("$Mine")
    p1.count("MC") shouldBe 41

    p1.runOperation("$Research")
    p1.runOperation("$SmallAsteroid")
    p1.count("MC") shouldBe 41
  }

  @Test
  internal fun `Sagitta ignores cards played by another player`() {
    newGame(PreludeExpansion, Prelude2CardPack, players = 2)
    val p2 = requireP2()
    p1.runOperation("$SagittaFrontierServices")
    val startingMoney = p1.count("MC")
    admin.phase("Prelude")

    p2.playPrelude(SpaceLanes)

    p1.count("MC") shouldBe startingMoney
  }

  @Test
  internal fun `Sagitta rewards a wild-only card as tagless`() {
    newGame(PreludeExpansion, Prelude2CardPack, FakeStuffBundle)
    p1.playCorp(SagittaFrontierServices, 1)
    admin.phase("Action")

    p1.playProject(FakeResearchCoordination, 4).expect("0 MC")
  }

  // https://www.reddit.com/r/TerraformingMarsGame/comments/1kgksgg
  @Test
  internal fun `A prelude remains playable when its global parameter is already maximized`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    admin.phase("Prelude")
    val oceans = p1.list("WaterArea").take(9).joinToString { "OceanTile<$it>" }
    p1.runOperation("5 MC, 19 TemperatureStep, $oceans")
    val startingMoney = p1.count("MC")

    p1.playPrelude(HugeAsteroid)

    admin.count("TemperatureStep") shouldBe 19
    p1.count("MC") shouldBe startingMoney - 5
    p1.count("$HugeAsteroid") shouldBe 1
  }

  @Test
  internal fun `political preludes grant their ongoing and delegate benefits`() {
    newGame(PreludeExpansion, Prelude2CardPack, TurmoilExpansion)
    val startingTr = p1.count("TerraformRating")
    val startingMoney = p1.count("MC")
    val startingProduction = p1.production(cn("MC"))

    p1.runOperation("$HighCircles") {
      doTask("2 PartyDelegate<Unity>")
    }
    p1.count("ProjectCard") shouldBe 1

    p1.runOperation("$CorridorsOfPower")
    p1.runOperation("PartyLeader<Scientists>")
    p1.count("ProjectCard") shouldBe 2

    p1.runOperation("$RiseToPower") {
      doTask("PartyDelegate<Scientists>")
      doTask("PartyDelegate<Reds>")
      doTask("PartyDelegate<Greens>")
    }
    admin.runOperation("MeasureInfluence<Player1>")

    p1.count("TerraformRating") shouldBe startingTr + 2
    p1.count("MC") shouldBe startingMoney + 4
    p1.count("HighCirclesInfluence") shouldBe 1
    p1.production(cn("MC")) shouldBe startingProduction + 3
    p1.assertCounts(
        2 to "PartyDelegate<Unity>",
        1 to "PartyDelegate<Scientists>",
        1 to "PartyDelegate<Reds>",
        1 to "PartyDelegate<Greens>",
    )
  }

  @Test
  internal fun `representation and envoys apply benefits once per colony occurrence`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        TurmoilExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2, "Luna", "Io"),
    )
    p1.runOperation("2 Colony<Luna>")
    requireP2().runOperation("Colony<Io>")
    val startingMoney = p1.count("MC")

    p1.runOperation("$ColonialRepresentation")
    p1.runOperation("$ColonialEnvoys") {
      doTask("PartyDelegate<Scientists>")
      doTask("PartyDelegate<Greens>")
    }
    admin.runOperation("MeasureInfluence<Player1>")

    p1.count("MC") shouldBe startingMoney + 6
    p1.count("ColonialRepresentationInfluence") shouldBe 1
    p1.count("PartyDelegate<Scientists>") shouldBe 1
    p1.count("PartyDelegate<Greens>") shouldBe 1
  }
}
