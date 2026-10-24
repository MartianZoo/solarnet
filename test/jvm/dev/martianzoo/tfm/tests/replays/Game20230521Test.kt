package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.World
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Explicit player-task replay; Admin keeps its normal autoexecution.
// Source: _local/replays/Game20230521/game-replay.rego (game gf386a4cd5de1).
internal class Game20230521Test : AbstractFullGameTest() {

  override val config =
      GameConfig(
          """
          VenusNextExpansion, PreludeExpansion, promos:cards
          -WorldGovernmentRule
          """,
          "Player1",
          "Player2",
      )

  @Test
  internal fun game20230521() {
    listOf(p1, p2).forEach { it.autoExecPolicy = NONE }
    admin.startGame()
    p1.doTasks("20 TerraformRating")
    p2.doTasks("20 TerraformRating")
    p1.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    p2.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    p1.doTasks("-5 ProjectCard<Selecting>")
    p2.doTasks("-6 ProjectCard<Selecting>")
    // Generation 1

    p1.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Manutech>>",
          "$Manutech FROM CorporationCard",
          "35 MC",
          "PROD[Steel]",
          "Steel",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }

    p2.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Factorum>>",
          "$Factorum FROM CorporationCard",
          "37 MC",
          "PROD[Steel]",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    p1.turn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$NewPartner>>",
          "$NewPartner FROM PreludeCard",
          "PROD[MC]",
          "2 PreludeCard<Selecting>",
          "MC",
          "PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>",
          "$UnmiContractor FROM PreludeCard<Selecting>",
          "3 TerraformRating",
          "ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AlliedBank>>",
          "$AlliedBank FROM PreludeCard",
          "PROD[4 MC]",
          "7 MC",
      )
    }

    p2.turn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AcquiredSpaceAgency>>",
          "$AcquiredSpaceAgency FROM PreludeCard",
          "6 Titanium",
          "2 SearchForCard<TagFilter<Class<SpaceTag>>>",
      )
    }

    p2.turn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$IoResearchOutpost>>",
          "$IoResearchOutpost FROM PreludeCard",
          "PROD[Titanium]",
          "ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$InventorsGuild"),
          "9 Pay<> FROM MC",
          "$InventorsGuild FROM ProjectCard",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$ArcticAlgae"),
          "12 Pay<> FROM MC",
          "$ArcticAlgae FROM ProjectCard",
          "Plant",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-0 ProjectCard<Selecting>", // keep card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$Factorum"),
          "PROD[Energy]",
      )
    }

    p1.turn {
      doTasks(
          *standardProjectTasks("PowerPlantProject"),
          "11 Pay<> FROM MC",
          "PROD[Energy]",
          "Energy",
      )
      doTasks(
          *playCardTasks("$BuildingIndustries"),
          "1 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$BuildingIndustries FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 Steel]",
          "2 Steel",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no mc paid
          "$RotatorImpacts FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no mc paid
          "Asteroid<$RotatorImpacts>",
      )
    }

    p1.turn {
      doTasks("Pass")
    }

    p2.turn {
      doTasks(
          *playCardTasks("$CarbonateProcessing"),
          "6 Pay<> FROM MC",
          "$CarbonateProcessing FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 Heat]",
      )
      doTasks(
          *playCardTasks("$Archaebacteria"),
          "6 Pay<> FROM MC",
          "$Archaebacteria FROM ProjectCard",
          "PROD[Plant]",
      )
      doTasks("Pass")
    }

    // Generation 2
    p1.doTasks("Heat FROM Energy", "28 MC", "3 Steel")
    p2.doTasks("20 MC", "Steel", "Titanium", "3 Heat", "Plant")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    p1.assertProduction(m = 5, s = 3, t = 0, p = 0, e = 0, h = 0)
    p1.assertResources(m = 23, s = 5, t = 0, p = 0, e = 0, h = 1)
    p1.assertDashMiddle(played = 6, actions = 1, tr = 23, hand = 7)
    p1.assertTags(but = 2, sct = 1, eat = 2)
    p1.assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    p2.assertProduction(m = 0, s = 1, t = 1, p = 1, e = 0, h = 3)
    p2.assertResources(m = 15, s = 1, t = 3, p = 2, e = 0, h = 3)
    p2.assertDashMiddle(played = 7, actions = 2, tr = 20, hand = 5)
    p2.assertTags(but = 2, spt = 1, sct = 1, pot = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 0)

    p2.turn {
      doTasks(
          *cardActionTasks("$Factorum", "Action2"),
          "3 Pay<> FROM MC",
          "SearchForCard<TagFilter<Class<BuildingTag>>>",
      )
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "1 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$MarsUniversity FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-0 ProjectCard<Selecting>", // keep card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$EarthOffice"),
          "1 Pay<> FROM MC",
          "$EarthOffice FROM ProjectCard",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$DevelopmentCenter"),
          "5 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$DevelopmentCenter FROM ProjectCard",
      )
      doTasks(
          *standardProjectTasks("PowerPlantProject"),
          "11 Pay<> FROM MC",
          "PROD[Energy]",
          "Energy",
      )
    }

    p2.turn {
      doTasks("Pass")
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )

      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )

      doTasks(
          *playCardTasks("$DeuteriumExport"),
          "11 Pay<> FROM MC",
          "$DeuteriumExport FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$DeuteriumExport"),
          "Floater<$DeuteriumExport>",
      )
      doTasks("Pass")
    }

    // Generation 3
    p1.doTasks("27 MC", "3 Steel", "Energy")
    p2.doTasks("21 MC", "Steel", "Titanium", "3 Heat", "Plant")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    p1.assertProduction(m = 4, s = 3, t = 0, p = 0, e = 1, h = 0)
    p1.assertResources(m = 27, s = 3, t = 0, p = 0, e = 1, h = 1)
    p1.assertDashMiddle(played = 10, actions = 3, tr = 23, hand = 7)
    p1.assertTags(but = 3, spt = 1, sct = 2, pot = 1, eat = 3, vet = 1)
    p1.assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    p2.assertProduction(m = 0, s = 1, t = 1, p = 1, e = 0, h = 3)
    p2.assertResources(m = 21, s = 1, t = 4, p = 3, e = 0, h = 6)
    p2.assertDashMiddle(played = 8, actions = 2, tr = 21, hand = 7)
    p2.assertTags(but = 3, spt = 1, sct = 2, pot = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 0, venus = 2)

    p1.turn {
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$Factorum"),
          "PROD[Energy]",
      )

      doTasks(
          *playCardTasks("$AsteroidCard"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$AsteroidCard FROM ProjectCard",
          "-0 Plant<Anyone>", // no plants removed
          "TemperatureStep",
          "TerraformRating",
          "2 Titanium",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$CorporateStronghold"),
          "3 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$CorporateStronghold FROM ProjectCard",
          "PROD[3 MC]",
          "3 MC",
          "PROD[-Energy]",
          "CityTile<Tharsis_4_6>",
          "Plant",
      )

      doTasks(
          *playCardTasks("$OptimalAerobraking"),
          "7 Pay<> FROM MC",
          "$OptimalAerobraking FROM ProjectCard",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$TransNeptuneProbe"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no mc paid
          "$TransNeptuneProbe FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "6 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DeuteriumExport", "Action2"),
          "-Floater<$DeuteriumExport>",
          "PROD[Energy]",
          "Energy",
      )

      doTasks(
          *playCardTasks("$ImportedGhg"),
          "4 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "7 Heat",
          "3 MC",
      )
    }

    p2.turn {
      doTasks("Pass")
    }

    p1.turn {
      doTasks("Pass")
    }

    // Generation 4
    p1.doTasks("Heat FROM Energy", "30 MC", "3 Steel", "Energy", "Heat")
    p2.doTasks("22 MC", "Steel", "Titanium", "3 Heat", "Plant", "Energy")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    p1.assertProduction(m = 7, s = 3, t = 0, p = 0, e = 1, h = 1)
    p1.assertResources(m = 44, s = 3, t = 0, p = 1, e = 1, h = 10)
    p1.assertDashMiddle(played = 13, actions = 3, tr = 23, hand = 6)
    p1.assertTags(but = 4, spt = 2, sct = 2, pot = 1, eat = 3, vet = 1, cit = 1)
    p1.assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 1 to "CityTile")

    p2.assertProduction(m = 0, s = 1, t = 1, p = 1, e = 1, h = 3)
    p2.assertResources(m = 29, s = 2, t = 1, p = 4, e = 1, h = 9)
    p2.assertDashMiddle(played = 10, actions = 2, tr = 22, hand = 7)
    p2.assertTags(but = 3, spt = 2, sct = 3, pot = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 4, temp = -28, oxygen = 0, oceans = 0, venus = 2)

    p2.turn {
      doTasks(
          *cardActionTasks("$Factorum", "Action2"),
          "3 Pay<> FROM MC",
          "SearchForCard<TagFilter<Class<BuildingTag>>>",
      )
      doTasks(
          *playCardTasks("$AquiferPumping"),
          "2 Pay<Class<Steel>> FROM Steel",
          "14 Pay<> FROM MC",
          "$AquiferPumping FROM ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-0 ProjectCard<Selecting>", // keep card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Tharsis_2_6>",
          "TerraformRating",
          "2 ProjectCard",
          "2 Plant",
      )
      doTasks(
          *playCardTasks("$SearchForLife"),
          "3 Pay<> FROM MC",
          "$SearchForLife FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DeuteriumExport"),
          "Floater<$DeuteriumExport>",
      )
      doTasks(
          *playCardTasks("$TectonicStressPower"),
          "3 Pay<Class<Steel>> FROM Steel",
          "12 Pay<> FROM MC",
          "$TectonicStressPower FROM ProjectCard",
          "PROD[3 Energy]",
          "3 Energy",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<MicrobeTag>>, SearchForLife>", // no reward
          "-ProjectCard<Revealed>",
      )
    }

    p1.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
          "Heat",
      )
    }

    p2.turn {
      doTasks("Pass")
    }

    p1.turn {
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "1 MC FROM ProjectCard",
      )

      doTasks(
          *playCardTasks("$SpinInducingAsteroid"),
          "16 Pay<> FROM MC",
          "$SpinInducingAsteroid FROM ProjectCard",
          "2 VenusStep",
          "2 TerraformRating",
          "ProjectCard",
          "3 MC",
          "3 Heat",
      )

      doTasks("Pass")
    }

    // Generation 5
    p1.doTasks("3 Heat FROM Energy", "34 MC", "3 Steel", "4 Energy", "2 Heat")
    p2.doTasks("Heat FROM Energy", "24 MC", "Steel", "Titanium", "3 Heat", "Plant", "Energy")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    p1.assertProduction(m = 7, s = 3, t = 0, p = 0, e = 4, h = 2)
    p1.assertResources(m = 28, s = 3, t = 0, p = 1, e = 4, h = 11)
    p1.assertDashMiddle(played = 15, actions = 3, tr = 27, hand = 9)
    p1.assertTags(but = 5, spt = 2, sct = 2, pot = 2, eat = 3, vet = 1, cit = 1)
    p1.assertCounts(3 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 1 to "CityTile")

    p2.assertProduction(m = 0, s = 1, t = 1, p = 1, e = 1, h = 3)
    p2.assertResources(m = 15, s = 1, t = 2, p = 7, e = 1, h = 13)
    p2.assertDashMiddle(played = 12, actions = 4, tr = 24, hand = 11)
    p2.assertTags(but = 4, spt = 2, sct = 4, pot = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 5, temp = -24, oxygen = 0, oceans = 1, venus = 8)

    checkSummaryAfterGen4(game)

    p1.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$SmallAsteroid"),
          "10 Pay<> FROM MC",
          "$SmallAsteroid FROM ProjectCard",
          "-2 Plant<Player2>",
          "TemperatureStep",
          "3 MC",
          "TerraformRating",
          "PROD[Heat]",
          "4 Heat",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$Factorum", "Action2"),
          "3 Pay<> FROM MC",
          "SearchForCard<TagFilter<Class<BuildingTag>>>",
      )
      doTasks(
          *playCardTasks("$DirectedImpactors"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$DirectedImpactors FROM ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
    }

    p2.turn {
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "1 MC FROM ProjectCard",
      )
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "1 MC FROM ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DeuteriumExport", "Action2"),
          "-Floater<$DeuteriumExport>",
          "PROD[Energy]",
          "Energy",
      )
      doTasks(
          *playCardTasks("$DomedCrater"),
          "3 Pay<Class<Steel>> FROM Steel",
          "18 Pay<> FROM MC",
          "$DomedCrater FROM ProjectCard",
          "3 Plant",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Tharsis_3_4>",
          "3 MC",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "6 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$FueledGenerators"),
          "1 Pay<> FROM MC",
          "$FueledGenerators FROM ProjectCard",
          "PROD[-MC]",
          "PROD[Energy]",
          "Energy",
      )
    }

    p2.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )

      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "1 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "OceanTile<Tharsis_1_4>",
          "TerraformRating",
          "ProjectCard",
          "2 Plant",
      )
    }

    p1.turn {
      doTasks("Pass")
    }

    p2.turn {
      doTasks("Pass")
    }

    // Generation 6
    p1.doTasks("5 Heat FROM Energy", "38 MC", "3 Steel", "5 Energy", "3 Heat")
    p2.doTasks("Heat FROM Energy", "27 MC", "Steel", "Titanium", "3 Heat", "Plant", "Energy")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-0 ProjectCard<Selecting>", // keep all four
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    p1.assertProduction(m = 9, s = 3, t = 0, p = 0, e = 5, h = 3)
    p1.assertResources(m = 31, s = 3, t = 0, p = 4, e = 5, h = 15)
    p1.assertDashMiddle(played = 18, actions = 3, tr = 29, hand = 11)
    p1.assertTags(but = 7, spt = 2, sct = 2, pot = 3, eat = 3, vet = 1, cit = 2)
    p1.assertCounts(4 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")

    p2.assertProduction(m = 0, s = 1, t = 1, p = 1, e = 1, h = 3)
    p2.assertResources(m = 21, s = 1, t = 1, p = 8, e = 1, h = 9)
    p2.assertDashMiddle(played = 13, actions = 5, tr = 27, hand = 12)
    p2.assertTags(but = 4, spt = 3, sct = 4, pot = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 6, temp = -18, oxygen = 0, oceans = 2, venus = 10)

    p2.turn {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_8_7>",
          "OxygenStep",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$Factorum", "Action2"),
          "3 Pay<> FROM MC",
          "SearchForCard<TagFilter<Class<BuildingTag>>>",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )

      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-0 ProjectCard<Selecting>", // keep card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$PowerPlant"),
          "1 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$PowerPlant FROM ProjectCard",
          "PROD[Energy]",
      )
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Tharsis_1_5>",
          "4 MC",
          "TerraformRating",
          "2 Plant",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$OlympusConference"),
          "3 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$OlympusConference FROM ProjectCard",
          "Science<$OlympusConference>* OR ProjectCard FROM Science<$OlympusConference>",
      )

      doTasks(
          *playCardTasks("$SisterPlanetSupport"),
          "4 Pay<> FROM MC",
          "$SisterPlanetSupport FROM ProjectCard",
          "PROD[3 MC]",
          "3 MC",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$DuskLaserMining"),
          "8 Pay<> FROM MC",
          "$DuskLaserMining FROM ProjectCard",
          "PROD[Titanium]",
          "PROD[-Energy]",
          "5 Titanium",
      )

      doTasks(
          *playCardTasks("$MirandaResort"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no mc paid
          "$MirandaResort FROM ProjectCard",
          "PROD[5 MC]",
          "5 MC",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$Mine"),
          "4 Pay<> FROM MC",
          "$Mine FROM ProjectCard",
          "PROD[Steel]",
      )
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<MicrobeTag>>, SearchForLife>", // no reward
          "-ProjectCard<Revealed>",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$Solarnet"),
          "7 Pay<> FROM MC",
          "$Solarnet FROM ProjectCard",
          "2 ProjectCard",
      )

      doTasks(
          *playCardTasks("$MiningQuota"),
          "5 Pay<> FROM MC",
          "$MiningQuota FROM ProjectCard",
          "PROD[2 Steel]",
          "2 Steel",
      )
    }

    p2.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    p1.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$DeuteriumExport"),
          "Floater<$DeuteriumExport>",
      )
    }

    p2.turn {
      doTasks("Pass")
    }

    p1.turn {
      doTasks(
          *playCardTasks("$LagrangeObservatory"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "6 Pay<> FROM MC",
          "$LagrangeObservatory FROM ProjectCard",
          "ProjectCard FROM Science<$OlympusConference>",
          "ProjectCard",
      )

      doTasks(
          *playCardTasks("$VenusGovernor"),
          "4 Pay<> FROM MC",
          "$VenusGovernor FROM ProjectCard",
          "PROD[2 MC]",
          "2 MC",
      )
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "1 MC FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$Moss"),
          "4 Pay<> FROM MC",
          "$Moss FROM ProjectCard",
          "-Plant",
          "PROD[Plant]",
          "Plant",
      )

      doTasks("Pass")
    }

    // Generation 7
    p1.doTasks("4 Heat FROM Energy", "49 MC", "5 Steel", "4 Energy", "3 Heat", "Titanium", "Plant")
    p2.doTasks("Heat FROM Energy", "31 MC", "2 Steel", "Titanium", "3 Heat", "Plant", "2 Energy")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    p1.assertProduction(m = 19, s = 5, t = 1, p = 1, e = 4, h = 3)
    p1.assertResources(m = 40, s = 7, t = 1, p = 5, e = 4, h = 14)
    p1.assertDashMiddle(played = 27, actions = 3, tr = 30, hand = 10)
    p1.assertTags(but = 9, spt = 5, sct = 4, pot = 3, eat = 5, jot = 1, vet = 4, plt = 1, cit = 2)
    p1.assertCounts(4 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")

    p2.assertProduction(m = 0, s = 2, t = 1, p = 1, e = 2, h = 3)
    p2.assertResources(m = 32, s = 2, t = 1, p = 3, e = 2, h = 5)
    p2.assertDashMiddle(played = 15, actions = 5, tr = 31, hand = 13)
    p2.assertTags(but = 6, spt = 3, sct = 4, pot = 2, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 7, temp = -14, oxygen = 1, oceans = 3, venus = 12)

    p1.turn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Builder8",
      )

      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$InventionContest"),
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTasks(
          *playCardTasks("$QuantumExtractor"),
          "13 Pay<> FROM MC",
          "$QuantumExtractor FROM ProjectCard",
          "Science",
          "PROD[4 Energy]",
          "4 Energy",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$BioPrintingFacility"),
          "2 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$BioPrintingFacility FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "2 Plant",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DeuteriumExport", "Action2"),
          "-Floater<$DeuteriumExport>",
          "PROD[Energy]",
          "Energy",
      )
      doTasks(
          *playCardTasks("$ProjectInspection"),
          "$ProjectInspection FROM ProjectCard",
          "UseAction<$DevelopmentCenter, Action1>",
          "1 Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$Factorum"),
          "PROD[Energy]",
      )

      doTasks(
          *playCardTasks("$PowerSupplyConsortium"),
          "3 Pay<> FROM MC",
          "$PowerSupplyConsortium FROM ProjectCard",
          "PROD[-Energy<Player1>]",
          "PROD[Energy]",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$FloatingHabs"),
          "5 Pay<> FROM MC",
          "$FloatingHabs FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$FloatingHabs"),
          "2 Pay<> FROM MC",
          "Floater<$DeuteriumExport>",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$TitaniumMine"),
          "5 Pay<> FROM MC",
          "$TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
      )
    }

    p1.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )

      doTasks(
          *playCardTasks("$StratosphericBirds"),
          "12 Pay<> FROM MC",
          "$StratosphericBirds FROM ProjectCard",
          "-Floater",
      )
    }

    p2.turn {
      doTasks("Pass")
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$StratosphericBirds"),
          "Animal<$StratosphericBirds>",
      )

      doTasks("Pass")
    }

    // Generation 8
    p1.doTasks("7 Heat FROM Energy", "50 MC", "5 Steel", "8 Energy", "3 Heat", "Titanium", "Plant")
    p2.doTasks("31 MC", "2 Steel", "2 Titanium", "3 Heat", "Plant", "4 Energy")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    p1.assertProduction(m = 19, s = 5, t = 1, p = 1, e = 8, h = 3)
    p1.assertResources(m = 44, s = 12, t = 2, p = 6, e = 8, h = 16)
    p1.assertDashMiddle(played = 31, actions = 5, tr = 31, hand = 10)
    p1.assertTags(
        9,
        spt = 5,
        sct = 5,
        pot = 4,
        eat = 5,
        jot = 1,
        vet = 6,
        plt = 1,
        ant = 1,
        cit = 2,
    )
    p1.assertCounts(5 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")

    p2.assertProduction(m = 0, s = 2, t = 2, p = 1, e = 4, h = 3)
    p2.assertResources(m = 25, s = 2, t = 3, p = 6, e = 4, h = 8)
    p2.assertDashMiddle(played = 20, actions = 6, tr = 31, hand = 11)
    p2.assertTags(but = 8, spt = 3, sct = 4, pot = 3, eat = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 8, temp = -12, oxygen = 1, oceans = 3, venus = 12)

    p2.turn {
      doTasks(
          *playCardTasks("$AdvancedAlloys"),
          "7 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )

      doTasks(
          *playCardTasks("$AiCentral"),
          "2 Pay<Class<Steel>> FROM Steel",
          "13 Pay<> FROM MC",
          "$AiCentral FROM ProjectCard",
          "PROD[-Energy]",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$ExtractorBalloons"),
          "21 Pay<> FROM MC",
          "$ExtractorBalloons FROM ProjectCard",
          "3 Floater<$ExtractorBalloons>",
      )

      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }

    p1.assertCounts(23 to "MC")

    p2.turn {
      doTasks(
          *cardActionTasks("$AiCentral"),
          "2 ProjectCard",
      )

      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
    }

    admin.assertCounts(6 to "VenusStep")

    p1.turn {
      doTasks(
          *playCardTasks("$SulphurExports"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "13 Pay<> FROM MC",
          "$SulphurExports FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "PROD[8 MC]",
          "8 MC",
      )

      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "2 TerraformRating",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )

      doTasks(
          *playCardTasks("$IshtarMining"),
          "3 Pay<> FROM MC",
          "$IshtarMining FROM ProjectCard",
          "PROD[Titanium]",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$MoholeLake"),
          "12 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$MoholeLake FROM ProjectCard",
          "OceanTile<Tharsis_5_5>",
          "5 Plant",
          "TemperatureStep",
          "2 TerraformRating",
      )
      p2.doTasks("2 Plant")

      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Terraformer35",
      )
    }

    p2.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_8_6>",
          "OxygenStep",
          "TerraformRating",
          "ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-0 ProjectCard<Selecting>", // keep card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *cardActionTasks("$DeuteriumExport"),
          "Floater<$DeuteriumExport>",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "2 Plant",
      )
    }

    p1.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_3_5>",
          "OxygenStep",
          "TerraformRating",
      )
    }

    p2.turn {
      doTasks("Pass")
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$StratosphericBirds"),
          "Animal<$StratosphericBirds>",
      )

      doTasks(
          *cardActionTasks("$MoholeLake"),
          "Animal<$StratosphericBirds>",
      )
      doTasks("Pass")
    }

    // Generation 9
    p1.doTasks("7 Heat FROM Energy", "65 MC", "5 Steel", "8 Energy", "3 Heat", "Titanium", "Plant")
    p2.doTasks(
        "2 Heat FROM Energy",
        "34 MC",
        "2 Steel",
        "3 Titanium",
        "3 Heat",
        "Plant",
        "3 Energy",
    )
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    p1.assertProduction(m = 27, s = 5, t = 1, p = 1, e = 8, h = 3)
    p1.assertResources(m = 56, s = 5, t = 1, p = 4, e = 8, h = 18)
    p1.assertDashMiddle(played = 34, actions = 7, tr = 38, hand = 12)
    p1.assertTags(10, 6, sct = 5, pot = 4, eat = 5, jot = 1, vet = 8, plt = 1, ant = 1, cit = 2)
    p1.assertCounts(5 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")

    p2.assertProduction(m = 0, s = 2, t = 3, p = 1, e = 3, h = 3)
    p2.assertResources(m = 28, s = 2, t = 5, p = 3, e = 3, h = 5)
    p2.assertDashMiddle(played = 23, actions = 7, tr = 34, hand = 13)
    p2.assertTags(but = 9, spt = 3, sct = 6, pot = 3, eat = 1, jot = 1, vet = 1, plt = 1, mit = 1)
    p2.assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 9, temp = -6, oxygen = 3, oceans = 4, venus = 18)

    p1.turn {
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-0 ProjectCard<Selecting>", // keep card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$DeimosDownPromo"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "9 Pay<> FROM MC",
          "$DeimosDownPromo FROM ProjectCard",
          "3 TemperatureStep",
          "OceanTile<Tharsis_6_7>",
          "DeimosDownPromo_SpecialTile<Tharsis_2_5>",
          "-4 Plant<Player1>",
          "4 Steel",
          "3 Plant",
          "4 TerraformRating",
          "6 MC",
      )
      doTasks(
          *cardActionTasks("$AiCentral"),
          "2 ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // no mc paid
          "OceanTile<Tharsis_5_6>",
          "4 MC",
          "TerraformRating",
          "4 Plant",
      )
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_9_7>",
          "OxygenStep",
          "TerraformRating",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$RegoPlastics"),
          "Ok", // no steel paid
          "10 Pay<> FROM MC",
          "$RegoPlastics FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$SfMemorial"),
          "2 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$SfMemorial FROM ProjectCard",
          "ProjectCard",
      )
    }

    p2.turn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Gardener",
      )
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "6 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$FloatingHabs"),
          "2 Pay<> FROM MC",
          "Floater<$ExtractorBalloons>",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$EcologicalZone"),
          "10 Pay<> FROM MC",
          "$EcologicalZone FROM ProjectCard",
          "EcologicalZone_SpecialTile<Tharsis_4_5>",
          "2 Animal<$EcologicalZone>",
          "4 MC",
          "2 Plant",
      )

      doTasks(
          *playCardTasks("$Harvest"),
          "2 Pay<> FROM MC",
          "$Harvest FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "12 MC",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$NoctisFarming"),
          "3 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$NoctisFarming FROM ProjectCard",
          "2 Plant",
          "PROD[MC]",
          "MC",
      )

      doTasks(
          *cardActionTasks("$DeuteriumExport", "Action2"),
          "-Floater<$DeuteriumExport>",
          "PROD[Energy]",
          "Energy",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "Animal<$EcologicalZone>",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$MoholeLake"),
          "Animal<$StratosphericBirds>",
      )

      doTasks(
          *cardActionTasks("$StratosphericBirds"),
          "Animal<$StratosphericBirds>",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$Factorum", "Action2"),
          "3 Pay<> FROM MC",
          "SearchForCard<TagFilter<Class<BuildingTag>>>",
      )

      doTasks(
          *playCardTasks("$NaturalPreserve"),
          "2 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$NaturalPreserve FROM ProjectCard",
          "-ProjectCard",
          "NaturalPreserve_SpecialTile<Tharsis_3_1>",
          "2 ProjectCard",
          "PROD[MC]",
      )
    }

    p1.turn {
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "3 MC FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$WaterToVenus"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<> FROM MC",
          "$WaterToVenus FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "3 MC",
          "3 Heat",
      )
    }

    p2.turn {
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "2 MC FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$KelpFarming"),
          "15 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "2 Plant",
          "PROD[2 MC]",
          "PROD[3 Plant]",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$Trees"),
          "13 Pay<> FROM MC",
          "$Trees FROM ProjectCard",
          "PROD[3 Plant]",
          "4 Plant",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Banker")
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<MicrobeTag>>, SearchForLife>", // no reward
          "-ProjectCard<Revealed>",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$VenusianInsects"),
          "5 Pay<> FROM MC",
          "$VenusianInsects FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$VenusianInsects"),
          "Microbe<$VenusianInsects>",
      )
    }

    p2.turn {
      doTasks("Pass")
    }

    p1.turn {
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Venuphile")
      doTasks("Pass")
    }

    // Generation 10
    p1.doTasks(
        "8 Heat FROM Energy",
        "70 MC",
        "5 Steel",
        "9 Energy",
        "3 Heat",
        "Titanium",
        "4 Plant",
    )
    p2.doTasks(
        "Heat FROM Energy",
        "44 MC",
        "2 Steel",
        "3 Titanium",
        "3 Heat",
        "4 Plant",
        "3 Energy",
    )
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    p1.assertProduction(m = 28, s = 5, t = 1, p = 4, e = 9, h = 3)
    p1.assertResources(m = 66, s = 5, t = 1, p = 10, e = 9, h = 16)
    p1.assertDashMiddle(played = 40, actions = 8, tr = 42, hand = 8)
    p1.assertTags(13, 6, 5, pot = 4, eat = 5, jot = 1, vet = 9, plt = 3, mit = 1, ant = 1, cit = 2)
    p1.assertCounts(6 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")

    p2.assertProduction(m = 3, s = 2, t = 3, p = 4, e = 3, h = 3)
    p2.assertResources(m = 36, s = 3, t = 3, p = 10, e = 3, h = 9)
    p2.assertDashMiddle(played = 28, actions = 7, tr = 41, hand = 13)
    p2.assertTags(10, 3, sct = 7, pot = 3, eat = 1, jot = 1, vet = 1, plt = 3, mit = 1, ant = 1)
    p2.assertCounts(4 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 10, temp = 4, oxygen = 4, oceans = 6, venus = 24)

    p2.turn {
      doTasks(
          *playCardTasks("$HiredRaiders"),
          "$HiredRaiders FROM ProjectCard",
          "2 Steel FROM Steel<Player1>",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    p1.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Tharsis_7_6>",
          "PROD[MC]",
          "MC",
      )
    }

    p2.turn {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_9_6>",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )

      doTasks(
          *cardActionTasks("$AiCentral"),
          "2 ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$MercurianAlloys"),
          "Ok", // no titanium paid
          "1 Pay<> FROM MC",
          "$MercurianAlloys FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$AerialMappers"),
          "9 Pay<> FROM MC",
          "$AerialMappers FROM ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$LavaTubeSettlement"),
          "3 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$LavaTubeSettlement FROM ProjectCard",
          "CityTile<Tharsis_2_2>",
          "Steel",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "2 MC",
      )

      doTasks(
          *playCardTasks("$UrbanizedArea"),
          "1 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$UrbanizedArea FROM ProjectCard",
          "CityTile<Tharsis_2_3>",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "2 MC",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$Atmoscoop"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$Atmoscoop FROM ProjectCard",
          "2 VenusStep",
          "2 Floater<$AerialMappers>",
          "2 TerraformRating",
      )

      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$NitrogenRichAsteroid"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "26 Pay<> FROM MC",
          "$NitrogenRichAsteroid FROM ProjectCard",
          "PROD[4 Plant]",
          "2 TerraformRating",
          "0 TemperatureStep", // temperature maxed
          "3 MC",
          "3 Heat",
          "4 Plant",
      )

      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_3_3>",
          "OxygenStep",
          "TerraformRating",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "Animal<$EcologicalZone>",
      )
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "6 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$VenusianInsects"),
          "Microbe<$VenusianInsects>",
      )
      doTasks(
          *cardActionTasks("$StratosphericBirds"),
          "Animal<$StratosphericBirds>",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )

      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "OceanTile<Tharsis_9_9>",
          "2 Titanium",
          "TerraformRating",
          "2 Plant",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$PowerInfrastructure"),
          "4 Pay<> FROM MC",
          "$PowerInfrastructure FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$PowerInfrastructure"))
      selectTask(p1.tasks.extract { it }.single().bindXTo(8))
      doTask("8 MC")
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$Factorum", "Action2"),
          "3 Pay<> FROM MC",
          "SearchForCard<TagFilter<Class<BuildingTag>>>",
      )
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "2 MC FROM ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DeuteriumExport"),
          "Floater<$DeuteriumExport>",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons"),
          "Floater<$ExtractorBalloons>",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$Bushes"),
          "8 Pay<> FROM MC",
          "$Bushes FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "2 Plant",
          "PROD[2 Plant]",
      )
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "1 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "PROD[-Energy<Player1>]",
          "PROD[Energy]",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$FloatingHabs"),
          "2 Pay<> FROM MC",
          "Floater<$FloatingHabs>",
      )
      doTasks(
          *cardActionTasks("$MoholeLake"),
          "Animal<$StratosphericBirds>",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$NuclearPower"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // no mc paid
          "$NuclearPower FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[3 Energy]",
      )
      doTasks(
          *playCardTasks("$BiomassCombustors"),
          "1 Pay<Class<Steel>> FROM Steel",
          "Ok", // no mc paid
          "$BiomassCombustors FROM ProjectCard",
          "PROD[-Plant<Player1>]",
          "PROD[2 Energy]",
      )
    }

    p1.turn {
      doTasks("Pass")
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<MicrobeTag>>, SearchForLife>", // no reward
          "-ProjectCard<Revealed>",
      )
      doTasks("Pass")
    }
    // Generation 11
    p1.doTasks("79 MC", "5 Steel", "6 Energy", "3 Heat", "Titanium", "7 Plant")
    p2.doTasks(
        "Heat FROM Energy",
        "48 MC",
        "2 Steel",
        "3 Titanium",
        "3 Heat",
        "6 Plant",
        "9 Energy",
    )
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    p1.turn {
      doTasks(
          *playCardTasks("$ImportedNitrogen"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "15 Pay<> FROM MC",
          "$ImportedNitrogen FROM ProjectCard",
          "3 Microbe<$VenusianInsects>",
          "2 Animal<$StratosphericBirds>",
          "TerraformRating",
          "4 Plant",
          "3 MC",
          "3 Heat",
      )
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$AiCentral"),
          "2 ProjectCard",
      )
      doTasks(
          *cardActionTasks("$Factorum", "Action2"),
          "3 Pay<> FROM MC",
          "SearchForCard<TagFilter<Class<BuildingTag>>>",
      )
    }

    p1.turn {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_2_4>",
          "2 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$MediaGroup"),
          "4 Pay<> FROM MC",
          "$MediaGroup FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$MiningExpedition"),
          "10 Pay<> FROM MC",
          "$MiningExpedition FROM ProjectCard",
          "-2 Plant<Player1>",
          "OxygenStep",
          "TerraformRating",
          "2 Steel",
          "3 MC",
          "0 TemperatureStep", // temperature maxed
      )
    }

    p1.turn {
      doTasks(*cardActionTasks("$PowerInfrastructure"))
      selectTask(p1.tasks.extract { it }.single().bindXTo(5))
      doTask("5 MC")
      doTasks(
          *cardActionTasks("$ExtractorBalloons"),
          "Floater<$ExtractorBalloons>",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "Animal<$EcologicalZone>",
      )
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "OceanTile<Tharsis_5_4>",
          "2 MC",
          "TerraformRating",
          "4 Plant",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$BusinessNetwork"),
          "1 Pay<> FROM MC",
          "$BusinessNetwork FROM ProjectCard",
          "PROD[-MC]",
      )

      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-0 ProjectCard<Selecting>", // keep card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    p2.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Tharsis_8_5>",
          "PROD[MC]",
      )
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_8_4>",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$DeuteriumExport", "Action2"),
          "-Floater<$DeuteriumExport>",
          "PROD[Energy]",
          "Energy",
      )

      doTasks(
          *cardActionTasks("$FloatingHabs"),
          "2 Pay<> FROM MC",
          "Floater<$FloatingHabs>",
      )
    }

    p2.turn {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_9_5>",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )

      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$StratosphericBirds"),
          "Animal<$StratosphericBirds>",
      )
      doTasks(
          *cardActionTasks("$MoholeLake"),
          "Animal<$StratosphericBirds>",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$MagneticFieldGeneratorsPromo"),
          "6 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$MagneticFieldGeneratorsPromo FROM ProjectCard",
          "MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_6_6>",
          "PROD[-4 Energy]",
          "PROD[2 Plant]",
          "3 TerraformRating",
          "6 MC",
          "Plant",
      )

      doTasks(
          *playCardTasks("$TowingAComet"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$TowingAComet FROM ProjectCard",
          "OceanTile<Tharsis_6_8>",
          "5 Plant",
          "OxygenStep",
          "2 TerraformRating",
          "5 MC",
      )
    }

    p1.turn {
      doTasks(
          *cardActionTasks("$VenusianInsects"),
          "Microbe<$VenusianInsects>",
      )
      doTasks(
          *playCardTasks("$StandardTechnology"),
          "6 Pay<> FROM MC",
          "$StandardTechnology FROM ProjectCard",
          "ProjectCard FROM Science",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$AtalantaPlanitiaLab"),
          "8 Pay<> FROM MC",
          "$AtalantaPlanitiaLab FROM ProjectCard",
          "2 ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "3 MC FROM ProjectCard",
      )
    }
    admin.assertCounts(9 to "OceanTile")
    p1.turn {
      doTasks(
          *playCardTasks("$LargeConvoy"),
          "31 Pay<> FROM MC",
          "$LargeConvoy FROM ProjectCard",
          "4 Animal<$StratosphericBirds>",
          "Ok", // oceans full
          "2 ProjectCard",
          "3 MC",
          "3 Heat",
      )

      doTasks(
          *playCardTasks("$WaterSplittingPlant"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // no mc paid
          "$WaterSplittingPlant FROM ProjectCard",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$RobotPollinators"),
          "7 Pay<> FROM MC",
          "$RobotPollinators FROM ProjectCard",
          "PROD[Plant]",
          "4 Plant",
      )

      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_7_4>",
          "OxygenStep",
          "TerraformRating",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$MediaArchives"),
          "5 Pay<> FROM MC",
          "$MediaArchives FROM ProjectCard",
          "16 MC",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_5_7>",
          "9 MC",
          "2 Plant",
          "OxygenStep",
          "TerraformRating",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$Greenhouses"),
          "4 Pay<> FROM MC",
          "$Greenhouses FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "6 Plant",
      )

      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_9_8>",
          "2 MC",
          "OxygenStep",
          "TerraformRating",
      )
    }

    p1.turn {
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Thermalist")
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Tharsis_4_4>",
          "Plant",
          "4 MC",
          "0 OxygenStep", // oxygen maxed
      )
    }

    p2.turn {
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "3 MC FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$Penguins"),
          "5 Pay<> FROM MC",
          "$Penguins FROM ProjectCard",
          "Animal<$EcologicalZone>",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$AdvancedEcosystems"),
          "11 Pay<> FROM MC",
          "$AdvancedEcosystems FROM ProjectCard",
      )
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "4 MC FROM ProjectCard",
      )
    }

    p2.turn {
      doTasks(
          *cardActionTasks("$Penguins"),
          "Animal<$Penguins>",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$GeneRepair"),
          "12 Pay<> FROM MC",
          "$GeneRepair FROM ProjectCard",
          "PROD[2 MC]",
          "Science",
          "2 MC",
      )
    }

    p2.turn {
      doTasks("Pass")
    }

    p1.turn {
      doTasks("Pass")
    }

    p1.doTasks("Heat FROM Energy", "83 MC")
    p2.doTasks("7 Heat FROM Energy", "60 MC")
    p1.doTasks("5 Steel")
    p2.doTasks("2 Steel", "3 Titanium", "3 Heat", "9 Plant", "5 Energy")
    p1.doTasks("7 Energy", "3 Heat", "Titanium", "7 Plant")

    // Final greenery placement
    p1.doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
    p1.doTasks(
        "GreeneryTile<Tharsis_6_5>",
        "Plant",
        "4 MC",
    )
    p1.selectTask("PassBaton") // no more greenery
    p2.doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
    p2.doTasks(
        "GreeneryTile<Tharsis_8_8>",
        "2 MC",
    )
    p2.selectTask("PassBaton") // no more greenery

    p1.assertCounts(126 to "VictoryPoint", 1 to "Victory")
    p2.assertCounts(92 to "VictoryPoint", 0 to "Victory")
    game.isIdle() shouldBe true

    val summ = Summarizer(game)
    summ.net("$Manutech", "Resource") shouldBe 104
    summ.net("Production<Player2>", "Resource<Player2>") shouldBe 187

    summ.net("$EarthOffice", "Owed") shouldBe -24
    // Random automatic order may attribute fewer saturated removals here; see TESTING.md.
    summ.net(
        "GrantedResourceValue<Player2, Class<Metal>, $AdvancedAlloys<Player2>>",
        "Owed",
    ) shouldBe -31
    summ.net("$EarthCatapult<Player2>", "Owed") shouldBe -55
    summ.net("$QuantumExtractor", "Owed") shouldBe -10 // oof

    summ.net("$AquiferPumping", "OceanTile") shouldBe 6
    summ.net("$ArcticAlgae", "Plant") shouldBe 19
    summ.net("$OptimalAerobraking", "Resource") shouldBe 42
    summ.net("$SearchForLife", "Science") shouldBe 0

    summ.net("TerraformRating<Player1>", "MC<Player1>") shouldBe 361
    summ.net("TerraformRating<Player1>", "MC") shouldBe 361
    summ.net("TerraformRating", "MC<Player1>") shouldBe 361
    summ.net("TerraformRating<Player2>", "MC<Player2>") shouldBe 356
    summ.net("TerraformRating<Player2>", "MC") shouldBe 356
    summ.net("TerraformRating", "MC<Player2>") shouldBe 356

    summ.net("TerraformRating", "MC") shouldBe 717

    summ.net("TerraformRating<Player1>", "MC<Player2>") shouldBe 0
    summ.net("TerraformRating<Player2>", "MC<Player1>") shouldBe 0
  }

  private fun checkSummaryAfterGen4(game: World) {
    val summer = Summarizer(game)

    summer.net("$ArcticAlgae", "Plant") shouldBe 3

    summer.net("$DeuteriumExport", "Floater") shouldBe 1
    summer.net("$DeuteriumExport", "Production<Class<Energy>>") shouldBe 1

    summer.net("$EarthOffice", "Owed<Player1>") shouldBe -6

    summer.net("$Manutech", "MC<Player1>") shouldBe 43

    summer.net("TerraformRating", "MC<Player2>") shouldBe 87
    summer.net("TerraformRating<Player2>", "MC") shouldBe 87
    summer.net("TerraformRating<Player2>", "MC<Player2>") shouldBe 87
    summer.net("TerraformRating", "MC") shouldBe 183

    summer.net("GlobalParameter", "TerraformRating<Player1>") shouldBe 4
    summer.net("GlobalParameter", "TerraformRating<Player2>") shouldBe 4
  }
}
