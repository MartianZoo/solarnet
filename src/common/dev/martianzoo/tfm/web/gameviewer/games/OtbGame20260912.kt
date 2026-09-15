package dev.martianzoo.tfm.web.gameviewer.games

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.web.gameviewer.RecordedGame
import dev.martianzoo.tfm.web.gameviewer.cardnames.*
import dev.martianzoo.tfm.web.gameviewer.fakeWildTags

/** Three-player physical game begun Saturday, 2026-09-12. */
public class OtbGame20260912 : RecordedGame() {
  protected override val config: GameConfig =
      GameConfig(
          """
          VastitasMap
          VenusNextExpansion, PreludeExpansion, Prelude2CardPack, TurmoilExpansion, PromoCardPack
          ColoniesExpansion
          FakeStuffBundle

          Farmer, Generalist, Lobbyist, Philantropist, Producer
          Collector, Forecaster, Landscaper, Politician, Suburbian
          """
      )

  protected override val playerClassPets: String =
      """
      CLASS Green : Player { SetupPhase: PreludeCard }
      CLASS Yellow : Player { SetupPhase: PreludeCard }
      CLASS Blue : Player { SetupPhase: 3 TerraformRating, PreludeCard }
      """
          .trimIndent()

  protected override fun play() {
    TfmWorkflow.Automatic(agents).launch()
    val green = player(1).requireExplicitUnusedActionCards()
    val yellow = player(2).requireExplicitUnusedActionCards()
    val blue = player(3).requireExplicitUnusedActionCards()
    green.doTask("Ok")
    yellow.doTask("-3 ProjectCard<Hand>")
    blue.doTask("-2 ProjectCard<Hand>")

    admin.doTask("MudSlides")
    admin.doTask("VenusInfrastructure")

    green.playCorp(FakeSeptemTribus, 10)
    yellow.playCorp(Pristar, 7)
    blue.playCorp(TychoMagnetics, 8)

    green.turn {
      playPrelude(FakeNobelPrize)
      playPrelude(ExperimentalForest) { placeTile(6, 2) }
      green.exMachina("4 MC")
      playPrelude(FakeHeadStart) {
        useStdAction("PlayCardFromHandAction", payment = {}) {
          this.playProject(SfMemorial, 3, steel = 2)
        }
        useStdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
      }
    }
    yellow.turn {
      playPrelude(MartianIndustries)
      playPrelude(FakeAppliedScience)
      playPrelude(FakeEstablishedMethods) {
        useStdProject("GreeneryProject") { placeTile(5, 9) }
        useStdProject("AsteroidProject")
      }
      yellow.exMachina("4 MC")
    }
    blue.turn {
      playPrelude(AlliedBank)
      playPrelude(DomeFarming)
      playPrelude(PowerGeneration)
    }

    green.turn {
      playProject(ArtificialPhotosynthesis, 12) { doTask("PROD[2 Energy]") }
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") }
    }
    yellow.turn {
      playProject(LavaFlows, 18) { placeTile(2, 6) }
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
    }
    blue.turn {
      playProject(PowerSupplyConsortium, 5) { doTask("PROD[-Energy<Green>]") }
    }
    green.turn {
      playProject(Lichen, 7)
    }
    yellow.turn {
      playProject(HermeticOrderOfMars, 10)
    }
    blue.turn {
      playProject(LunarBeam, 13)
    }
    green.turn {
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Reds>") }
    }
    yellow.turn {
      playProject(TitaniumMine, 7)
    }
    blue.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") }
    }
    green.turn {
      cardAction1(FakeSeptemTribus)
    }
    yellow.turn {
      playProject(SearchForLife, 3)
    }
    blue.turn {
      playProject(DirectedHeatUsage, 1)
    }
    green.turn {
      playProject(OrbitalCleanup, 14)
    }
    yellow.turn {
      cardAction1(SearchForLife) { declineTask() }
    }
    blue.pass(unused = DirectedHeatUsage, TychoMagnetics)
    green.turn {
      cardAction1(OrbitalCleanup)
    }
    yellow.turn {
      playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Blue>]") }
    }
    green.pass()
    yellow.pass(unused = FakeAppliedScience)

    green.exMachina("2 MC")

    green.wgt("OceanTile<Vastitas_5_8>")
    admin.doTask("SponsoredProjects")

    green.exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")

    green.buyCards(4)
    yellow.buyCards(2)
    blue.buyCards(3)

    yellow.turn {
      playProject(SupportedResearch, 3)
    }
    blue.turn {
      playProject(FuelFactory, 6)
    }
    green.turn {
      playProject(InvestmentLoan, 3)
    }
    yellow.turn {
      playProject(LocalShading, 4)
    }
    blue.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Unity>") }
    }
    green.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
    }
    yellow.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Reds>") }
    }
    blue.turn {
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") }
    }
    green.turn {
      playProject(Recruitment, 2) {
        doTask("RecruitmentExchange<Greens>")
        doTask("-PartyDelegate<Greens, Neutral>")
      }
    }

    yellow.turn {
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Reds>") }
    }
    blue.turn {
      playProject(HousePrinting, 10)
    }
    green.turn {
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Kelvinists>") }
    }
    yellow.turn {
      cardAction1(LocalShading)
    }
    blue.pass(unused = DirectedHeatUsage, TychoMagnetics)

    green.turn {
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
    }

    yellow.turn {
      cardAction1(SearchForLife) { declineTask() }
    }
    green.turn {
      claimMilestone(cn("Lobbyist"))
    }
    yellow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(LocalShading) }
    }
    green.turn {
      cardAction1(FakeSeptemTribus)
    }
    yellow.pass()

    green.turn {
      playProject(EventAnalysts, 5)
    }
    green.turn {
      cardAction1(OrbitalCleanup)
    }
    green.turn {
      playProject(PeroxidePower, 7)
    }
    green.turn {
      sellPatents(1)
    }
    green.turn {
      playProject(LightningHarvest, 8)
    }
    green.pass()

    yellow.wgt("OceanTile<Vastitas_5_7>")
    admin.doTask("SpinOffProducts")

    green.buyCards(3)
    yellow.buyCards(3)
    blue.buyCards(3)

    blue.turn {
      cardAction1(DirectedHeatUsage) { doTask("4 MC") }
    }
    green.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<MarsFirst>") }
    }
    yellow.turn {
      playProject(SponsoredAcademies, 9)
      cardAction2(LocalShading)
    }
    blue.turn {
      claimMilestone(cn("Generalist"))
    }
    green.turn {
      cardAction1(FakeSeptemTribus)
    }
    yellow.exMachina("EarthTag<$FakeAppliedScience>")
    yellow.turn {
      playProject(SpaceHotels, 6, titanium = 2)
    }
    yellow.exMachina("-EarthTag<$FakeAppliedScience>")
    blue.turn {
      playProject(NaturalPreserve, 7, steel = 1) { placeTile(4, 1) }
      claimMilestone(cn("Producer"))
    }
    green.turn {
      playProject(WaterSplittingPlant, 12)
    }
    yellow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(LocalShading) }
      playProject(Mine, steel = 2)
    }
    blue.turn {
      cardAction1(TychoMagnetics, x = 5)
    }
    green.turn {
      cardAction1(WaterSplittingPlant)
    }
    yellow.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Unity>") }
      playProject(RedAppeasement, 0)
    }
    yellow.exMachina("-Pass")
    blue.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
    }
    green.turn {
      cardAction1(OrbitalCleanup)
    }
    green.exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
    yellow.pass(unused = SearchForLife)
    blue.pass()
    green.pass()

    blue.wgt("VenusStep")
    admin.doTask("Diversity")

    green.buyCards(0)
    yellow.buyCards(1)
    blue.buyCards(3)

    green.turn {
      playProject(CulturalMetropolis, 20) {
        placeTile(6, 9)
        doTask("2 PartyDelegate<Reds>")
      }
    }
    yellow.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Unity>") }
    }
    blue.turn {
      playProject(MiningArea, steel = 2) { placeTile(4, 2) }
    }
    green.turn {
      playProject(DesignedMicroorganisms, 16)
    }
    yellow.turn {
      playProject(Archaebacteria, 6)
    }
    blue.turn {
      intentionalUnderpay()
      playProject(AsteroidHollowing, 8, titanium = 2)
    }
    green.turn {
      exMachina(fakeWildTags("ScienceTag", 2))
      cardAction1(OrbitalCleanup)
    }
    yellow.turn {
      intentionalUnderpay()
      playProject(SecurityFleet, 12)
      cardAction1(SecurityFleet)
    }
    blue.turn {
      cardAction1(AsteroidHollowing)
    }
    green.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Kelvinists>") }
      playProject(SponsoredMohole, 5)
    }
    yellow.turn {
      playProject(FakeResearchCoordination, 4)
      exMachina("FakeWildTagUse, JovianTag<FakeWildTagUse>, PlantTag<FakeWildTagUse>")
      playProject(InterplanetaryTrade, 27)
    }
    blue.turn {
      playProject(BioPrintingFacility, 5, steel = 1)
      cardAction1(BioPrintingFacility) { doTask("2 Plant") }
    }
    green.turn { cardAction1(FakeSeptemTribus) }
    yellow.turn {
      cardAction1(SearchForLife) { declineTask() }
    }
    blue.turn {
      cardAction1(TychoMagnetics, x = 1)
    }
    green.turn {
      cardAction1(WaterSplittingPlant)
    }
    yellow.turn {
      cardAction2(LocalShading)
    }
    blue.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") } }
    green.pass()
    yellow.turn { cardAction1(FakeAppliedScience) { addCardResources(SecurityFleet) } }
    blue.turn { cardAction1(DirectedHeatUsage) { doTask("4 MC") } }
    yellow.pass()
    blue.turn {
      playProject(BusinessContacts, 7)
      playProject(RadChemFactory, 8)
    }
    blue.turn { sellPatents(2) }
    blue.pass()

    yellow.exMachina("-6 MC")

    green.wgt("OxygenStep")
    admin.doTask("ImprovedEnergyTemplates")

    yellow.buyCards(3)
    blue.buyCards(1)
    blue.exMachina("3 MC")
    green.buyCards(3)

    yellow.turn { fundAward(cn("Collector"), 8) }
    blue.turn {
      playProject(Stratopolis, 22)
      exMachina("PROD[-2 MC]")
    }
    green.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Reds>") }
    }
    yellow.turn {
      playProject(DuskLaserMining, 5, titanium = 1)
      exMachina(fakeWildTags("SpaceTag", 2))
      playProject(Satellites, 1, titanium = 3)
    }
    blue.turn { cardAction1(AsteroidHollowing) }
    green.turn { playProject(EarthCatapult, 23) }
    yellow.turn { cardAction1(SecurityFleet) }
    blue.turn { sellPatents(2) }
    green.turn { playProject(MercurianAlloys, 1) }
    yellow.turn { cardAction2(LocalShading) }
    blue.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<MarsFirst>") } }
    green.turn { cardAction1(FakeSeptemTribus) }
    yellow.turn {
      cardAction1(SearchForLife) { doTask("Science<$SearchForLife>") }
    }
    blue.turn { cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) } }
    green.turn {
      playProject(DeuteriumExport, 1, titanium = 2)
    }
    yellow.turn { cardAction1(FakeAppliedScience) { addCardResources(SecurityFleet) } }
    blue.turn {
      playProject(AsteroidDeflectionSystem, 8, steel = 1, titanium = 1)
      exMachina("PROD[Energy]")
    }
    green.turn { cardAction1(DeuteriumExport) }
    yellow.turn { playProject(ImportOfAdvancedGhg, 9) }
    blue.turn { playProject(DustSeals, 2) }
    green.turn {
      exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")
      cardAction1(OrbitalCleanup)
      exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
    }
    yellow.turn { playProject(RedShips, 2) }
    blue.turn { cardAction1(DirectedHeatUsage) { doTask("4 MC") } }
    green.turn {
      playProject(HeatTrappers, 4) { doTask("PROD[-2 Heat<Yellow>]") }
    }
    yellow.turn { playProject(GhgFactories, 3, steel = 4) }
    blue.turn {
      cardAction1(AsteroidDeflectionSystem) { declineTask() }
    }
    green.turn { playProject(CarbonateProcessing, 4) }
    yellow.turn { cardAction1(RedShips) }
    blue.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<MarsFirst>") } }
    green.turn { playProject(Supercapacitors, 2) }
    yellow.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") } }
    blue.turn { cardAction1(TychoMagnetics, x = 2) }
    green.pass(unused = WaterSplittingPlant)
    yellow.pass()
    blue.turn { cardAction1(BioPrintingFacility) { doTask("2 Plant") } }
    blue.pass()
    green.declineTask()

    yellow.exMachina("6 MC")
    yellow.wgt("OceanTile<Vastitas_4_3>")
    admin.doTask("Revolution")
    blue.exMachina("4 MC")

    blue.buyCards(2)
    green.buyCards(3)
    yellow.buyCards(4)

    blue.turn {
      cardAction1(AsteroidHollowing)
      exMachina("PROD[-MC]")
    }
    green.turn {
      playProject(OptimalAerobraking, 5)
      exMachina("5 MC")
      playProject(CometForVenus, 9) { declineTask() }
    }
    yellow.turn {
      convertHeat()
      convertHeat()
    }
    blue.turn { convertHeat() }

    green.turn {
      convertPlants { placeTile(6, 8) }
    }
    yellow.turn {
      exMachina(fakeWildTags("VenusTag", 2))
      playProject(EnvoysFromVenus, 1) {
        doTask("2 PartyDelegate<Unity>")
      }
      exMachina("-2 PartyDelegate<Unity>")
      green.exMachina("2 PartyDelegate<Unity>")
      exMachina("MC")
      playProject(EcologicalZone, 12) {
        placeTile(4, 8)
        doTask("PartyDelegate<Greens>")
      }
    }
    blue.turn {
      playProject(FoodFactory, 10, steel = 1)
    }
    green.turn {
      playProject(MartianLumberCorp, 4)
    }
    yellow.turn {
      stdAction("UseTurmoilPolicyAction")
    }
    blue.turn { playProject(ReleaseOfInertGases, 14) }
    green.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") } }
    yellow.turn {
      exMachina(fakeWildTags("EarthTag", 2))
      intentionalUnderpay()
      playProject(SummitLogistics, 3, steel = 2, titanium = 1)
    }
    blue.turn { cardAction1(BioPrintingFacility) { doTask("2 Plant") } }
    green.turn {
      exMachina(fakeWildTags("AnimalTag"))
      playProject(AdvancedEcosystems, 9)
    }
    yellow.turn { playProject(DiversitySupport, 1) }
    blue.turn {
      cardAction1(AsteroidDeflectionSystem) { addCardResources(AsteroidDeflectionSystem) }
    }
    green.turn { cardAction2(DeuteriumExport) }
    yellow.turn {
      intentionalUnderpay()
      playProject(MoholeLake, 31) { placeTile(6, 7) }
    }
    blue.turn { cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) } }
    green.turn { cardAction1(FakeSeptemTribus) }
    yellow.turn {
      playProject(AstraMechanica, 7) {
        doWithoutAutoExec(yellow) {
          doTask("ProjectCard FROM PlayedEvent<Class<$DiversitySupport>>")
          doTask("ProjectCard FROM PlayedEvent<Class<$EnvoysFromVenus>>")
        }
      }
      playProject(DiversitySupport, 1)
    }
    blue.turn { cardAction1(TychoMagnetics, x = 1) }
    green.turn { playProject(TransNeptuneProbe, 4) }
    yellow.turn { cardAction1(SecurityFleet) }
    blue.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") } }
    green.turn {
      intentionalUnderpay()
      playProject(AquiferPumping, 16)
    }
    yellow.turn { cardAction1(FakeAppliedScience) { addCardResources(SecurityFleet) } }
    blue.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") } }
    green.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(5, 6)
      }
    }
    yellow.turn { cardAction2(LocalShading) }
    blue.pass(unused = DirectedHeatUsage)
    green.turn {
      exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")
      cardAction1(OrbitalCleanup)
      exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
      playProject(TollStation, 10)
    }
    yellow.turn { cardAction1(MoholeLake) { addCardResources(EcologicalZone) } }
    green.turn { convertHeat() }
    yellow.turn { cardAction1(RedShips) }
    yellow.exMachina("Heat")
    green.pass(unused = WaterSplittingPlant)
    yellow.pass(unused = SearchForLife)

    blue.exMachina("-MC")
    green.declineTask()

    blue.wgt("VenusStep")
    admin.doTask("SnowCover")
    yellow.exMachina("-Chairman<Neutral>, Chairman, TerraformRating")
    blue.exMachina("MC")

    green.buyCards(3)
    yellow.buyCards(4)
    blue.buyCards(2)

    green.turn {
      cardAction1(WaterSplittingPlant)
      convertPlants { placeTile(7, 9) }
    }
    yellow.turn {
      playProject(WgProject, 9) { playPrelude(CorporateArchives) }
    }
    blue.turn {
      playProject(CorporateStronghold, 9, steel = 1) { placeTile(7, 8) }
      convertPlants { placeTile(7, 7) }
      exMachina("-4 MC")
    }
    green.turn { playProject(OrbitalReflectors, 24) }
    yellow.turn {
      exMachina(fakeWildTags("ScienceTag", 2))
      playProject(AntiGravityTechnology, 14)
    }
    blue.turn { cardAction1(DirectedHeatUsage) { doTask("4 MC") } }
    green.turn {
      cardAction1(DeuteriumExport)
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(6, 6)
      }
    }
    yellow.turn {
      intentionalUnderpay()
      playProject(SpaceStation, 5, titanium = 1)
    }
    blue.turn { cardAction1(AsteroidHollowing) }
    green.turn { cardAction1(FakeSeptemTribus) }
    yellow.turn { cardAction1(SecurityFleet) }
    blue.turn { playProject(MartianMediaCenter, 7) }
    green.turn { playProject(Grass, 9) }
    yellow.turn { playProject(Virus, 0) { doTask("-4 Plant<Green>") } }
    blue.turn { playProject(ProtectedValley, 23) { placeTile(3, 2) } }
    green.turn {
      exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")
      cardAction1(OrbitalCleanup)
      exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
      playProject(AdaptedLichen, 7)
    }
    yellow.turn { playProject(ImportOfAdvancedGhg, 5) }
    blue.turn {
      cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) }
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
    }
    green.turn { convertHeat() }
    yellow.turn { cardAction1(FakeAppliedScience) { addCardResources(SecurityFleet) } }
    blue.turn { cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Unity>") } }
    green.turn { convertHeat() }
    yellow.turn { playProject(SnowAlgae, 10) }
    blue.turn { convertPlants { placeTile(8, 9) } }
    green.turn { sellPatents(1) }
    yellow.turn { playProject(Moss, 2) }
    blue.turn { cardAction1(AsteroidDeflectionSystem) { declineTask() } }
    green.turn {
      playProject(TopsoilContract, 6)
      exMachina("MC")
    }
    yellow.turn { cardAction1(MoholeLake) { addCardResources(EcologicalZone) } }
    blue.turn { stdProject("AirScrappingProject") }
    green.pass()
    yellow.turn { cardAction1(LocalShading) }
    blue.turn { cardAction1(TychoMagnetics, x = 1) }
    yellow.turn { playProject(EnergyMarket, 1) }
    blue.turn { cardAction1(BioPrintingFacility) { doTask("2 Plant") } }
    yellow.turn {
      playProject(Greenhouses, steel = 2)
      convertPlants { placeTile(4, 7) }
    }
    blue.turn { playProject(OutdoorSports, 8) }
    yellow.turn {
      convertHeat()
      cardAction1(RedShips)
    }
    blue.pass()
    yellow.turn {
      stdProject("AsteroidProject")
      playProject(Trees, 11)
    }
    yellow.pass(unused = SearchForLife, EnergyMarket)

    green.declineTask()

    green.wgt("OxygenStep")
    admin.doTask("EcoSabotage")

    yellow.buyCards(2)
    blue.buyCards(1)
    green.buyCards(1)

    yellow.turn {
      convertPlants { placeTile(4, 6) }
      intentionalUnderpay()
      playProject(GiantSolarShade, 19, titanium = 1)
    }
    blue.turn {
      playProject(MagneticFieldGeneratorsPromo, 20, steel = 1) { placeTile(5, 4) }
    }
    green.turn {
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<MarsFirst>") }
      exMachina("5 MC")
      intentionalUnderpay()
      playProject(FrontierTown, 9) {
        placeTile(5, 5)
        placeTile(3, 3)
      }
    }
    yellow.turn {
      playProject(OpenCity, 17, steel = 2) { placeTile(3, 6) }
      convertHeat()
    }
    blue.turn {
      cardAction1(AsteroidHollowing)
      playProject(StratosphericBirds, 12)
    }
    green.turn {
      convertHeat()
      stdProject("CityProject") { placeTile(3, 4) }
    }
    yellow.turn { playProject(Research, 9) }
    blue.turn { playProject(VenusianAnimals, 15) }
    green.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(5, 3)
      }
      convertPlants { placeTile(4, 4) }
    }
    yellow.turn { playProject(SolarWindPower, 3, titanium = 1) }
    blue.turn { cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) } }
    green.turn { cardAction1(FakeSeptemTribus) }
    yellow.turn { cardAction1(SecurityFleet) }
    blue.turn {
      cardAction1(AsteroidDeflectionSystem) { addCardResources(AsteroidDeflectionSystem) }
    }
    green.turn {
      playProject(TundraFarming, 14)
      convertPlants { placeTile(4, 5) }
    }
    yellow.turn {
      playProject(DawnCity, 7, titanium = 1)
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Unity>") }
    }
    blue.turn {
      cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Greens>") }
    }
    green.turn {
      exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")
      cardAction1(OrbitalCleanup)
      exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
    }
    yellow.turn { cardAction1(MoholeLake) { addCardResources(EcologicalZone) } }
    blue.turn { cardAction1(StratosphericBirds) }
    green.turn { cardAction2(DeuteriumExport) }
    yellow.turn { cardAction1(FakeAppliedScience) { addCardResources(SecurityFleet) } }
    blue.turn { cardAction1(TychoMagnetics, x = 1) }
    green.turn { playProject(ParliamentHall, 6) }
    yellow.turn { cardAction1(RedShips) }
    blue.turn { sellPatents(4) }
    green.turn {
      exMachina(
          "FakeWildTagUse, EarthTag<FakeWildTagUse>, JovianTag<FakeWildTagUse>, VenusTag<FakeWildTagUse>"
      )
      playProject(LuxuryFoods, 6)
    }
    yellow.turn { playProject(Farming, 14) }
    blue.turn {
      cardAction1(BioPrintingFacility) { addCardResources(VenusianAnimals) }
    }
    green.pass(unused = WaterSplittingPlant)
    yellow.turn { playProject(Heather, 4) }
    blue.turn { cardAction1(DirectedHeatUsage) { doTask("4 MC") } }
    yellow.turn { cardAction2(LocalShading) }
    blue.pass()
    yellow.pass(unused = SearchForLife, EnergyMarket)

    green.declineTask()

    yellow.wgt("TemperatureStep")
    admin.doTask("InterplanetaryTradeGlobalEvent")
    green.exMachina("-MC")
    yellow.exMachina("-MC")

    blue.buyCards(1)
    green.buyCards(2)
    yellow.buyCards(2)

    blue.turn { stdProject("AquiferProject") { placeTile(7, 6) } }
    green.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") }
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<MarsFirst>") }
    }
    yellow.turn {
      stdProject("CityProject") { placeTile(8, 7) }
      convertPlants { placeTile(9, 7) }
    }
    blue.turn {
      cardAction1(DirectedHeatUsage) { doTask("2 Plant") }
      convertPlants { placeTile(2, 2) }
    }
    green.turn {
      stdProject("GreeneryProject") { placeTile(3, 5) }
      convertPlants { placeTile(5, 2) }
    }
    yellow.turn {
      stdProject("CityProject") { placeTile(2, 1) }
      convertPlants { placeTile(9, 8) }
      exMachina("-Steel, Titanium")
    }
    blue.turn {
      stdProject("CityProject") { placeTile(6, 4) }
      exMachina("PROD[-MC]")
    }
    green.turn { playProject(LagrangeObservatory, 7) }
    yellow.turn { playProject(Decomposers, 3) }
    blue.turn { cardAction1(AsteroidHollowing) }
    green.turn {
      exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")
      cardAction1(OrbitalCleanup)
      exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
      fundAward(cn("Landscaper"), 14)
    }
    yellow.turn {
      playProject(BactoviralResearch, 8) { addCardResources(Decomposers) }
      exMachina("Microbe<$Decomposers>")
    }
    blue.turn { cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Reds>") } }
    green.turn { playProject(Mangrove, 10) { placeTile(6, 5) } }
    yellow.turn {
      playProject(NoctisFarming, steel = 4)
      cardAction1(SecurityFleet)
    }
    blue.turn {
      playProject(DevelopmentCenter, 3, steel = 4)
      cardAction1(DevelopmentCenter)
    }
    green.turn {
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Reds>") }
      exMachina(fakeWildTags("JovianTag", 2))
      playProject(DiasporaMovement, 5)
    }
    yellow.turn { cardAction1(LocalShading) }
    blue.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<Unity>") } }
    green.turn { cardAction1(FakeSeptemTribus) }
    yellow.turn { cardAction2(EnergyMarket) }
    blue.turn {
      cardAction1(AsteroidDeflectionSystem) { addCardResources(AsteroidDeflectionSystem) }
    }
    green.turn { fundAward(cn("Politician"), 20) }
    yellow.turn { playProject(PhobosSpaceHaven, 12, titanium = 3) }
    blue.turn { cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) } }
    green.pass(unused = WaterSplittingPlant, AquiferPumping, DeuteriumExport)
    yellow.turn { sellPatents(1) }
    blue.turn { playProject(UnexpectedApplication, 4) }
    yellow.turn { playProject(ArtificialLake, 7, steel = 3) }
    blue.turn {
      playProject(SubCrustMeasurements, 20)
      exMachina("-2 MC")
      cardAction1(SubCrustMeasurements)
    }
    yellow.turn { cardAction1(MoholeLake) { addCardResources(EcologicalZone) } }
    blue.turn { cardAction1(StratosphericBirds) }
    yellow.turn { sellPatents(1) }
    blue.turn {
      exMachina("ProjectCard")
      sellPatents(2)
    }
    yellow.turn {
      cardAction1(RedShips)
      exMachina("MC")
    }
    blue.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<Greens>") } }
    yellow.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Greens>") }
    }
    blue.pass(unused = TychoMagnetics, BioPrintingFacility)
    yellow.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<Kelvinists>") } }
    yellow.pass(unused = SearchForLife, FakeAppliedScience)

    green.declineTask()
    yellow.exMachina("Microbe<$Decomposers>, -Science<$SearchForLife>, -Animal<$EcologicalZone>")

    blue.declineTask()
    green.declineTask()
    yellow.convertPlants { placeTile(8, 6) }
    yellow.declineTask()
  }
}
