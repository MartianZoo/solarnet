package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.Agent.OperationBody
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2Expansion
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class Prelude2CardsTest : CardTest() {
  @Test
  internal fun `Nirgal pays nothing for milestones and awards`() {
    newGame(Prelude2Expansion)
    p1.manual("$NirgalEnterprises, 16 ProjectCard")
    val startingMoney = p1.count("MC")
    admin.phase("Action")

    p1.claimMilestone(cn("Planner")).expect("Milestone")
    p1.fundAward(cn("Landlord"), 0).expect("Award")

    p1.count("MC") shouldBe startingMoney
  }

  // https://boardgamegeek.com/thread/3412262/i-bit-confused-on-combining-this-and-prelude-1-int
  @Test
  internal fun `Prelude and Prelude 2 share one setup and phase`() {
    newGame(Prelude2Expansion)

    admin.phase("Prelude")

    admin.count("PreludePhase") shouldBe 1
    p1.count("PreludeCard") shouldBe 2
    requireP2().count("PreludeCard") shouldBe 2
  }

  @Test
  internal fun `Board of Directors remains in play and can play another prelude`() {
    newGame(Prelude2Expansion)
    admin.phase("Prelude")
    p1.manual("12 MC, 2 PreludeCard")
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
        Prelude2Expansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    admin.phase("Action")
    p1.manual("13 MC, PreludeCard, ProjectCard, $BoardOfDirectors, $SkyDocks")

    p1.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          p1.playPrelude(EcologyExperts) { p1.playProject(DustSeals, 1) }
        }
        .expect("-13 MC")

    p1.assertCounts(1 to "$EcologyExperts", 1 to "$DustSeals")
  }

  @Test
  internal fun `Terraforming Deal pays two per TR step`() {
    newGame(Prelude2Expansion)
    p1.manual("20 MC, $TerraformingDeal")
    admin.phase("Action")
    val startingTr = p1.count("TerraformRating")
    val startingMoney = p1.count("MC")

    p1.manual("2 TerraformRating")

    p1.count("TerraformRating") shouldBe startingTr + 2
    p1.count("MC") shouldBe startingMoney + 4
  }

  @Test
  internal fun `World Government Advisor lets its owner choose rather than the start player`() {
    newGame(Prelude2Expansion)
    val p2 = requireP2()
    p2.manual("$WorldGovernmentAdvisor")
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
            "Prelude2Expansion, VenusNextExpansion, -WorldGovernmentRule",
            "Player1",
            "Player2",
        )
    )
    p1.manual("$WorldGovernmentAdvisor")
    admin.phase("Action")
    val startingTr = p1.count("TerraformRating")

    p1.cardAction1(WorldGovernmentAdvisor) { wgt("VenusStep") }

    admin.count("VenusStep") shouldBe 1
    p1.count("TerraformRating") shouldBe startingTr
  }

  @Test
  internal fun `EcoTec rewards both of its starting tags`() {
    newGame(Prelude2Expansion)

    p1.manual("$EcoTec") {
      doTask("Plant")
      doTask("Plant")
    }

    p1.count("Plant") shouldBe 2
  }

  @Test
  internal fun `Spire draws four cards and discards three as its first action`() {
    newGame(Prelude2Expansion)
    p1.manual("$Spire")
    admin.phase("Action")

    p1.stdAction("DoRequiredActionsAction")

    p1.count("ProjectCard") shouldBe 1
    p1.count("RequiredAction") shouldBe 0
  }

  @Test
  internal fun `Spire counts the derived event tag toward its two-tag requirement`() {
    newGame(Prelude2Expansion, CorporateEraExpansion)
    p1.manual("$Spire")
    val startingScience = p1.count("Science<$Spire>")

    p1.manual("$BusinessContacts")
    p1.count("Science<$Spire>") shouldBe startingScience + 1

    p1.manual("$MineralDeposit")
    p1.count("Science<$Spire>") shouldBe startingScience + 1
  }

  @Test
  internal fun `Spire science pays two toward standard projects`() {
    newGame(Prelude2Expansion, CorporateEraExpansion)
    p1.manual("$Spire, 20 MC")
    val startingScience = p1.count("Science<$Spire>")
    p1.manual("$Research")
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
    newGame(Prelude2Expansion)
    p1.manual("$Spire, Science<$Spire>")

    shouldThrow<TaskException> {
      p1.manual("10 Owed<>") { doTask("PayFromCard<$Spire> FROM Science<$Spire>") }
    }
  }

  @Test
  internal fun `Selling patents does not offer Spire science for later debts`() {
    newGame(Prelude2Expansion)
    p1.manual("$Spire, Science<$Spire>, ProjectCard<Hand>")
    p1.manual("-RequiredAction!")
    admin.phase("Action")
    p1.sellPatents(1)

    shouldThrow<TaskException> {
      p1.manual("10 Owed<>") { doTask("PayFromCard<$Spire> FROM Science<$Spire>") }
    }
  }

  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  internal fun `Suitable Infrastructure pays once for each action`() {
    newGame(Prelude2Expansion)
    admin.phase("Prelude")
    p1.manual("$SuitableInfrastructure")
    val beforeTwoProductions = p1.count("MC")

    p1.playPrelude(DomeFarming)
    p1.count("MC") shouldBe beforeTwoProductions + 2

    p1.manual("50 MC")
    admin.phase("Action")
    val startingMoney = p1.count("MC")

    p1.manual("NewTurn") {
      doTask("UseAction<UseStandardProjectAction, Action1>")
      doTask("UseAction<PowerPlantProject, Action1>")
      doTask("Pay<Class<MC>> FROM MC / Owed<>")
    }
    p1.count("MC") shouldBe startingMoney - 9

    p1.manual("SecondAction") {
      doTask("UseAction<UseStandardProjectAction, Action1>")
      doTask("UseAction<PowerPlantProject, Action1>")
      doTask("Pay<Class<MC>> FROM MC / Owed<>")
    }

    p1.count("MC") shouldBe startingMoney - 18
  }

  @Test
  internal fun `Focused Organization may gain a different resource than it spends`() {
    newGame(Prelude2Expansion)
    p1.manual("$FocusedOrganization") { doTask("Steel") }
    admin.phase("Action")

    p1.cardAction1(FocusedOrganization) { doTask("Plant") }

    p1.count("ProjectCard") shouldBe 1
    p1.count("Steel") shouldBe 0
    p1.count("Plant") shouldBe 1
  }

  @Test
  internal fun `Early Colonization advances every track twice and Solar reuses the same operation`() {
    val colonyTiles = testColonyTiles(2, "Luna")
    newGame(Prelude2Expansion, ColoniesExpansion, colonyTiles = colonyTiles)
    admin.manual("5 ColonyProduction<Luna>")

    p1.manual("$EarlyColonization") { doTask("Colony<Luna>") }

    colonyTiles.forEach { tile ->
      admin.count("ColonyProduction<$tile>") shouldBe if (tile == cn("Luna")) 6 else 3
    }
    p1.count("Energy") shouldBe 3

    admin.phase("Production")
    TfmWorkflow.Manual(game).solarPhase()
    colonyTiles.forEach { tile ->
      admin.count("ColonyProduction<$tile>") shouldBe if (tile == cn("Luna")) 6 else 4
    }
  }

  @Test
  internal fun `Industrial Complex raises only production tracks below one`() {
    newGame(Prelude2Expansion)
    p1.manual("18 MC, PROD[-5 MC], PROD[2 Titanium], PROD[Plant]")

    p1.manual("$IndustrialComplex")

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
    newGame(GameConfig("Prelude2Expansion, QuickStartVariant", "Player1", "Player2"))
    p1.manual("18 MC, PROD[-6 MC], PROD[2 Titanium], PROD[-Plant]")

    p1.manual("$IndustrialComplex")

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
    newGame(Prelude2Expansion, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p2.manual("4 MC, PROD[-4 MC]")
    p3.manual("5 MC, PROD[2 MC]")
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
    newGame(PreludeExpansion, Prelude2Expansion)
    val p2 = requireP2()
    admin.phase("Prelude")
    p1.playPrelude(Donation)
    p2.playPrelude(Loan)
    p1.playPrelude(BoardOfDirectors)
    p2.playPrelude(Biolab)
    admin.phase("Action")
    p1.manual("ProjectCard")
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
    newGame(PreludeExpansion, Prelude2Expansion, PromoCardPack, players = 5)
    val playerActors = Player.players(5)
    val players = playerActors.map { game.tfm(it) }
    val mons = players[1]
    val victims = players.drop(2)
    val victimActors = playerActors.drop(2)
    mons.manual("$MonsInsurance")
    mons.manual("-27 MC")
    victims.forEach { it.manual("5 MC") }
    admin.phase("Prelude")
    p1.autoExecMode = SAFE

    fun OperationBody.settle(
        victim: Player,
        secondPayout: Int = 3,
    ) {
      doTask("-5 MC<$victim>")
      doTask("3 MC<$victim> FROM MC<Player2>")
      doTask("PROD[-1 MC<$victim>]")
      doTask("$secondPayout MC<$victim> FROM MC<Player2>")
    }

    p1.playPrelude(Recession) {
      doTask(
          "EACH Player(HAS MAX 0 $Recession<Anyone>) { " +
              "-5 MC<Owner>., -Production<Owner, Class<MC>>! }"
      )
      doTask("-5 MC<Player2>")
      doTask("PROD[-1 MC<Player2>]")
      doTask("3 MC<Player2> FROM MC<Player2>")
      doTask("3 MC<Player2> FROM MC<Player2>")
      settle(victimActors[0])
      settle(victimActors[2])
      settle(victimActors[1], secondPayout = 1)
    }

    // https://boardgamegeek.com/thread/3334230/article/44565901#44565901
    mons.count("MC") shouldBe 0
    victims.map { it.count("MC") } shouldBe listOf(6, 4, 6)
    mons.count("PreludeCard") shouldBe 2

    shouldThrow<LimitsException> { mons.playPrelude(MainBeltAsteroids) }
    shouldThrow<LimitsException> { mons.playPrelude(BusinessEmpire) }

    mons.startTurn()
    mons.doTask("-PreludeCard")
    mons.playPrelude(BusinessEmpire)

    mons.count("MC") shouldBe 9
    mons.count("$BusinessEmpire") shouldBe 1
    mons.count("PreludeCard") shouldBe 0
  }

  @Test
  internal fun `Cloud Tourism uses the lower Earth and Venus tag count`() {
    newGame(Prelude2Expansion, VenusNextExpansion, CorporateEraExpansion)
    p1.manual("$Sponsors, $EarthOffice, $VenusGovernor, $VenusWaystation, $ForcedPrecipitation")
    val firstStartingProduction = p1.production(cn("MC"))
    p1.manual("$CloudTourism")
    p1.production(cn("MC")) shouldBe firstStartingProduction + 2

    newGame(Prelude2Expansion, VenusNextExpansion, CorporateEraExpansion)
    p1.manual("$Sponsors, $EarthOffice, $EarthCatapult, $AcquiredCompany, $MediaGroup")
    p1.manual("$ForcedPrecipitation")
    val secondStartingProduction = p1.production(cn("MC"))
    p1.manual("$CloudTourism")
    p1.production(cn("MC")) shouldBe secondStartingProduction + 2
  }

  // https://boardgamegeek.com/thread/3154781/do-event-tags-count-for-sagitta
  @Test
  internal fun `Sagitta treats the event icon as an additional printed tag`() {
    newGame(
        Prelude2Expansion,
        CorporateEraExpansion,
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
    )
    val p2 = requireP2()

    p1.manual("$SagittaFrontierServices")
    p1.count("MC") shouldBe 35

    p1.manual("$AtmoCollectors") { addCardResources(AtmoCollectors) }
    p1.count("MC") shouldBe 39

    p2.manual("7 MC")
    p1.manual("$Sabotage") { doTask("-7 MC<Player2>") }
    p1.count("MC") shouldBe 40

    p1.manual("$Mine")
    p1.count("MC") shouldBe 41

    p1.manual("$Research")
    p1.manual("$SmallAsteroid")
    p1.count("MC") shouldBe 41
  }

  @Test
  internal fun `Sagitta ignores cards played by another player`() {
    newGame(Prelude2Expansion, players = 2)
    val p2 = requireP2()
    p1.manual("$SagittaFrontierServices")
    val startingMoney = p1.count("MC")
    admin.phase("Prelude")

    p2.playPrelude(SpaceLanes)

    p1.count("MC") shouldBe startingMoney
  }

  // https://www.reddit.com/r/TerraformingMarsGame/comments/1kgksgg
  @Test
  internal fun `A prelude remains playable when its global parameter is already maximized`() {
    newGame(Prelude2Expansion)
    admin.phase("Prelude")
    val oceans = p1.list("WaterArea").take(9).joinToString { "OceanTile<$it>" }
    p1.manual("5 MC, 19 TemperatureStep, $oceans")
    val startingMoney = p1.count("MC")

    p1.playPrelude(HugeAsteroid)

    admin.count("TemperatureStep") shouldBe 19
    p1.count("MC") shouldBe startingMoney - 5
    p1.count("$HugeAsteroid") shouldBe 1
  }

  @Test
  internal fun `Venus Orbital Survey follows both reveal outcomes`() {
    newGame(Prelude2Expansion, VenusNextExpansion)
    p1.manual("$VenusOrbitalSurvey, 3 MC")
    admin.phase("Action")

    p1.cardAction1(VenusOrbitalSurvey) {
      doTask("ProjectCard<Hand FROM Selecting>")
      p1.buyCards(1)
    }

    p1.count("ProjectCard") shouldBe 2
    p1.count("MC") shouldBe 0
  }

  @Test
  internal fun `Venus Shuttles action cost is reduced by Venus tags`() {
    newGame(Prelude2Expansion, VenusNextExpansion)
    p1.manual("$VenusGovernor, $VenusWaystation, $ForcedPrecipitation, $VenusMagnetizer, 20 MC")
    p1.manual("$VenusShuttles") { addCardResources(ForcedPrecipitation) }
    admin.phase("Action")
    val startingMoney = p1.count("MC")
    val startingVenus = admin.count("VenusStep")

    p1.cardAction1(VenusShuttles)

    p1.count("MC") shouldBe startingMoney - 6
    admin.count("VenusStep") shouldBe startingVenus + 1
  }

  @Test
  internal fun `Turmoil linked projects are unavailable without Turmoil`() {
    newGame(Prelude2Expansion)
    admin.phase("Action")
    p1.manual("10 MC, ProjectCard")

    shouldThrow<DeadEndException> { p1.playProject(SummitLogistics, 10) }
  }

  @Test
  internal fun `party requirements accept two delegates`() {
    newGame(
        Prelude2Expansion,
        TurmoilExpansion,
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    admin.phase("Action")
    p1.manual("10 MC, ProjectCard")

    shouldThrow<RequirementException> { p1.playProject(SummitLogistics, 10) }

    p1.manual(
        "PartyDelegate<Scientists> FROM ReserveDelegate, " +
            "PartyDelegate<Scientists> FROM ReserveDelegate"
    )
    p1.playProject(SummitLogistics, 10)

    p1.count("ProjectCard") shouldBe 2
  }

  @Test
  internal fun `Red Appeasement passes and requires every other player to remain active`() {
    newGame(Prelude2Expansion, TurmoilExpansion)
    admin.phase("Action")
    p1.manual(
        "ProjectCard, PartyDelegate<Reds> FROM ReserveDelegate, " +
            "PartyDelegate<Reds> FROM ReserveDelegate"
    )
    requireP2().manual("Pass")

    shouldThrow<RequirementException> { p1.playProject(RedAppeasement, 0) }

    newGame(Prelude2Expansion, TurmoilExpansion)
    admin.phase("Action")
    p1.manual(
        "ProjectCard, PartyDelegate<Reds> FROM ReserveDelegate, " +
            "PartyDelegate<Reds> FROM ReserveDelegate"
    )
    val startingProduction = p1.production(cn("MC"))

    p1.playProject(RedAppeasement, 0)

    p1.production(cn("MC")) shouldBe startingProduction + 2
    p1.count("Pass") shouldBe 1
  }

  @Test
  internal fun `political preludes grant their ongoing and delegate benefits`() {
    newGame(Prelude2Expansion, TurmoilExpansion)
    val startingTr = p1.count("TerraformRating")
    val startingMoney = p1.count("MC")
    val startingProduction = p1.production(cn("MC"))

    p1.manual("$HighCircles") {
      doTask("PartyDelegate<Unity> FROM ReserveDelegate")
    }
    p1.count("ProjectCard") shouldBe 1

    p1.manual("$CorridorsOfPower")
    p1.manual("PartyLeader<Scientists>")
    p1.count("ProjectCard") shouldBe 2

    p1.manual("$RiseToPower") {
      doTask("PartyDelegate<Scientists> FROM ReserveDelegate")
      doTask("PartyDelegate<Reds> FROM ReserveDelegate")
      doTask("PartyDelegate<Greens> FROM ReserveDelegate")
    }
    admin.manual("MeasureInfluence<Player1>")

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
        Prelude2Expansion,
        TurmoilExpansion,
        CorporateEraExpansion,
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2, "Luna", "Io"),
    )
    p1.manual("$EarthOffice, $VestaShipyard, $VenusGovernor, Colony<Luna>, Colony<Io>")
    val startingMoney = p1.count("MC")
    val startingCards = p1.count("ProjectCard")

    p1.manual("$SummitLogistics")

    p1.count("MC") shouldBe startingMoney + 6
    p1.count("ProjectCard") shouldBe startingCards + 2

    p1.manual("$ForcedPrecipitation, 3 Floater<$ForcedPrecipitation>")
    val startingHeat = p1.count("Heat")
    val startingHeatProduction = p1.production(cn("Heat"))

    p1.manual("$GhgShipment")

    p1.count("Heat") shouldBe startingHeat + 3
    p1.production(cn("Heat")) shouldBe startingHeatProduction + 1
  }

  @Test
  internal fun `representation and envoys apply colony scaled political benefits`() {
    newGame(
        Prelude2Expansion,
        TurmoilExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2, "Luna", "Io"),
    )
    p1.manual("Colony<Luna>, Colony<Io>")
    val startingMoney = p1.count("MC")

    p1.manual("$ColonialRepresentation")
    p1.manual("$ColonialEnvoys") {
      doTask("PartyDelegate<Scientists> FROM ReserveDelegate")
      doTask("PartyDelegate<Greens> FROM ReserveDelegate")
    }
    admin.manual("MeasureInfluence<Player1>")

    p1.count("MC") shouldBe startingMoney + 6
    p1.count("ColonialRepresentationInfluence") shouldBe 1
    p1.count("PartyDelegate<Scientists>") shouldBe 1
    p1.count("PartyDelegate<Greens>") shouldBe 1
  }

  @Test
  internal fun `Special Permit steals four plants from one player`() {
    newGame(Prelude2Expansion, TurmoilExpansion)
    val p2 = requireP2()
    p2.manual("4 Plant")

    p1.manual("$SpecialPermit")

    p1.count("Plant") shouldBe 4
    p2.count("Plant") shouldBe 0
  }

  @Test
  internal fun `Frontier Town repeats only its printed placement bonus`() {
    newGame(Prelude2Expansion, TurmoilExpansion)
    p1.manual("PROD[Energy]")

    p1.manual("$FrontierTown") { placeTile(4, 2) }

    p1.assertProds(0 to "Energy")
    p1.count("Plant") shouldBe 3
  }

  @Test
  internal fun `WG Project draws three preludes and plays one`() {
    newGame(Prelude2Expansion, TurmoilExpansion)
    admin.manual("ReserveDelegate<Neutral> FROM Chairman<Neutral>")
    p1.manual("Chairman FROM ReserveDelegate, 9 MC, ProjectCard")
    admin.phase("Action")

    p1.playProject(WgProject, 9) {
      p1.playPrelude(HighCircles) {
        doTask("PartyDelegate<Unity> FROM ReserveDelegate")
      }
    }

    p1.count("PreludeCard<Selecting>") shouldBe 0
  }
}
