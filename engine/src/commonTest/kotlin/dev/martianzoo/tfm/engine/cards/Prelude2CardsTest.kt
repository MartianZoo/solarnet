package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.Player.Companion.PLAYER3
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.engine.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.engine.TestOption.Prelude2Expansion
import dev.martianzoo.tfm.engine.TestOption.PreludeExpansion
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class Prelude2CardsTest : CardTest() {
  @Test
  fun `Nirgal pays nothing for milestones and awards`() {
    newGame(Prelude2Expansion)
    p1.manual("$NirgalEnterprises, 16 ProjectCard")
    val startingMoney = p1.count("Megacredit")
    engine.phase("Action")

    p1.stdAction("ClaimMilestoneSA") { doTask("Planner") }.expect("Milestone")
    p1.stdAction("FundAwardSA") { doTask("Landlord") }.expect("Award")

    p1.count("Megacredit") shouldBe startingMoney
  }

  // https://boardgamegeek.com/thread/3412262/i-bit-confused-on-combining-this-and-prelude-1-int
  @Test
  fun `Prelude and Prelude 2 share one setup and phase`() {
    newGame(PreludeExpansion, Prelude2Expansion)

    engine.phase("Prelude")

    engine.count("PreludePhase") shouldBe 1
    p1.count("PreludeCard") shouldBe 2
    requireP2().count("PreludeCard") shouldBe 2
  }

  @Test
  fun `Applied Science supplies a wild tag and converts its science`() {
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
  fun `Nobel Prize supplies its wild tag and immediate gains`() {
    newGame(Prelude2Expansion)
    p1.manual("$NobelPrize")

    p1.count("WildTag") shouldBe 1
    p1.count("Megacredit") shouldBe 5
    p1.count("ProjectCard") shouldBe 2
  }

  @Test
  fun `Board of Directors remains in play and can play another prelude`() {
    newGame(Prelude2Expansion)
    engine.phase("Prelude")
    p1.manual("12, 2 PreludeCard")
    p1.playPrelude(BoardOfDirectors)
    engine.phase("Action")

    p1.cardAction1(BoardOfDirectors) {
      doTask(
          "-12 THEN -Director<$BoardOfDirectors> THEN " +
              "PlayCard<Class<PreludeCard>, Class<$Recession>>"
      )
    }

    p1.count("Director<$BoardOfDirectors>") shouldBe 3
    p1.count("$Recession") shouldBe 1
  }

  @Test
  fun `Terraforming Deal pays two per TR step`() {
    newGame(Prelude2Expansion)
    p1.manual("20, $TerraformingDeal")
    engine.phase("Action")
    val startingTr = p1.count("TerraformRating")
    val startingMoney = p1.count("Megacredit")

    p1.manual("2 TerraformRating")

    p1.count("TerraformRating") shouldBe startingTr + 2
    p1.count("Megacredit") shouldBe startingMoney + 4
  }

  @Test
  fun `World Government Advisor raises a parameter as Engine`() {
    newGame(Prelude2Expansion)
    p1.manual("$WorldGovernmentAdvisor")
    engine.phase("Action")
    val startingTr = p1.count("TerraformRating")

    p1.cardAction1(WorldGovernmentAdvisor) { wgt("TemperatureStep") }

    engine.count("TemperatureStep") shouldBe 1
    p1.count("TerraformRating") shouldBe startingTr
  }

  @Test
  fun `World Government Advisor works with Venus while World Government is disabled`() {
    newGame(
        GameConfig(
            "Prelude2Expansion, VenusNextExpansion, -WorldGovernmentOption",
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
  fun `EcoTec rewards both of its starting tags`() {
    newGame(Prelude2Expansion)

    p1.manual("$EcoTec") {
      doTask("Plant")
      doTask("Plant")
    }

    p1.count("Plant") shouldBe 2
  }

  @Test
  fun `Spire draws four cards and discards three as its first action`() {
    newGame(Prelude2Expansion)
    p1.manual("$Spire")
    engine.phase("Action")

    p1.stdAction("HandleMandates")

    p1.count("ProjectCard") shouldBe 1
    p1.count("Mandate") shouldBe 0
  }

  @Test
  fun `Spire counts the derived event tag toward its two-tag requirement`() {
    newGame(Prelude2Expansion, CorporateEraExpansion)
    p1.manual("$Spire")
    val startingScience = p1.count("Science<$Spire>")

    p1.manual("$BusinessContacts")
    p1.count("Science<$Spire>") shouldBe startingScience + 1

    p1.manual("$MineralDeposit")
    p1.count("Science<$Spire>") shouldBe startingScience + 1
  }

  @Test
  fun `Spire science pays two toward standard projects`() {
    newGame(Prelude2Expansion, CorporateEraExpansion)
    p1.manual("$Spire, 20")
    val startingScience = p1.count("Science<$Spire>")
    p1.manual("$Research")
    p1.count("Science<$Spire>") shouldBe startingScience + 1
    engine.phase("Action")
    p1.stdAction("HandleMandates")

    p1.stdProject(
            "PowerPlantSP",
            payment = {
              doTask("PayFromCard<$Spire> FROM Science<$Spire>")
              doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<>")
            },
        )
        .expect("-Science<$Spire>, -9 Megacredit, PROD[Energy]")
  }

  @Test
  fun `Spire science cannot pay other debts`() {
    newGame(Prelude2Expansion)
    p1.manual("$Spire, Science<$Spire>")

    shouldThrow<NarrowingException> {
      p1.manual("10 Owed<>") {
        doTask("PayFromCard<$Spire> FROM Science<$Spire>")
      }
    }
  }

  // https://boardgamegeek.com/thread/3335155/article/44576777#44576777
  @Test
  fun `Suitable Infrastructure pays once for each action`() {
    newGame(PreludeExpansion, Prelude2Expansion)
    engine.phase("Prelude")
    p1.manual("$SuitableInfrastructure")
    val beforeTwoProductions = p1.count("Megacredit")

    p1.playPrelude(DomeFarming)
    p1.count("Megacredit") shouldBe beforeTwoProductions + 2

    p1.manual("50")
    engine.phase("Action")
    val startingMoney = p1.count("Megacredit")

    p1.manual("NewTurn") {
      doTask("UseAction<UseStandardProjectSA, First>")
      doTask("UseAction<PowerPlantSP, First>")
      doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<>")
    }
    p1.count("Megacredit") shouldBe startingMoney - 9

    p1.manual("SecondAction") {
      doTask("UseAction<UseStandardProjectSA, First>")
      doTask("UseAction<PowerPlantSP, First>")
      doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<>")
    }

    p1.count("Megacredit") shouldBe startingMoney - 18
  }

  @Test
  fun `Focused Organization may gain a different resource than it spends`() {
    newGame(Prelude2Expansion)
    p1.manual("$FocusedOrganization") { doTask("Steel") }
    engine.phase("Action")

    p1.cardAction1(FocusedOrganization) { doTask("Plant") }

    p1.count("ProjectCard") shouldBe 1
    p1.count("Steel") shouldBe 0
    p1.count("Plant") shouldBe 1
  }

  @Test
  fun `Early Colonization advances every track twice and Solar reuses the same operation`() {
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
  fun `Industrial Complex raises only production tracks below one`() {
    newGame(Prelude2Expansion)
    p1.manual("18, PROD[-5 Megacredit], PROD[2 Titanium], PROD[Plant]")

    p1.manual("$IndustrialComplex")

    p1.count("Megacredit") shouldBe 0
    p1.assertProds(
        1 to "Megacredit",
        1 to "Steel",
        2 to "Titanium",
        1 to "Plant",
        1 to "Energy",
        1 to "Heat",
    )
  }

  @Test
  fun `Recession applies each opponent loss as much as possible`() {
    newGame(Prelude2Expansion, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p2.manual("3, PROD[-5 Megacredit]")
    p3.manual("10, PROD[2 Megacredit]")

    p1.manual("$Recession")

    p1.count("Megacredit") shouldBe 10
    p2.count("Megacredit") shouldBe 0
    p3.count("Megacredit") shouldBe 5
    p2.assertProds(-5 to "Megacredit")
    p3.assertProds(1 to "Megacredit")
  }

  @Test
  fun `Recession losses are performed by each opponent`() {
    newGame(Prelude2Expansion, PromoCardPack, players = 3)
    val p2 = requireP2()
    val p3 = game.tfm(PLAYER3)
    p2.manual("5")
    p3.manual("$MonsInsurance")

    p1.manual("$Recession")

    // Mons Insurance does not compensate an opponent for their self-performed Recession loss.
    p2.count("Megacredit") shouldBe 0
  }

  @Test
  fun `Cloud Tourism uses the lower Earth and Venus tag count`() {
    newGame(Prelude2Expansion, VenusNextExpansion, CorporateEraExpansion)
    p1.manual("$Sponsors, $EarthOffice, $VenusGovernor, $VenusWaystation, $ForcedPrecipitation")
    val firstStartingProduction = p1.production(cn("Megacredit"))
    p1.manual("$CloudTourism")
    p1.production(cn("Megacredit")) shouldBe firstStartingProduction + 2

    newGame(Prelude2Expansion, VenusNextExpansion, CorporateEraExpansion)
    p1.manual("$Sponsors, $EarthOffice, $EarthCatapult, $AcquiredCompany, $MediaGroup")
    p1.manual("$ForcedPrecipitation")
    val secondStartingProduction = p1.production(cn("Megacredit"))
    p1.manual("$CloudTourism")
    p1.production(cn("Megacredit")) shouldBe secondStartingProduction + 2
  }

  // https://boardgamegeek.com/thread/3154781/do-event-tags-count-for-sagitta
  @Test
  fun `Sagitta treats the event icon as an additional printed tag`() {
    newGame(
        Prelude2Expansion,
        CorporateEraExpansion,
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
    )
    val p2 = requireP2()

    p1.manual("$SagittaFrontierServices")
    p1.count("Megacredit") shouldBe 35

    p1.manual("$AtmoCollectors") { addCardResources(AtmoCollectors) }
    p1.count("Megacredit") shouldBe 39

    p2.manual("7")
    p1.manual("$Sabotage") { doTask("-7 Megacredit<Player2>") }
    p1.count("Megacredit") shouldBe 40

    p1.manual("$Mine")
    p1.count("Megacredit") shouldBe 41

    p1.manual("$Research")
    p1.manual("$SmallAsteroid")
    p1.count("Megacredit") shouldBe 41
  }

  @Test
  fun `Sagitta ignores cards played by another player`() {
    newGame(Prelude2Expansion, players = 2)
    val p2 = requireP2()
    p1.manual("$SagittaFrontierServices")
    val startingMoney = p1.count("Megacredit")
    engine.phase("Prelude")

    p2.playPrelude(NobelPrize)

    p1.count("Megacredit") shouldBe startingMoney
  }

  // https://www.reddit.com/r/TerraformingMarsGame/comments/1kgksgg
  @Test
  fun `A prelude remains playable when its global parameter is already maximized`() {
    newGame(PreludeExpansion, Prelude2Expansion)
    engine.phase("Prelude")
    val oceans = p1.list("WaterArea").take(9).joinToString { "OceanTile<$it>" }
    p1.manual("5, 19 TemperatureStep, $oceans")
    val startingMoney = p1.count("Megacredit")

    p1.playPrelude(HugeAsteroid)

    engine.count("TemperatureStep") shouldBe 19
    p1.count("Megacredit") shouldBe startingMoney - 5
    p1.count("$HugeAsteroid") shouldBe 1
  }

  @Test
  fun `Venus Orbital Survey follows both reveal outcomes`() {
    newGame(Prelude2Expansion, VenusNextExpansion)
    p1.manual("$VenusOrbitalSurvey, 3")
    engine.phase("Action")

    p1.cardAction1(VenusOrbitalSurvey) {
      doTask("ProjectCard")
      doTask("BuyCard")
    }

    p1.count("ProjectCard") shouldBe 2
    p1.count("Megacredit") shouldBe 0
  }

  @Test
  fun `Venus Shuttles action cost is reduced by Venus tags`() {
    newGame(Prelude2Expansion, VenusNextExpansion)
    p1.manual("$VenusGovernor, $VenusWaystation, $ForcedPrecipitation, $VenusMagnetizer, 20")
    p1.manual("$VenusShuttles") { addCardResources(ForcedPrecipitation) }
    engine.phase("Action")
    val startingMoney = p1.count("Megacredit")
    val startingVenus = engine.count("VenusStep")

    p1.cardAction1(VenusShuttles)

    p1.count("Megacredit") shouldBe startingMoney - 6
    engine.count("VenusStep") shouldBe startingVenus + 1
  }
}
