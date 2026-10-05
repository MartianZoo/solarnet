package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.generated.Biologist
import dev.martianzoo.generated.Botanist
import dev.martianzoo.generated.Class
import dev.martianzoo.generated.Collector
import dev.martianzoo.generated.Diversifier
import dev.martianzoo.generated.Forecaster
import dev.martianzoo.generated.HellasMap
import dev.martianzoo.generated.Mayor
import dev.martianzoo.generated.PreludeExpansion
import dev.martianzoo.generated.PromoCardPack
import dev.martianzoo.generated.SpaceBaron
import dev.martianzoo.generated.Sponsor
import dev.martianzoo.generated.Trader
import dev.martianzoo.generated.Tycoon
import dev.martianzoo.generated.gameConfig
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete archive replay: Synthetic Proton Fragment (g9ea8656f1c7e)
// https://terraforming-mars.herokuapp.com/the-end?id=p9d6d3ff25b39
internal class SyntheticProtonFragmentTest : AbstractFullGameTest() {
  // Player-record evidence: Hellas, Corporate Era, Prelude, promo cards, drafting, fast mode,
  // three players, no Venus/Colonies/Turmoil, and these full-random milestone and award pools.
  override val config =
      gameConfig(
          modules =
              listOf(Class.of(HellasMap), Class.of(PreludeExpansion), Class.of(PromoCardPack)),
          milestones =
              listOf(
                  Class.of(Mayor),
                  Class.of(Diversifier),
                  Class.of(Trader),
                  Class.of(Sponsor),
                  Class.of(Tycoon),
              ),
          awards =
              listOf(
                  Class.of(Biologist),
                  Class.of(SpaceBaron),
                  Class.of(Forecaster),
                  Class.of(Botanist),
                  Class.of(Collector),
              ),
      )
  // Player-record evidence: Purple has a five-TR handicap.
  override val playerClassPets =
      """
      CLASS Player1 : Player { SetupPhase: 5 TerraformRating<This> }
      CLASS Player2 : Player
      CLASS Player3 : Player
      """
          .trimIndent()

  @Test
  internal fun game20260811() {
    TfmWorkflow.Automatic(agents).launch()
    val purple = p1
    val pink = p2
    val green = p3

    admin.assertCounts(1 to "Generation")

    purple.playCorp(Recyclon) {
      purple.buyCards(4)
    }

    pink.playCorp(RobinsonIndustries) {
      pink.buyCards(5)
    }

    green.playCorp(SpliceTacticalGenomics, 7) { doTask("2 MC") }

    purple.turn {
      playPrelude(AlbedoPlants).expect("PROD[Plant], Plant, 3 Heat")
      playPrelude(SocietySupport).expect("PROD[-1 MC, Plant, Energy, Heat]")
    }

    pink.turn {
      playPrelude(MetalsCompany).expect("PROD[1 MC, Steel, Titanium]")
      playPrelude(Biolab)
    }

    green.turn {
      playPrelude(Merger) {
        playCorp(Inventrix)
      }
      playPrelude(MoholeExcavation).expect("PROD[Steel, 2 Heat], 2 Heat")
    }

    purple.playProject(DeepWellHeating, 13)
    purple.declineSecondAction()

    pink.playProject(EarthCatapult, 23)
    pink.cardAction1(RobinsonIndustries) {
      // Titanium production is not tied for lowest, so Robinson Industries cannot raise it.
      shouldThrow<NarrowingException> { doTask("PROD[Titanium]") }
      doTask("PROD[Heat]")
    }

    // Splice Tactical Genomics' mandatory first action prevents Green from playing Lichen first.
    shouldThrow<RequirementException> { green.playProject(Lichen, 7) }

    green.stdAction("DoRequiredActionsAction")
    green.playProject(Lichen, 7)

    // (Purple already passed early)
    purple.pass()
    pink.pass()
    green.playProject(SolarLogistics, 20)
    green.pass()

    // Game20260811-dashboards-gen2.png was taken before cards were bought.
    purple.assertResources(m = 38, s = 1, t = 0, p = 3, e = 2, h = 4)
    purple.assertProduction(m = -1, s = 1, t = 0, p = 2, e = 2, h = 1)
    pink.assertResources(m = 26, s = 1, t = 1, p = 1, e = 0, h = 1)
    pink.assertProduction(m = 1, s = 1, t = 1, p = 1, e = 0, h = 1)
    green.assertResources(m = 23, s = 1, t = 2, p = 1, e = 0, h = 4)
    green.assertProduction(m = 0, s = 1, t = 0, p = 1, e = 0, h = 2)
    pink.buyCards(1)
    purple.buyCards(2)
    green.buyCards(3)

    pink.playProject(MediaGroup, 4)
    pink
        .playProject(Sabotage, 0) {
          doTask("-7 MC<Player1>")
        }
        .expect("3 MC")

    green.playProject(AsteroidRights, 2, titanium = 2)
    green.cardAction2(AsteroidRights) { doTask("2 Titanium") }

    purple.playProject(ReleaseOfInertGases, 14)
    purple
        .playProject(MagneticFieldDome, 3, steel = 1) {
          doTask("-2 Microbe<$Recyclon> THEN PROD[Plant]")
        }
        .expect("PROD[2 Plant, -2 Energy]")

    pink.playProject(SolarWindPower, 6, titanium = 1).expect("PROD[Energy], Titanium")
    pink
        .playProject(HiredRaiders, 0) {
          doTask("3 MC<Player2> FROM MC<Player3>")
        }
        .expect("6 MC")

    green.playProject(Archaebacteria, 6) { doTask("2 MC") }
    green.declineSecondAction()
    purple.pass()

    pink.playProject(LagrangeObservatory, 1, titanium = 2)
    pink.cardAction1(RobinsonIndustries) { doTask("PROD[Titanium]") }
    // (Green already passed early)
    green.pass()
    pink.playProject(MiningRights, 5, steel = 1) {
      placeTile(8, 8)
    }
    pink.playProject(DirectedImpactors, 3, titanium = 1)
    pink.cardAction1(DirectedImpactors) {
      pink.pay(6)
      addCardResources(DirectedImpactors)
    }
    pink.doTask("Pass")

    // Game20260811-dashboards-gen3.png was taken before cards were bought.
    purple.assertResources(m = 36, s = 1, t = 0, p = 7, e = 0, h = 7)
    purple.assertProduction(m = -1, s = 1, t = 0, p = 4, e = 0, h = 1)
    pink.assertResources(m = 24, s = 1, t = 3, p = 2, e = 1, h = 2)
    pink.assertProduction(m = 1, s = 1, t = 3, p = 1, e = 1, h = 1)
    green.assertResources(m = 27, s = 2, t = 2, p = 3, e = 0, h = 6)
    green.assertProduction(m = 0, s = 1, t = 0, p = 2, e = 0, h = 2)
    purple.buyCards(2)
    pink.buyCards(3)
    green.buyCards(3)

    green.playProject(FueledGenerators, 1).expect("PROD[-1 MC, Energy]")
    green.cardAction2(AsteroidRights) { doTask("2 Titanium") }

    purple.playProject(PeroxidePower, 5, steel = 1).expect("PROD[-1 MC, 2 Energy]")
    purple.playProject(Hospitals, 8)

    pink.cardAction1(RobinsonIndustries) { doTask("PROD[Steel]") }
    pink.playProject(GhgFactories, 7, steel = 1).expect("PROD[-Energy, 4 Heat]")

    // Green does not meet Diversifier before Corporate Stronghold enters play.
    shouldThrow<RequirementException> {
      green.stdAction("ClaimMilestoneAction") { doTask("Diversifier") }
    }
    green
        .playProject(CorporateStronghold, 7, steel = 2) {
          placeTile(2, 4)
        }
        .expect("Disease<Player1>")
    purple.assertCounts(7 to "Plant")
    green.stdAction("ClaimMilestoneAction") { doTask("Diversifier") }

    purple.playProject(IndustrialCenter, 4) {
      placeTile(2, 3)
      doTask("-2 Microbe<$Recyclon> THEN PROD[Plant]")
    }
    purple.assertCounts(8 to "Plant")
    purple.convertPlants {
      placeTile(1, 2)
    }

    pink.cardAction1(DirectedImpactors) {
      pink.pay(titanium = 2)
      addCardResources(DirectedImpactors)
    }
    pink.declineSecondAction()
    green.pass()

    purple
        .playProject(EcologicalZone, 12) {
          placeTile(1, 3)
        }
        .expect("2 Plant")
    purple.convertHeat()
    // (Pink already passed early)
    pink.pass()
    purple.pass()

    // Game20260811-dashboards-gen4.png was taken before cards were bought.
    purple.assertResources(m = 30, s = 1, t = 0, p = 9, e = 1, h = 3)
    purple.assertProduction(m = -2, s = 1, t = 0, p = 5, e = 1, h = 1)
    pink.assertResources(m = 25, s = 2, t = 4, p = 3, e = 0, h = 8)
    pink.assertProduction(m = 1, s = 2, t = 3, p = 1, e = 0, h = 5)
    green.assertResources(m = 24, s = 2, t = 4, p = 6, e = 0, h = 8)
    green.assertProduction(m = 2, s = 1, t = 0, p = 2, e = 0, h = 2)
    pink.buyCards(3)
    purple.buyCards(2)
    green.buyCards(2)

    purple.stdProject("AquiferProject") {
      placeTile(2, 1)
    }
    purple.convertPlants {
      placeTile(3, 3)
    }

    pink.convertHeat()
    pink
        .playProject(PublicPlans, 5) {
          // Source log: six cards revealed and 6 M€ gained.
          doTask("6 ProjectCard<Revealed FROM Hand>")
        }
        .expect("4 MC")

    green.playProject(HeatTrappers, 2, steel = 2) {
      // Purple has only one heat production, so Green cannot choose her instead.
      shouldThrow<LimitsException> { doTask("PROD[-2 Heat<Player1>]") }
      shouldThrow<NarrowingException> { doTask("PROD[-Heat<Player1>]") }
      doTask("PROD[-2 Heat<Player2>]")
    }
    green.playProject(RoboticWorkforce, 9) {
      doTask("CopyProductionBox<$HeatTrappers>")
      doTask("PROD[-2 Heat<Player2>]")
    }

    purple
        .playProject(NaturalPreserve, 5, steel = 2) {
          placeTile(3, 7)
        }
        .expect("PROD[1 MC], Plant")
    purple.sellPatents(1)

    pink.playProject(ProtectedGrowth, mc = 0)
    pink.playProject(Soletta, 21, titanium = 4).expect("PROD[7 Heat]")

    green.cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }
    // Reason 3: Mercurian Alloys makes Green's retained titanium worth more after this payment; the
    // next authoritative dashboard confirms that he retained all four.
    green.intentionalUnderpay()
    green.playProject(MercurianAlloys, 3)

    purple.cardAction1(Hospitals)
    purple
        .playProject(Psychrophiles, 2) {
          addCardResources(Psychrophiles)
        }
        .expect("2 MC<Player3>")

    pink.playProject(Mine, steel = 1)
    pink.declineSecondAction()
    green.pass()

    purple.cardAction1(Psychrophiles)
    purple.declineSecondAction()
    // (Pink already passed early)
    pink.pass()
    purple.pass()

    // Game20260811-dashboards-gen5.png was taken before cards were bought.
    purple.assertResources(m = 33, s = 1, t = 0, p = 9, e = 1, h = 5)
    purple.assertProduction(m = -1, s = 1, t = 0, p = 5, e = 1, h = 1)
    pink.assertResources(m = 24, s = 4, t = 3, p = 5, e = 0, h = 9)
    pink.assertProduction(m = 1, s = 3, t = 3, p = 1, e = 0, h = 9)
    green.assertResources(m = 27, s = 1, t = 4, p = 8, e = 2, h = 10)
    green.assertProduction(m = 2, s = 1, t = 0, p = 2, e = 2, h = 2)
    purple.buyCards(1)
    pink.buyCards(3)
    green.buyCards(3)

    // Pink does not meet Tycoon before Hermetic Order of Mars enters play.
    shouldThrow<RequirementException> {
      pink.stdAction("ClaimMilestoneAction") { doTask("Tycoon") }
    }
    pink.playProject(HermeticOrderOfMars, 8).expect("PROD[2 MC], -2 MC")
    pink.stdAction("ClaimMilestoneAction") { doTask("Tycoon") }

    green.playProject(Ants, 9) { doTask("2 MC") }
    green.cardAction1(Ants) {
      doTask("-Microbe<Player1, $Recyclon<Player1>>")
    }

    purple.cardAction1(Psychrophiles)
    purple.stdProject("PowerPlantProject")

    pink.convertHeat()
    pink.cardAction2(DirectedImpactors)

    green.playProject(WeatherBalloons, 11)
    green.cardAction1(WeatherBalloons)

    purple.playProject(TollStation, 12).expect("PROD[7 MC]")
    purple.convertPlants {
      placeTile(4, 4)
    }

    pink.playProject(BribedCommittee, 5)
    pink.sellPatents(1)

    green.playProject(DiversitySupport, 1)
    green.cardAction2(AsteroidRights) { doTask("2 Titanium") }

    purple.cardAction1(IndustrialCenter)
    purple.declineSecondAction()

    pink.cardAction1(RobinsonIndustries)
    pink.declineSecondAction()

    green.playProject(CallistoPenalMines, titanium = 6)
    green.convertHeat()

    // (Purple already passed early)
    purple.pass()
    // (Pink already passed early)
    pink.pass()
    green.convertPlants {
      placeTile(1, 4)
    }
    green.doTask("Pass")

    // Game20260811-dashboards-gen6.png was taken before cards were bought.
    purple.assertResources(m = 40, s = 5, t = 0, p = 6, e = 2, h = 7)
    purple.assertProduction(m = 6, s = 2, t = 0, p = 5, e = 2, h = 1)
    pink.assertResources(m = 28, s = 7, t = 6, p = 6, e = 1, h = 11)
    pink.assertProduction(m = 3, s = 3, t = 3, p = 1, e = 1, h = 10)
    green.assertResources(m = 29, s = 3, t = 0, p = 3, e = 2, h = 6)
    green.assertProduction(m = 5, s = 1, t = 0, p = 2, e = 2, h = 2)
    purple.buyCards(2)
    green.buyCards(2)
    pink.buyCards(3)

    green.playProject(CupolaCity, 10, steel = 3) {
      placeTile(4, 3)
    }
    green.cardAction1(Ants) {
      doTask("-Microbe<Player1, $Psychrophiles<Player1>>")
    }

    purple.stdAction("ClaimMilestoneAction") { doTask("Trader") }
    purple
        .playProject(ProtectedValley, 9, steel = 5) {
          doTask("-2 Microbe<$Psychrophiles>")
          placeTile(1, 1)
        }
        .expect("2 Plant, -5 Steel")

    pink.playProject(LavaFlows, 16) {
      placeTile(2, 2)
    }
    pink.convertPlants {
      placeTile(8, 7)
    }

    green.cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }
    green.playProject(Hackers, 3) { doTask("PROD[-2 MC<Player2>]") }

    purple.convertHeat()
    purple.stdProject("AquiferProject") {
      placeTile(4, 7)
    }

    pink.convertHeat()
    pink.cardAction1(RobinsonIndustries) { doTask("PROD[Plant]") }

    // The temperature is now too high for Designed Microorganisms.
    shouldThrow<RequirementException> {
      green.playProject(DesignedMicroorganisms, 9)
    }
    // Viral Enhancers reacts to its own tag.
    green
        .playProject(ViralEnhancers, 9) {
          doTask("Plant")
          doTask("2 MC")
        }
        .expect("Plant")
    green.cardAction2(WeatherBalloons)

    purple.cardAction1(Psychrophiles)
    purple.convertPlants {
      placeTile(3, 6)
    }

    pink.cardAction2(DirectedImpactors)
    pink.playProject(RadChemFactory, mc = 0, steel = 3)

    green.pass()
    purple.pass()
    pink.pass()

    // We have no screencap for generation 7, so these only assert what the actual values happen to
    // be.
    purple.assertResources(m = 49, s = 2, t = 0, p = 8, e = 2, h = 5)
    purple.assertProduction(m = 8, s = 2, t = 0, p = 5, e = 2, h = 1)
    pink.assertResources(m = 37, s = 7, t = 9, p = 2, e = 0, h = 16)
    pink.assertProduction(m = 1, s = 3, t = 3, p = 2, e = 0, h = 10)
    green.assertResources(m = 39, s = 2, t = 0, p = 6, e = 0, h = 10)
    green.assertProduction(m = 10, s = 1, t = 0, p = 2, e = 0, h = 2)
    pink.buyCards(3)
    purple.buyCards(1)
    green.buyCards(2)

    purple.stdProject("CityProject") {
      placeTile(4, 5)
    }
    purple.convertPlants {
      placeTile(5, 5)
    }

    pink.cardAction1(RobinsonIndustries)
    pink.playProject(DuskLaserMining, titanium = 2).expect("PROD[Titanium, -Energy], 2 Titanium")

    green.cardAction1(Ants) {
      doTask("-Microbe<Player1, $Recyclon<Player1>>")
    }
    green.cardAction2(AsteroidRights) { doTask("2 Titanium") }

    purple.cardAction1(Psychrophiles)
    purple.playProject(BioPrintingFacility, 1, steel = 3)

    pink.stdAction("FundAwardAction") { doTask("SpaceBaron") }
    pink.playProject(AsteroidCard, titanium = 4) {
      doTask("-3 Plant<Player3>")
    }
    green.stdProject("AsteroidProject")
    green.convertHeat() {
      placeTile(3, 1)
    }

    purple.cardAction1(BioPrintingFacility) { addCardResources(EcologicalZone) }
    purple
        .playProject(ImportedNutrients, 14) {
          addCardResources(Recyclon)
        }
        .expect("4 Plant")
    pink.convertHeat()
    pink.convertHeat()

    green.playProject(SterlingVents, 1, steel = 2).expect("PROD[2 Energy, -2 Heat]")
    green.playProject(Algae, 10) { doTask("Plant") }.expect("PROD[2 Plant], 2 Plant")

    purple
        .playProject(Supercapacitors, 4) {
          doTask("-2 Microbe<$Recyclon>")
        }
        .expect("PROD[1 MC, Plant]")
    purple.cardAction1(Hospitals)

    pink.playProject(IoMiningIndustries, 12, titanium = 9).expect("PROD[2 MC, 2 Titanium]")
    pink.cardAction1(DirectedImpactors) {
      pink.pay(6)
      addCardResources(DirectedImpactors)
    }

    green.sellPatents(1)
    green.playProject(Grass, 11) { doTask("Plant") }.expect("PROD[Plant], 4 Plant")

    purple.pass()
    pink.pass()
    green.convertPlants {
      placeTile(3, 2)
    }
    green.cardAction1(WeatherBalloons)
    green.doTask("Pass")

    // Game20260811-dashboards-gen8.png was taken before cards were bought.
    purple.assertResources(m = 55, s = 2, t = 0, p = 10, e = 2, h = 6)
    purple.assertProduction(m = 10, s = 2, t = 0, p = 6, e = 2, h = 1)
    pink.assertResources(m = 39, s = 10, t = 6, p = 4, e = 0, h = 10)
    pink.assertProduction(m = 3, s = 3, t = 6, p = 2, e = 0, h = 10)
    green.assertResources(m = 41, s = 1, t = 2, p = 8, e = 2, h = 2)
    green.assertProduction(m = 10, s = 1, t = 0, p = 5, e = 2, h = 0)
    green.assertCounts(9 to "ProjectCard")
    pink.buyCards(3)
    green.buyCards(3)
    purple.buyCards(3)
    pink.convertHeat()
    pink.cardAction2(DirectedImpactors)
    green.cardAction1(Ants) {
      doTask("-Microbe<Player1, $Recyclon<Player1>>")
    }
    green.stdAction("FundAwardAction", which = 2) { doTask("Forecaster") }

    purple.stdProject("AquiferProject") {
      placeTile(5, 6)
    }
    purple.convertPlants {
      placeTile(6, 6)
    }

    pink
        .playProject(LakeMarineris, 16) {
          doTask("OceanTile<Hellas_4_6>")
          doTask("OceanTile<Hellas_5_7>")
        }
        .expect("Plant, -6 MC, 3 Heat")
    pink
        .playProject(MiningExpedition, 10) {
          doTask("-2 Plant<Player3>")
        }
        .expect("2 Steel, -7 MC")

    green
        .playProject(IceAsteroid, 15, titanium = 2) {
          doTask("OceanTile<Hellas_7_3>")
          doTask("OceanTile<Hellas_4_1>")
        }
        .expect("0 Titanium")
    green.cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }

    purple.cardAction1(BioPrintingFacility) { addCardResources(EcologicalZone) }
    purple
        .playProject(FieldCappedCity, 25, steel = 2) {
          placeTile(6, 5)
          doTask("-2 Microbe<$Recyclon> THEN PROD[Plant]")
        }
        .expect("PROD[2 MC, Energy, Plant], 3 Plant, Disease")

    pink
        .playProject(ConvoyFromEuropa, 1, titanium = 4) {
          placeTile(5, 8)
        }
        .expect("6 MC")
    pink.playProject(VestaShipyard, 7, titanium = 2)

    green.cardAction2(WeatherBalloons)
    green.sellPatents(1)

    purple.cardAction1(Psychrophiles)
    purple.cardAction1(Hospitals)

    pink.cardAction1(RobinsonIndustries)
    pink.playProject(SaturnSurfing, 11).expect("3 Floater")

    green.playProject(RobotPollinators, 9)
    green.convertPlants {
      placeTile(4, 2)
    }

    purple.playProject(InventionContest, 2)
    purple.playProject(ProjectInspection, mc = 0) {
      doTask("UseAction<$Hospitals, Action1>")
    }

    pink.cardAction1(SaturnSurfing)
    pink.declineSecondAction()
    green.sellPatents(5)
    green.playProject(EquatorialMagnetizer, 9, steel = 1)

    purple.playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Player3>]") }
    purple.declineSecondAction()
    // (Pink already passed early)
    pink.pass()
    green.cardAction1(EquatorialMagnetizer)
    green.declineSecondAction()
    // (Purple already passed early)
    purple.pass()
    // (Green already passed early)
    green.doTask("Pass")

    // Game20260811-dashboards-gen9.png was taken before cards were bought.
    purple.assertResources(m = 62, s = 2, t = 0, p = 12, e = 4, h = 7)
    purple.assertProduction(m = 12, s = 2, t = 0, p = 7, e = 4, h = 1)
    pink.assertResources(m = 48, s = 15, t = 7, p = 7, e = 1, h = 15)
    pink.assertProduction(m = 3, s = 3, t = 7, p = 2, e = 1, h = 10)
    green.assertResources(m = 41, s = 1, t = 2, p = 9, e = 0, h = 4)
    green.assertProduction(m = 10, s = 1, t = 0, p = 6, e = 0, h = 0)
    green.assertCounts(5 to "ProjectCard")
    green.buyCards(1)
    purple.buyCards(3)
    pink.buyCards(2)

    green.stdProject("GreeneryProject") {
      placeTile(2, 5)
    }
    green.convertPlants {
      placeTile(5, 3)
    }

    purple
        .convertPlants {
          placeTile(7, 6)
        }
        .expect("0 TerraformRating")
    purple.playProject(ProtectedHabitats, 5)

    pink
        .stdProject("CityProject") {
          placeTile(7, 7)
        }
        .expect("Disease<Player1>")
    pink.playProject(HousePrinting, steel = 4)

    green.playProject(TechnologyDemonstration, 1, titanium = 1)
    green.sellPatents(1)

    purple
        .playProject(AdaptedLichen, 3) {
          doTask("-3 Microbe<$Psychrophiles>")
        }
        .expect("Animal")
    purple.cardAction1(BioPrintingFacility) { addCardResources(EcologicalZone) }

    pink.cardAction1(SaturnSurfing)
    pink.playProject(BeamFromAThoriumAsteroid, 9, titanium = 7)

    green.playProject(Windmills, 4, steel = 1)
    green.playProject(BactoviralResearch, 10) {
      doTask("Plant")
      doTask("2 MC")
      addCardResources(Ants)
    }

    purple.cardAction1(Psychrophiles)
    purple.cardAction1(Hospitals)

    pink
        .playProject(MagneticFieldGeneratorsPromo, steel = 10) {
          placeTile(5, 1)
        }
        .expect("PROD[2 Plant, -4 Energy], 2 MC")
    pink.sellPatents(1)

    green
        .cardAction1(Ants) {
          // Protected Habitats now prevents Green from removing Purple's microbe.
          shouldThrow<DeadEndException> { doTask("-Microbe<Player1, $Psychrophiles<Player1>>") }
          doTask("-Microbe<Player3, $Ants<Player3>>")
        }
        .expect("0 Microbe<$Ants>")
    green.cardAction1(EquatorialMagnetizer)

    purple.sellPatents(3)
    purple.stdProject("CityProject") {
      placeTile(1, 5)
    }

    pink.sellPatents(1)
    pink.sellPatents(1)

    green.cardAction2(AsteroidRights) { doTask("2 Titanium") }
    green.cardAction1(WeatherBalloons)

    purple.stdAction("FundAwardAction", which = 3) { doTask("Botanist") }
    purple.cardAction1(IndustrialCenter)

    pink.pass()
    green.sellPatents(6)
    green.declineSecondAction()
    purple.pass()
    green.pass()

    // Decline Purple's first final greenery placement.
    purple.declineTask()
    green.convertPlants { placeTile(3, 5) }
    // Decline another final greenery placement for Green.
    green.declineTask()
    purple.convertPlants { placeTile(2, 6) }
    // Decline another final greenery placement for Purple.
    purple.declineTask()
    pink.convertPlants { placeTile(7, 8) }
    // Decline another final greenery placement for Pink.
    pink.declineTask()
    purple.assertCounts(0 to "ProjectCard")
    pink.assertCounts(0 to "ProjectCard")
    green.assertCounts(0 to "ProjectCard")
    with(purple) {
      assertResources(m = 56, s = 5, t = 0, p = 6, e = 4, h = 13)
      assertProduction(m = 13, s = 3, t = 0, p = 8, e = 4, h = 1)
    }
    with(pink) {
      assertResources(m = 63, s = 5, t = 7, p = 3, e = 0, h = 29)
      assertProduction(m = 4, s = 4, t = 7, p = 4, e = 0, h = 13)
    }
    with(green) {
      assertResources(m = 57, s = 1, t = 3, p = 1, e = 0, h = 4)
      assertProduction(m = 10, s = 1, t = 0, p = 6, e = 0, h = 0)
    }

    purple.assertCounts(1 to "FirstPlace<Player1>", 0 to "SecondPlace<Player1>")
    pink.assertCounts(1 to "FirstPlace<Player2>", 1 to "SecondPlace<Player2>")
    green.assertCounts(1 to "FirstPlace<Player3>", 2 to "SecondPlace<Player3>")

    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<Player1>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Player2>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Player3>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Player1>") shouldBe 5
    score.net("SecondPlace", "VictoryPoint<Player1>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<Player2>") shouldBe 5
    score.net("SecondPlace", "VictoryPoint<Player2>") shouldBe 2
    score.net("FirstPlace", "VictoryPoint<Player3>") shouldBe 5
    score.net("SecondPlace", "VictoryPoint<Player3>") shouldBe 4
    score.net("GreeneryTile", "VictoryPoint<Player1>") shouldBe 9
    score.net("GreeneryTile", "VictoryPoint<Player2>") shouldBe 2
    score.net("GreeneryTile", "VictoryPoint<Player3>") shouldBe 6
    score.net("CityTile", "VictoryPoint<Player1>") shouldBe 9
    score.net("CityTile", "VictoryPoint<Player2>") shouldBe 4
    score.net("CityTile", "VictoryPoint<Player3>") shouldBe 8
    score.net("Card", "VictoryPoint<Player1>") shouldBe 4
    score.net("Card", "VictoryPoint<Player2>") shouldBe 12
    score.net("Card", "VictoryPoint<Player3>") shouldBe 5
    score.net("$EcologicalZone", "VictoryPoint<Player1>") shouldBe 3
    score.net("$IoMiningIndustries", "VictoryPoint<Player2>") shouldBe 4
    score.net("$SaturnSurfing", "VictoryPoint<Player2>") shouldBe 1
    score.net("$Ants", "VictoryPoint<Player3>") shouldBe 4

    purple.assertCounts(42 to "TerraformRating")
    pink.assertCounts(44 to "TerraformRating")
    green.assertCounts(34 to "TerraformRating")
    purple.assertCounts(74 to "VictoryPoint")
    pink.assertCounts(74 to "VictoryPoint")
    green.assertCounts(67 to "VictoryPoint")
    purple.assertCounts(0 to "Victory")
    pink.assertCounts(1 to "Victory")
    green.assertCounts(0 to "Victory")
  }
}
