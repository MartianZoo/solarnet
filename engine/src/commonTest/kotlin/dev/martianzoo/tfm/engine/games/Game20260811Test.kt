package dev.martianzoo.tfm.engine.games

import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.GameConfig
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Synthetic Proton Fragment - https://terraforming-mars.herokuapp.com/the-end?id=p9d6d3ff25b39
class Game20260811Test : CardTrackingFullGameTest() {
  // The archived metadata specifies Hellas, Corporate Era, Prelude, promo cards, three players,
  // no Venus/Colonies/Turmoil, and the following full-random milestone and award pools.
  override val config =
      GameConfig(
          """
          HellasMapOption
          PreludeExpansion, MilestonesAwardsExpansion, PromoCardPack

          Mayor, Diversifier, Trader, Sponsor, MilestoneMM35
          Biologist, SpaceBaron, Forecaster, Botanist, Collector
          """,
          "Player1",
          "Player2",
          "Player3",
      )

  @Test
  fun game20260811() {
    TfmWorkflow.Auto(game).launch()

    val mom = p1
    val ellie = p2
    val dad = p3

    // Mom gets a handicap
    mom.exMachina("5 TR")

    engine.assertCounts(1 to "Generation")

    // Player-record inference: Mom's dealt projects plus her later plays identify these four.
    mom.playCorp(Recyclon) {
          mom.buyCards(EcologicalZone, ReleaseOfInertGases, DeepWellHeating, IndustrialCenter)
        }
        .expect("PROD[S], 26")

    // Player-record inference: Ellie's dealt projects plus her later plays identify these five.
    ellie
        .playCorp(RobinsonIndustries) {
          ellie.buyCards(
              LagrangeObservatory,
              SolarWindPower,
              EarthCatapult,
              Sabotage,
              MediaGroup,
          )
        }
        .expect("32")

    // User recollection recorded in _local/Game20260811/sources.md: Dad's otherwise-unidentified
    // seventh project was Cyberia Systems; the player record and later plays identify the other
    // six.
    dad.playCorp(SpliceTacticalGenomics) {
          doTask("2")
          dad.buyCards(
              Lichen,
              MercurianAlloys,
              Archaebacteria,
              RoboticWorkforce,
              HeatTrappers,
              IceAsteroid,
              CyberiaSystems,
          )
        }
        .expect("27")

    mom.playPrelude(AlbedoPlants).expect("PROD[P], P, 3 H")
    mom.playPrelude(SocietySupport).expect("PROD[-M, P, E, H]")

    ellie.playPrelude(MetalsCompany).expect("PROD[M, S, T]")
    ellie
        .playPrelude(Biolab) { ellie.draw(HiredRaiders, MeatIndustry, MiningRights) }
        .expect("PROD[P]")

    dad.playPrelude(Merger) {
      doTask("PlayCard<Class<CorporationCard>, Class<$Inventrix>>")
    }
    dad.playPrelude(MoholeExcavation).expect("PROD[S, 2 H], 2 H")

    mom.playProject(DeepWellHeating, 13).expect("PROD[E], TR")
    mom.declineSecondAction()

    ellie.playProject(EarthCatapult, 23)
    ellie.cardAction1(RobinsonIndustries) {
      // NOPE: gotta pick a lower one
      shouldThrow<NarrowingException> { doTask("PROD[T]") }
      doTask("PROD[H]")
    }

    // NOPE: can't choose any other first action
    shouldThrow<RequirementException> { dad.playProject(Lichen, 7) }

    dad.stdAction("HandleMandates") {
      dad.draw(Ants, CorporateStronghold, SolarLogistics, DiversitySupport)
    }
    dad.playProject(Lichen, 7).expect("PROD[P]")

    // (Mom already passed early)
    mom.pass()
    ellie.pass()
    dad.playProject(SolarLogistics, 20).expect("2 T")
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
          doTask("-7 M<Player1>")
        }
        .expect("3 M")

    dad.playProject(AsteroidRights, 2, titanium = 2).expect("2 Asteroid")
    dad.cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid")

    mom.playProject(ReleaseOfInertGases, 14).expect("2 TR")
    mom.playProject(MagneticFieldDome, 3, steel = 1) {
          doTask("-2 Microbe<$Recyclon> THEN PROD[P]")
        }
        .expect("PROD[2 P, -2 E]")

    ellie.playProject(SolarWindPower, 6, titanium = 1).expect("PROD[E], T")
    ellie
        .playProject(HiredRaiders, 0) {
          doTask("3 M<Player2> FROM M<Player3>")
        }
        .expect("6 M")

    dad.playProject(Archaebacteria, 6).expect("PROD[P], -2")
    dad.declineSecondAction()
    mom.pass()

    ellie.playProject(LagrangeObservatory, 1, titanium = 2) { ellie.draw(CloudSeeding) }
    ellie.cardAction1(RobinsonIndustries) { doTask("PROD[T]") }
    // (Dad already passed early)
    dad.pass()
    ellie
        .playProject(MiningRights, 5, steel = 1) {
          doTask("Card067_SpecialTile<Hellas_8_8>")
        }
        .expect("PROD[T], T")
    ellie.playProject(DirectedImpactors, 3, titanium = 1)
    ellie.cardAction1(DirectedImpactors) {
      ellie.pay(6)
      doTask("Asteroid<$DirectedImpactors>")
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

    dad.playProject(FueledGenerators, 1).expect("PROD[-M, E]")
    dad.cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid")

    mom.playProject(PeroxidePower, 5, steel = 1).expect("PROD[-M, 2 E]")
    mom.playProject(Hospitals, 8).expect("PROD[-E]")

    ellie.cardAction1(RobinsonIndustries) { doTask("PROD[S]") }
    ellie.playProject(GhgFactories, 7, steel = 1).expect("PROD[-E, 4 H]")

    // NOPE: almost there, not quite
    shouldThrow<RequirementException> {
      dad.stdAction("ClaimMilestoneSA") { doTask("Diversifier") }
    }
    dad.playProject(CorporateStronghold, 7, steel = 2) {
          doTask("CityTile<Hellas_2_4>")
        }
        .expect("PROD[3 M, -E], P, -S, Disease<Player1>")
    mom.assertCounts(7 to "P")
    dad.stdAction("ClaimMilestoneSA") { doTask("Diversifier") }

    mom.playProject(IndustrialCenter, 4) {
          doTask("Card123_SpecialTile<Hellas_2_3>")
          doTask("-2 Microbe<$Recyclon> THEN PROD[P]")
        }
        .expect("P")
    mom.assertCounts(8 to "P")
    mom.stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_1_2>")
        }
        .expect("-6 P, TR")

    ellie.cardAction1(DirectedImpactors) {
      ellie.pay(titanium = 2)
      doTask("Asteroid<$DirectedImpactors>")
    }
    ellie.declineSecondAction()
    dad.pass()

    mom.playProject(EcologicalZone, 12) {
          doTask("Card128_SpecialTile<Hellas_1_3>")
        }
        .expect("3 H, 2 Animal, 2 P")
    mom.stdAction("ConvertHeatSA").expect("TR")
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
          doTask("OceanTile<Hellas_2_1>")
        }
        .expect("TR, 2 P")
    mom.stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_3_3>")
        }
        .expect("S, TR")

    ellie.stdAction("ConvertHeatSA").expect("PROD[H], TR")
    ellie
        .playProject(PublicPlans, 5) {
          doTask("6")
        }
        .expect("4")

    dad.playProject(HeatTrappers, 2, steel = 2) {
          // NOPE: Mom had only 1 heat production, so Dad could not choose her instead.
          shouldThrow<LimitsException> { doTask("PROD[-2 H<Player1>]") }
          shouldThrow<NarrowingException> { doTask("PROD[-H<Player1>]") }
          doTask("PROD[-2 H<Player2>]")
        }
        .expect("PROD[E]")
    dad.playProject(RoboticWorkforce, 9) {
          doTask("CopyProductionBox<$HeatTrappers>")
          doTask("PROD[-2 Heat<Player2>]")
        }
        .expect("PROD[E]")

    mom.playProject(NaturalPreserve, 5, steel = 2) {
          doTask("Card044_SpecialTile<Hellas_3_7>")
          mom.draw(Psychrophiles)
        }
        .expect("PROD[M], P")
    // Fixture inference: the log gives only the count; Optimal Aerobraking is never played later.
    mom.sellPatents(OptimalAerobraking)

    ellie.playProject(ProtectedGrowth, megacredits = 0).expect("P, 3")
    ellie.playProject(Soletta, 21, titanium = 4).expect("PROD[7 H]")

    dad.cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidRights>") }
    // Reason 3: Mercurian Alloys makes Dad's retained titanium worth more after this payment; the
    // next authoritative dashboard confirms that he retained all four.
    dad.intentionalUnderpay()
    dad.playProject(MercurianAlloys, 3)

    mom.cardAction1(Hospitals).expect("-Disease, 1")
    mom.playProject(Psychrophiles, 2) {
          doTask("Microbe<$Psychrophiles>!")
        }
        .expect("2 M<Player3>")

    ellie.playProject(Mine, steel = 1).expect("PROD[S]")
    ellie.declineSecondAction()
    dad.pass()

    mom.cardAction1(Psychrophiles).expect("Microbe")
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

    // NOPE: almost there, not quite
    shouldThrow<RequirementException> {
      ellie.stdAction("ClaimMilestoneSA") { doTask("Tycoon") }
    }
    ellie.playProject(HermeticOrderOfMars, 8).expect("PROD[2 M], -2")
    ellie.stdAction("ClaimMilestoneSA") { doTask("Tycoon") }

    dad.playProject(Ants, 9) { doTask("2") }.expect("-5")
    dad.cardAction1(Ants) {
          doTask("-Microbe<Player1, $Recyclon<Player1>>")
        }
        .expect("Microbe<$Ants>")

    mom.cardAction1(Psychrophiles).expect("Microbe")
    mom.stdProject("PowerPlantSP").expect("PROD[E]")

    ellie.stdAction("ConvertHeatSA").expect("TR")
    ellie.cardAction2(DirectedImpactors).expect("-Asteroid, TemperatureStep, PROD[H], TR")

    dad.playProject(WeatherBalloons, 11) { dad.draw(JovianEmbassy) }
    dad.cardAction1(WeatherBalloons).expect("Floater")

    mom.playProject(TollStation, 12).expect("PROD[7 M]")
    mom.stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_4_4>")
        }
        .expect("2 S, TR")

    ellie.playProject(BribedCommittee, 5).expect("-2")
    // Fixture inference: the log gives only the count; Meat Industry is never played later.
    ellie.sellPatents(MeatIndustry)

    dad.playProject(DiversitySupport, 1)
    dad.cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid")

    mom.cardAction1(IndustrialCenter).expect("PROD[S]")
    mom.declineSecondAction()

    ellie.cardAction1(RobinsonIndustries).expect("PROD[E]")
    ellie.declineSecondAction()

    dad.playProject(CallistoPenalMines, titanium = 6).expect("PROD[3 M]")
    dad.stdAction("ConvertHeatSA").expect("TR")

    // (Mom already passed early)
    mom.pass()
    // (Ellie already passed early)
    ellie.pass()
    dad.stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_1_4>")
        }
        .expect("-7 P, S, TR")
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
          doTask("CityTile<Hellas_4_3>")
        }
        .expect("PROD[3 M, -E], -2 S, Disease<Player1>")
    dad.cardAction1(Ants) {
          doTask("-Microbe<Player1, $Psychrophiles<Player1>>")
        }
        .expect("Microbe<$Ants>")

    mom.stdAction("ClaimMilestoneSA") { doTask("Trader") }
    mom.playProject(ProtectedValley, 9, steel = 5) {
          doTask("-2 Microbe<$Psychrophiles> THEN -4 Owed<Class<Megacredit>>")
          doTask("GreeneryTile<Hellas_1_1>")
        }
        .expect("PROD[2 M], 3 H, Animal, TR, 2 P, -7")

    ellie
        .playProject(LavaFlows, 16) {
          doTask("Card140_SpecialTile<Hellas_2_2>")
        }
        .expect("2 TR, 2 P, -11")
    ellie
        .stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_8_7>")
        }
        .expect("2 H, TR")

    dad.cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidRights>") }
    dad.playProject(Hackers, 3) { doTask("PROD[-2 M<Player2>]") }.expect("PROD[2 M, -E]")

    mom.stdAction("ConvertHeatSA").expect("TR")
    mom.stdProject("AquiferSP") {
          doTask("OceanTile<Hellas_4_7>")
        }
        .expect("TR, P")

    ellie.stdAction("ConvertHeatSA").expect("TR")
    ellie.cardAction1(RobinsonIndustries) { doTask("PROD[P]") }

    // NOPE: missed my chance and didn't even realize it!
    shouldThrow<RequirementException> {
      dad.playProject(DesignedMicroorganisms, 9)
    }
    dad.playProject(ViralEnhancers, 9).expect("P, -5")
    dad.cardAction2(WeatherBalloons).expect("-Floater, 2")

    mom.cardAction1(Psychrophiles).expect("Microbe")
    mom.stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_3_6>")
        }
        .expect("-6 P, 2, TR")

    ellie.cardAction2(DirectedImpactors).expect("-Asteroid, TemperatureStep, TR")
    ellie.playProject(RadChemFactory, megacredits = 0, steel = 3).expect("PROD[-E], 2 TR")

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
          doTask("CityTile<Hellas_4_5>")
        }
        .expect("S, Disease")
    mom.stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_5_5>")
        }
        .expect("2 TR")

    ellie.cardAction1(RobinsonIndustries).expect("PROD[E]")
    ellie.playProject(DuskLaserMining, titanium = 2).expect("PROD[T, -E], 2 T")

    dad.cardAction1(Ants) {
          doTask("-Microbe<Player1, $Recyclon<Player1>>")
        }
        .expect("Microbe<$Ants>")
    dad.cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid")

    mom.cardAction1(Psychrophiles).expect("Microbe")
    mom.playProject(BioPrintingFacility, 1, steel = 3).expect("Microbe")

    ellie.stdAction("FundAwardSA") { doTask("SpaceBaron") }
    ellie
        .playProject(AsteroidCard, titanium = 4) {
          dad.draw(SterlingVents)
          doTask("-3 P<Player3>")
        }
        .expect("-2 T, -3 P<Player3>")
    dad.stdProject("AsteroidSP").expect("TR")
    dad.stdAction("ConvertHeatSA") {
          doTask("OceanTile<Hellas_3_1>")
        }
        .expect("2 TR, P, 2")

    mom.cardAction1(BioPrintingFacility) { doTask("Animal<$EcologicalZone>") }
    mom.playProject(ImportedNutrients, 14) {
          dad.draw(Grass)
          doTask("4 Microbe<$Recyclon>")
        }
        .expect("4 P")
    ellie.stdAction("ConvertHeatSA")
    ellie.stdAction("ConvertHeatSA")

    dad.playProject(SterlingVents, 1, steel = 2).expect("PROD[2 E, -2 H]")
    dad.playProject(Algae, 10).expect("PROD[2 P], 2 P")

    mom.playProject(Supercapacitors, 4) {
          doTask("-2 Microbe<$Recyclon>")
        }
        .expect("PROD[M, P]")
    mom.cardAction1(Hospitals).expect("-Disease, 3")

    ellie.playProject(IoMiningIndustries, 12, titanium = 9).expect("PROD[2 M, 2 T]")
    ellie.cardAction1(DirectedImpactors) {
      ellie.pay(6)
      doTask("Asteroid<$DirectedImpactors>")
    }

    // Fixture inference: the log gives only the count; Cyberia Systems is never played later.
    dad.sellPatents(CyberiaSystems)
    dad.playProject(Grass, 11).expect("PROD[P], 4 P")

    mom.pass()
    ellie.pass()
    dad.stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_3_2>")
        }
        .expect("-7 P, 4, TR")
    dad.cardAction1(WeatherBalloons).expect("Floater")
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
    ellie.stdAction("ConvertHeatSA")
    ellie.cardAction2(DirectedImpactors).expect("-Asteroid, TemperatureStep, TR")
    dad.cardAction1(Ants) {
          doTask("-Microbe<Player1, $Recyclon<Player1>>")
        }
        .expect("Microbe<$Ants>")
    dad.stdAction("FundAwardSA") { doTask("Forecaster") }

    mom.stdProject("AquiferSP") {
          doTask("OceanTile<Hellas_5_6>")
          mom.draw(ProjectInspection)
        }
        .expect("TR")
    mom.stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_6_6>")
        }
        .expect("2, TR")

    ellie
        .playProject(LakeMarineris, 16) {
          doTask("OceanTile<Hellas_4_6>")
          doTask("OceanTile<Hellas_5_7>")
        }
        .expect("2 TR, P, -6, 3 H")
    ellie
        .playProject(MiningExpedition, 10) {
          doTask("-2 P<Player3>")
        }
        .expect("TR, 2 S, -7")

    dad.playProject(IceAsteroid, 15, titanium = 2) {
          dad.draw(RobotPollinators)
          doTask("OceanTile<Hellas_7_3>")
          doTask("OceanTile<Hellas_4_1>")
        }
        .expect("2 TR, 0 T, P, -13")
    dad.cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidRights>") }

    mom.cardAction1(BioPrintingFacility) { doTask("Animal<$EcologicalZone>") }
    mom.playProject(FieldCappedCity, 25, steel = 2) {
          doTask("CityTile<Hellas_6_5>")
          doTask("-2 Microbe<$Recyclon> THEN PROD[P]")
        }
        .expect("PROD[2 M, E, P], 3 P, Disease")

    ellie
        .playProject(ConvoyFromEuropa, 1, titanium = 4) {
          ellie.draw(MagneticFieldGenerators)
          dad.draw(LocalHeatTrapping)
          doTask("OceanTile<Hellas_5_8>")
        }
        .expect("TR, 6 M")
    ellie.playProject(VestaShipyard, 7, titanium = 2).expect("PROD[T]")

    dad.cardAction2(WeatherBalloons).expect("-Floater, 4")
    // Fixture inference: the log gives only the count; Medical Lab is never played later.
    dad.sellPatents(MedicalLab)

    mom.cardAction1(Psychrophiles).expect("Microbe")
    mom.cardAction1(Hospitals).expect("-Disease, 4")

    ellie.cardAction1(RobinsonIndustries).expect("PROD[E]")
    ellie.playProject(SaturnSurfing, 11).expect("3 Floater")

    dad.playProject(RobotPollinators, 9).expect("PROD[P], 3 P")
    dad.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Hellas_4_2>") }.expect("-7 P, 4, TR")

    mom.playProject(InventionContest, 2) { mom.draw(WaterImportFromEuropa) }
    mom.playProject(ProjectInspection, megacredits = 0) {
          doTask("UseAction1<$Hospitals>")
        }
        .expect("-Disease, 4")

    ellie.cardAction1(SaturnSurfing).expect("3")
    ellie.declineSecondAction()
    // Fixture inference: the log gives only the count; none of these cards is played later.
    dad.sellPatents(
        AiCentral,
        DesignedMicroorganisms,
        LunarBeam,
        AntiGravityTechnology,
        JovianEmbassy,
    )
    dad.playProject(EquatorialMagnetizer, 9, steel = 1)

    mom.playProject(EnergyTapping, 3) { doTask("PROD[-E<Player3>]") }.expect("PROD[E]")
    mom.declineSecondAction()
    // (Ellie already passed early)
    ellie.pass()
    dad.cardAction1(EquatorialMagnetizer).expect("PROD[-E], TR")
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
          doTask("GreeneryTile<Hellas_2_5>")
        }
        .expect("P, TR")
    dad.stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_5_3>")
        }
        .expect("TR")

    mom.stdAction("ConvertPlantsSA") {
          doTask("GreeneryTile<Hellas_7_6>")
          mom.draw(LightningHarvest)
        }
        .expect("0 TR")
    mom.playProject(ProtectedHabitats, 5)

    ellie
        .stdProject("CitySP") {
          doTask("CityTile<Hellas_7_7>")
        }
        .expect("Disease<Player1>")
    ellie.playProject(HousePrinting, steel = 4).expect("PROD[S]")

    dad.playProject(TechnologyDemonstration, 1, titanium = 1) {
      dad.draw(MartianRails, AcquiredCompany, Windmills)
    }
    // Fixture inference: the log gives only the count; Fusion Power is never played later.
    dad.sellPatents(FusionPower)

    mom.playProject(AdaptedLichen, 3) {
          doTask("-3 Microbe<$Psychrophiles> THEN -6 Owed<Class<Megacredit>>")
        }
        .expect("PROD[P], 3 H, Animal")
    mom.cardAction1(BioPrintingFacility) { doTask("Animal<$EcologicalZone>") }

    ellie.cardAction1(SaturnSurfing).expect("2")
    ellie.playProject(BeamFromAThoriumAsteroid, 9, titanium = 7).expect("PROD[3 E, 3 H]")

    dad.playProject(Windmills, 4, steel = 1).expect("PROD[E]")
    dad.playProject(BactoviralResearch, 10) {
          dad.draw(Potatoes)
          doTask("5 Microbe<$Ants>")
        }
        .expect("P, -6")

    mom.cardAction1(Psychrophiles).expect("Microbe")
    mom.cardAction1(Hospitals).expect("-Disease, 5")

    ellie
        .playProject(MagneticFieldGenerators, steel = 10) {
          doTask("CardX33_SpecialTile<Hellas_5_1>")
          ellie.draw(AsteroidHollowing)
        }
        .expect("PROD[2 P, -4 E], 2 M")
    // Fixture inference: the log gives only the count; Cloud Seeding is never played later.
    ellie.sellPatents(CloudSeeding)

    dad.cardAction1(Ants) {
          // NOPE: Mom is protected now, so this was pointless?
          shouldThrow<DeadEndException> { doTask("-Microbe<Player1, $Psychrophiles<Player1>>") }
          doTask("-Microbe<Player3, $Ants<Player3>>")
        }
        .expect("0 Microbe<$Ants>")
    dad.cardAction1(EquatorialMagnetizer).expect("PROD[-E], TR")

    // Fixture inference: the log gives only the count; none of these cards is played later.
    mom.sellPatents(WaterImportFromEuropa, PhysicsComplex, LightningHarvest)
    mom.stdProject("CitySP") {
          doTask("CityTile<Hellas_1_5>")
        }
        .expect("P, Disease")

    // Fixture inference: the log gives only the count; Methane From Titan is never played later.
    ellie.sellPatents(MethaneFromTitan)
    // Fixture inference: the log gives only the count; Asteroid Hollowing is never played later.
    ellie.sellPatents(AsteroidHollowing)

    dad.cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid")
    dad.cardAction1(WeatherBalloons).expect("Floater")

    mom.stdAction("FundAwardSA") { doTask("Botanist") }
    mom.cardAction1(IndustrialCenter).expect("PROD[S]")

    ellie.pass()
    // Fixture inference: the log gives only the count; these are Dad's remaining tracked cards.
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

    mom.doTask("Ok")
    dad.doTask("UseAction1<ConvertPlantsSA>")
    dad.doTask("GreeneryTile<Hellas_3_5>")
    dad.doTask("Ok")
    mom.doTask("UseAction1<ConvertPlantsSA>")
    mom.doTask("GreeneryTile<Hellas_2_6>")
    mom.doTask("Ok")
    ellie.doTask("UseAction1<ConvertPlantsSA>")
    ellie.doTask("GreeneryTile<Hellas_7_8>")
    ellie.doTask("Ok")

    assertCardTrackingComplete()
    mom.cardsInHand shouldBe emptySet()
    ellie.cardsInHand shouldBe emptySet()
    dad.cardsInHand shouldBe emptySet()
    val score = Summarizer(game)

    mom.assertCounts(42 to "TR")
    ellie.assertCounts(44 to "TR")
    dad.assertCounts(34 to "TR")

    mom.assertCounts(1 to "FirstPlace<Player1>", 0 to "SecondPlace<Player1>")
    ellie.assertCounts(1 to "FirstPlace<Player2>", 1 to "SecondPlace<Player2>")
    dad.assertCounts(1 to "FirstPlace<Player3>", 2 to "SecondPlace<Player3>")

    score.net("Milestone", "VP<Player1>") shouldBe 5
    score.net("Milestone", "VP<Player2>") shouldBe 5
    score.net("Milestone", "VP<Player3>") shouldBe 5
    score.net("FirstPlace", "VP<Player1>") shouldBe 5
    score.net("SecondPlace", "VP<Player1>") shouldBe 0
    score.net("FirstPlace", "VP<Player2>") shouldBe 5
    score.net("SecondPlace", "VP<Player2>") shouldBe 2
    score.net("FirstPlace", "VP<Player3>") shouldBe 5
    score.net("SecondPlace", "VP<Player3>") shouldBe 4
    score.net("GreeneryTile", "VP<Player1>") shouldBe 9
    score.net("GreeneryTile", "VP<Player2>") shouldBe 2
    score.net("GreeneryTile", "VP<Player3>") shouldBe 6
    score.net("CityTile", "VP<Player1>") shouldBe 9
    score.net("CityTile", "VP<Player2>") shouldBe 4
    score.net("CityTile", "VP<Player3>") shouldBe 8
    score.net("Card", "VP<Player1>") shouldBe 4
    score.net("Card", "VP<Player2>") shouldBe 12
    score.net("Card", "VP<Player3>") shouldBe 5

    score.net("$EcologicalZone", "VP<Player1>") shouldBe 3
    score.net("$IoMiningIndustries", "VP<Player2>") shouldBe 4
    score.net("$SaturnSurfing", "VP<Player2>") shouldBe 1
    score.net("$Ants", "VP<Player3>") shouldBe 4

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
    mom.assertCounts(74 to "VP", 0 to "Victory")
    ellie.assertCounts(74 to "VP", 1 to "Victory")
    dad.assertCounts(67 to "VP", 0 to "Victory")
  }
}
