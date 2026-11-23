package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.Test

internal class StinaGameTest : CardTrackingFullGameTest() {
  override val soloTileLocations =
      listOf("Elysium_5_6", "Elysium_5_5", "Elysium_7_7", "Elysium_7_6")
  override val config = GameConfig("ElysiumMap, PreludeExpansion", "Me")
  // Test inference: the source names only the selected cards. CrediCor, Allied Bank, and
  // Business Empire are invented identities for the three rejected setup cards, not source facts.
  override val cardArrivalOrder =
      mapOf(
          cn("CorporationCard") to mapOf(cn("Me") to listOf(SaturnSystems, CrediCor)),
          cn("PreludeCard") to
              mapOf(cn("Me") to listOf(Biolab, AcquiredSpaceAgency, AlliedBank, BusinessEmpire)),
      )
  override val cardDepartureOrder =
      mapOf(
          cn("CorporationCard") to mapOf(cn("Me") to listOf(CrediCor)),
          cn("PreludeCard") to mapOf(cn("Me") to listOf(AlliedBank, BusinessEmpire)),
      )
  // Test assignments: the source does not name the Invention Contest or Business Contacts
  // rejects. These four late-game cards do not help terraform the planet in generation 1.
  override val selectingArrivalOrder =
      mapOf(
          cn("ProjectCard") to
              mapOf(
                  cn("Me") to
                      listOf(
                          EarthOffice,
                          MediaGroup,
                          InvestmentLoan,
                          IndenturedWorkers,
                          EarthCatapult,
                          HiredRaiders,
                          OlympusConference,
                          AdvancedAlloys,
                          MineralDeposit,
                          ResearchOutpost,
                          // Invention Contest
                          MassConverter,
                          SecurityFleet,
                          Penguins,
                          // Business Contacts
                          TowingAComet,
                          AdaptationTechnology,
                          Herbivores,
                          Predators,
                      ),
              ),
      )
  override val selectingDepartureOrder =
      mapOf(
          cn("ProjectCard") to
              mapOf(cn("Me") to listOf(SecurityFleet, Penguins, Herbivores, Predators)),
      )

  private val me
    get() = p1

  @Test
  internal fun stinaSaturnSystemsGame() {
    startGame()
    with(me) {
      // Test inference: unnamed draws are assigned in the order the cards are later played.
      doTasks(
          "14 TerraformRating",
          "2 CorporationCard",
          "-CorporationCard",
          "10 ProjectCard<Selecting>",
          "4 PreludeCard",
          "-2 PreludeCard",
          "Ok", // keep all ten project cards
      )
      draw(
          EarthOffice,
          MediaGroup,
          InvestmentLoan,
          IndenturedWorkers,
          EarthCatapult,
          HiredRaiders,
          OlympusConference,
          AdvancedAlloys,
          MineralDeposit,
          ResearchOutpost,
      )
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$SaturnSystems>>",
          "42 MC",
          "PROD[Titanium]",
          "PROD[MC]",
          "30 Pay<> FROM MC",
          "10 ProjectCard<Hand FROM Selecting>",
      )

      draw(InventionContest, BusinessContacts, QuantumExtractor)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Biolab>>",
          "PROD[Plant]",
          "3 ProjectCard",
      )
      draw(SpaceStation, OptimalAerobraking)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AcquiredSpaceAgency>>",
          "6 Titanium",
          "2 SearchForCard<TagFilter<Class<SpaceTag>>>",
      )

      me.turn {
        doTasks(*playCardTasks("$EarthOffice"), "1 Pay<> FROM MC")
        doTasks(*playCardTasks("$MediaGroup"), "3 Pay<> FROM MC")
        doTasks(
            *playCardTasks("$InvestmentLoan"),
            "PROD[-MC]",
            "13 MC",
        )
        doTasks(
            *playCardTasks("$IndenturedWorkers"),
            "3 MC",
            "IndenturedWorkers_NextCardEffect",
        )
        doTasks(
            *playCardTasks("$EarthCatapult"),
            "12 Pay<> FROM MC",
        )
        doTasks(
            *playCardTasks("$HiredRaiders"),
            "2 Steel<Me> FROM Steel<SoloOpponent>",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$OlympusConference"),
            "2 Pay<Class<Steel>> FROM Steel",
            "1 Pay<> FROM MC",
            "Science<$OlympusConference>",
        )
        draw(TechnologyDemonstration)
        doTasks(
            *playCardTasks("$AdvancedAlloys"),
            "7 Pay<> FROM MC",
            "ProjectCard FROM Science",
        )
        doTasks(
            *playCardTasks("$MineralDeposit"),
            "3 Pay<> FROM MC",
            "5 Steel",
            "3 MC",
        )
        draw(ImportOfAdvancedGhg)
        doTasks(
            *playCardTasks("$ResearchOutpost"),
            "5 Pay<Class<Steel>> FROM Steel",
            "1 Pay<> FROM MC",
            "CityTile<Elysium_9_7>",
            "Science<$OlympusConference>",
            "ProjectCard",
        )
        draw(ImportedGhg, MassConverter)
        doTasks(
            *playCardTasks("$InventionContest"),
            "ProjectCard FROM Science",
            "3 ProjectCard<Selecting>",
            "ProjectCard<Hand FROM Selecting>",
            "3 MC",
        )
        draw(TowingAComet, AdaptationTechnology)
        doTasks(
            *playCardTasks("$BusinessContacts"),
            "1 Pay<> FROM MC",
            "4 ProjectCard<Selecting>",
            "2 ProjectCard<Hand FROM Selecting>",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$QuantumExtractor"),
            "10 Pay<> FROM MC",
            "Science<$OlympusConference>",
            "PROD[4 Energy]",
        )
        doTasks(
            *playCardTasks("$SpaceStation"),
            "1 Pay<Class<Titanium>> FROM Titanium",
            "1 Pay<> FROM MC",
        )
        doTasks(*playCardTasks("$OptimalAerobraking"))
        // Test inference: Lagrange Observatory is free after the accumulated space discounts and
        // draws twice with Olympus Conference. Convoy from Europa costs 3 M€, rebates 6 M€, draws a
        // replacement card, and places an ocean.
        draw(SpecialDesign, Shuttles, LagrangeObservatory)
        doTasks(
            *playCardTasks("$TechnologyDemonstration"),
            "2 ProjectCard",
            "ProjectCard FROM Science",
            "6 MC",
            "3 Heat",
        )
        doTasks(
            *playCardTasks("$ImportOfAdvancedGhg"),
            "PROD[2 Heat]",
            "6 MC",
            "3 Heat",
        )
        doTasks(
            *playCardTasks("$ImportedGhg"),
            "PROD[Heat]",
            "6 Heat",
            "6 MC",
        )
        doTasks(
            *playCardTasks("$MassConverter"),
            "5 Pay<> FROM MC",
            "Science<$OlympusConference>",
            "PROD[6 Energy]",
        )
        doTasks(
            *playCardTasks("$TowingAComet"),
            "3 Pay<Class<Titanium>> FROM Titanium",
            "2 Pay<> FROM MC",
            "2 Plant",
            "OxygenStep",
            "TerraformRating",
            "OceanTile<Elysium_1_2>",
            "Titanium",
            "TerraformRating",
            "6 MC",
            "3 Heat",
        )
        doTasks(
            *playCardTasks("$AdaptationTechnology"),
            "9 Pay<> FROM MC",
            "Science<$OlympusConference>",
        )
        draw(ConvoyFromEuropa)
        doTasks(
            *playCardTasks("$SpecialDesign"),
            "1 Pay<> FROM MC",
            "ProjectCard FROM Science",
            "3 MC",
            "SpecialDesign_NextCardEffect",
        )
        doTasks(
            *playCardTasks("$Shuttles"),
            "Ok", // no titanium paid
            "1 Pay<> FROM MC",
            "PROD[-Energy]",
            "PROD[2 MC]",
        )
      }

      assertCardTrackingComplete()

      assertResources(m = 9, s = 0, t = 3, p = 2, e = 0, h = 15)
      assertProduction(m = 2, s = 0, t = 1, p = 1, e = 9, h = 3)
      assertCounts(
          16 to "TerraformRating",
          2 to "ProjectCard",
          26 to "CardFront OR PlayedEvent",
          12 to "ActiveCard",
          0 to "AutomatedCard",
          11 to "PlayedEvent",
      )
      assertTags(but = 2, spt = 3, sct = 7, pot = 2, eat = 4, jot = 1, cit = 1)
      assertCounts(
          1 to "Generation",
          0 to "TemperatureStep",
          1 to "OxygenStep",
          1 to "OceanTile",
          1 to "Science",
      )
    }
  }
}
