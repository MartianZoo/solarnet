package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.giveTask
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

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
  private val greenStartingCards =
      listOf(
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
                  cn("KB") to
                      greenStartingCards +
                          listOf(ImmigrantCity, MagneticFieldGenerators, Ants, LargeConvoy),
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
    startGame()
    pink.doTask("StandardOption")
    green.doTask("BeginnerOption")
    green.doTask("20 TerraformRating")
    pink.doTask("20 TerraformRating")
    pink.doTask("2 CorporationCard")
    pink.doTask("10 ProjectCard<Selecting>")
    pink.doTask("-CorporationCard")
    pink.doTask("-5 ProjectCard<Selecting>")
    green.doTask("BeginnerCard")
    green.doTask("10 ProjectCard<Selecting>")

    generation1()
    generation2()

    green.doTask("Pass")
    green.doTasks("22 MC", "Steel", "Titanium", "Plant", "4 Heat FROM Energy", "5 Energy", "Heat")
    pink.doTasks(
        "20 MC",
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

    assertCardTrackingComplete()

    // _local/replays/Game20260920/Heroku-gadeac80e6446/game-gadeac80e6446.sqlite; source save 37,
    // replay endpoint.
    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -              LSS   WSS    L     WC    W
        |
        | 2 -            L     VS    L     L     L    [O]
        |
        | 3 -         VC    L     L     L     L     L     LS
        |
        | 4 -     VPT    LP    LP    LP   LPP    LP    LP   WPP
        |
        | 5 -  VPP   LPP   NPP   WPP   WPP   WPP   LPP   LPP   LPP
        |
        | 6 -      LP   LPP    LP    LP    LP    WP    WP    WP
        |
        | 7 -         L     L     L     L     L     LP    L
        |
        | 8 -           LSS    L     LC    LC    L     LT
        |
        | 9 -               LS   LSS    L     L    [O]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
  }

  private fun generation1() {
    green.draw(*greenStartingCards.toTypedArray())
    with(green) {
      doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation1>, Hand>")
      doTask("BeginnerCorporation1 FROM BeginnerCard")
      doTask("42 MC")
      doTask("10 ProjectCard<Hand FROM Selecting>")
    }
    pink.draw(
        PowerPlant,
        Flooding,
        ArcticAlgae,
        LavaFlows,
        CupolaCity,
    )
    with(pink) {
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

      green.giveTask(pink, "2 Plant<ER>")
      green.doTask("2 ProjectCard")
    }
    pink.turn {
      doTasks(
          *playCardTasks("$PowerPlant"),
          "2 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // no mc paid
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
    }
    green.doTask("Pass")
  }

  private fun generation2() {
    green.doTasks("22 MC", "Steel", "Titanium", "Plant", "4 Energy", "Heat")
    pink.doTasks("21 MC", "2 Steel", "Titanium", "Plant", "2 Energy", "Heat")
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

      doTasks("PROD[Steel]", "2 Titanium")
    }
    green.turn {
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
    }
    pink.doTask("Pass")
  }
}
