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

// Partial database replay through generation 2: Active Proton Flow (gadeac80e6446)
// Source: _local/replays/Game20260920/Heroku-gadeac80e6446/
// http://newazure.local:8080/the-end?id=pf8b6b214192d
internal class ActiveProtonFlowTest : CardTrackingFullGameTest() {
  override val config =
      GameConfig(
          """
          TharsisMap, BeginnerVariant, QuickStartVariant

          Sponsor, Producer, Engineer, Fundraiser, Spacefarer
          Forecaster, EstateDealer, SpaceBaron, Suburbian, Celebrity
          """,
          "KB",
          "ER",
      )
  override val cardArrivalOrder =
      mapOf(
          cn("BeginnerCard") to mapOf(cn("KB") to listOf(cn("BeginnerCorporation1"))),
          cn("CorporationCard") to mapOf(cn("ER") to listOf(ThorGate, MiningGuild)),
      )
  override val cardDepartureOrder =
      mapOf(cn("CorporationCard") to mapOf(cn("ER") to listOf(ThorGate)))
  // Source save 0 and saves 27-28 identify these modeled setup and Research offers.
  override val selectingArrivalOrder =
      mapOf(
          cn("ProjectCard") to
              mapOf(
                  cn("ER") to
                      listOf(
                          ArcticAlgae,
                          ReleaseOfInertGases,
                          Flooding,
                          LavaFlows,
                          RadChemFactory,
                          PowerPlant,
                          RegolithEaters,
                          Greenhouses,
                          AsteroidMining,
                          CupolaCity,
                          BlackPolarDust,
                          NitrophilicMoss,
                          HeatTrappers,
                          Farming,
                      ),
                  cn("KB") to listOf(ImmigrantCity, MagneticFieldGenerators, Ants, LargeConvoy),
              ),
      )
  override val selectingDepartureOrder =
      mapOf(
          cn("ProjectCard") to
              mapOf(
                  cn("ER") to
                      listOf(
                          ReleaseOfInertGases,
                          RadChemFactory,
                          RegolithEaters,
                          Greenhouses,
                          AsteroidMining,
                          Farming,
                      ),
                  cn("KB") to listOf(Ants, LargeConvoy),
              ),
      )

  private val green
    get() = p1

  private val pink
    get() = p2

  @Test
  internal fun activeProtonFlowThroughGeneration2() {
    green.autoExecPolicy = NONE
    pink.autoExecPolicy = NONE
    admin.startGame()
    pink.selectTask("BeginnerOption OR StandardOption")
    pink.narrowTask("StandardOption")
    green.selectTask("BeginnerOption OR StandardOption")
    green.narrowTask("BeginnerOption")
    green.doTask("20 TerraformRating")
    pink.doTask("20 TerraformRating")
    pink.doTask("2 CorporationCard")
    pink.doTask("10 ProjectCard<Selecting>")
    pink.doTask("-CorporationCard")
    pink.doTask("-5 ProjectCard<Selecting>")
    green.doTask("BeginnerCard")

    generation1()
    generation2()

    green.doTask("Pass")
    green.doTasks(
        "6 MC",
        "21 MC",
        "-5 MC",
        "Steel",
        "Titanium",
        "Plant",
        "4 Heat FROM Energy",
        "5 Energy",
        "Heat",
    )
    pink.doTasks(
        "4 MC",
        "21 MC",
        "-5 MC",
        "3 Steel",
        "Titanium",
        "Plant",
        "2 Heat FROM Energy",
        "2 Energy",
        "4 Heat",
    )

    // Database save 37: generation 3 Research, immediately after generation 2 production.
    green.assertResources(m = 33, s = 2, t = 2, p = 2, e = 5, h = 9)
    green.assertProduction(m = 1, s = 1, t = 1, p = 1, e = 5, h = 1)
    green.assertCounts(21 to "TerraformRating", 5 to "CardFront OR PlayedEvent")
    green.cardsHand shouldBe
        setOf(
            GreatDam,
            IceCapMelting,
            Predators,
            NuclearPower,
            EquatorialMagnetizer,
            PermafrostExtraction,
            OreProcessor,
            ImportedHydrogen,
            ResearchOutpost,
            MagneticFieldGenerators,
            ImmigrantCity,
        )

    pink.assertResources(m = 20, s = 8, t = 4, p = 7, e = 2, h = 7)
    pink.assertProduction(m = -1, s = 3, t = 1, p = 1, e = 2, h = 4)
    pink.assertCounts(21 to "TerraformRating", 4 to "CardFront OR PlayedEvent")
    pink.cardsHand shouldBe setOf(Flooding, LavaFlows, CupolaCity, HeatTrappers, NitrophilicMoss)

    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 2)
    admin.assertCounts(2 to "Tile")
    assertCardTrackingComplete()
    expectPendingTasksAtEnd(4) // Generation 3 Research offers and their deferred phase work
  }

  private fun generation1() {
    green.draw(
        GreatDam,
        GeothermalPower,
        IceCapMelting,
        OptimalAerobraking,
        Predators,
        NuclearPower,
        EquatorialMagnetizer,
        ConvoyFromEuropa,
        SpaceMirrors,
        PermafrostExtraction,
    )
    green.inTurn {
      doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation1>, Hand>")
      doTask("BeginnerCorporation1 FROM BeginnerCard")
      doTask("42 MC")
      doTask("10 ProjectCard")
    }
    pink.draw(
        PowerPlant,
        Flooding,
        ArcticAlgae,
        LavaFlows,
        CupolaCity,
    )
    pink.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$MiningGuild>>",
          "$MiningGuild FROM CorporationCard",
          "30 MC",
          "5 Steel",
          "PROD[Steel]",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }

    green.turn {
      doTasks(*playCardTasks("$SpaceMirrors"), "3 Pay<> FROM MC", "$SpaceMirrors FROM ProjectCard")
    }
    pink.turn {
      doTasks(
          *playCardTasks("$ArcticAlgae"),
          "12 Pay<> FROM MC",
          "$ArcticAlgae FROM ProjectCard",
          "Plant",
      )
    }
    green.turn {
      doTasks(
          *playCardTasks("$OptimalAerobraking"),
          "7 Pay<> FROM MC",
          "$OptimalAerobraking FROM ProjectCard",
      )
      draw(
          OreProcessor,
          ImportedHydrogen,
          ResearchOutpost,
      )
      doTasks(
          *playCardTasks("$ConvoyFromEuropa"),
          "15 Pay<> FROM MC",
          "$ConvoyFromEuropa FROM ProjectCard",
      )
      doTask("ProjectCard")
      doTask("3 MC")
      doTask("3 Heat")
      doTask("OceanTile<Tharsis_2_6>")
      doTask("TerraformRating")
    }
    pink.doTask("2 Plant")
    green.doTask("2 ProjectCard")
    pink.turn {
      doTasks(
          *playCardTasks("$PowerPlant"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // no mc paid
          "$PowerPlant FROM ProjectCard",
          "PROD[Energy]",
      )
    }
    green.turn {
      doTasks(
          *playCardTasks("$GeothermalPower"),
          "11 Pay<> FROM MC",
          "$GeothermalPower FROM ProjectCard",
          "PROD[2 Energy]",
      )
    }
    pink.doTask("Pass")
    green.turn {
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
      doTask("Pass")
    }
  }

  private fun generation2() {
    green.doTasks("6 MC", "21 MC", "-5 MC", "Steel", "Titanium", "Plant", "4 Energy", "Heat")
    pink.doTasks("6 MC", "20 MC", "-5 MC", "2 Steel", "Titanium", "Plant", "2 Energy", "Heat")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    pink.draw(HeatTrappers, NitrophilicMoss, BlackPolarDust)
    pink.doTasks("-ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
    green.draw(MagneticFieldGenerators, ImmigrantCity)
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    // Database save 28: immediately after both generation 2 Research purchases.
    green.assertResources(m = 18, s = 1, t = 1, p = 1, e = 4, h = 4)
    green.assertProduction(m = 1, s = 1, t = 1, p = 1, e = 4, h = 1)
    green.assertCounts(21 to "TerraformRating", 11 to "ProjectCard")
    pink.assertResources(m = 15, s = 5, t = 1, p = 4, e = 2, h = 1)
    pink.assertProduction(m = 1, s = 2, t = 1, p = 1, e = 2, h = 1)
    pink.assertCounts(20 to "TerraformRating", 6 to "ProjectCard")
    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 1)
    pink.turn {
      doTasks(
          *playCardTasks("$BlackPolarDust"),
          "15 Pay<> FROM MC",
          "$BlackPolarDust FROM ProjectCard",
          "OceanTile<Tharsis_9_9>",
          "TerraformRating",
          "2 Plant",
          "PROD[-2 MC]",
          "PROD[3 Heat]",
      )
    }
    pink.doTasks("PROD[Steel]", "2 Titanium")
    pink.doTask("PassBaton")
    green.turn { doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]") }
    pink.doTask("Pass")
  }
}
