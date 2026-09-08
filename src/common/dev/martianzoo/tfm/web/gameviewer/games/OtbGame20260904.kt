package dev.martianzoo.tfm.web.gameviewer.games

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.web.gameviewer.RecordedGame
import dev.martianzoo.tfm.web.gameviewer.cardnames.*
import dev.martianzoo.tfm.web.gameviewer.fakeWildTags

/** Four-player physical game begun Friday, 2026-09-04; the recording ends before G6 Research. */
public class OtbGame20260904 : RecordedGame() {
  protected override val config: GameConfig =
      GameConfig(
          """
          AmazonisMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, PromoCardPack
          FakeStuffBundle

          Builder, Diversifier, Generalist, Landshaper, Tactician
          Administrator, Excentric, Highlander, Promoter, Thermalist
          """,
          "Yellow",
          "Rainbow",
          "Blue",
          "Green",
      )

  protected override fun play() {
    TfmWorkflow.Auto(game).launch()
    val yellow = player(1).requireExplicitUnusedActionCards()
    val rainbow = player(2).requireExplicitUnusedActionCards()
    val blue = player(3).requireExplicitUnusedActionCards()
    val green = player(4).requireExplicitPaymentChoices().requireExplicitUnusedActionCards()
    yellow.doTask("-6 ProjectCard<Hand>")
    rainbow.doTask("-4 ProjectCard<Hand>")
    blue.doTask("-5 ProjectCard<Hand>")
    green.doTask("-6 ProjectCard<Hand>")

    yellow.playCorp(Ecoline, 4)
    rainbow.playCorp(MorningStarInc, 6)
    blue.playCorp(FakeHelion, 5)
    green.playCorp(Factorum, 4)

    // "We'll give you two and we'll give her four." Blue and Rainbow used those TR handicaps;
    // the joking suggestion of six for Green never reached any player record.
    blue.exMachina("2 TerraformRating")
    rainbow.exMachina("4 TerraformRating")

    yellow.turn {
      playPrelude(DomeFarming)
      playPrelude(HugeAsteroid)
    }
    rainbow.turn {
      playPrelude(FakeAppliedScience)
      playPrelude(SpaceLanes)
    }
    blue.turn {
      // "Here for a plant and a card." "Row four, column nine."
      playPrelude(SelfSufficientSettlement) { placeTile(4, 9) }
      playPrelude(TerraformingDeal)
    }
    // The G2 dashboard and later correction show that neither placement bonus was retained yet.
    blue.exMachina("-Plant, -ProjectCard")
    green.turn {
      playPrelude(SuitableInfrastructure)
      // "I raised temp two times. So temp is already at minus twenty."
      playPrelude(AtmosphericEnhancers) { doTask("2 TemperatureStep") }
    }
    // I already forgot to get the SuitableInfrastructure bonus!
    green.exMachina("-2 MC")

    yellow.turn { playProject(SpaceMirrors, 3) }
    rainbow.turn {
      stdAction("DoRequiredActions")
      // Rainbow first paid seven, then took back the evidenced Space Lanes discount.
      rainbow.exMachina(fakeWildTags("VenusTag"))
      playProject(FloatingRefinery, 5)
    }
    blue.turn { playProject(HomeostasisBureau, 16) }
    green.turn { playProject(TitaniumMine, 1, steel = 3) }
    yellow.turn {
      cardAction1(SpaceMirrors)
      playProject(BuildingIndustries, 6)
    }
    rainbow.turn { playProject(MediaGroup, 4) }
    blue.turn { playProject(SolarWindPower, 11) }
    green.turn {
      playProject(OlympusConference, 6, steel = 2)
      playProject(RoboticWorkforce, 9) {
        doTask("CopyProductionBox<$TitaniumMine>")
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    yellow.pass()
    rainbow.turn {
      // "Lava Flows." "Hecate's Tholus ... five one."
      playProject(LavaFlows, 18) { placeTile(5, 1) }
    }
    blue.pass()
    green.turn { cardAction1(Factorum) }
    rainbow.turn { cardAction2(FloatingRefinery) }
    green.pass()
    rainbow.pass(unused = FakeAppliedScience) // didn't know what resource she wanted

    // "Eight four for two cards and nobody gets the cards, of course."
    yellow.wgt("OceanTile<Amazonis_08_04>")

    // Blue noticed the two settlement bonuses while buying cards and restored them together.
    blue.exMachina("Plant, ProjectCard")

    rainbow.buyCards(3)
    blue.buyCards(2)
    green.buyCards(3)
    yellow.buyCards(2)

    rainbow.turn { stdProject("AirScrappingSP") }
    blue.turn { playProject(NuclearZone, 10) { placeTile(3, 3) } }
    green.turn {
      cardAction2(Factorum)
      playProject(Pets, 10)
    }
    yellow.turn { cardAction1(SpaceMirrors) }
    rainbow.turn {
      playProject(SulphurEatingBacteria, 4)
      cardAction1(SulphurEatingBacteria)
    }
    blue.turn { playProject(Sponsors, 6) }
    green.pass()
    yellow.pass()
    rainbow.turn { cardAction1(FloatingRefinery) }
    blue.turn { playProject(NeptunianPowerConsultants, 12, heat = 2) }
    rainbow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
    }
    blue.pass()
    rainbow.turn { playProject(SpaceStation, 1, titanium = 3) }

    rainbow.pass()
    rainbow.wgt("VenusStep")

    blue.buyCards(1)
    green.buyCards(3)
    yellow.buyCards(3)
    rainbow.buyCards(4)

    blue.turn { playProject(LunarBeam, 13) }
    green.turn { playProject(CloudTourism, 11) }
    yellow.turn { cardAction1(SpaceMirrors) }
    rainbow.turn { playProject(IshtarMining, 3) }
    blue.turn { playProject(AdvancedAlloys, 9) }
    green.turn { cardAction2(Factorum) }
    yellow.turn {
      playProject(Flooding, 7) {
        // Blue does not have to spend a heat (couldn't if she wanted to)
        doTask("OceanTile<Amazonis_05_10>! THEN -3 MC<Blue>")
        blue.declineTask()
      }
      playProject(UndergroundCity, 10, steel = 4) { placeTile(10, 10) }
    }
    rainbow.turn {
      rainbow.exMachina(fakeWildTags("VenusTag"))
      playProject(SulphurExports, 14, titanium = 1)
    }
    blue.turn { convertHeat() }
    green.turn {
      playProject(VenusShuttles, 9) { addCardResources(CloudTourism, 2) }
    }
    yellow.turn {
      convertPlants { placeTile(10, 9) }
      convertPlants { placeTile(10, 11) }
    }
    rainbow.turn { cardAction2(FloatingRefinery) }
    blue.pass()
    green.turn {
      sellPatents(2)
      cardAction1(VenusShuttles)
    }
    yellow.turn {
      playProject(NaturalPreserve, 5, steel = 2) { placeTile(1, 4) }
    }
    rainbow.turn { playProject(RoverConstruction, 4, steel = 2) }
    green.turn { cardAction1(CloudTourism) }
    yellow.pass()
    rainbow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
    }
    green.pass()
    rainbow.turn { cardAction1(SulphurEatingBacteria) }
    rainbow.pass()

    blue.wgt("OceanTile<Amazonis_02_06>")
    blue.doTask("UseAction<NeptunianOption, Action1>")
    blue.pay(5)

    green.buyCards(4)
    yellow.buyCards(3)
    rainbow.buyCards(3)
    blue.buyCards(3)

    green.turn {
      playProject(EarthOffice, 1)
      playProject(TopsoilContract, 5)
    }
    yellow.turn { playProject(Sabotage, 1) { doTask("-3 Steel<Green>") } }
    rainbow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
      cardAction2(SulphurEatingBacteria, x = 5)
    }
    blue.turn {
      playProject(ProtectedValley, 18, heat = 5) { placeTile(2, 7) }
      doTask("UseAction<ClaimMilestone, Action1>")
      pay(4, heat = 4)
      doTask("Landshaper")
    }
    green.turn {
      sellPatents(1)
      claimMilestone(cn("Diversifier"))
    }
    yellow.turn { cardAction1(SpaceMirrors) }
    rainbow.turn { playProject(WavePower, 8) }
    blue.pass()
    green.turn { cardAction1(CloudTourism) }
    yellow.turn { playProject(AdaptedLichen, 9) }
    rainbow.turn { playProject(IoSulphurResearch, 15) { doTask("3 ProjectCard") } }
    green.pass(unused = setOf(Factorum, VenusShuttles))
    yellow.pass()
    rainbow.turn {
      rainbow.exMachina(fakeWildTags("ScienceTag"))
      playProject(BactoviralResearch, 10) {
        addCardResources(SulphurEatingBacteria)
      }
      rainbow.exMachina(fakeWildTags("ScienceTag"))
      playProject(AtalantaPlanitiaLab, 8)
    }
    rainbow.pass(unused = FloatingRefinery)

    green.wgt("OxygenStep")

    yellow.buyCards(2)
    rainbow.buyCards(3)
    blue.buyCards(2)
    green.buyCards(2)

    yellow.turn {
      playProject(CuttingEdgeTechnology, 12)
      playProject(MartianSurvey, 7)
    }
    rainbow.turn { claimMilestone(cn("Tactician")) }
    blue.turn {
      playProject(BigAsteroid, 11, titanium = 4) { doTask("-4 Plant<Yellow>") }
    }
    green.turn {
      // "Cost me full price": Green preserved titanium despite the Space tag.
      intentionalUnderpay()
      playProject(TechnologyDemonstration, 5)
      playProject(DiversitySupport, 1)
    }
    // Diversity Support's TR was overlooked at the table; Green's app remains at 23 until Rad-Chem.
    green.exMachina("-TerraformRating")
    yellow.turn { playProject(LagrangeObservatory, 9) }
    rainbow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
      cardAction2(SulphurEatingBacteria, x = 4)
    }
    blue.turn { convertHeat() }
    green.turn { playProject(IcyImpactors, titanium = 5) }
    yellow.pass(unused = SpaceMirrors)
    rainbow.turn {
      // It appears that when she meant to pay 25 she typed just "2"
      exMachina("23 MC")
      // "My Giant Ice Asteroid should be thirty four ... three oceans to place."
      playProject(GiantIceAsteroid, 25, titanium = 3) {
        placeTile(7, 4)
        autoExecNow()
        selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
        blue.doTask("UseAction<NeptunianOption, Action1>")
        blue.pay(5)
        placeTile(9, 4)
        autoExecNow()
        selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
        blue.doTask("UseAction<NeptunianOption, Action1>")
        blue.pay(5)
        // Crossing 0°C supplies Amazonis's temperature-track ocean bonus.
        placeTile(6, 11)
        autoExecNow()
        selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
        blue.doTask("UseAction<NeptunianOption, Action1>")
        blue.pay(5)
        // "Up to six plants from Ellie" cleaned out Yellow's actual five.
        doTask("-5 Plant<Yellow>!")
      }
    }
    // She forgot the Media Group rebate
    rainbow.exMachina("-3 MC")
    // Then she realized she must not have paid, and paid the full 25 (not 23)
    rainbow.exMachina("-25 MC")

    blue.turn {
      playProject(GiantSpaceMirror, 1, titanium = 4)
    }
    green.turn { playProject(RadChemFactory, 6, steel = 1) }
    rainbow.turn { cardAction1(FloatingRefinery) }
    blue.pass()
    // "Three titanium and one real money."
    green.turn { cardAction1(IcyImpactors) { pay(1, titanium = 3) } }
    rainbow.turn { playProject(Moss, 4) }
    green.turn { playProject(VenusGovernor, 4) }
    rainbow.turn { sellPatents(2) }
    green.turn {
      cardAction2(Factorum)
      cardAction1(CloudTourism)
    }
    rainbow.turn {
      playProject(Thermophiles, 7)
      cardAction1(Thermophiles) { addCardResources(SulphurEatingBacteria) }
    }
    green.pass(unused = VenusShuttles)
    rainbow.pass()

    yellow.wgt("TemperatureStep")

    // board-16-07-06.jpg and all four app histories: Generation 6 before Research.

    rainbow.exMachina("5 MC") // full reconciliation
    green.exMachina("1 TerraformRating, 3 MC") // full reconciliation
  }
}
