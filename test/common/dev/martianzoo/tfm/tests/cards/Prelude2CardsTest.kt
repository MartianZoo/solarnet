package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class Prelude2CardsTest : CardTest() {
  @Test
  internal fun `Nirgal pays nothing for milestones and awards`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("$NirgalEnterprises, 16 ProjectCard")
    val startingMoney = p1.count("MC")
    admin.phase("Action")

    p1.claimMilestone(cn("Planner")).expect("Milestone")
    p1.fundAward(cn("Landlord"), 0).expect("Award")

    p1.count("MC") shouldBe startingMoney
  }

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
  internal fun `Board of Directors remains in play and can play another prelude`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    admin.phase("Prelude")
    p1.runOperation("12 MC, 2 PreludeCard")
    p1.playPrelude(BoardOfDirectors)
    admin.phase("Action")

    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(Recession)
    }

    p1.count("Director<$BoardOfDirectors>") shouldBe 3
    p1.count("$Recession") shouldBe 1
  }

  @Test
  internal fun `Sky Docks discounts a project played through Board of Directors and Ecology Experts`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    admin.phase("Action")
    p1.runOperation("13 MC, PreludeCard, ProjectCard, $BoardOfDirectors, $SkyDocks")

    with(p1) {
      cardAction1(BoardOfDirectors) {
            doTask("-12 MC")
            playPrelude(EcologyExperts) {
              playProject(DustSeals, 1)
            }
          }
          .expect("-13 MC")
    }

    p1.assertCounts(1 to "$EcologyExperts", 1 to "$DustSeals")
  }

  @Test
  internal fun `Terraforming Deal pays two per TR step`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("20 MC, $TerraformingDeal")
    admin.phase("Action")
    val startingTr = p1.count("TerraformRating")
    val startingMoney = p1.count("MC")

    p1.runOperation("2 TerraformRating")

    p1.count("TerraformRating") shouldBe startingTr + 2
    p1.count("MC") shouldBe startingMoney + 4
  }

  @Test
  internal fun `World Government Advisor lets its owner choose rather than the start player`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    val p2 = requireP2()
    p2.runOperation("$WorldGovernmentAdvisor")
    admin.phase("Action")
    val startingTr = p2.count("TerraformRating")

    p2.cardAction1(WorldGovernmentAdvisor) { wgt("TemperatureStep") }

    admin.count("TemperatureStep") shouldBe 1
    p2.count("TerraformRating") shouldBe startingTr
  }

  @Test
  internal fun `World Government Advisor works with Venus while World Government is disabled`() {
    newGame(
        GameConfig(
            "PreludeExpansion, Prelude2CardPack, VenusNextExpansion, -WorldGovernmentRule",
            "Player1",
            "Player2",
        )
    )
    val p2 = requireP2()
    p1.runOperation("$WorldGovernmentAdvisor")
    p2.runOperation("$Aphrodite")
    admin.phase("Action")
    val startingTr = p1.count("TerraformRating")
    val aphroditeMoney = p2.count("MC")

    p1.cardAction1(WorldGovernmentAdvisor) { wgt("VenusStep") }

    admin.count("VenusStep") shouldBe 1
    p1.count("TerraformRating") shouldBe startingTr
    p2.count("MC") shouldBe aphroditeMoney + 2
  }

  @Test
  internal fun `World Government Advisor triggers effects that observe anyone placing an ocean`() {
    newGame(
        GameConfig(
            "PreludeExpansion, Prelude2CardPack, LakefrontResorts, " +
                "Hydrologist, Builder, Engineer",
            "Player1",
            "Player2",
        )
    )
    val p2 = requireP2()
    p1.runOperation("$WorldGovernmentAdvisor")
    p2.runOperation("$ArcticAlgae, $LakefrontResorts")
    admin.phase("Action")
    val startingPlants = p2.count("Plant")
    val startingMoneyProduction = p2.production(cn("MC"))

    p1.cardAction1(WorldGovernmentAdvisor) { wgt("OceanTile<Tharsis_1_2>") }

    p2.count("Plant") shouldBe startingPlants + 2
    p2.production(cn("MC")) shouldBe startingMoneyProduction + 1
    p1.count("OceanCredit") shouldBe 0
  }

  @Test
  internal fun `World Government Advisor owner places the neutral ocean awarded at zero degrees`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("$WorldGovernmentAdvisor")
    admin.runOperation("14 TemperatureStep")
    admin.phase("Action")
    val startingTr = p1.count("TerraformRating")

    p1.cardAction1(WorldGovernmentAdvisor) {
      wgt("TemperatureStep")
      doTask("OceanTile<Tharsis_1_2> BY Admin")
    }

    admin.count("TemperatureStep") shouldBe 15
    admin.count("OceanTile<Tharsis_1_2>") shouldBe 1
    p1.count("TerraformRating") shouldBe startingTr
  }

  @Test
  internal fun `Ecotec rewards both of its starting tags`() {
    newGame(PreludeExpansion, Prelude2CardPack)

    p1.runOperation("$Ecotec") {
      doTask("Plant")
      doTask("Plant")
    }

    p1.count("Plant") shouldBe 2
  }

  @Test
  internal fun `Spire draws four cards and discards three as its first action`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("$Spire")
    admin.phase("Action")

    p1.stdAction("DoRequiredActionsAction")

    p1.count("ProjectCard") shouldBe 1
    p1.count("RequiredAction") shouldBe 0
  }

  @Test
  internal fun `Spire counts the derived event tag toward its two-tag requirement`() {
    newGame(PreludeExpansion, Prelude2CardPack, CorporateEraExpansion)
    p1.runOperation("$Spire")
    val startingScience = p1.count("Science<$Spire>")

    p1.runOperation("$BusinessContacts")
    p1.count("Science<$Spire>") shouldBe startingScience + 1

    p1.runOperation("$MineralDeposit")
    p1.count("Science<$Spire>") shouldBe startingScience + 1
  }

  @Test
  internal fun `Spire science pays two toward standard projects`() {
    newGame(PreludeExpansion, Prelude2CardPack, CorporateEraExpansion)
    p1.runOperation("$Spire, 20 MC")
    val startingScience = p1.count("Science<$Spire>")
    p1.runOperation("$Research")
    p1.count("Science<$Spire>") shouldBe startingScience + 1
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction")

    p1.stdProject(
            "PowerPlantProject",
            payment = {
              doTask("PayFromCard<$Spire> FROM Science<$Spire>")
              doTask("Pay<Class<MC>> FROM MC / Owed<>")
            },
        )
        .expect("-Science<$Spire>, -9 MC, PROD[Energy]")
  }

  @Test
  internal fun `Spire science cannot pay other debts`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("$Spire, Science<$Spire>")

    shouldThrow<TaskException> {
      p1.runOperation("10 Owed<>") { doTask("PayFromCard<$Spire> FROM Science<$Spire>") }
    }
  }

  @Test
  internal fun `Selling patents does not offer Spire science for later debts`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("$Spire, Science<$Spire>, ProjectCard")
    p1.runOperation("-RequiredAction!")
    admin.phase("Action")
    p1.sellPatents(1)

    shouldThrow<TaskException> {
      p1.runOperation("10 Owed<>") { doTask("PayFromCard<$Spire> FROM Science<$Spire>") }
    }
  }

  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  internal fun `Suitable Infrastructure pays once for each action`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("$SuitableInfrastructure")
    admin.phase("Prelude")
    val beforeTwoProductions = p1.count("MC")

    p1.playPrelude(DomeFarming)
    p1.count("MC") shouldBe beforeTwoProductions + 2

    p1.runOperation("50 MC")
    admin.phase("Action")
    val startingMoney = p1.count("MC")

    p1.runOperation("NewTurn") {
      doTask("UseAction<UseStandardProjectAction, Action1>")
      doTask("UseAction<PowerPlantProject, Action1>")
      doTask("Pay<Class<MC>> FROM MC / Owed<>")
    }
    p1.count("MC") shouldBe startingMoney - 9

    p1.runOperation("SecondAction") {
      doTask("UseAction<UseStandardProjectAction, Action1>")
      doTask("UseAction<PowerPlantProject, Action1>")
      doTask("Pay<Class<MC>> FROM MC / Owed<>")
    }

    p1.count("MC") shouldBe startingMoney - 18
  }

  @Test
  internal fun `Suitable Infrastructure covers production inside required actions`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("$SuitableInfrastructure, $ValleyTrust")
    admin.phase("Action")
    val startingMoney = p1.count("MC")

    p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(DomeFarming) }

    p1.assertProds(2 to "MC", 1 to "Plant")
    p1.count("MC") shouldBe startingMoney + 2
  }

  @Test
  internal fun `Suitable Infrastructure pays separately for Head Start's nested actions`() {
    newGame(PreludeExpansion, Prelude2CardPack, FakeStuffBundle)
    p1.runOperation("$SuitableInfrastructure")
    admin.phase("Prelude")
    p1.runOperation("30 MC, PreludeCard")
    val startingMoney = p1.count("MC")

    p1.turn {
      playPrelude(FakeHeadStart) {
        useStdProject("PowerPlantProject")
        useStdProject("PowerPlantProject")
      }
    }

    p1.assertProds(2 to "Energy")
    p1.count("MC") shouldBe startingMoney - 18
  }

  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  internal fun `Suitable Infrastructure covers production from a corporation played during setup`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, VenusNextExpansion)
    p1.runOperation("$SuitableInfrastructure")
    admin.phase("Prelude")
    p1.runOperation("42 MC, PreludeCard")
    val startingMoney = p1.count("MC")

    p1.playPrelude(Merger) { p1.playCorp(Manutech) }

    p1.assertProds(1 to "Steel")
    p1.count("MC") shouldBe startingMoney - 5
  }

  @Test
  internal fun `Focused Organization may gain a different resource than it spends`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("$FocusedOrganization") { doTask("Steel") }
    admin.phase("Action")

    p1.cardAction1(FocusedOrganization) { doTask("Plant") }

    p1.count("ProjectCard") shouldBe 1
    p1.count("Steel") shouldBe 0
    p1.count("Plant") shouldBe 1
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

    shouldThrowAny { p1.playPrelude(EarlyColonization) }

    p1.count("$EarlyColonization") shouldBe 0
    p1.count("Energy") shouldBe 0
  }

  @Test
  internal fun `Early Colonization is unplayable when an active track cannot advance twice`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    admin.phase("Prelude")
    p1.runOperation("PreludeCard")
    admin.runOperation("4 ColonyProduction<Luna>")

    shouldThrowAny {
      p1.playPrelude(EarlyColonization) { doTask("Colony<Ceres>") }
    }

    p1.assertCounts(0 to "$EarlyColonization", 0 to "Energy", 0 to "Colony<Ceres>")
    admin.count("ColonyProduction<Luna>") shouldBe 5
  }

  @Test
  internal fun `Industrial Complex raises only production tracks below one`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.runOperation("18 MC, PROD[-5 MC], PROD[2 Titanium], PROD[Plant]")

    p1.runOperation("$IndustrialComplex")

    p1.count("MC") shouldBe 0
    p1.assertProds(
        1 to "MC",
        1 to "Steel",
        2 to "Titanium",
        1 to "Plant",
        1 to "Energy",
        1 to "Heat",
    )
  }

  @Test
  internal fun `Industrial Complex raises production tracks below two in Quick Start`() {
    newGame(
        GameConfig("PreludeExpansion, Prelude2CardPack, QuickStartVariant", "Player1", "Player2")
    )
    p1.runOperation("18 MC, PROD[-6 MC], PROD[2 Titanium], PROD[-Plant]")

    p1.runOperation("$IndustrialComplex")

    p1.count("MC") shouldBe 0
    p1.assertProds(
        2 to "MC",
        2 to "Steel",
        3 to "Titanium",
        2 to "Plant",
        2 to "Energy",
        2 to "Heat",
    )
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
      doTask("3 MC<$victim> FROM MC<Player2>")
      doTask("PROD[-1 MC<$victim>]")
      doTask("$secondPayout MC<$victim> FROM MC<Player2>")
    }

    p1.playPrelude(Recession) {
      p1.autoExecPolicy = NONE
      doTask("EACH Player(NOT Player1) { -5 MC<Owner>., PROD[-1 MC<Owner>] }")
      doTask("-5 MC<Player2>")
      doTask("PROD[-1 MC<Player2>]")
      doTask("3 MC<Player2> FROM MC<Player2>")
      doTask("3 MC<Player2> FROM MC<Player2>")
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
  internal fun `Cloud Tourism uses the lower Earth and Venus tag count`() {
    newGame(PreludeExpansion, Prelude2CardPack, VenusNextExpansion, CorporateEraExpansion)
    p1.runOperation(
        "$Sponsors, $EarthOffice, $VenusGovernor, $VenusWaystation, $ForcedPrecipitation"
    )
    val firstStartingProduction = p1.production(cn("MC"))
    p1.runOperation("$CloudTourism")
    p1.production(cn("MC")) shouldBe firstStartingProduction + 2

    newGame(PreludeExpansion, Prelude2CardPack, VenusNextExpansion, CorporateEraExpansion)
    p1.runOperation("$Sponsors, $EarthOffice, $EarthCatapult, $AcquiredCompany, $MediaGroup")
    p1.runOperation("$ForcedPrecipitation")
    val secondStartingProduction = p1.production(cn("MC"))
    p1.runOperation("$CloudTourism")
    p1.production(cn("MC")) shouldBe secondStartingProduction + 2
  }

  @Test
  internal fun `Floating Refinery counts its own Venus tag`() {
    newGame(PreludeExpansion, Prelude2CardPack, VenusNextExpansion)
    p1.runOperation("$ForcedPrecipitation")

    p1.runOperation("$FloatingRefinery")

    p1.count("Floater<$FloatingRefinery>") shouldBe 2
  }

  // https://boardgamegeek.com/thread/3154781/do-event-tags-count-for-sagitta
  @Test
  internal fun `Sagitta treats the event icon as an additional printed tag`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        CorporateEraExpansion,
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
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

  // https://boardgamegeek.com/thread/3577088/article/46624092#46624092
  @Test
  internal fun `Unexpected Application requires the discard before raising Venus`() {
    newGame(PreludeExpansion, Prelude2CardPack, VenusNextExpansion)
    p1.runOperation("4 MC, 3 VenusStep, ProjectCard")
    admin.phase("Action")

    shouldThrowAny { p1.playProject(UnexpectedApplication, 4) }

    p1.assertCounts(
        4 to "MC",
        3 to "VenusStep",
        1 to "ProjectCard",
        0 to "$UnexpectedApplication",
    )
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
  internal fun `Venus Orbital Survey follows both reveal outcomes`() {
    newGame(PreludeExpansion, Prelude2CardPack, VenusNextExpansion)
    p1.runOperation("$VenusOrbitalSurvey, 3 MC")
    admin.phase("Action")

    p1.cardAction1(VenusOrbitalSurvey) {
      // The two modeled offers are indistinguishable; identify either before choosing the free
      // tagged outcome, then buy the other.
      doTask("SearchForCard<TagFilter<Class<VenusTag>>>", tasks.extract { it }.first().id)
      p1.buyCards(1)
    }

    p1.count("ProjectCard") shouldBe 2
    p1.count("MC") shouldBe 0
  }

  @Test
  internal fun `Venus Shuttles action cost is reduced by Venus tags`() {
    newGame(PreludeExpansion, Prelude2CardPack, VenusNextExpansion)
    p1.runOperation(
        "$VenusGovernor, $VenusWaystation, $ForcedPrecipitation, $VenusMagnetizer, 20 MC"
    )
    p1.runOperation("$VenusShuttles") { addCardResources(ForcedPrecipitation) }
    admin.phase("Action")
    val startingMoney = p1.count("MC")
    val startingVenus = admin.count("VenusStep")

    p1.cardAction1(VenusShuttles)

    p1.count("MC") shouldBe startingMoney - 6
    admin.count("VenusStep") shouldBe startingVenus + 1
  }

  @Test
  internal fun `Turmoil linked projects are unavailable without Turmoil`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    admin.phase("Action")
    p1.runOperation("10 MC, ProjectCard")

    shouldThrow<DeadEndException> { p1.playProject(SummitLogistics, 10) }
  }

  @Test
  internal fun `Summit Logistics counts planetary tags without Venus Next`() {
    val game =
        newGame(
            PreludeExpansion,
            Prelude2CardPack,
            TurmoilExpansion,
            CorporateEraExpansion,
            ColoniesExpansion,
            colonyTiles = testColonyTiles(1, "Luna"),
        )
    (cn("VenusTag") in game.classTable.allClassNames) shouldBe false
    p1.runOperation("$EarthOffice, $VestaShipyard, Colony<Luna>")
    val startingMoney = p1.count("MC")
    val startingCards = p1.count("ProjectCard")

    p1.runOperation("$SummitLogistics")

    p1.count("MC") shouldBe startingMoney + 3
    p1.count("ProjectCard") shouldBe startingCards + 2
  }

  @Test
  internal fun `party requirements accept two delegates`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        TurmoilExpansion,
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    admin.phase("Action")
    p1.runOperation("10 MC, ProjectCard")

    shouldThrow<RequirementException> { p1.playProject(SummitLogistics, 10) }

    p1.runOperation("PartyDelegate<Scientists>, PartyDelegate<Scientists>")
    p1.playProject(SummitLogistics, 10)

    p1.count("ProjectCard") shouldBe 2
  }

  @Test
  internal fun `Red Appeasement passes and requires every other player to remain active`() {
    newGame(PreludeExpansion, Prelude2CardPack, TurmoilExpansion)
    admin.phase("Action")
    p1.runOperation("ProjectCard, PartyDelegate<Reds>, PartyDelegate<Reds>")
    requireP2().runOperation("Pass")

    shouldThrow<RequirementException> { p1.playProject(RedAppeasement, 0) }

    newGame(PreludeExpansion, Prelude2CardPack, TurmoilExpansion)
    admin.phase("Action")
    p1.runOperation("ProjectCard, PartyDelegate<Reds>, PartyDelegate<Reds>")
    val startingProduction = p1.production(cn("MC"))

    p1.playProject(RedAppeasement, 0)

    p1.production(cn("MC")) shouldBe startingProduction + 2
    p1.count("Pass") shouldBe 1
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
  internal fun `Summit Logistics and GHG Shipment count the supported resources`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        TurmoilExpansion,
        CorporateEraExpansion,
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2, "Luna", "Io"),
    )
    p1.runOperation("$EarthOffice, $VestaShipyard, $VenusGovernor, Colony<Luna>, Colony<Io>")
    val startingMoney = p1.count("MC")
    val startingCards = p1.count("ProjectCard")

    p1.runOperation("$SummitLogistics")

    p1.count("MC") shouldBe startingMoney + 6
    p1.count("ProjectCard") shouldBe startingCards + 2

    p1.runOperation("$ForcedPrecipitation, 3 Floater<$ForcedPrecipitation>")
    val startingHeat = p1.count("Heat")
    val startingHeatProduction = p1.production(cn("Heat"))

    p1.runOperation("$GhgShipment")

    p1.count("Heat") shouldBe startingHeat + 3
    p1.production(cn("Heat")) shouldBe startingHeatProduction + 1
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

  @Test
  internal fun `Special Permit steals four plants from one player`() {
    newGame(PreludeExpansion, Prelude2CardPack, TurmoilExpansion)
    val p2 = requireP2()
    p2.runOperation("4 Plant")

    p1.runOperation("$SpecialPermit")

    p1.count("Plant") shouldBe 4
    p2.count("Plant") shouldBe 0
  }

  @Test
  internal fun `Frontier Town repeats only its printed placement bonus`() {
    newGame(PreludeExpansion, Prelude2CardPack, TurmoilExpansion)
    p1.runOperation("PROD[Energy]")

    p1.runOperation("$FrontierTown") { placeTile(4, 2) }

    p1.assertProds(0 to "Energy")
    p1.count("Plant") shouldBe 3
  }

  @Test
  internal fun `WG Project gains and plays one prelude`() {
    newGame(PreludeExpansion, Prelude2CardPack, TurmoilExpansion)
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman, 9 MC, ProjectCard")
    admin.phase("Action")

    p1.playProject(WgProject, 9) {
      p1.playPrelude(HighCircles) {
        doTask("2 PartyDelegate<Unity>")
      }
    }

    p1.count("PreludeCard") shouldBe 2
    p1.count("$HighCircles") shouldBe 1
  }
}
