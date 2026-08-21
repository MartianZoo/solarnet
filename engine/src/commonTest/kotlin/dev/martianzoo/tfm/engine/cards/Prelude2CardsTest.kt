package dev.martianzoo.tfm.engine.cards

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
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class Prelude2CardsTest : CardTest() {
  @Test
  fun `Applied Science supplies a wild tag and converts its science`() {
    newGame(Prelude2Expansion)
    p1.manual("$AppliedScience")

    p1.count("WildTag") shouldBe 1
    p1.count("Science<$AppliedScience>") shouldBe 6

    engine.phase("Action")
    p1.startTurn()
    p1.doTask("PlantTag<WildTagUse<$AppliedScience>>")
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
    newGame(Prelude2Expansion, PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("12, 2 PreludeCard")
    p1.playPrelude(BoardOfDirectors)
    engine.phase("Action")

    p1.cardAction1(BoardOfDirectors) {
      doTask(
          "-12 THEN -Director<$BoardOfDirectors> THEN " +
              "PlayCard<Class<PreludeCard>, Class<$Donation>>"
      )
    }

    p1.count("Director<$BoardOfDirectors>") shouldBe 3
    p1.count("$Donation") shouldBe 1
  }

  @Test
  fun `Preservation Program cancels only the first TR increase each action phase`() {
    newGame(Prelude2Expansion)
    p1.manual("$PreservationProgram")
    engine.phase("Action")
    val startingTr = p1.count("TerraformRating")

    p1.manual("2 TerraformRating")
    p1.count("TerraformRating") shouldBe startingTr + 1

    p1.manual("TemperatureStep")
    p1.count("TerraformRating") shouldBe startingTr + 2

    engine.nextGeneration(0, 0)
    p1.manual("TemperatureStep")
    p1.count("TerraformRating") shouldBe startingTr + 2
  }

  @Test
  fun `Terraforming Deal pays only for TR that Preservation does not skip`() {
    newGame(Prelude2Expansion)
    p1.manual("20, $PreservationProgram, $TerraformingDeal")
    engine.phase("Action")
    val startingTr = p1.count("TerraformRating")
    val startingMoney = p1.count("Megacredit")

    p1.manual("2 TerraformRating")

    p1.count("TerraformRating") shouldBe startingTr + 1
    p1.count("Megacredit") shouldBe startingMoney + 2
  }

  @Test
  fun `World Government Advisor raises a parameter as Engine`() {
    newGame(Prelude2Expansion)
    p1.manual("$WorldGovernmentAdvisor")
    engine.phase("Action")
    val startingTr = p1.count("TerraformRating")

    p1.cardAction1(WorldGovernmentAdvisor) { doTask("TemperatureStep! BY Engine") }

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

    p1.cardAction1(WorldGovernmentAdvisor) { doTask("VenusStep! BY Engine") }

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
  fun `Suitable Infrastructure pays once for each action`() {
    newGame(Prelude2Expansion, PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("$SuitableInfrastructure")
    val beforeTwoProductions = p1.count("Megacredit")

    p1.manual("$DomeFarming")
    p1.count("Megacredit") shouldBe beforeTwoProductions + 2

    p1.manual("50")
    engine.phase("Action")
    val startingMoney = p1.count("Megacredit")

    p1.manual("NewTurn") {
      doTask("UseAction1<UseStandardProjectSA>")
      doTask("UseAction1<PowerPlantSP>")
      doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<Class<Megacredit>>")
    }
    p1.count("Megacredit") shouldBe startingMoney - 9

    p1.manual("SecondAction") {
      doTask("UseAction1<UseStandardProjectSA>")
      doTask("UseAction1<PowerPlantSP>")
      doTask("Pay<Class<Megacredit>> FROM Megacredit / Owed<Class<Megacredit>>")
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
      engine.count("ColonyProduction<$tile>") shouldBe
          if (tile.toString() == "ColonyTile07") 6 else 3
    }
    p1.count("Energy") shouldBe 3

    engine.phase("Production")
    TfmWorkflow.Manual(game).solarPhase()
    colonyTiles.forEach { tile ->
      engine.count("ColonyProduction<$tile>") shouldBe
          if (tile.toString() == "ColonyTile07") 6 else 4
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

    p1.manual("$AtmoCollectors") { doTask("2 Floater<$AtmoCollectors>") }
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
    p1.manual("$VenusShuttles") { doTask("2 Floater<$ForcedPrecipitation>") }
    engine.phase("Action")
    val startingMoney = p1.count("Megacredit")
    val startingVenus = engine.count("VenusStep")

    p1.cardAction1(VenusShuttles)

    p1.count("Megacredit") shouldBe startingMoney - 6
    engine.count("VenusStep") shouldBe startingVenus + 1
  }
}
