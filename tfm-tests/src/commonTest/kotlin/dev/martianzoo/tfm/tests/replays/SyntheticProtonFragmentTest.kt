package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete archive replay: Synthetic Proton Fragment (g9ea8656f1c7e)
// https://terraforming-mars.herokuapp.com/the-end?id=p9d6d3ff25b39
internal class SyntheticProtonFragmentTest : CardTrackingFullGameTest() {
  // Player-record evidence: Hellas, Corporate Era, Prelude, promo cards, drafting, fast mode,
  // three players, no Venus/Colonies/Turmoil, and these full-random milestone and award pools.
  override val config =
      GameConfig(
          """
          HellasMap
          PreludeExpansion, PromoCardPack

          Mayor, Diversifier, Trader, Sponsor, Tycoon10
          Biologist, SpaceBaron, Forecaster, Botanist, Collector
          """,
          "Player1",
          "Player2",
          "Player3",
      )
  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  @Test
  internal fun game20260811() {
    TfmWorkflow.Auto(game).launch()

    val mom = p1
    val ellie = p2
    val dad = p3

    // Player-record evidence: Mom has a five-TR handicap, which GameConfig cannot express.
    mom.exMachina("5 TerraformRating")

    engine.assertCounts(1 to "Generation")

    // Test inference: Mom's dealt projects plus her later plays identify these four.
    mom.playCorp(Recyclon) {
      mom.buyCards(EcologicalZone, ReleaseOfInertGases, DeepWellHeating, IndustrialCenter)
    }

    // Test inference: Ellie's dealt projects plus her later plays identify these five.
    ellie.playCorp(RobinsonIndustries) {
      ellie.buyCards(
          LagrangeObservatory,
          SolarWindPower,
          EarthCatapult,
          Sabotage,
          MediaGroup,
      )
    }

    // User recollection recorded in _local/Game20260811/sources.md: Dad's otherwise-unidentified
    // seventh project was Cyberia Systems; the player record and later plays identify the other
    // six.
    dad.draw(
        Lichen,
        MercurianAlloys,
        Archaebacteria,
        RoboticWorkforce,
        HeatTrappers,
        IceAsteroid,
        CyberiaSystems,
    )
    dad.playCorp(SpliceTacticalGenomics, 7)

    mom.turn {
      playPrelude(AlbedoPlants).expect("PROD[Plant], Plant, 3 Heat")
      playPrelude(SocietySupport).expect("PROD[-1, Plant, Energy, Heat]")
    }

    ellie.turn {
      playPrelude(MetalsCompany).expect("PROD[1, Steel, Titanium]")
      playPrelude(Biolab) { draw(HiredRaiders, MeatIndustry, MiningRights) }
    }

    dad.turn {
      playPrelude(Merger) {
        doTask("PlayCard<Class<CorporationCard>, Class<$Inventrix>>")
      }
      playPrelude(MoholeExcavation).expect("PROD[Steel, 2 Heat], 2 Heat")
    }

    mom.playProject(DeepWellHeating, 13)
    mom.declineSecondAction()

    ellie.playProject(EarthCatapult, 23)
    ellie.cardAction1(RobinsonIndustries) {
      // Titanium production is not tied for lowest, so Robinson Industries cannot raise it.
      shouldThrow<NarrowingException> { doTask("PROD[Titanium]") }
      doTask("PROD[Heat]")
    }

    // Splice Tactical Genomics' mandatory first action prevents Dad from playing Lichen first.
    shouldThrow<RequirementException> { dad.playProject(Lichen, 7) }

    dad.stdAction("HandleMandates") {
      dad.draw(Ants, CorporateStronghold, SolarLogistics, DiversitySupport)
    }
    dad.playProject(Lichen, 7)

    // (Mom already passed early)
    mom.pass()
    ellie.pass()
    dad.playProject(SolarLogistics, 20)
    dad.pass()

    // Game20260811-dashboards-gen2.png was taken before cards were bought.
    mom.assertResources(m = 38, s = 1, t = 0, p = 3, e = 2, h = 4)
    mom.assertProduction(m = -1, s = 1, t = 0, p = 2, e = 2, h = 1)
    ellie.assertResources(m = 26, s = 1, t = 1, p = 1, e = 0, h = 1)
    ellie.assertProduction(m = 1, s = 1, t = 1, p = 1, e = 0, h = 1)
    dad.assertResources(m = 23, s = 1, t = 2, p = 1, e = 0, h = 4)
    dad.assertProduction(m = 0, s = 1, t = 0, p = 1, e = 0, h = 2)
    ellie.buyCards(DirectedImpactors)
    mom.buyCards(MagneticFieldDome, OptimalAerobraking)
    dad.buyCards(MedicalLab, AiCentral, AsteroidRights)

    ellie.playProject(MediaGroup, 4)
    ellie
        .playProject(Sabotage, 0) {
          doTask("-7 Megacredit<Player1>")
        }
        .expect("3")

    dad.playProject(AsteroidRights, 2, titanium = 2)
    dad.cardAction2(AsteroidRights) { doTask("2 Titanium") }

    mom.playProject(ReleaseOfInertGases, 14)
    mom.playProject(MagneticFieldDome, 3, steel = 1) {
          doTask("-2 Microbe<$Recyclon> THEN PROD[Plant]")
        }
        .expect("PROD[2 Plant, -2 Energy]")

    ellie.playProject(SolarWindPower, 6, titanium = 1).expect("PROD[Energy], Titanium")
    ellie
        .playProject(HiredRaiders, 0) {
          doTask("3 Megacredit<Player2> FROM Megacredit<Player3>")
        }
        .expect("6")

    dad.playProject(Archaebacteria, 6)
    dad.declineSecondAction()
    mom.pass()

    ellie.playProject(LagrangeObservatory, 1, titanium = 2) { ellie.draw(CloudSeeding) }
    ellie.cardAction1(RobinsonIndustries) { doTask("PROD[Titanium]") }
    // (Dad already passed early)
    dad.pass()
    ellie.playProject(MiningRights, 5, steel = 1) {
      placeTile(8, 8)
    }
    ellie.playProject(DirectedImpactors, 3, titanium = 1)
    ellie.cardAction1(DirectedImpactors) {
      ellie.pay(6)
      addCardResources(DirectedImpactors)
    }
    ellie.doTask("Pass")

    // Game20260811-dashboards-gen3.png was taken before cards were bought.
    mom.assertResources(m = 36, s = 1, t = 0, p = 7, e = 0, h = 7)
    mom.assertProduction(m = -1, s = 1, t = 0, p = 4, e = 0, h = 1)
    ellie.assertResources(m = 24, s = 1, t = 3, p = 2, e = 1, h = 2)
    ellie.assertProduction(m = 1, s = 1, t = 3, p = 1, e = 1, h = 1)
    dad.assertResources(m = 27, s = 2, t = 2, p = 3, e = 0, h = 6)
    dad.assertProduction(m = 0, s = 1, t = 0, p = 2, e = 0, h = 2)
    mom.buyCards(PeroxidePower, Hospitals)
    ellie.buyCards(ProtectedGrowth, GhgFactories, Soletta)
    dad.buyCards(FueledGenerators, CupolaCity, DesignedMicroorganisms)

    dad.playProject(FueledGenerators, 1).expect("PROD[-1, Energy]")
    dad.cardAction2(AsteroidRights) { doTask("2 Titanium") }

    mom.playProject(PeroxidePower, 5, steel = 1).expect("PROD[-1, 2 Energy]")
    mom.playProject(Hospitals, 8)

    ellie.cardAction1(RobinsonIndustries) { doTask("PROD[Steel]") }
    ellie.playProject(GhgFactories, 7, steel = 1).expect("PROD[-Energy, 4 Heat]")

    // Dad does not meet Diversifier before Corporate Stronghold enters play.
    shouldThrow<RequirementException> {
      dad.stdAction("ClaimMilestoneSA") { doTask("Diversifier") }
    }
    dad.playProject(CorporateStronghold, 7, steel = 2) {
          placeTile(2, 4)
        }
        .expect("Disease<Player1>")
    mom.assertCounts(7 to "Plant")
    dad.stdAction("ClaimMilestoneSA") { doTask("Diversifier") }

    mom.playProject(IndustrialCenter, 4) {
      placeTile(2, 3)
      doTask("-2 Microbe<$Recyclon> THEN PROD[Plant]")
    }
    mom.assertCounts(8 to "Plant")
    mom.convertPlants {
      placeTile(1, 2)
    }

    ellie.cardAction1(DirectedImpactors) {
      ellie.pay(titanium = 2)
      addCardResources(DirectedImpactors)
    }
    ellie.declineSecondAction()
    dad.pass()

    mom.playProject(EcologicalZone, 12) {
          placeTile(1, 3)
        }
        .expect("2 Plant")
    mom.convertHeat()
    // (Ellie already passed early)
    ellie.pass()
    mom.pass()

    // Game20260811-dashboards-gen4.png was taken before cards were bought.
    mom.assertResources(m = 30, s = 1, t = 0, p = 9, e = 1, h = 3)
    mom.assertProduction(m = -2, s = 1, t = 0, p = 5, e = 1, h = 1)
    ellie.assertResources(m = 25, s = 2, t = 4, p = 3, e = 0, h = 8)
    ellie.assertProduction(m = 1, s = 2, t = 3, p = 1, e = 0, h = 5)
    dad.assertResources(m = 24, s = 2, t = 4, p = 6, e = 0, h = 8)
    dad.assertProduction(m = 2, s = 1, t = 0, p = 2, e = 0, h = 2)
    ellie.buyCards(Mine, BribedCommittee, PublicPlans)
    mom.buyCards(TollStation, NaturalPreserve)
    dad.buyCards(LunarBeam, WeatherBalloons)

    mom.stdProject("AquiferSP") {
      placeTile(2, 1)
    }
    mom.convertPlants {
      placeTile(3, 3)
    }

    ellie.convertHeat()
    ellie
        .playProject(PublicPlans, 5) {
          doTask("6")
        }
        .expect("4")

    dad.playProject(HeatTrappers, 2, steel = 2) {
      // Mom has only one heat production, so Dad cannot choose her instead.
      shouldThrow<LimitsException> { doTask("PROD[-2 Heat<Player1>]") }
      shouldThrow<NarrowingException> { doTask("PROD[-Heat<Player1>]") }
      doTask("PROD[-2 Heat<Player2>]")
    }
    dad.playProject(RoboticWorkforce, 9) {
      doTask("CopyProductionBox<$HeatTrappers>")
      doTask("PROD[-2 Heat<Player2>]")
    }

    mom.playProject(NaturalPreserve, 5, steel = 2) {
          placeTile(3, 7)
          mom.draw(Psychrophiles)
        }
        .expect("PROD[1], Plant")
    // Test inference: the log gives only the count; Optimal Aerobraking is never played later.
    mom.sellPatents(OptimalAerobraking)

    ellie.playProject(ProtectedGrowth, megacredits = 0)
    ellie.playProject(Soletta, 21, titanium = 4).expect("PROD[7 Heat]")

    dad.cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }
    // Reason 3: Mercurian Alloys makes Dad's retained titanium worth more after this payment; the
    // next authoritative dashboard confirms that he retained all four.
    dad.intentionalUnderpay()
    dad.playProject(MercurianAlloys, 3)

    mom.cardAction1(Hospitals)
    mom.playProject(Psychrophiles, 2) {
          addCardResources(Psychrophiles)
        }
        .expect("2 Megacredit<Player3>")

    ellie.playProject(Mine, steel = 1)
    ellie.declineSecondAction()
    dad.pass()

    mom.cardAction1(Psychrophiles)
    mom.declineSecondAction()
    // (Ellie already passed early)
    ellie.pass()
    mom.pass()

    // Game20260811-dashboards-gen5.png was taken before cards were bought.
    mom.assertResources(m = 33, s = 1, t = 0, p = 9, e = 1, h = 5)
    mom.assertProduction(m = -1, s = 1, t = 0, p = 5, e = 1, h = 1)
    ellie.assertResources(m = 24, s = 4, t = 3, p = 5, e = 0, h = 9)
    ellie.assertProduction(m = 1, s = 3, t = 3, p = 1, e = 0, h = 9)
    dad.assertResources(m = 27, s = 1, t = 4, p = 8, e = 2, h = 10)
    dad.assertProduction(m = 2, s = 1, t = 0, p = 2, e = 2, h = 2)
    mom.buyCards(BioPrintingFacility)
    ellie.buyCards(HermeticOrderOfMars, MiningExpedition, LavaFlows)
    dad.buyCards(AntiGravityTechnology, Hackers, CallistoPenalMines)

    // Ellie does not meet Tycoon before Hermetic Order of Mars enters play.
    shouldThrow<RequirementException> {
      ellie.stdAction("ClaimMilestoneSA") { doTask("Tycoon10") }
    }
    ellie.playProject(HermeticOrderOfMars, 8).expect("PROD[2], -2")
    ellie.stdAction("ClaimMilestoneSA") { doTask("Tycoon10") }

    dad.playProject(Ants, 9) { doTask("2") }
    dad.cardAction1(Ants) {
      doTask("-Microbe<Player1, $Recyclon<Player1>>")
    }

    mom.cardAction1(Psychrophiles)
    mom.stdProject("PowerPlantSP")

    ellie.convertHeat()
    ellie.cardAction2(DirectedImpactors)

    dad.playProject(WeatherBalloons, 11) { dad.draw(JovianEmbassy) }
    dad.cardAction1(WeatherBalloons)

    mom.playProject(TollStation, 12).expect("PROD[7]")
    mom.convertPlants {
      placeTile(4, 4)
    }

    ellie.playProject(BribedCommittee, 5)
    // Test inference: the log gives only the count; Meat Industry is never played later.
    ellie.sellPatents(MeatIndustry)

    dad.playProject(DiversitySupport, 1)
    dad.cardAction2(AsteroidRights) { doTask("2 Titanium") }

    mom.cardAction1(IndustrialCenter)
    mom.declineSecondAction()

    ellie.cardAction1(RobinsonIndustries)
    ellie.declineSecondAction()

    dad.playProject(CallistoPenalMines, titanium = 6)
    dad.convertHeat()

    // (Mom already passed early)
    mom.pass()
    // (Ellie already passed early)
    ellie.pass()
    dad.convertPlants {
      placeTile(1, 4)
    }
    dad.doTask("Pass")

    // Game20260811-dashboards-gen6.png was taken before cards were bought.
    mom.assertResources(m = 40, s = 5, t = 0, p = 6, e = 2, h = 7)
    mom.assertProduction(m = 6, s = 2, t = 0, p = 5, e = 2, h = 1)
    ellie.assertResources(m = 28, s = 7, t = 6, p = 6, e = 1, h = 11)
    ellie.assertProduction(m = 3, s = 3, t = 3, p = 1, e = 1, h = 10)
    dad.assertResources(m = 29, s = 3, t = 0, p = 3, e = 2, h = 6)
    dad.assertProduction(m = 5, s = 1, t = 0, p = 2, e = 2, h = 2)
    mom.buyCards(ImportedNutrients, ProtectedValley)
    dad.buyCards(FusionPower, ViralEnhancers)
    ellie.buyCards(RadChemFactory, SaturnSurfing, IoMiningIndustries)

    dad.playProject(CupolaCity, 10, steel = 3) {
      placeTile(4, 3)
    }
    dad.cardAction1(Ants) {
      doTask("-Microbe<Player1, $Psychrophiles<Player1>>")
    }

    mom.stdAction("ClaimMilestoneSA") { doTask("Trader") }
    mom.playProject(ProtectedValley, 9, steel = 5) {
          doTask("2 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
          placeTile(1, 1)
        }
        .expect("2 Plant, -5 Steel")

    ellie.playProject(LavaFlows, 16) {
      placeTile(2, 2)
    }
    ellie.convertPlants {
      placeTile(8, 7)
    }

    dad.cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }
    dad.playProject(Hackers, 3) { doTask("PROD[-2 Megacredit<Player2>]") }

    mom.convertHeat()
    mom.stdProject("AquiferSP") {
      placeTile(4, 7)
    }

    ellie.convertHeat()
    ellie.cardAction1(RobinsonIndustries) { doTask("PROD[Plant]") }

    // The temperature is now too high for Designed Microorganisms.
    shouldThrow<RequirementException> {
      dad.playProject(DesignedMicroorganisms, 9)
    }
    // Viral Enhancers reacts to its own tag.
    dad.playProject(ViralEnhancers, 9).expect("Plant")
    dad.cardAction2(WeatherBalloons)

    mom.cardAction1(Psychrophiles)
    mom.convertPlants {
      placeTile(3, 6)
    }

    ellie.cardAction2(DirectedImpactors)
    ellie.playProject(RadChemFactory, megacredits = 0, steel = 3)

    dad.pass()
    mom.pass()
    ellie.pass()

    // We have no screencap for generation 7, so these only assert what the actual values happen to
    // be.
    mom.assertResources(m = 49, s = 2, t = 0, p = 8, e = 2, h = 5)
    mom.assertProduction(m = 8, s = 2, t = 0, p = 5, e = 2, h = 1)
    ellie.assertResources(m = 37, s = 7, t = 9, p = 2, e = 0, h = 16)
    ellie.assertProduction(m = 1, s = 3, t = 3, p = 2, e = 0, h = 10)
    dad.assertResources(m = 39, s = 2, t = 0, p = 6, e = 0, h = 10)
    dad.assertProduction(m = 10, s = 1, t = 0, p = 2, e = 0, h = 2)
    ellie.buyCards(DuskLaserMining, AsteroidCard, MethaneFromTitan)
    mom.buyCards(Supercapacitors)
    dad.buyCards(Algae, BactoviralResearch)

    mom.stdProject("CitySP") {
      placeTile(4, 5)
    }
    mom.convertPlants {
      placeTile(5, 5)
    }

    ellie.cardAction1(RobinsonIndustries)
    ellie.playProject(DuskLaserMining, titanium = 2).expect("PROD[Titanium, -Energy], 2 Titanium")

    dad.cardAction1(Ants) {
      doTask("-Microbe<Player1, $Recyclon<Player1>>")
    }
    dad.cardAction2(AsteroidRights) { doTask("2 Titanium") }

    mom.cardAction1(Psychrophiles)
    mom.playProject(BioPrintingFacility, 1, steel = 3)

    ellie.stdAction("FundAwardSA") { doTask("SpaceBaron") }
    ellie.playProject(AsteroidCard, titanium = 4) {
      dad.draw(SterlingVents)
      doTask("-3 Plant<Player3>")
    }
    dad.stdProject("AsteroidSP")
    dad.convertHeat() {
      placeTile(3, 1)
    }

    mom.cardAction1(BioPrintingFacility) { addCardResources(EcologicalZone) }
    mom.playProject(ImportedNutrients, 14) {
          dad.draw(Grass)
          addCardResources(Recyclon)
        }
        .expect("4 Plant")
    ellie.convertHeat()
    ellie.convertHeat()

    dad.playProject(SterlingVents, 1, steel = 2).expect("PROD[2 Energy, -2 Heat]")
    dad.playProject(Algae, 10).expect("PROD[2 Plant], 2 Plant")

    mom.playProject(Supercapacitors, 4) {
          doTask("-2 Microbe<$Recyclon>")
        }
        .expect("PROD[1, Plant]")
    mom.cardAction1(Hospitals)

    ellie.playProject(IoMiningIndustries, 12, titanium = 9).expect("PROD[2, 2 Titanium]")
    ellie.cardAction1(DirectedImpactors) {
      ellie.pay(6)
      addCardResources(DirectedImpactors)
    }

    // Test inference: the log gives only the count; Cyberia Systems is never played later.
    dad.sellPatents(CyberiaSystems)
    dad.playProject(Grass, 11).expect("PROD[Plant], 4 Plant")

    mom.pass()
    ellie.pass()
    dad.convertPlants {
      placeTile(3, 2)
    }
    dad.cardAction1(WeatherBalloons)
    dad.doTask("Pass")

    // Game20260811-dashboards-gen8.png was taken before cards were bought.
    mom.assertResources(m = 55, s = 2, t = 0, p = 10, e = 2, h = 6)
    mom.assertProduction(m = 10, s = 2, t = 0, p = 6, e = 2, h = 1)
    ellie.assertResources(m = 39, s = 10, t = 6, p = 4, e = 0, h = 10)
    ellie.assertProduction(m = 3, s = 3, t = 6, p = 2, e = 0, h = 10)
    dad.assertResources(m = 41, s = 1, t = 2, p = 8, e = 2, h = 2)
    dad.assertProduction(m = 10, s = 1, t = 0, p = 5, e = 2, h = 0)
    dad.assertCounts(9 to "ProjectCard")
    ellie.buyCards(ConvoyFromEuropa, VestaShipyard, LakeMarineris)
    dad.buyCards(InventorsGuild, CometAiming, EquatorialMagnetizer)
    mom.buyCards(EnergyTapping, InventionContest, FieldCappedCity)
    ellie.convertHeat()
    ellie.cardAction2(DirectedImpactors)
    dad.cardAction1(Ants) {
      doTask("-Microbe<Player1, $Recyclon<Player1>>")
    }
    dad.stdAction("FundAwardSA", which = 2) { doTask("Forecaster") }

    mom.stdProject("AquiferSP") {
      placeTile(5, 6)
      mom.draw(ProjectInspection)
    }
    mom.convertPlants {
      placeTile(6, 6)
    }

    ellie
        .playProject(LakeMarineris, 16) {
          doTask("OceanTile<Hellas_4_6>")
          doTask("OceanTile<Hellas_5_7>")
        }
        .expect("Plant, -6, 3 Heat")
    ellie
        .playProject(MiningExpedition, 10) {
          doTask("-2 Plant<Player3>")
        }
        .expect("2 Steel, -7")

    dad.playProject(IceAsteroid, 15, titanium = 2) {
          dad.draw(RobotPollinators)
          doTask("OceanTile<Hellas_7_3>")
          doTask("OceanTile<Hellas_4_1>")
        }
        .expect("0 Titanium")
    dad.cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }

    mom.cardAction1(BioPrintingFacility) { addCardResources(EcologicalZone) }
    mom.playProject(FieldCappedCity, 25, steel = 2) {
          placeTile(6, 5)
          doTask("-2 Microbe<$Recyclon> THEN PROD[Plant]")
        }
        .expect("PROD[2, Energy, Plant], 3 Plant, Disease")

    ellie
        .playProject(ConvoyFromEuropa, 1, titanium = 4) {
          ellie.draw(MagneticFieldGeneratorsPromo)
          dad.draw(LocalHeatTrapping)
          placeTile(5, 8)
        }
        .expect("6")
    ellie.playProject(VestaShipyard, 7, titanium = 2)

    dad.cardAction2(WeatherBalloons)
    // Test inference: the log gives only the count; Medical Lab is never played later.
    dad.sellPatents(MedicalLab)

    mom.cardAction1(Psychrophiles)
    mom.cardAction1(Hospitals)

    ellie.cardAction1(RobinsonIndustries)
    ellie.playProject(SaturnSurfing, 11).expect("3 Floater")

    dad.playProject(RobotPollinators, 9)
    dad.convertPlants {
      placeTile(4, 2)
    }

    mom.playProject(InventionContest, 2) { mom.draw(WaterImportFromEuropa) }
    mom.playProject(ProjectInspection, megacredits = 0) {
      doTask("UseAction<$Hospitals, First>")
    }

    ellie.cardAction1(SaturnSurfing)
    ellie.declineSecondAction()
    // Test inference: the log gives only the count; none of these cards is played later.
    dad.sellPatents(
        AiCentral,
        DesignedMicroorganisms,
        LunarBeam,
        AntiGravityTechnology,
        JovianEmbassy,
    )
    dad.playProject(EquatorialMagnetizer, 9, steel = 1)

    mom.playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Player3>]") }
    mom.declineSecondAction()
    // (Ellie already passed early)
    ellie.pass()
    dad.cardAction1(EquatorialMagnetizer)
    dad.declineSecondAction()
    // (Mom already passed early)
    mom.pass()
    // (Dad already passed early)
    dad.doTask("Pass")

    // Game20260811-dashboards-gen9.png was taken before cards were bought.
    mom.assertResources(m = 62, s = 2, t = 0, p = 12, e = 4, h = 7)
    mom.assertProduction(m = 12, s = 2, t = 0, p = 7, e = 4, h = 1)
    ellie.assertResources(m = 48, s = 15, t = 7, p = 7, e = 1, h = 15)
    ellie.assertProduction(m = 3, s = 3, t = 7, p = 2, e = 1, h = 10)
    dad.assertResources(m = 41, s = 1, t = 2, p = 9, e = 0, h = 4)
    dad.assertProduction(m = 10, s = 1, t = 0, p = 6, e = 0, h = 0)
    dad.assertCounts(5 to "ProjectCard")
    dad.buyCards(TechnologyDemonstration)
    mom.buyCards(ProtectedHabitats, PhysicsComplex, AdaptedLichen)
    ellie.buyCards(HousePrinting, BeamFromAThoriumAsteroid)

    dad.stdProject("GreenerySP") {
      placeTile(2, 5)
    }
    dad.convertPlants {
      placeTile(5, 3)
    }

    mom.convertPlants {
          placeTile(7, 6)
          mom.draw(LightningHarvest)
        }
        .expect("0 TerraformRating")
    mom.playProject(ProtectedHabitats, 5)

    ellie
        .stdProject("CitySP") {
          placeTile(7, 7)
        }
        .expect("Disease<Player1>")
    ellie.playProject(HousePrinting, steel = 4)

    dad.playProject(TechnologyDemonstration, 1, titanium = 1) {
      dad.draw(MartianRails, AcquiredCompany, Windmills)
    }
    // Test inference: the log gives only the count; Fusion Power is never played later.
    dad.sellPatents(FusionPower)

    mom.playProject(AdaptedLichen, 3) {
          doTask("3 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
        }
        .expect("Animal")
    mom.cardAction1(BioPrintingFacility) { addCardResources(EcologicalZone) }

    ellie.cardAction1(SaturnSurfing)
    ellie.playProject(BeamFromAThoriumAsteroid, 9, titanium = 7)

    dad.playProject(Windmills, 4, steel = 1)
    dad.playProject(BactoviralResearch, 10) {
      dad.draw(Potatoes)
      addCardResources(Ants)
    }

    mom.cardAction1(Psychrophiles)
    mom.cardAction1(Hospitals)

    ellie
        .playProject(MagneticFieldGeneratorsPromo, steel = 10) {
          placeTile(5, 1)
          ellie.draw(AsteroidHollowing)
        }
        .expect("PROD[2 Plant, -4 Energy], 2")
    // Test inference: the log gives only the count; Cloud Seeding is never played later.
    ellie.sellPatents(CloudSeeding)

    dad.cardAction1(Ants) {
          // Protected Habitats now prevents Dad from removing Mom's microbe.
          shouldThrow<DeadEndException> { doTask("-Microbe<Player1, $Psychrophiles<Player1>>") }
          doTask("-Microbe<Player3, $Ants<Player3>>")
        }
        .expect("0 Microbe<$Ants>")
    dad.cardAction1(EquatorialMagnetizer)

    // Test inference: the log gives only the count; none of these cards is played later.
    mom.sellPatents(WaterImportFromEuropa, PhysicsComplex, LightningHarvest)
    mom.stdProject("CitySP") {
      placeTile(1, 5)
    }

    // Test inference: the log gives only the count; Methane From Titan is never played later.
    ellie.sellPatents(MethaneFromTitan)
    // Test inference: the log gives only the count; Asteroid Hollowing is never played later.
    ellie.sellPatents(AsteroidHollowing)

    dad.cardAction2(AsteroidRights) { doTask("2 Titanium") }
    dad.cardAction1(WeatherBalloons)

    mom.stdAction("FundAwardSA", which = 3) { doTask("Botanist") }
    mom.cardAction1(IndustrialCenter)

    ellie.pass()
    // Test inference: the log gives only the count; these are Dad's remaining tracked cards.
    dad.sellPatents(
        InventorsGuild,
        CometAiming,
        LocalHeatTrapping,
        MartianRails,
        AcquiredCompany,
        Potatoes,
    )
    dad.declineSecondAction()
    mom.pass()
    dad.pass()

    // Decline Mom's first final greenery placement.
    mom.declineTask()
    dad.convertPlants { placeTile(3, 5) }
    // Decline another final greenery placement for Dad.
    dad.declineTask()
    mom.convertPlants { placeTile(2, 6) }
    // Decline another final greenery placement for Mom.
    mom.declineTask()
    ellie.convertPlants { placeTile(7, 8) }
    // Decline another final greenery placement for Ellie.
    ellie.declineTask()

    assertCardTrackingComplete()
    mom.cardsInHand shouldBe emptySet()
    ellie.cardsInHand shouldBe emptySet()
    dad.cardsInHand shouldBe emptySet()

    with(mom) {
      assertResources(m = 56, s = 5, t = 0, p = 6, e = 4, h = 13)
      assertProduction(m = 13, s = 3, t = 0, p = 8, e = 4, h = 1)
    }
    with(ellie) {
      assertResources(m = 63, s = 5, t = 7, p = 3, e = 0, h = 29)
      assertProduction(m = 4, s = 4, t = 7, p = 4, e = 0, h = 13)
    }
    with(dad) {
      assertResources(m = 57, s = 1, t = 3, p = 1, e = 0, h = 4)
      assertProduction(m = 10, s = 1, t = 0, p = 6, e = 0, h = 0)
    }

    mom.assertCounts(1 to "FirstPlace<Player1>", 0 to "SecondPlace<Player1>")
    ellie.assertCounts(1 to "FirstPlace<Player2>", 1 to "SecondPlace<Player2>")
    dad.assertCounts(1 to "FirstPlace<Player3>", 2 to "SecondPlace<Player3>")

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

    mom.assertCounts(42 to "TerraformRating")
    ellie.assertCounts(44 to "TerraformRating")
    dad.assertCounts(34 to "TerraformRating")
    mom.assertCounts(74 to "VictoryPoint")
    ellie.assertCounts(74 to "VictoryPoint")
    dad.assertCounts(67 to "VictoryPoint")
    mom.assertCounts(0 to "Victory")
    ellie.assertCounts(1 to "Victory")
    dad.assertCounts(0 to "Victory")
  }
}
