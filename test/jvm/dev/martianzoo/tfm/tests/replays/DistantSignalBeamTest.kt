package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.retainStartingProjects
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete database replay through the final retained save immediately before Purple resigned:
// Distant Signal Beam (g1ddd59fe5633), save 79, generation 3.
// Source: _local/replays/Game20260905/game-g1ddd59fe5633.sqlite
// https://terraforming-mars.herokuapp.com/the-end?id=p35d2aed733c5
internal class DistantSignalBeamTest : CardTrackingFullGameTest() {
  override val config =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, ColoniesExpansion, PromoCardPack
          Aridor

          Diversifier, Merchant, Fundraiser, Terraformer, Producer, Trader
          Manufacturer, Cultivator, Banker, Collector, Founder, Forecaster
          Ceres, Enceladus, Europa, Miranda, Triton
          """,
          "Pink",
          "Purple",
      )

  private val pink
    get() = p1

  private val purple
    get() = p2

  @Test
  internal fun distantSignalBeam() {
    TfmWorkflow.Auto(game).launch()
    game.retainStartingProjects(7, 10)
    generation1()
    generation2()
    generation3BeforeResignation()
  }

  private fun generation1() {
    // Save 0 exposes every setup choice. Corporation offers were Vitor, Morning Star Inc., and
    // PolderTECH Dutch for Pink; Aridor, Ecoline, and Point Luna for Purple. Prelude offers were
    // Polar Industries, Atmospheric Enhancers, Loan, and Power Generation for Pink; Biosphere
    // Support, Focused Organization, Project Eden, and Allied Bank for Purple.
    pink.expectProjectCards(
        HousePrinting,
        Insects,
        CloudSeeding,
        MicroMills,
        Pets,
        LagrangeObservatory,
        GhgImportFromVenus,
        EcologyResearch,
        SecurityFleet,
        IoSulphurResearch,
    )
    pink.playCorp(MorningStarInc) {
      buyCards(
          HousePrinting,
          CloudSeeding,
          MicroMills,
          Pets,
          LagrangeObservatory,
          GhgImportFromVenus,
          IoSulphurResearch,
      )
    }
    pink.discardUnselectedProjectCards(Insects, EcologyResearch, SecurityFleet)

    purple.expectProjectCards(
        RotatorImpacts,
        PowerSupplyConsortium,
        Tardigrades,
        InterplanetaryTrade,
        Casinos,
        AdaptationTechnology,
        FusionPower,
        BreathingFilters,
        PeroxidePower,
        NeutralizerFactory,
    )
    purple.playCorp(Aridor) {
      buyCards(
          RotatorImpacts,
          PowerSupplyConsortium,
          Tardigrades,
          InterplanetaryTrade,
          Casinos,
          AdaptationTechnology,
          FusionPower,
          BreathingFilters,
          PeroxidePower,
          NeutralizerFactory,
      )
    }

    pink.turn {
      playPrelude(AtmosphericEnhancers) {
        // The filtered draw revealed and rejected these sixteen cards before finding two cards with
        // floater icons. Save 2 preserves the exact deck exits.
        discardProjectCardsFromDeck(
            InterplanetaryColonyShip,
            AsteroidDeflectionSystem,
            GreatEscarpmentConsortium,
            VenusianAnimals,
            Vermin,
            CometAiming,
            SfMemorial,
            MinorityRefuge,
            MicrogravityNutrition,
            CeosFavoriteProject,
            IceAsteroid,
            BioPrintingFacility,
            Heather,
            EquatorialMagnetizer,
            TollStation,
            SelfReplicatingRobots,
        )
        draw(TitanShuttles, AtmoCollectors)
        doTask("2 VenusStep")
      }
      playPrelude(PolarIndustries) {
        placeTile(5, 6)
        draw(Cartel)
      }
    }

    purple.turn {
      playPrelude(ProjectEden) {
        // All three placement choices are pending together, so name the sourced tile types.
        doTask("OceanTile<Hellas_4_6>")
        doTask("GreeneryTile<Hellas_4_5>")
        doTask("CityTile<Hellas_4_4>")
      }
      discard(PowerSupplyConsortium, AdaptationTechnology, BreathingFilters)
      // Database save 4: Project Eden introduced Purple's first city and plant tags.
      playPrelude(AlliedBank)
    }
    // Database save 5: Allied Bank introduced Purple's first Earth tag.

    pink.turn {
      stdAction("DoRequiredActionsAction") {
        // Morning Star revealed these nine non-Venus cards before drawing the following three.
        discardProjectCardsFromDeck(
            AsteroidHollowing,
            GiantSpaceMirror,
            TectonicStressPower,
            MiningArea,
            MarketManipulation,
            SaturnSurfing,
            GreatDamPromo,
            CeresTechMarket,
            CrashSiteCleanup,
        )
        draw(VenusSoils, SulphurEatingBacteria, StratosphericExpedition)
      }
      playProject(TitanShuttles, 23)
    }
    purple.turn { stdAction("DoRequiredActionsAction") { doTask("Luna") } }

    pink.turn { cardAction1(TitanShuttles) { addCardResources(TitanShuttles, 2) } }
    purple.turn { playProject(PeroxidePower, 1, steel = 3) }
    // Database save 15: Peroxide Power introduced Purple's first power and building tags.
    pink.turn { playProject(SulphurEatingBacteria, 6) }
    purple.turn { playProject(Casinos, 5) }
    pink.turn { cardAction1(SulphurEatingBacteria) }
    purple.turn { playProject(Tardigrades, 4) }
    // Database save 27: Tardigrades introduced Purple's first microbe tag.
    pink.pass()
    purple.turn {
      cardAction1(Tardigrades)
      playProject(RotatorImpacts, 6)
      // Database save 33: Rotator Impacts introduced Purple's first space tag.
      pass()
    }
    pink.wgt("OceanTile<Hellas_5_7>")
  }

  private fun generation2() {
    // The retained draft saves prove that both players saw all eight cards. Solarnet models each
    // recovered post-draft four-card set as its eventual owner's ordinary Research offer.
    pink.buyCards(HeatTrappers)
    pink.discardUnselectedProjectCards(GeothermalPower, PhobosSpaceHaven, DirectedHeatUsage)
    purple.buyCards(Omnicourt, BactoviralResearch, SolarReflectors)
    purple.discardUnselectedProjectCards(SoilFactory)

    // Database save 42: immediately after both Research purchases.
    pink.assertResources(m = 20, s = 0, t = 0, p = 0, e = 0, h = 2)
    pink.assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 2)
    purple.assertResources(m = 30, s = 0, t = 0, p = 1, e = 1, h = 0)
    purple.assertProduction(m = 14, s = 0, t = 0, p = 0, e = 1, h = 0)
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 3, venus = 4)
    checkHandSizes()

    purple.turn { claimMilestone(cn("Fundraiser")) }
    pink.turn {
      cardAction2(TitanShuttles, 2)
      playProject(LagrangeObservatory, 3, titanium = 2) { draw(StaticHarvesting) }
    }
    purple.turn {
      playProject(BactoviralResearch, 10) {
        draw(LocalShading)
        addCardResources(Tardigrades)
      }
      // Database save 49: Bactoviral Research introduced Purple's first science tag.
      claimMilestone(cn("Diversifier"))
    }
    pink.turn {
      playProject(HousePrinting, 10)
      playProject(StaticHarvesting, 5)
    }
    purple.turn {
      cardAction1(Tardigrades)
      playProject(LocalShading, 4)
    }
    // Database save 56: Local Shading introduced Purple's first Venus tag.
    pink.turn {
      cardAction1(SulphurEatingBacteria)
      playProject(MicroMills, 3)
    }
    purple.turn { cardAction1(LocalShading) }
    pink.pass()
    purple.pass()
    purple.wgt("OxygenStep")
  }

  private fun generation3BeforeResignation() {
    // Again, both players saw all eight draft cards. Pink's final set was Terraforming Contract,
    // Electro Catapult, Underground City, and Shuttles; Purple's was Red Spot Observatory, Mining
    // Colony, Outdoor Sports, and Soil Enrichment.
    pink.buyCards(TerraformingContract)
    pink.discardUnselectedProjectCards(ElectroCatapult, UndergroundCity, Shuttles)
    purple.buyCards(MiningColony)
    purple.discardUnselectedProjectCards(RedSpotObservatory, OutdoorSports, SoilEnrichment)

    // Database save 74: immediately after both Research purchases.
    pink.assertResources(m = 21, s = 1, t = 0, p = 0, e = 1, h = 5)
    pink.assertProduction(m = 0, s = 1, t = 0, p = 0, e = 1, h = 3)
    purple.assertResources(m = 35, s = 0, t = 0, p = 1, e = 1, h = 1)
    purple.assertProduction(m = 16, s = 0, t = 0, p = 0, e = 1, h = 0)
    checkHandSizes()

    pink.turn { cardAction1(TitanShuttles) { addCardResources(TitanShuttles, 2) } }
    purple.turn {
      playProject(InterplanetaryTrade, 27).expect("PROD[9 MC]")
      claimMilestone(cn("Producer"))
    }

    // Save 79 is the last retained state. User recollection supplies the subsequent resignation;
    // the database contains no resignation save or log entry.
    pink.assertResources(m = 21, s = 1, t = 0, p = 0, e = 1, h = 5)
    pink.assertProduction(m = 0, s = 1, t = 0, p = 0, e = 1, h = 3)
    pink.assertCounts(23 to "TerraformRating")
    pink.assertCardResources(2 to TitanShuttles, 2 to SulphurEatingBacteria)
    pink.cardsHand shouldBe
        setOf(
            Pets,
            CloudSeeding,
            IoSulphurResearch,
            GhgImportFromVenus,
            AtmoCollectors,
            Cartel,
            VenusSoils,
            StratosphericExpedition,
            HeatTrappers,
            TerraformingContract,
        )

    purple.assertResources(m = 0, s = 0, t = 0, p = 1, e = 1, h = 1)
    purple.assertProduction(m = 25, s = 0, t = 0, p = 0, e = 1, h = 0)
    purple.assertCounts(
        22 to "TerraformRating",
        1 to "Fundraiser",
        1 to "Diversifier",
        1 to "Producer",
    )
    purple.assertCardResources(3 to Tardigrades, 1 to LocalShading)
    purple.cardsHand shouldBe
        setOf(NeutralizerFactory, FusionPower, Omnicourt, SolarReflectors, MiningColony)

    assertSidebar(gen = 3, temp = -30, oxygen = 2, oceans = 3, venus = 4)
    admin.assertCounts(5 to "Tile")
    checkHandSizes()
    assertCardTrackingComplete()
  }
}
