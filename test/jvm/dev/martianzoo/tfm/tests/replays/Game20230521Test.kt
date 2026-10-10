package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.World
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Explicit player-task replay; Admin keeps its normal autoexecution.
// Source: _local/replays/Game20230521/game-replay.rego (game gf386a4cd5de1).
internal class Game20230521Test : AbstractFullGameTest() {

  override val config =
      GameConfig(
          """
          VenusNextExpansion, PreludeExpansion, PromoCardPack
          -WorldGovernmentRule
          """,
          "Player1",
          "Player2",
      )

  @Test
  internal fun game20230521() {
    listOf(p1, p2).forEach { it.autoExecPolicy = NONE }
    TfmWorkflow.Automatic(agents).launch()
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

    p1.doTasks(
        "PlayCard<Class<CorporationCard>, Class<$Manutech>>",
        "$Manutech FROM CorporationCard",
        "35 MC",
        "PROD[Steel]",
        "Steel",
        "-15 MC",
        "5 ProjectCard<Hand FROM Selecting>",
    )

    p2.doTasks(
        "PlayCard<Class<CorporationCard>, Class<$Factorum>>",
        "$Factorum FROM CorporationCard",
        "37 MC",
        "PROD[Steel]",
        "-12 MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    p1.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$NewPartner>>",
        "$NewPartner FROM PreludeCard",
        "PROD[MC]",
        "2 PreludeCard<Selecting>",
        "MC",
        "-PreludeCard<Selecting>",
        "PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>",
        "$UnmiContractor FROM PreludeCard<Selecting>",
        "3 TerraformRating",
        "ProjectCard",
    )

    p1.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$AlliedBank>>",
        "$AlliedBank FROM PreludeCard",
        "PROD[4 MC]",
        "7 MC",
    )

    p2.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$AcquiredSpaceAgency>>",
        "$AcquiredSpaceAgency FROM PreludeCard",
        "6 Titanium",
        "2 SearchForCard<TagFilter<Class<SpaceTag>>>",
    )
    p2.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$IoResearchOutpost>>",
        "$IoResearchOutpost FROM PreludeCard",
        "PROD[Titanium]",
        "ProjectCard",
    )

    p1.doTasks(
        "PlayProject<Class<$InventorsGuild>>",
        "-9 MC",
        "$InventorsGuild FROM ProjectCard",
    )

    p1.doTasks("Ok") // end turn

    p2.doTasks(
        "PlayProject<Class<$ArcticAlgae>>",
        "-12 MC",
        "$ArcticAlgae FROM ProjectCard",
        "Plant",
    )

    p2.doTasks("Ok") // end turn

    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "Ok", // keep card
        "-3 MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    p1.doTasks("Ok") // end turn

    p2.doTasks(
        "UseCardAction<$Factorum, Action1>",
        "PROD[Energy]",
    )

    p2.doTasks("Ok") // end turn

    p1.doTasks(
        "UseStandardProject<PowerPlantProject>",
        "-11 MC",
        "PROD[Energy]",
        "Energy",
    )
    p1.doTasks(
        "PlayProject<Class<$BuildingIndustries>>",
        "-1 Steel",
        "-4 MC",
        "$BuildingIndustries FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[2 Steel]",
        "2 Steel",
    )

    p2.doTasks(
        "PlayProject<Class<$RotatorImpacts>>",
        "-2 Titanium",
        "Ok", // no mc paid
        "$RotatorImpacts FROM ProjectCard",
    )
    p2.doTasks(
        "UseCardAction<$RotatorImpacts, Action1>",
        "-2 Titanium",
        "Ok", // no mc paid
        "Asteroid<$RotatorImpacts>",
    )

    p1.doTasks("Pass")

    p2.doTasks(
        "PlayProject<Class<$CarbonateProcessing>>",
        "-6 MC",
        "Ok", // no steel paid
        "$CarbonateProcessing FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[3 Heat]",
    )
    p2.doTasks(
        "PlayProject<Class<$Archaebacteria>>",
        "-6 MC",
        "$Archaebacteria FROM ProjectCard",
        "PROD[Plant]",
    )
    p2.doTasks("Pass")

    // Generation 2
    p1.doTasks("Heat FROM Energy", "28 MC", "3 Steel")
    p2.doTasks("20 MC", "Steel", "Titanium", "3 Heat", "Plant")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")

    p1.assertProduction(m = 5, s = 3, t = 0, p = 0, e = 0, h = 0)
    p1.assertResources(m = 23, s = 5, t = 0, p = 0, e = 0, h = 1)
    p1.assertDashMiddle(played = 6, actions = 1, vp = 23, tr = 23, hand = 7)
    p1.assertTags(but = 2, sct = 1, eat = 2)
    p1.assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    p2.assertProduction(m = 0, s = 1, t = 1, p = 1, e = 0, h = 3)
    p2.assertResources(m = 15, s = 1, t = 3, p = 2, e = 0, h = 3)
    p2.assertDashMiddle(played = 7, actions = 2, vp = 20, tr = 20, hand = 5)
    p2.assertTags(but = 2, spt = 1, sct = 1, pot = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 0)

    p2.doTasks(
        "UseCardAction<$Factorum, Action2>",
        "-3 MC",
        "SearchForCard<TagFilter<Class<BuildingTag>>>",
    )
    p2.doTasks(
        "PlayProject<Class<$MarsUniversity>>",
        "-1 Steel",
        "-6 MC",
        "$MarsUniversity FROM ProjectCard",
        "-ProjectCard",
        "ProjectCard",
    )

    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "Ok", // keep card
        "-3 MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    p1.doTasks(
        "PlayProject<Class<$EarthOffice>>",
        "-1 MC",
        "$EarthOffice FROM ProjectCard",
    )

    p2.doTasks(
        "UseCardAction<$RotatorImpacts, Action2>",
        "-Asteroid<$RotatorImpacts>",
        "VenusStep",
        "TerraformRating",
    )

    p2.doTasks("Ok") // end turn

    p1.doTasks(
        "PlayProject<Class<$DevelopmentCenter>>",
        "-5 Steel",
        "-1 MC",
        "$DevelopmentCenter FROM ProjectCard",
    )
    p1.doTasks(
        "UseStandardProject<PowerPlantProject>",
        "-11 MC",
        "PROD[Energy]",
        "Energy",
    )

    p2.doTasks("Pass")

    p1.doTasks(
        "UseCardAction<$DevelopmentCenter, Action1>",
        "-Energy",
        "ProjectCard",
    )

    p1.doTasks(
        "PlayProject<Class<$InvestmentLoan>>",
        "$InvestmentLoan FROM ProjectCard",
        "PROD[-MC]",
        "10 MC",
    )

    p1.doTasks(
        "PlayProject<Class<$DeuteriumExport>>",
        "-11 MC",
        "Ok", // no titanium paid
        "$DeuteriumExport FROM ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$DeuteriumExport, Action1>",
        "Floater<$DeuteriumExport>",
    )
    p1.doTasks("Pass")

    // Generation 3
    p1.doTasks("27 MC", "3 Steel", "Energy")
    p2.doTasks("21 MC", "Steel", "Titanium", "3 Heat", "Plant")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")

    p1.assertProduction(m = 4, s = 3, t = 0, p = 0, e = 1, h = 0)
    p1.assertResources(m = 27, s = 3, t = 0, p = 0, e = 1, h = 1)
    p1.assertDashMiddle(played = 10, actions = 3, vp = 23, tr = 23, hand = 7)
    p1.assertTags(but = 3, spt = 1, sct = 2, pot = 1, eat = 3, vet = 1)
    p1.assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    p2.assertProduction(m = 0, s = 1, t = 1, p = 1, e = 0, h = 3)
    p2.assertResources(m = 21, s = 1, t = 4, p = 3, e = 0, h = 6)
    p2.assertDashMiddle(played = 8, actions = 2, vp = 22, tr = 21, hand = 7)
    p2.assertTags(but = 3, spt = 1, sct = 2, pot = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 0, venus = 2)

    p1.doTasks(
        "UseCardAction<$DevelopmentCenter, Action1>",
        "-Energy",
        "ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "-1 ProjectCard<Selecting>",
    )

    p2.doTasks(
        "UseCardAction<$Factorum, Action1>",
        "PROD[Energy]",
    )

    p2.doTasks(
        "PlayProject<Class<$AsteroidCard>>",
        "-4 Titanium",
        "-2 MC",
        "$AsteroidCard FROM ProjectCard",
        "Ok", // no plants removed
        "TemperatureStep",
        "TerraformRating",
        "2 Titanium",
    )

    p1.doTasks(
        "PlayProject<Class<$CorporateStronghold>>",
        "-3 Steel",
        "-5 MC",
        "$CorporateStronghold FROM ProjectCard",
        "PROD[3 MC]",
        "3 MC",
        "PROD[-Energy]",
        "CityTile<Tharsis_4_6>",
        "Plant",
    )

    p1.doTasks(
        "PlayProject<Class<$OptimalAerobraking>>",
        "-7 MC",
        "Ok", // no titanium paid
        "$OptimalAerobraking FROM ProjectCard",
    )

    p2.doTasks(
        "PlayProject<Class<$TransNeptuneProbe>>",
        "-2 Titanium",
        "Ok", // no mc paid
        "$TransNeptuneProbe FROM ProjectCard",
        "-ProjectCard",
        "ProjectCard",
    )
    p2.doTasks(
        "UseCardAction<$RotatorImpacts, Action1>",
        "-6 MC",
        "Ok", // no titanium paid
        "Asteroid<$RotatorImpacts>",
    )

    p1.doTasks(
        "UseCardAction<$DeuteriumExport, Action2>",
        "-Floater<$DeuteriumExport>",
        "PROD[Energy]",
        "Energy",
    )

    p1.doTasks(
        "PlayProject<Class<$ImportedGhg>>",
        "-4 MC",
        "Ok", // no titanium paid
        "$ImportedGhg FROM ProjectCard",
        "PROD[Heat]",
        "7 Heat",
        "3 MC",
    )

    p2.doTasks("Pass")

    p1.doTasks("Pass")

    // Generation 4
    p1.doTasks("Heat FROM Energy", "30 MC", "3 Steel", "Energy", "Heat")
    p2.doTasks("22 MC", "Steel", "Titanium", "3 Heat", "Plant", "Energy")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("-3 ProjectCard<Selecting>", "-3 MC", "ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")

    p1.assertProduction(m = 7, s = 3, t = 0, p = 0, e = 1, h = 1)
    p1.assertResources(m = 44, s = 3, t = 0, p = 1, e = 1, h = 10)
    p1.assertDashMiddle(played = 13, actions = 3, vp = 21, tr = 23, hand = 6)
    p1.assertTags(but = 4, spt = 2, sct = 2, pot = 1, eat = 3, vet = 1, cit = 1)
    p1.assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 1 to "CityTile")

    p2.assertProduction(m = 0, s = 1, t = 1, p = 1, e = 1, h = 3)
    p2.assertResources(m = 29, s = 2, t = 1, p = 4, e = 1, h = 9)
    p2.assertDashMiddle(played = 10, actions = 2, vp = 24, tr = 22, hand = 7)
    p2.assertTags(but = 3, spt = 2, sct = 3, pot = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 4, temp = -28, oxygen = 0, oceans = 0, venus = 2)

    p2.doTasks(
        "UseCardAction<$Factorum, Action2>",
        "-3 MC",
        "SearchForCard<TagFilter<Class<BuildingTag>>>",
    )
    p2.doTasks(
        "PlayProject<Class<$AquiferPumping>>",
        "-2 Steel",
        "-14 MC",
        "$AquiferPumping FROM ProjectCard",
    )

    p1.doTasks(
        "UseCardAction<$DevelopmentCenter, Action1>",
        "-Energy",
        "ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "Ok", // keep card
        "-3 MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    p2.doTasks(
        "UseCardAction<$AquiferPumping, Action1>",
        "-8 MC",
        "Ok", // no steel paid
        "OceanTile<Tharsis_2_6>",
        "TerraformRating",
        "2 ProjectCard",
        "2 Plant",
    )
    p2.doTasks(
        "PlayProject<Class<$SearchForLife>>",
        "-3 MC",
        "$SearchForLife FROM ProjectCard",
        "-ProjectCard",
        "ProjectCard",
    )

    p1.doTasks(
        "UseCardAction<$DeuteriumExport, Action1>",
        "Floater<$DeuteriumExport>",
    )
    p1.doTasks(
        "PlayProject<Class<$TectonicStressPower>>",
        "-3 Steel",
        "-12 MC",
        "$TectonicStressPower FROM ProjectCard",
        "PROD[3 Energy]",
        "3 Energy",
    )

    p2.doTasks(
        "UseCardAction<$RotatorImpacts, Action2>",
        "-Asteroid<$RotatorImpacts>",
        "VenusStep",
        "TerraformRating",
    )
    p2.doTasks(
        "UseCardAction<$SearchForLife, Action1>",
        "-MC",
        "ProjectCard<Revealed>",
        "Ok", // no reward
        "-ProjectCard<Revealed>",
    )

    p1.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    p1.doTasks(
        "UseStandardProject<AsteroidProject>",
        "-14 MC",
        "TemperatureStep",
        "TerraformRating",
        "PROD[Heat]",
        "Heat",
    )

    p2.doTasks("Pass")

    p1.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "1 MC FROM ProjectCard",
    )

    p1.doTasks(
        "PlayProject<Class<$SpinInducingAsteroid>>",
        "-16 MC",
        "Ok", // no titanium paid
        "$SpinInducingAsteroid FROM ProjectCard",
        "2 VenusStep",
        "2 TerraformRating",
        "ProjectCard",
        "3 MC",
        "3 Heat",
    )

    p1.doTasks("Pass")

    // Generation 5
    p1.doTasks("3 Heat FROM Energy", "34 MC", "3 Steel", "4 Energy", "2 Heat")
    p2.doTasks("Heat FROM Energy", "24 MC", "Steel", "Titanium", "3 Heat", "Plant", "Energy")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("-1 ProjectCard<Selecting>", "-9 MC", "3 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-1 ProjectCard<Selecting>", "-9 MC", "3 ProjectCard<Hand FROM Selecting>")
    p1.assertProduction(m = 7, s = 3, t = 0, p = 0, e = 4, h = 2)
    p1.assertResources(m = 28, s = 3, t = 0, p = 1, e = 4, h = 11)
    p1.assertDashMiddle(played = 15, actions = 3, vp = 26, tr = 27, hand = 9)
    p1.assertTags(but = 5, spt = 2, sct = 2, pot = 2, eat = 3, vet = 1, cit = 1)
    p1.assertCounts(3 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 1 to "CityTile")

    p2.assertProduction(m = 0, s = 1, t = 1, p = 1, e = 1, h = 3)
    p2.assertResources(m = 15, s = 1, t = 2, p = 7, e = 1, h = 13)
    p2.assertDashMiddle(played = 12, actions = 4, vp = 26, tr = 24, hand = 11)
    p2.assertTags(but = 4, spt = 2, sct = 4, pot = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 5, temp = -24, oxygen = 0, oceans = 1, venus = 8)

    checkSummaryAfterGen4(game)

    p1.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    p1.doTasks(
        "PlayProject<Class<$SmallAsteroid>>",
        "-10 MC",
        "Ok", // no titanium paid
        "$SmallAsteroid FROM ProjectCard",
        "-2 Plant<Player2>",
        "TemperatureStep",
        "3 MC",
        "TerraformRating",
        "PROD[Heat]",
        "4 Heat",
    )

    p2.doTasks(
        "UseCardAction<$Factorum, Action2>",
        "-3 MC",
        "SearchForCard<TagFilter<Class<BuildingTag>>>",
    )
    p2.doTasks(
        "PlayProject<Class<$DirectedImpactors>>",
        "-2 Titanium",
        "-2 MC",
        "$DirectedImpactors FROM ProjectCard",
    )

    p1.doTasks(
        "UseCardAction<$DevelopmentCenter, Action1>",
        "-Energy",
        "ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "-1 ProjectCard<Selecting>",
    )

    p2.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "1 MC FROM ProjectCard",
    )
    p2.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "1 MC FROM ProjectCard",
    )

    p1.doTasks(
        "UseCardAction<$DeuteriumExport, Action2>",
        "-Floater<$DeuteriumExport>",
        "PROD[Energy]",
        "Energy",
    )
    p1.doTasks(
        "PlayProject<Class<$DomedCrater>>",
        "-3 Steel",
        "-18 MC",
        "$DomedCrater FROM ProjectCard",
        "3 Plant",
        "PROD[-Energy]",
        "PROD[3 MC]",
        "CityTile<Tharsis_3_4>",
        "3 MC",
    )

    p2.doTasks(
        "UseCardAction<$DirectedImpactors, Action1>",
        "-6 MC",
        "Ok", // no titanium paid
        "Asteroid<$RotatorImpacts>",
    )
    p2.doTasks(
        "UseCardAction<$RotatorImpacts, Action2>",
        "-Asteroid<$RotatorImpacts>",
        "VenusStep",
        "TerraformRating",
    )

    p1.doTasks(
        "PlayProject<Class<$FueledGenerators>>",
        "-1 MC",
        "Ok", // no steel paid
        "$FueledGenerators FROM ProjectCard",
        "PROD[-MC]",
        "PROD[Energy]",
        "Energy",
    )

    p1.doTasks("Ok") // end turn

    p2.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )

    p2.doTasks(
        "UseCardAction<$AquiferPumping, Action1>",
        "-1 Steel",
        "-6 MC",
        "OceanTile<Tharsis_1_4>",
        "TerraformRating",
        "ProjectCard",
        "2 Plant",
    )

    p1.doTasks("Pass")

    p2.doTasks("Pass")

    // Generation 6
    p1.doTasks("5 Heat FROM Energy", "38 MC", "3 Steel", "5 Energy", "3 Heat")
    p2.doTasks("Heat FROM Energy", "27 MC", "Steel", "Titanium", "3 Heat", "Plant", "Energy")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "Ok", // keep all four
        "-12 MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")

    p1.assertProduction(m = 9, s = 3, t = 0, p = 0, e = 5, h = 3)
    p1.assertResources(m = 31, s = 3, t = 0, p = 4, e = 5, h = 15)
    p1.assertDashMiddle(played = 18, actions = 3, vp = 29, tr = 29, hand = 11)
    p1.assertTags(but = 7, spt = 2, sct = 2, pot = 3, eat = 3, vet = 1, cit = 2)
    p1.assertCounts(4 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")

    p2.assertProduction(m = 0, s = 1, t = 1, p = 1, e = 1, h = 3)
    p2.assertResources(m = 21, s = 1, t = 1, p = 8, e = 1, h = 9)
    p2.assertDashMiddle(played = 13, actions = 5, vp = 29, tr = 27, hand = 12)
    p2.assertTags(but = 4, spt = 3, sct = 4, pot = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 6, temp = -18, oxygen = 0, oceans = 2, venus = 10)

    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Tharsis_8_7>",
        "OxygenStep",
        "TerraformRating",
        "ProjectCard",
    )
    p2.doTasks(
        "UseCardAction<$Factorum, Action2>",
        "-3 MC",
        "SearchForCard<TagFilter<Class<BuildingTag>>>",
    )

    p1.doTasks(
        "UseCardAction<$DevelopmentCenter, Action1>",
        "-Energy",
        "ProjectCard",
    )

    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "Ok", // keep card
        "-3 MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    p2.doTasks(
        "PlayProject<Class<$PowerPlant>>",
        "-1 Steel",
        "-2 MC",
        "$PowerPlant FROM ProjectCard",
        "PROD[Energy]",
    )
    p2.doTasks(
        "UseCardAction<$AquiferPumping, Action1>",
        "-8 MC",
        "Ok", // no steel paid
        "OceanTile<Tharsis_1_5>",
        "4 MC",
        "TerraformRating",
        "2 Plant",
    )

    p1.doTasks(
        "PlayProject<Class<$OlympusConference>>",
        "-3 Steel",
        "-1 MC",
        "$OlympusConference FROM ProjectCard",
        "Science<$OlympusConference>. OR ProjectCard FROM Science<$OlympusConference>",
    )

    p1.doTasks(
        "PlayProject<Class<$SisterPlanetSupport>>",
        "-4 MC",
        "$SisterPlanetSupport FROM ProjectCard",
        "PROD[3 MC]",
        "3 MC",
    )

    p2.doTasks(
        "UseCardAction<$DirectedImpactors, Action1>",
        "-1 Titanium",
        "-3 MC",
        "Asteroid<$RotatorImpacts>",
    )
    p2.doTasks(
        "UseCardAction<$RotatorImpacts, Action2>",
        "-Asteroid<$RotatorImpacts>",
        "VenusStep",
        "TerraformRating",
    )

    p1.doTasks(
        "PlayProject<Class<$DuskLaserMining>>",
        "-8 MC",
        "Ok", // no titanium paid
        "$DuskLaserMining FROM ProjectCard",
        "PROD[Titanium]",
        "PROD[-Energy]",
        "5 Titanium",
    )

    p1.doTasks(
        "PlayProject<Class<$MirandaResort>>",
        "-4 Titanium",
        "Ok", // no mc paid
        "$MirandaResort FROM ProjectCard",
        "PROD[5 MC]",
        "5 MC",
    )

    p2.doTasks(
        "PlayProject<Class<$Mine>>",
        "-4 MC",
        "Ok", // no steel paid
        "$Mine FROM ProjectCard",
        "PROD[Steel]",
    )
    p2.doTasks(
        "UseCardAction<$SearchForLife, Action1>",
        "-MC",
        "ProjectCard<Revealed>",
        "Ok", // no reward
        "-ProjectCard<Revealed>",
    )

    p1.doTasks(
        "PlayProject<Class<$Solarnet>>",
        "-7 MC",
        "$Solarnet FROM ProjectCard",
        "2 ProjectCard",
    )

    p1.doTasks(
        "PlayProject<Class<$MiningQuota>>",
        "-5 MC",
        "Ok", // no steel paid
        "$MiningQuota FROM ProjectCard",
        "PROD[2 Steel]",
        "2 Steel",
    )

    p2.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )

    p2.doTasks("Ok") // end turn

    p1.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    p1.doTasks(
        "UseCardAction<$DeuteriumExport, Action1>",
        "Floater<$DeuteriumExport>",
    )

    p2.doTasks("Pass")
    p1.doTasks(
        "PlayProject<Class<$LagrangeObservatory>>",
        "-1 Titanium",
        "-6 MC",
        "$LagrangeObservatory FROM ProjectCard",
        "ProjectCard FROM Science<$OlympusConference>",
        "ProjectCard",
    )

    p1.doTasks(
        "PlayProject<Class<$VenusGovernor>>",
        "-4 MC",
        "$VenusGovernor FROM ProjectCard",
        "PROD[2 MC]",
        "2 MC",
    )
    p1.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "1 MC FROM ProjectCard",
    )
    p1.doTasks(
        "PlayProject<Class<$Moss>>",
        "-4 MC",
        "$Moss FROM ProjectCard",
        "-Plant",
        "PROD[Plant]",
        "Plant",
    )

    p1.doTasks("Pass")

    // Generation 7
    p1.doTasks("4 Heat FROM Energy", "49 MC", "5 Steel", "4 Energy", "3 Heat", "Titanium", "Plant")
    p2.doTasks("Heat FROM Energy", "31 MC", "2 Steel", "Titanium", "3 Heat", "Plant", "2 Energy")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("-1 ProjectCard<Selecting>", "-9 MC", "3 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-3 ProjectCard<Selecting>", "-3 MC", "ProjectCard<Hand FROM Selecting>")

    p1.assertProduction(m = 19, s = 5, t = 1, p = 1, e = 4, h = 3)
    p1.assertResources(m = 40, s = 7, t = 1, p = 5, e = 4, h = 14)
    p1.assertDashMiddle(played = 27, actions = 3, vp = 34, tr = 30, hand = 10)
    p1.assertTags(but = 9, spt = 5, sct = 4, pot = 3, eat = 5, jot = 1, vet = 4, plt = 1, cit = 2)
    p1.assertCounts(4 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")

    p2.assertProduction(m = 0, s = 2, t = 1, p = 1, e = 2, h = 3)
    p2.assertResources(m = 32, s = 2, t = 1, p = 3, e = 2, h = 5)
    p2.assertDashMiddle(played = 15, actions = 5, vp = 34, tr = 31, hand = 13)
    p2.assertTags(but = 6, spt = 3, sct = 4, pot = 2, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 7, temp = -14, oxygen = 1, oceans = 3, venus = 12)

    p1.doTasks(
        "ClaimMilestone<Class<Builder8>>",
        "-8 MC",
        "Builder8",
    )

    p1.doTasks(
        "UseCardAction<$DevelopmentCenter, Action1>",
        "-Energy",
        "ProjectCard",
    )

    p2.doTasks(
        "PlayProject<Class<$EarthCatapult>>",
        "-23 MC",
        "$EarthCatapult FROM ProjectCard",
    )
    p2.doTasks(
        "PlayProject<Class<$InventionContest>>",
        "$InventionContest FROM ProjectCard",
        "3 ProjectCard<Selecting>",
        "ProjectCard<Hand FROM Selecting>",
        "-2 ProjectCard<Selecting>",
        "-ProjectCard",
        "ProjectCard",
    )

    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "-1 ProjectCard<Selecting>",
    )
    p1.doTasks(
        "PlayProject<Class<$QuantumExtractor>>",
        "-13 MC",
        "$QuantumExtractor FROM ProjectCard",
        "Science",
        "PROD[4 Energy]",
        "4 Energy",
    )

    p2.doTasks(
        "PlayProject<Class<$BioPrintingFacility>>",
        "-2 Steel",
        "-1 MC",
        "$BioPrintingFacility FROM ProjectCard",
    )
    p2.doTasks(
        "UseCardAction<$BioPrintingFacility, Action1>",
        "-2 Energy",
        "2 Plant",
    )

    p1.doTasks(
        "UseCardAction<$DeuteriumExport, Action2>",
        "-Floater<$DeuteriumExport>",
        "PROD[Energy]",
        "Energy",
    )
    p1.doTasks(
        "PlayProject<Class<$ProjectInspection>>",
        "$ProjectInspection FROM ProjectCard",
        "UseAction<$DevelopmentCenter, Action1>",
        "-1 Energy",
        "ProjectCard",
    )

    p2.doTasks(
        "UseCardAction<$Factorum, Action1>",
        "PROD[Energy]",
    )

    p2.doTasks(
        "PlayProject<Class<$PowerSupplyConsortium>>",
        "-3 MC",
        "$PowerSupplyConsortium FROM ProjectCard",
        "PROD[-Energy<Player1>]",
        "PROD[Energy]",
    )

    p1.doTasks(
        "PlayProject<Class<$FloatingHabs>>",
        "-5 MC",
        "$FloatingHabs FROM ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$FloatingHabs, Action1>",
        "-2 MC",
        "Floater<$DeuteriumExport>",
    )

    p2.doTasks(
        "PlayProject<Class<$TitaniumMine>>",
        "-5 MC",
        "Ok", // no steel paid
        "$TitaniumMine FROM ProjectCard",
        "PROD[Titanium]",
    )
    p2.doTasks("Ok") // end turn

    p1.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )

    p1.doTasks(
        "PlayProject<Class<$StratosphericBirds>>",
        "-12 MC",
        "$StratosphericBirds FROM ProjectCard",
        "-Floater",
    )

    p2.doTasks("Pass")

    p1.doTasks(
        "UseCardAction<$StratosphericBirds, Action1>",
        "Animal<$StratosphericBirds>",
    )

    p1.doTasks("Pass")

    // Generation 8
    p1.doTasks("7 Heat FROM Energy", "50 MC", "5 Steel", "8 Energy", "3 Heat", "Titanium", "Plant")
    p2.doTasks("31 MC", "2 Steel", "2 Titanium", "3 Heat", "Plant", "4 Energy")
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")

    p1.assertProduction(m = 19, s = 5, t = 1, p = 1, e = 8, h = 3)
    p1.assertResources(m = 44, s = 12, t = 2, p = 6, e = 8, h = 16)
    p1.assertDashMiddle(played = 31, actions = 5, vp = 41, tr = 31, hand = 10)
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
    p2.assertDashMiddle(played = 20, actions = 6, vp = 36, tr = 31, hand = 11)
    p2.assertTags(but = 8, spt = 3, sct = 4, pot = 3, eat = 1, jot = 1, plt = 1, mit = 1)
    p2.assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 8, temp = -12, oxygen = 1, oceans = 3, venus = 12)

    p2.doTasks(
        "PlayProject<Class<$AdvancedAlloys>>",
        "-7 MC",
        "$AdvancedAlloys FROM ProjectCard",
        "-ProjectCard",
        "ProjectCard",
    )

    p2.doTasks(
        "PlayProject<Class<$AiCentral>>",
        "-2 Steel",
        "-13 MC",
        "$AiCentral FROM ProjectCard",
        "PROD[-Energy]",
        "-ProjectCard",
        "ProjectCard",
    )

    p1.doTasks(
        "PlayProject<Class<$ExtractorBalloons>>",
        "-21 MC",
        "$ExtractorBalloons FROM ProjectCard",
        "3 Floater<$ExtractorBalloons>",
    )

    p1.doTasks(
        "UseCardAction<$DevelopmentCenter, Action1>",
        "-Energy",
        "ProjectCard",
    )

    p1.assertCounts(23 to "MC")

    p2.doTasks(
        "UseCardAction<$AiCentral, Action1>",
        "2 ProjectCard",
    )

    p2.doTasks(
        "UseCardAction<$DirectedImpactors, Action1>",
        "-1 Titanium",
        "-2 MC",
        "Asteroid<$RotatorImpacts>",
    )

    admin.assertCounts(6 to "VenusStep")

    p1.doTasks(
        "PlayProject<Class<$SulphurExports>>",
        "-2 Titanium",
        "-13 MC",
        "$SulphurExports FROM ProjectCard",
        "VenusStep",
        "TerraformRating",
        "PROD[8 MC]",
        "8 MC",
    )

    p1.doTasks(
        "UseCardAction<$ExtractorBalloons, Action2>",
        "-2 Floater<$ExtractorBalloons>",
        "VenusStep",
        "2 TerraformRating",
    )

    p2.doTasks(
        "UseCardAction<$RotatorImpacts, Action2>",
        "-Asteroid<$RotatorImpacts>",
        "VenusStep",
        "TerraformRating",
    )

    p2.doTasks(
        "PlayProject<Class<$IshtarMining>>",
        "-3 MC",
        "$IshtarMining FROM ProjectCard",
        "PROD[Titanium]",
    )

    p1.doTasks(
        "PlayProject<Class<$MoholeLake>>",
        "-12 Steel",
        "-7 MC",
        "$MoholeLake FROM ProjectCard",
        "OceanTile<Tharsis_5_5>",
        "5 Plant",
        "TemperatureStep",
        "2 TerraformRating",
    )
    p1.selectTask("2 Plant<Player2>!")
    p2.doTasks("2 Plant")

    p1.doTasks(
        "ClaimMilestone<Class<Terraformer35>>",
        "-8 MC",
        "Terraformer35",
    )

    p2.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Tharsis_8_6>",
        "OxygenStep",
        "TerraformRating",
        "ProjectCard",
    )

    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "Ok", // keep card
        "-3 MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    p1.doTasks(
        "UseCardAction<$DeuteriumExport, Action1>",
        "Floater<$DeuteriumExport>",
    )

    p2.doTasks(
        "UseCardAction<$BioPrintingFacility, Action1>",
        "-2 Energy",
        "2 Plant",
    )

    p2.doTasks("Ok") // end turn

    p1.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    p1.doTasks("ConvertPlants", "-8 Plant")
    p1.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p1.doTasks(
        "GreeneryTile<Tharsis_3_5>",
        "OxygenStep",
        "TerraformRating",
    )

    p2.doTasks("Pass")

    p1.doTasks(
        "UseCardAction<$StratosphericBirds, Action1>",
        "Animal<$StratosphericBirds>",
    )

    p1.doTasks(
        "UseCardAction<$MoholeLake, Action1>",
        "Animal<$StratosphericBirds>",
    )
    p1.doTasks("Pass")

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
    p1.doTasks("-1 ProjectCard<Selecting>", "-9 MC", "3 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")

    p1.assertProduction(m = 27, s = 5, t = 1, p = 1, e = 8, h = 3)
    p1.assertResources(m = 56, s = 5, t = 1, p = 4, e = 8, h = 18)
    p1.assertDashMiddle(played = 34, actions = 7, vp = 58, tr = 38, hand = 12)
    p1.assertTags(10, 6, sct = 5, pot = 4, eat = 5, jot = 1, vet = 8, plt = 1, ant = 1, cit = 2)
    p1.assertCounts(5 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")

    p2.assertProduction(m = 0, s = 2, t = 3, p = 1, e = 3, h = 3)
    p2.assertResources(m = 28, s = 2, t = 5, p = 3, e = 3, h = 5)
    p2.assertDashMiddle(played = 23, actions = 7, vp = 41, tr = 34, hand = 13)
    p2.assertTags(but = 9, spt = 3, sct = 6, pot = 3, eat = 1, jot = 1, vet = 1, plt = 1, mit = 1)
    p2.assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 9, temp = -6, oxygen = 3, oceans = 4, venus = 18)

    p1.doTasks(
        "UseCardAction<$DevelopmentCenter, Action1>",
        "-Energy",
        "ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "Ok", // keep card
        "-3 MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    p2.doTasks(
        "PlayProject<Class<$DeimosDownPromo>>",
        "-5 Titanium",
        "-9 MC",
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
    p2.doTasks(
        "UseCardAction<$AiCentral, Action1>",
        "2 ProjectCard",
    )

    p1.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    p1.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )

    p2.doTasks(
        "UseCardAction<$AquiferPumping, Action1>",
        "-3 Steel",
        "Ok", // no mc paid
        "OceanTile<Tharsis_5_6>",
        "4 MC",
        "TerraformRating",
        "4 Plant",
    )
    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Tharsis_9_7>",
        "OxygenStep",
        "TerraformRating",
    )

    p1.doTasks(
        "PlayProject<Class<$RegoPlastics>>",
        "-10 MC",
        "Ok", // no steel paid
        "$RegoPlastics FROM ProjectCard",
    )
    p1.doTasks(
        "PlayProject<Class<$SfMemorial>>",
        "-2 Steel",
        "-1 MC",
        "$SfMemorial FROM ProjectCard",
        "ProjectCard",
    )

    p2.doTasks(
        "ClaimMilestone<Class<Gardener>>",
        "-8 MC",
        "Gardener",
    )
    p2.doTasks(
        "UseCardAction<$DirectedImpactors, Action1>",
        "-6 MC",
        "Ok", // no titanium paid
        "Asteroid<$RotatorImpacts>",
    )

    p1.doTasks(
        "UseCardAction<$FloatingHabs, Action1>",
        "-2 MC",
        "Floater<$ExtractorBalloons>",
    )
    p1.doTasks(
        "UseCardAction<$ExtractorBalloons, Action2>",
        "-2 Floater<$ExtractorBalloons>",
        "VenusStep",
        "TerraformRating",
    )

    p2.doTasks(
        "PlayProject<Class<$EcologicalZone>>",
        "-10 MC",
        "$EcologicalZone FROM ProjectCard",
        "EcologicalZone_SpecialTile<Tharsis_4_5>",
        "2 Animal<$EcologicalZone>",
        "4 MC",
        "2 Plant",
    )

    p2.doTasks(
        "PlayProject<Class<$Harvest>>",
        "-2 MC",
        "$Harvest FROM ProjectCard",
        "Animal<$EcologicalZone>",
        "12 MC",
    )

    p1.doTasks(
        "PlayProject<Class<$NoctisFarming>>",
        "-3 Steel",
        "-1 MC",
        "$NoctisFarming FROM ProjectCard",
        "2 Plant",
        "PROD[MC]",
        "MC",
    )

    p1.doTasks(
        "UseCardAction<$DeuteriumExport, Action2>",
        "-Floater<$DeuteriumExport>",
        "PROD[Energy]",
        "Energy",
    )

    p2.doTasks(
        "UseCardAction<$BioPrintingFacility, Action1>",
        "-2 Energy",
        "Animal<$EcologicalZone>",
    )
    p2.doTasks(
        "UseCardAction<$RotatorImpacts, Action2>",
        "-Asteroid<$RotatorImpacts>",
        "VenusStep",
        "TerraformRating",
    )

    p1.doTasks(
        "UseCardAction<$MoholeLake, Action1>",
        "Animal<$StratosphericBirds>",
    )

    p1.doTasks(
        "UseCardAction<$StratosphericBirds, Action1>",
        "Animal<$StratosphericBirds>",
    )

    p2.doTasks(
        "UseCardAction<$Factorum, Action2>",
        "-3 MC",
        "SearchForCard<TagFilter<Class<BuildingTag>>>",
    )

    p2.doTasks(
        "PlayProject<Class<$NaturalPreserve>>",
        "-2 Steel",
        "-1 MC",
        "$NaturalPreserve FROM ProjectCard",
        "-ProjectCard",
        "NaturalPreserve_SpecialTile<Tharsis_3_1>",
        "2 ProjectCard",
        "PROD[MC]",
    )

    p1.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "3 MC FROM ProjectCard",
    )
    p1.doTasks(
        "PlayProject<Class<$WaterToVenus>>",
        "-1 Titanium",
        "-4 MC",
        "$WaterToVenus FROM ProjectCard",
        "VenusStep",
        "TerraformRating",
        "3 MC",
        "3 Heat",
    )

    p2.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "2 MC FROM ProjectCard",
    )
    p2.doTasks(
        "PlayProject<Class<$KelpFarming>>",
        "-15 MC",
        "$KelpFarming FROM ProjectCard",
        "Animal<$EcologicalZone>",
        "2 Plant",
        "PROD[2 MC]",
        "PROD[3 Plant]",
    )

    p1.doTasks(
        "PlayProject<Class<$Trees>>",
        "-13 MC",
        "$Trees FROM ProjectCard",
        "PROD[3 Plant]",
        "4 Plant",
    )
    p1.doTasks("FundAward<Class<Banker>>", "-8 MC", "Banker")

    p2.doTasks(
        "UseCardAction<$SearchForLife, Action1>",
        "-MC",
        "ProjectCard<Revealed>",
        "Ok", // no reward
        "-ProjectCard<Revealed>",
    )

    p2.doTasks("Ok") // end turn

    p1.doTasks(
        "PlayProject<Class<$VenusianInsects>>",
        "-5 MC",
        "$VenusianInsects FROM ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$VenusianInsects, Action1>",
        "Microbe<$VenusianInsects>",
    )

    p2.doTasks("Pass")

    p1.doTasks("FundAward<Class<Venuphile>>", "-14 MC", "Venuphile")
    p1.doTasks("Pass")

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
    p1.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-1 ProjectCard<Selecting>", "-9 MC", "3 ProjectCard<Hand FROM Selecting>")

    p1.assertProduction(m = 28, s = 5, t = 1, p = 4, e = 9, h = 3)
    p1.assertResources(m = 66, s = 5, t = 1, p = 10, e = 9, h = 16)
    p1.assertDashMiddle(played = 40, actions = 8, vp = 78, tr = 42, hand = 8)
    p1.assertTags(13, 6, 5, pot = 4, eat = 5, jot = 1, vet = 9, plt = 3, mit = 1, ant = 1, cit = 2)
    p1.assertCounts(6 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")

    p2.assertProduction(m = 3, s = 2, t = 3, p = 4, e = 3, h = 3)
    p2.assertResources(m = 36, s = 3, t = 3, p = 10, e = 3, h = 9)
    p2.assertDashMiddle(played = 28, actions = 7, vp = 58, tr = 41, hand = 13)
    p2.assertTags(10, 3, sct = 7, pot = 3, eat = 1, jot = 1, vet = 1, plt = 3, mit = 1, ant = 1)
    p2.assertCounts(4 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")

    assertSidebar(gen = 10, temp = 4, oxygen = 4, oceans = 6, venus = 24)

    p2.doTasks(
        "PlayProject<Class<$HiredRaiders>>",
        "$HiredRaiders FROM ProjectCard",
        "2 Steel FROM Steel<Player1>",
    )
    p2.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )

    p1.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
    )
    p1.doTasks(
        "UseStandardProject<CityProject>",
        "-25 MC",
        "CityTile<Tharsis_7_6>",
        "PROD[MC]",
        "MC",
    )

    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Tharsis_9_6>",
        "2 Steel",
        "OxygenStep",
        "TerraformRating",
    )

    p2.doTasks(
        "UseCardAction<$AiCentral, Action1>",
        "2 ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$DevelopmentCenter, Action1>",
        "-Energy",
        "ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "-1 ProjectCard<Selecting>",
    )
    p2.doTasks(
        "PlayProject<Class<$MercurianAlloys>>",
        "-1 MC",
        "Ok", // no titanium paid
        "$MercurianAlloys FROM ProjectCard",
    )
    p2.doTasks(
        "PlayProject<Class<$AerialMappers>>",
        "-9 MC",
        "$AerialMappers FROM ProjectCard",
    )
    p1.doTasks(
        "PlayProject<Class<$LavaTubeSettlement>>",
        "-3 Steel",
        "-6 MC",
        "$LavaTubeSettlement FROM ProjectCard",
        "CityTile<Tharsis_2_2>",
        "Steel",
        "PROD[-Energy]",
        "PROD[2 MC]",
        "2 MC",
    )

    p1.doTasks(
        "PlayProject<Class<$UrbanizedArea>>",
        "-1 Steel",
        "-7 MC",
        "$UrbanizedArea FROM ProjectCard",
        "CityTile<Tharsis_2_3>",
        "PROD[-Energy]",
        "PROD[2 MC]",
        "2 MC",
    )
    p2.doTasks(
        "PlayProject<Class<$Atmoscoop>>",
        "-3 Titanium",
        "-5 MC",
        "$Atmoscoop FROM ProjectCard",
        "2 VenusStep",
        "2 Floater<$AerialMappers>",
        "2 TerraformRating",
    )

    p2.doTasks(
        "UseCardAction<$AerialMappers, Action2>",
        "-Floater<$AerialMappers>",
        "ProjectCard",
    )
    p1.doTasks(
        "PlayProject<Class<$NitrogenRichAsteroid>>",
        "-1 Titanium",
        "-26 MC",
        "$NitrogenRichAsteroid FROM ProjectCard",
        "PROD[4 Plant]",
        "2 TerraformRating",
        "Ok", // temperature maxed
        "3 MC",
        "3 Heat",
        "4 Plant",
    )

    p1.doTasks("ConvertPlants", "-8 Plant")
    p1.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p1.doTasks(
        "GreeneryTile<Tharsis_3_3>",
        "OxygenStep",
        "TerraformRating",
    )
    p2.doTasks(
        "UseCardAction<$BioPrintingFacility, Action1>",
        "-2 Energy",
        "Animal<$EcologicalZone>",
    )
    p2.doTasks(
        "UseCardAction<$DirectedImpactors, Action1>",
        "-6 MC",
        "Ok", // no titanium paid
        "Asteroid<$RotatorImpacts>",
    )
    p1.doTasks(
        "UseCardAction<$VenusianInsects, Action1>",
        "Microbe<$VenusianInsects>",
    )
    p1.doTasks(
        "UseCardAction<$StratosphericBirds, Action1>",
        "Animal<$StratosphericBirds>",
    )
    p2.doTasks(
        "UseCardAction<$RotatorImpacts, Action2>",
        "-Asteroid<$RotatorImpacts>",
        "VenusStep",
        "TerraformRating",
    )

    p2.doTasks(
        "UseCardAction<$AquiferPumping, Action1>",
        "-2 Steel",
        "-2 MC",
        "OceanTile<Tharsis_9_9>",
        "2 Titanium",
        "TerraformRating",
        "2 Plant",
    )
    p1.doTasks(
        "PlayProject<Class<$PowerInfrastructure>>",
        "-4 MC",
        "Ok", // no steel paid
        "$PowerInfrastructure FROM ProjectCard",
    )
    p1.doTask("UseCardAction<$PowerInfrastructure, Action1>")
    p1.fillInTask(p1.tasks.ids().single()).apply { choose(decisions().single(), "8") }.commit()
    p1.doTask("8 MC")
    p2.doTasks(
        "UseCardAction<$Factorum, Action2>",
        "-3 MC",
        "SearchForCard<TagFilter<Class<BuildingTag>>>",
    )
    p2.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "2 MC FROM ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$DeuteriumExport, Action1>",
        "Floater<$DeuteriumExport>",
    )
    p1.doTasks(
        "UseCardAction<$ExtractorBalloons, Action1>",
        "Floater<$ExtractorBalloons>",
    )
    p2.doTasks(
        "PlayProject<Class<$Bushes>>",
        "-8 MC",
        "$Bushes FROM ProjectCard",
        "Animal<$EcologicalZone>",
        "2 Plant",
        "PROD[2 Plant]",
    )
    p2.doTasks(
        "PlayProject<Class<$EnergyTapping>>",
        "-1 MC",
        "$EnergyTapping FROM ProjectCard",
        "PROD[-Energy<Player1>]",
        "PROD[Energy]",
    )
    p1.doTasks(
        "UseCardAction<$FloatingHabs, Action1>",
        "-2 MC",
        "Floater<$FloatingHabs>",
    )
    p1.doTasks(
        "UseCardAction<$MoholeLake, Action1>",
        "Animal<$StratosphericBirds>",
    )
    p2.doTasks(
        "PlayProject<Class<$NuclearPower>>",
        "-3 Steel",
        "Ok", // no mc paid
        "$NuclearPower FROM ProjectCard",
        "PROD[-2 MC]",
        "PROD[3 Energy]",
    )
    p2.doTasks(
        "PlayProject<Class<$BiomassCombustors>>",
        "-1 Steel",
        "Ok", // no mc paid
        "$BiomassCombustors FROM ProjectCard",
        "PROD[-Plant<Player1>]",
        "PROD[2 Energy]",
    )
    p1.doTasks("Pass")
    p2.doTasks(
        "UseCardAction<$SearchForLife, Action1>",
        "-MC",
        "ProjectCard<Revealed>",
        "Ok", // no reward
        "-ProjectCard<Revealed>",
    )
    p2.doTasks("Pass")
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
    p1.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-3 ProjectCard<Selecting>", "-3 MC", "ProjectCard<Hand FROM Selecting>")
    p1.doTasks(
        "PlayProject<Class<$ImportedNitrogen>>",
        "-1 Titanium",
        "-15 MC",
        "$ImportedNitrogen FROM ProjectCard",
        "3 Microbe<$VenusianInsects>",
        "2 Animal<$StratosphericBirds>",
        "TerraformRating",
        "4 Plant",
        "3 MC",
        "3 Heat",
    )
    p1.doTasks(
        "UseCardAction<$DevelopmentCenter, Action1>",
        "-Energy",
        "ProjectCard",
    )
    p2.doTasks(
        "UseCardAction<$AiCentral, Action1>",
        "2 ProjectCard",
    )
    p2.doTasks(
        "UseCardAction<$Factorum, Action2>",
        "-3 MC",
        "SearchForCard<TagFilter<Class<BuildingTag>>>",
    )
    p1.doTasks("ConvertPlants", "-8 Plant")
    p1.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p1.doTasks(
        "GreeneryTile<Tharsis_2_4>",
        "2 MC",
        "OxygenStep",
        "TerraformRating",
    )
    p1.doTasks(
        "UseCardAction<$InventorsGuild, Action1>",
        "ProjectCard<Selecting>",
        "-1 ProjectCard<Selecting>",
    )
    p2.doTasks(
        "PlayProject<Class<$MediaGroup>>",
        "-4 MC",
        "$MediaGroup FROM ProjectCard",
    )
    p2.doTasks(
        "PlayProject<Class<$MiningExpedition>>",
        "-10 MC",
        "$MiningExpedition FROM ProjectCard",
        "-2 Plant<Player1>",
        "OxygenStep",
        "TerraformRating",
        "2 Steel",
        "3 MC",
        "Ok", // temperature maxed
    )
    p1.doTask("UseCardAction<$PowerInfrastructure, Action1>")
    p1.fillInTask(p1.tasks.ids().single()).apply { choose(decisions().single(), "5") }.commit()
    p1.doTask("5 MC")
    p1.doTasks(
        "UseCardAction<$ExtractorBalloons, Action1>",
        "Floater<$ExtractorBalloons>",
    )
    p2.doTasks(
        "UseCardAction<$BioPrintingFacility, Action1>",
        "-2 Energy",
        "Animal<$EcologicalZone>",
    )
    p2.doTasks(
        "UseCardAction<$AquiferPumping, Action1>",
        "-2 Steel",
        "-2 MC",
        "OceanTile<Tharsis_5_4>",
        "2 MC",
        "TerraformRating",
        "4 Plant",
    )
    p1.doTasks(
        "PlayProject<Class<$BusinessNetwork>>",
        "-1 MC",
        "$BusinessNetwork FROM ProjectCard",
        "PROD[-MC]",
    )

    p1.doTasks(
        "UseCardAction<$BusinessNetwork, Action1>",
        "ProjectCard<Selecting>",
        "Ok", // keep card
        "-3 MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "UseStandardProject<CityProject>",
        "-25 MC",
        "CityTile<Tharsis_8_5>",
        "PROD[MC]",
    )
    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Tharsis_8_4>",
        "2 Steel",
        "OxygenStep",
        "TerraformRating",
    )
    p1.doTasks(
        "UseCardAction<$DeuteriumExport, Action2>",
        "-Floater<$DeuteriumExport>",
        "PROD[Energy]",
        "Energy",
    )

    p1.doTasks(
        "UseCardAction<$FloatingHabs, Action1>",
        "-2 MC",
        "Floater<$FloatingHabs>",
    )
    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Tharsis_9_5>",
        "Steel",
        "OxygenStep",
        "TerraformRating",
    )

    p2.doTasks(
        "UseCardAction<$AerialMappers, Action2>",
        "-Floater<$AerialMappers>",
        "ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$StratosphericBirds, Action1>",
        "Animal<$StratosphericBirds>",
    )
    p1.doTasks(
        "UseCardAction<$MoholeLake, Action1>",
        "Animal<$StratosphericBirds>",
    )
    p2.doTasks(
        "PlayProject<Class<$MagneticFieldGeneratorsPromo>>",
        "-6 Steel",
        "-2 MC",
        "$MagneticFieldGeneratorsPromo FROM ProjectCard",
        "MagneticFieldGeneratorsPromo_SpecialTile<Tharsis_6_6>",
        "PROD[-4 Energy]",
        "PROD[2 Plant]",
        "3 TerraformRating",
        "6 MC",
        "Plant",
    )

    p2.doTasks(
        "PlayProject<Class<$TowingAComet>>",
        "-4 Titanium",
        "-1 MC",
        "$TowingAComet FROM ProjectCard",
        "OceanTile<Tharsis_6_8>",
        "5 Plant",
        "OxygenStep",
        "2 TerraformRating",
        "5 MC",
    )
    p1.doTasks(
        "UseCardAction<$VenusianInsects, Action1>",
        "Microbe<$VenusianInsects>",
    )
    p1.doTasks(
        "PlayProject<Class<$StandardTechnology>>",
        "-6 MC",
        "$StandardTechnology FROM ProjectCard",
        "ProjectCard FROM Science",
    )
    p2.doTasks(
        "PlayProject<Class<$AtalantaPlanitiaLab>>",
        "-8 MC",
        "$AtalantaPlanitiaLab FROM ProjectCard",
        "2 ProjectCard",
        "-ProjectCard",
        "ProjectCard",
    )
    p2.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "3 MC FROM ProjectCard",
    )
    admin.assertCounts(9 to "OceanTile")
    p1.doTasks(
        "PlayProject<Class<$LargeConvoy>>",
        "-31 MC",
        "Ok", // no titanium paid
        "$LargeConvoy FROM ProjectCard",
        "4 Animal<$StratosphericBirds>",
        "Ok", // oceans full
        "2 ProjectCard",
        "3 MC",
        "3 Heat",
    )

    p1.doTasks(
        "PlayProject<Class<$WaterSplittingPlant>>",
        "-4 Steel",
        "Ok", // no mc paid
        "$WaterSplittingPlant FROM ProjectCard",
    )
    p2.doTasks(
        "PlayProject<Class<$RobotPollinators>>",
        "-7 MC",
        "$RobotPollinators FROM ProjectCard",
        "PROD[Plant]",
        "4 Plant",
    )

    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Tharsis_7_4>",
        "OxygenStep",
        "TerraformRating",
    )
    p1.doTasks(
        "PlayProject<Class<$MediaArchives>>",
        "-5 MC",
        "$MediaArchives FROM ProjectCard",
        "16 MC",
    )
    p1.doTasks(
        "UseStandardProject<GreeneryProject>",
        "-23 MC",
        "GreeneryTile<Tharsis_5_7>",
        "9 MC",
        "2 Plant",
        "OxygenStep",
        "TerraformRating",
    )
    p2.doTasks(
        "PlayProject<Class<$Greenhouses>>",
        "-4 MC",
        "Ok", // no steel paid
        "$Greenhouses FROM ProjectCard",
        "Animal<$EcologicalZone>",
        "6 Plant",
    )

    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Tharsis_9_8>",
        "2 MC",
        "OxygenStep",
        "TerraformRating",
    )
    p1.doTasks("FundAward<Class<Thermalist>>", "-20 MC", "Thermalist")
    p1.doTasks("ConvertPlants", "-8 Plant")
    p1.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p1.doTasks(
        "GreeneryTile<Tharsis_4_4>",
        "Plant",
        "4 MC",
        "Ok", // oxygen maxed
    )
    p2.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "3 MC FROM ProjectCard",
    )
    p2.doTasks(
        "PlayProject<Class<$Penguins>>",
        "-5 MC",
        "$Penguins FROM ProjectCard",
        "Animal<$EcologicalZone>",
    )
    p1.doTasks(
        "PlayProject<Class<$AdvancedEcosystems>>",
        "-11 MC",
        "$AdvancedEcosystems FROM ProjectCard",
    )
    p1.doTasks(
        "UseStandardProject<SellPatentsProject>",
        "4 MC FROM ProjectCard",
    )
    p2.doTasks(
        "UseCardAction<$Penguins, Action1>",
        "Animal<$Penguins>",
    )

    p2.doTasks("Ok") // end turn
    p1.doTasks(
        "PlayProject<Class<$GeneRepair>>",
        "-12 MC",
        "$GeneRepair FROM ProjectCard",
        "PROD[2 MC]",
        "Science",
        "2 MC",
    )

    p1.doTasks("Ok") // end turn
    p2.doTasks("Pass")
    p1.doTasks("Pass")

    p1.doTasks("Heat FROM Energy", "83 MC")
    p2.doTasks("7 Heat FROM Energy", "60 MC")
    p1.doTasks("5 Steel")
    p2.doTasks("2 Steel", "3 Titanium", "3 Heat", "9 Plant", "5 Energy")
    p1.doTasks("7 Energy", "3 Heat", "Titanium", "7 Plant")

    // Final greenery placement
    p1.doTasks("ConvertPlants", "-8 Plant")
    p1.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p1.doTasks(
        "GreeneryTile<Tharsis_6_5>",
        "Plant",
        "4 MC",
    )
    p1.doTasks("Ok") // no more greenery
    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Tharsis_8_8>",
        "2 MC",
    )
    p2.doTasks("Ok") // no more greenery

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
