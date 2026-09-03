package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.Agent.OperationBody
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.data.Player.Companion.PLAYER3
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
    engine.phase("Action")

    p1.claimMilestone(cn("Planner")).expect("Milestone")
    p1.fundAward(cn("Landlord"), 0).expect("Award")

    p1.count("MC") shouldBe startingMoney
  }

  // https://boardgamegeek.com/thread/3412262/i-bit-confused-on-combining-this-and-prelude-1-int
  @Test
  internal fun `Prelude and Prelude 2 share one setup and phase`() {
    newGame(Prelude2Expansion)

    engine.phase("Prelude")

    engine.count("PreludePhase") shouldBe 1
    p1.count("PreludeCard") shouldBe 2
    requireP2().count("PreludeCard") shouldBe 2
  }

  @Test
  internal fun `Applied Science supplies a wild tag and converts its science`() {
    newGame(Prelude2Expansion)
    p1.manual("$AppliedScience")

    p1.count("WildTag") shouldBe 1
    p1.count("Science<$AppliedScience>") shouldBe 6

    engine.phase("Action")
    p1.startTurn()
    p1.assignWildTag(AppliedScience, "PlantTag")
    p1.cardAction1(AppliedScience) { doTask("Plant") }.expect("Plant")

    p1.count("Science<$AppliedScience>") shouldBe 5
  }

  @Test
  internal fun `Nobel Prize supplies its wild tag and immediate gains`() {
    newGame(Prelude2Expansion)
    p1.manual("$NobelPrize")

    p1.count("WildTag") shouldBe 1
    p1.count("MC") shouldBe 5
    p1.count("ProjectCard") shouldBe 2
  }

  @Test
  internal fun `Board of Directors remains in play and can play another prelude`() {
    newGame(Prelude2Expansion)
    engine.phase("Prelude")
    p1.manual("12 MC, 2 PreludeCard")
    p1.playPrelude(BoardOfDirectors)
    engine.phase("Action")

    p1.cardAction1(BoardOfDirectors) {
      doTask(
          "-12 MC THEN -Director<$BoardOfDirectors> THEN " +
              "PlayCard<Class<PreludeCard>, Class<$Recession>>"
      )
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
    engine.phase("Action")
    p1.manual("13 MC, PreludeCard, ProjectCard, $BoardOfDirectors, $SkyDocks")

    p1.cardAction1(BoardOfDirectors) {
          doTask(
              "-12 MC THEN -Director<$BoardOfDirectors> THEN " +
                  "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>"
          )
          doTask("PlayCard<Class<ProjectCard>, Class<$DustSeals>>")
          p1.pay(1)
        }
        .expect("-13 MC")

    p1.assertCounts(1 to "$EcologyExperts", 1 to "$DustSeals")
  }

  @Test
  internal fun `Terraforming Deal pays two per TR step`() {
    newGame(Prelude2Expansion)
    p1.manual("20 MC, $TerraformingDeal")
    engine.phase("Action")
    val startingTr = p1.count("TerraformRating")
    val startingMoney = p1.count("MC")

    p1.manual("2 TerraformRating")

    p1.count("TerraformRating") shouldBe startingTr + 2
    p1.count("MC") shouldBe startingMoney + 4
  }

  @Test
  internal fun `World Government Advisor raises a parameter as Engine`() {
    newGame(Prelude2Expansion)
    p1.manual("$WorldGovernmentAdvisor")
    engine.phase("Action")
    val startingTr = p1.count("TerraformRating")

    p1.cardAction1(WorldGovernmentAdvisor) { wgt("TemperatureStep") }

    engine.count("TemperatureStep") shouldBe 1
    p1.count("TerraformRating") shouldBe startingTr
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
    engine.phase("Action")
    val startingTr = p1.count("TerraformRating")

    p1.cardAction1(WorldGovernmentAdvisor) { wgt("VenusStep") }

    engine.count("VenusStep") shouldBe 1
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
    engine.phase("Action")

    p1.stdAction("HandleMandates")

    p1.count("ProjectCard") shouldBe 1
    p1.count("Mandate") shouldBe 0
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
    engine.phase("Action")
    p1.stdAction("HandleMandates")

    p1.stdProject(
            "PowerPlantSP",
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
    p1.manual("-Mandate!")
    engine.phase("Action")
    p1.sellPatents(1)

    shouldThrow<TaskException> {
      p1.manual("10 Owed<>") { doTask("PayFromCard<$Spire> FROM Science<$Spire>") }
    }
  }

  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  internal fun `Suitable Infrastructure pays once for each action`() {
    newGame(Prelude2Expansion)
    engine.phase("Prelude")
    p1.manual("$SuitableInfrastructure")
    val beforeTwoProductions = p1.count("MC")

    p1.playPrelude(DomeFarming)
    p1.count("MC") shouldBe beforeTwoProductions + 2

    p1.manual("50 MC")
    engine.phase("Action")
    val startingMoney = p1.count("MC")

    p1.manual("NewTurn") {
      doTask("UseAction<PowerPlantSP, Action1>")
      doTask("Pay<Class<MC>> FROM MC / Owed<>")
    }
    p1.count("MC") shouldBe startingMoney - 9

    p1.manual("SecondAction") {
      doTask("UseAction<PowerPlantSP, Action1>")
      doTask("Pay<Class<MC>> FROM MC / Owed<>")
    }

    p1.count("MC") shouldBe startingMoney - 18
  }

  @Test
  internal fun `Focused Organization may gain a different resource than it spends`() {
    newGame(Prelude2Expansion)
    p1.manual("$FocusedOrganization") { doTask("Steel") }
    engine.phase("Action")

    p1.cardAction1(FocusedOrganization) { doTask("Plant") }

    p1.count("ProjectCard") shouldBe 1
    p1.count("Steel") shouldBe 0
    p1.count("Plant") shouldBe 1
  }

  @Test
  internal fun `Early Colonization advances every track twice and Solar reuses the same operation`() {
    val colonyTiles = testColonyTiles(2, "Luna")
    newGame(Prelude2Expansion, ColoniesExpansion, colonyTiles = colonyTiles)
    engine.manual("5 ColonyProduction<Luna>")

    p1.manual("$EarlyColonization") { doTask("Colony<Luna>") }

    colonyTiles.forEach { tile ->
      engine.count("ColonyProduction<$tile>") shouldBe if (tile == cn("Luna")) 6 else 3
    }
    p1.count("Energy") shouldBe 3

    engine.phase("Production")
    TfmWorkflow.Manual(game).solarPhase()
    colonyTiles.forEach { tile ->
      engine.count("ColonyProduction<$tile>") shouldBe if (tile == cn("Luna")) 6 else 4
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
  internal fun `Recession applies each opponent loss as much as possible`() {
    newGame(Prelude2Expansion, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p2.manual("4 MC, PROD[-4 MC]")
    p3.manual("5 MC, PROD[2 MC]")
    engine.phase("Prelude")

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
    engine.phase("Prelude")
    p1.playPrelude(Donation)
    p2.playPrelude(Loan)
    p1.playPrelude(BoardOfDirectors)
    p2.playPrelude(Biolab)
    engine.phase("Action")
    p1.manual("ProjectCard")
    p1.sellPatents(1)
    p2.playProject(BlackPolarDust, 15) { placeTile(2, 6) }
    p2.playProject(PeroxidePower, 7)
    p2.assertProds(-5 to "MC")
    val p1MoneyBefore = p1.count("MC")
    val directorsBefore = p1.count("Director<$BoardOfDirectors>")

    shouldThrow<LimitsException> {
      p1.cardAction1(BoardOfDirectors) {
        doTask(
            "-12 MC THEN -Director<$BoardOfDirectors> THEN " +
                "PlayCard<Class<PreludeCard>, Class<$Recession>>"
        )
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
    engine.phase("Prelude")
    p1.autoExecMode = SAFE

    fun OperationBody.settle(
        victim: Player,
        secondPayout: Int = 3,
    ) {
      doTask("-5 MC<$victim> BY Player1")
      doTask("3 MC<$victim> FROM MC<Player2>")
      doTask("PROD[-1 MC<$victim>] BY Player1")
      doTask("$secondPayout MC<$victim> FROM MC<Player2>")
    }

    p1.playPrelude(Recession) {
      doTask("-5 MC<Player2> BY Player1")
      doTask("PROD[-1 MC<Player2>] BY Player1")
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
    engine.phase("Prelude")

    p2.playPrelude(NobelPrize)

    p1.count("MC") shouldBe startingMoney
  }

  // https://www.reddit.com/r/TerraformingMarsGame/comments/1kgksgg
  @Test
  internal fun `A prelude remains playable when its global parameter is already maximized`() {
    newGame(Prelude2Expansion)
    engine.phase("Prelude")
    val oceans = p1.list("WaterArea").take(9).joinToString { "OceanTile<$it>" }
    p1.manual("5 MC, 19 TemperatureStep, $oceans")
    val startingMoney = p1.count("MC")

    p1.playPrelude(HugeAsteroid)

    engine.count("TemperatureStep") shouldBe 19
    p1.count("MC") shouldBe startingMoney - 5
    p1.count("$HugeAsteroid") shouldBe 1
  }

  @Test
  internal fun `Venus Orbital Survey follows both reveal outcomes`() {
    newGame(Prelude2Expansion, VenusNextExpansion)
    p1.manual("$VenusOrbitalSurvey, 3 MC")
    engine.phase("Action")

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
    engine.phase("Action")
    val startingMoney = p1.count("MC")
    val startingVenus = engine.count("VenusStep")

    p1.cardAction1(VenusShuttles)

    p1.count("MC") shouldBe startingMoney - 6
    engine.count("VenusStep") shouldBe startingVenus + 1
  }
}
