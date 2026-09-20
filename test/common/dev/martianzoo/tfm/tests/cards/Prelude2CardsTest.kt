package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.generated.Class
import dev.martianzoo.generated.Prelude2CardPack
import dev.martianzoo.generated.PreludeExpansion
import dev.martianzoo.generated.QuickStartVariant
import dev.martianzoo.generated.VenusNextExpansion
import dev.martianzoo.generated.gameConfig
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Player
import dev.martianzoo.testsupport.PLAYER3
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack as Prelude2CardPackOption
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion as PreludeExpansionOption
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion as VenusNextExpansionOption
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class Prelude2CardsTest : CardTest() {
  @Test
  internal fun `Nirgal pays nothing for milestones and awards`() {
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption)

    admin.phase("Prelude")

    admin.count("PreludePhase") shouldBe 1
    p1.count("PreludeCard") shouldBe 2
    requireP2().count("PreludeCard") shouldBe 2
  }

  @Test
  internal fun `Board of Directors remains in play and can play another prelude`() {
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
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
        PreludeExpansionOption,
        Prelude2CardPackOption,
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
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
        gameConfig(
            modules =
                listOf(
                    Class.of(PreludeExpansion),
                    Class.of(Prelude2CardPack),
                    Class.of(VenusNextExpansion),
                ),
            extra = "-WorldGovernmentRule",
            playerNames = listOf("Player1", "Player2"),
        )
    )
    p1.runOperation("$WorldGovernmentAdvisor")
    admin.phase("Action")
    val startingTr = p1.count("TerraformRating")

    p1.cardAction1(WorldGovernmentAdvisor) { wgt("VenusStep") }

    admin.count("VenusStep") shouldBe 1
    p1.count("TerraformRating") shouldBe startingTr
  }

  @Test
  internal fun `Ecotec rewards both of its starting tags`() {
    newGame(PreludeExpansionOption, Prelude2CardPackOption)

    p1.runOperation("$Ecotec") {
      doTask("Plant")
      doTask("Plant")
    }

    p1.count("Plant") shouldBe 2
  }

  @Test
  internal fun `Spire draws four cards and discards three as its first action`() {
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
    p1.runOperation("$Spire")
    admin.phase("Action")

    p1.stdAction("DoRequiredActionsAction")

    p1.count("ProjectCard") shouldBe 1
    p1.count("RequiredAction") shouldBe 0
  }

  @Test
  internal fun `Spire counts the derived event tag toward its two-tag requirement`() {
    newGame(PreludeExpansionOption, Prelude2CardPackOption, CorporateEraExpansion)
    p1.runOperation("$Spire")
    val startingScience = p1.count("Science<$Spire>")

    p1.runOperation("$BusinessContacts")
    p1.count("Science<$Spire>") shouldBe startingScience + 1

    p1.runOperation("$MineralDeposit")
    p1.count("Science<$Spire>") shouldBe startingScience + 1
  }

  @Test
  internal fun `Spire science pays two toward standard projects`() {
    newGame(PreludeExpansionOption, Prelude2CardPackOption, CorporateEraExpansion)
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
    p1.runOperation("$Spire, Science<$Spire>")

    shouldThrow<TaskException> {
      p1.runOperation("10 Owed<>") { doTask("PayFromCard<$Spire> FROM Science<$Spire>") }
    }
  }

  @Test
  internal fun `Selling patents does not offer Spire science for later debts`() {
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
    p1.runOperation("$Spire, Science<$Spire>, ProjectCard<Hand>")
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
    p1.runOperation("$SuitableInfrastructure, $ValleyTrust")
    admin.phase("Action")
    val startingMoney = p1.count("MC")

    p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(DomeFarming) }

    p1.assertProds(2 to "MC", 1 to "Plant")
    p1.count("MC") shouldBe startingMoney + 2
  }

  @Test
  internal fun `Focused Organization may gain a different resource than it spends`() {
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
    p1.runOperation("$FocusedOrganization") { doTask("Steel") }
    admin.phase("Action")

    p1.cardAction1(FocusedOrganization) { doTask("Plant") }

    p1.count("ProjectCard") shouldBe 1
    p1.count("Steel") shouldBe 0
    p1.count("Plant") shouldBe 1
  }

  @Test
  internal fun `Early Colonization advances every track twice and Solar reuses the same operation`() {
    val colonyTiles = testColonyTiles(2, "Luna")
    newGame(
        PreludeExpansionOption,
        Prelude2CardPackOption,
        ColoniesExpansion,
        colonyTiles = colonyTiles,
    )
    admin.runOperation("5 ColonyProduction<Luna>")

    p1.runOperation("$EarlyColonization") { doTask("Colony<Luna>") }

    colonyTiles.forEach { tile ->
      admin.count("ColonyProduction<$tile>") shouldBe if (tile == cn("Luna")) 6 else 3
    }
    p1.count("Energy") shouldBe 3

    admin.phase("Production")
    TfmWorkflow.Stepwise(agents).solarPhase()
    colonyTiles.forEach { tile ->
      admin.count("ColonyProduction<$tile>") shouldBe if (tile == cn("Luna")) 6 else 4
    }
  }

  @Test
  internal fun `Industrial Complex raises only production tracks below one`() {
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
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
        gameConfig(
            modules =
                listOf(
                    Class.of(PreludeExpansion),
                    Class.of(Prelude2CardPack),
                    Class.of(QuickStartVariant),
                ),
            playerNames = listOf("Player1", "Player2"),
        )
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption, players = 3)
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption, PromoCardPack, players = 5)
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
    newGame(
        PreludeExpansionOption,
        Prelude2CardPackOption,
        VenusNextExpansionOption,
        CorporateEraExpansion,
    )
    p1.runOperation(
        "$Sponsors, $EarthOffice, $VenusGovernor, $VenusWaystation, $ForcedPrecipitation"
    )
    val firstStartingProduction = p1.production(cn("MC"))
    p1.runOperation("$CloudTourism")
    p1.production(cn("MC")) shouldBe firstStartingProduction + 2

    newGame(
        PreludeExpansionOption,
        Prelude2CardPackOption,
        VenusNextExpansionOption,
        CorporateEraExpansion,
    )
    p1.runOperation("$Sponsors, $EarthOffice, $EarthCatapult, $AcquiredCompany, $MediaGroup")
    p1.runOperation("$ForcedPrecipitation")
    val secondStartingProduction = p1.production(cn("MC"))
    p1.runOperation("$CloudTourism")
    p1.production(cn("MC")) shouldBe secondStartingProduction + 2
  }

  // https://boardgamegeek.com/thread/3154781/do-event-tags-count-for-sagitta
  @Test
  internal fun `Sagitta treats the event icon as an additional printed tag`() {
    newGame(
        PreludeExpansionOption,
        Prelude2CardPackOption,
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption, players = 2)
    val p2 = requireP2()
    p1.runOperation("$SagittaFrontierServices")
    val startingMoney = p1.count("MC")
    admin.phase("Prelude")

    p2.playPrelude(SpaceLanes)

    p1.count("MC") shouldBe startingMoney
  }

  // https://www.reddit.com/r/TerraformingMarsGame/comments/1kgksgg
  @Test
  internal fun `A prelude remains playable when its global parameter is already maximized`() {
    newGame(PreludeExpansionOption, Prelude2CardPackOption)
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption, VenusNextExpansionOption)
    p1.runOperation("$VenusOrbitalSurvey, 3 MC")
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
    newGame(PreludeExpansionOption, Prelude2CardPackOption, VenusNextExpansionOption)
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
}
