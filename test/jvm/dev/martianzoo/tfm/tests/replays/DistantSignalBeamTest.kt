package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
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
          VenusNextExpansion, PreludeExpansion, prelude2:cards, ColoniesExpansion, promos:cards
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
    pink.autoExecPolicy = NONE
    purple.autoExecPolicy = NONE
    admin.startGame()
    pink.doTask("20 TerraformRating")
    purple.doTask("20 TerraformRating")
    pink.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-3 ProjectCard<Selecting>",
    )
    purple.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "Ok",
    )
    generation1()
    generation2()
    generation3BeforeResignation()
    expectPendingTasksAtEnd(1) // Pink's next action offer
  }

  private fun generation1() {
    pink.draw(
        HousePrinting,
        CloudSeeding,
        MicroMills,
        Pets,
        LagrangeObservatory,
        GhgImportFromVenus,
        IoSulphurResearch,
    )
    pink.doTask("PlayCard<Class<CorporationCard>, Class<$MorningStarInc>>")
    pink.doTask("$MorningStarInc FROM CorporationCard")
    pink.doTasks("50 MC", "21 Pay<> FROM MC", "7 ProjectCard<Hand FROM Selecting>")
    purple.draw(
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
    purple.doTask("PlayCard<Class<CorporationCard>, Class<$Aridor>>")
    purple.doTask("$Aridor FROM CorporationCard")
    purple.doTasks("40 MC", "30 Pay<> FROM MC", "10 ProjectCard<Hand FROM Selecting>")

    pink.turn {
      draw(TitanShuttles, AtmoCollectors)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AtmosphericEnhancers>>",
          "$AtmosphericEnhancers FROM PreludeCard",
          "2 VenusStep",
          "2 TerraformRating",
          "2 SearchForCard<ReferenceFilter<Class<Floater>>>",
      )
      draw(Cartel)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$PolarIndustries>>",
          "$PolarIndustries FROM PreludeCard",
          "OceanTile<Hellas_5_6>",
          "TerraformRating",
          "PROD[2 Heat]",
          "ProjectCard",
      )
    }

    purple.turn {
      purple.discard(PowerSupplyConsortium, AdaptationTechnology, BreathingFilters)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ProjectEden>>",
          "$ProjectEden FROM PreludeCard",
          "-3 ProjectCard",
          "PROD[MC]",
          "PROD[MC]",
          "OceanTile<Hellas_4_6>",
          "GreeneryTile<Hellas_4_5>",
          "CityTile<Hellas_4_4>",
      )
      doTasks(
          "TerraformRating",
          "Plant",
          "2 MC",
          "OxygenStep",
          "TerraformRating",
          "Steel",
          "2 MC",
          "2 MC",
          "2 Steel",
      )
      // Database save 4: Project Eden introduced Purple's first city and plant tags.
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AlliedBank>>",
          "$AlliedBank FROM PreludeCard",
          "3 MC",
          "PROD[MC]",
          "PROD[4 MC]",
      )
    }
    // Database save 5: Allied Bank introduced Purple's first Earth tag.
    pink.turn {
      draw(VenusSoils, SulphurEatingBacteria, StratosphericExpedition)
      doTasks("DoRequiredActions", "3 SearchForCard<TagFilter<Class<VenusTag>>>")
      doTasks(
          *playCardTasks("$TitanShuttles"),
          "23 Pay<> FROM MC",
          "$TitanShuttles FROM ProjectCard",
      )
    }
    purple.turn {
      stdAction("DoRequiredActions") {
        selectTask("ColonyTileSelection")
        narrowTask("Luna")
      }
    }

    pink.turn { doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$TitanShuttles>") }
    purple.turn {
      doTasks(
          *playCardTasks("$PeroxidePower"),
          "3 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$PeroxidePower FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Energy]",
          "PROD[MC]",
          "PROD[MC]",
      )
    }
    // Database save 15: Peroxide Power introduced Purple's first power and building tags.
    pink.turn {
      doTasks(
          *playCardTasks("$SulphurEatingBacteria"),
          "6 Pay<> FROM MC",
          "$SulphurEatingBacteria FROM ProjectCard",
      )
    }
    purple.turn {
      doTasks(
          *playCardTasks("$Casinos"),
          "5 Pay<> FROM MC",
          "$Casinos FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 MC]",
      )
    }
    pink.turn {
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
    }
    purple.turn {
      doTasks(
          *playCardTasks("$Tardigrades"),
          "4 Pay<> FROM MC",
          "$Tardigrades FROM ProjectCard",
          "PROD[MC]",
      )
    }
    // Database save 27: Tardigrades introduced Purple's first microbe tag.
    pink.doTask("Pass")
    purple.turn {
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTasks(
          *playCardTasks("$RotatorImpacts"),
          "6 Pay<> FROM MC",
          "$RotatorImpacts FROM ProjectCard",
          "PROD[MC]",
      )
      // Database save 33: Rotator Impacts introduced Purple's first space tag.
      doTask("Pass")
    }
    pink.doTasks("5 MC", "23 MC", "2 Heat", "-5 MC")
    purple.doTasks("19 MC", "22 MC", "Energy", "-5 MC")
    pink.doTask("OceanTile<Hellas_5_7>! BY Admin")
  }

  private fun generation2() {
    pink.doTask("4 ProjectCard<Selecting>")
    purple.doTask("4 ProjectCard<Selecting>")
    pink.draw(HeatTrappers)
    pink.doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
    purple.draw(Omnicourt, BactoviralResearch, SolarReflectors)
    purple.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    // Database save 42: immediately after both Research purchases.
    pink.assertResources(m = 20, s = 0, t = 0, p = 0, e = 0, h = 2)
    pink.assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 2)
    purple.assertResources(m = 30, s = 0, t = 0, p = 1, e = 1, h = 0)
    purple.assertProduction(m = 14, s = 0, t = 0, p = 0, e = 1, h = 0)
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 3, venus = 4)

    purple.turn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Fundraiser")
    }
    pink.turn {
      doTasks(
          *cardActionTasks("$TitanShuttles", "Action2"),
          "-2 Floater THEN 2 Titanium",
          "2 Titanium",
      )
      draw(StaticHarvesting)
      doTasks(
          *playCardTasks("$LagrangeObservatory"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$LagrangeObservatory FROM ProjectCard",
          "ProjectCard",
      )
    }
    purple.turn {
      draw(LocalShading)
      doTasks(
          *playCardTasks("$BactoviralResearch"),
          "10 Pay<> FROM MC",
          "$BactoviralResearch FROM ProjectCard",
          "ProjectCard",
          "Microbe<$Tardigrades>",
          "PROD[MC]",
      )
      // Database save 49: Bactoviral Research introduced Purple's first science tag.
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Diversifier")
    }
    pink.turn {
      doTasks(
          *playCardTasks("$HousePrinting"),
          "10 Pay<> FROM MC",
          "$HousePrinting FROM ProjectCard",
          "PROD[Steel]",
      )
      doTasks(
          *playCardTasks("$StaticHarvesting"),
          "5 Pay<> FROM MC",
          "$StaticHarvesting FROM ProjectCard",
          "PROD[Energy]",
          "2 MC",
      )
    }
    purple.turn {
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTasks(
          *playCardTasks("$LocalShading"),
          "4 Pay<> FROM MC",
          "$LocalShading FROM ProjectCard",
          "PROD[MC]",
      )
    }
    // Database save 56: Local Shading introduced Purple's first Venus tag.
    pink.turn {
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTasks(
          *playCardTasks("$MicroMills"),
          "3 Pay<> FROM MC",
          "$MicroMills FROM ProjectCard",
          "PROD[Heat]",
      )
    }
    purple.turn { doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>") }
    pink.doTask("Pass")
    purple.doTask("Pass")
    purple.doTasks("21 MC", "22 MC", "Energy", "Heat FROM Energy", "-5 MC")
    pink.doTasks("5 MC", "23 MC", "3 Heat", "Steel", "Energy", "-5 MC")
    purple.doTask("OxygenStep! BY Admin")
  }

  private fun generation3BeforeResignation() {
    pink.doTask("4 ProjectCard<Selecting>")
    purple.doTask("4 ProjectCard<Selecting>")
    pink.draw(TerraformingContract)
    pink.doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")
    purple.draw(MiningColony)
    purple.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    // Database save 74: immediately after both Research purchases.
    pink.assertResources(m = 21, s = 1, t = 0, p = 0, e = 1, h = 5)
    pink.assertProduction(m = 0, s = 1, t = 0, p = 0, e = 1, h = 3)
    purple.assertResources(m = 35, s = 0, t = 0, p = 1, e = 1, h = 1)
    purple.assertProduction(m = 16, s = 0, t = 0, p = 0, e = 1, h = 0)

    pink.turn { doTasks(*cardActionTasks("$TitanShuttles"), "2 Floater<$TitanShuttles>") }
    purple.turn {
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "27 Pay<> FROM MC",
          "$InterplanetaryTrade FROM ProjectCard",
      )
      inTurn {
        selectTask("EACH @WildTagElection { Tag<@WildTagElection>? }")
        doTask("PROD[9 MC]")
      }
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Producer")
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
    assertCardTrackingComplete()
  }

  override val cardArrivalOrder =
      mapOf(
          // Source save 0: the third corporation offers were PolderTECH Dutch for Pink and
          // Point Luna for Purple.
          cn("CorporationCard") to
              mapOf(
                  cn("Pink") to listOf(Vitor, MorningStarInc),
                  cn("Purple") to listOf(Aridor, Ecoline),
              ),
          cn("PreludeCard") to
              mapOf(
                  cn("Pink") to
                      listOf(PolarIndustries, AtmosphericEnhancers, Loan, PowerGeneration),
                  cn("Purple") to
                      listOf(BiosphereSupport, FocusedOrganization, ProjectEden, AlliedBank),
              ),
      )

  override val cardDepartureOrder =
      mapOf(
          cn("CorporationCard") to
              mapOf(cn("Pink") to listOf(Vitor), cn("Purple") to listOf(Ecoline)),
          cn("PreludeCard") to
              mapOf(
                  cn("Pink") to listOf(Loan, PowerGeneration),
                  cn("Purple") to listOf(BiosphereSupport, FocusedOrganization),
              ),
      )

  // Source setup saves and saves 41-42, 73-74 identify these modeled offers.
  override val selectingArrivalOrder =
      mapOf(
          cn("ProjectCard") to
              mapOf(
                  cn("Pink") to
                      listOf(
                          HousePrinting,
                          CloudSeeding,
                          MicroMills,
                          Pets,
                          LagrangeObservatory,
                          GhgImportFromVenus,
                          IoSulphurResearch,
                          Insects,
                          EcologyResearch,
                          SecurityFleet,
                          GeothermalPower,
                          HeatTrappers,
                          PhobosSpaceHaven,
                          DirectedHeatUsage,
                          TerraformingContract,
                          ElectroCatapult,
                          UndergroundCity,
                          Shuttles,
                      ),
                  cn("Purple") to
                      listOf(
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
                          Omnicourt,
                          SolarReflectors,
                          SoilFactory,
                          BactoviralResearch,
                          RedSpotObservatory,
                          MiningColony,
                          OutdoorSports,
                          SoilEnrichment,
                      ),
              ),
      )
  override val selectingDepartureOrder =
      mapOf(
          cn("ProjectCard") to
              mapOf(
                  cn("Pink") to
                      listOf(
                          Insects,
                          EcologyResearch,
                          SecurityFleet,
                          GeothermalPower,
                          PhobosSpaceHaven,
                          DirectedHeatUsage,
                          ElectroCatapult,
                          UndergroundCity,
                          Shuttles,
                      ),
                  cn("Purple") to
                      listOf(SoilFactory, RedSpotObservatory, OutdoorSports, SoilEnrichment),
              ),
      )
}
