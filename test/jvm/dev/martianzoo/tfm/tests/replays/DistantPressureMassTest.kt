package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.giveTask
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import kotlin.test.Test
import kotlin.test.assertEquals

// Source: _local/replays/Game20260824/ (game log, player records, and screenshots)
// Complete archive replay: Distant Pressure Mass (gaf4dfdc697db)
// http://newazure.local:8080/the-end?id=paffe109dfc39
internal class DistantPressureMassTest : FullGameTest() {
  override val config =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, prelude2:cards, ColoniesExpansion, promos:cards, SagittaFrontierServices
          -Merger

          Ecologist, Terraformer, Terran, Mayor, Merchant, Researcher
          Electrician, Industrialist, Highlander, Investor, Scientist, Manufacturer
          Ceres, Ganymede, Io, Miranda, Titan
          """,
          "Keen",
          "Been",
      )
  // Solarnet's Terran is the archive's Terran5 milestone. It was available but never claimed.

  private val keen
    get() = p1

  private val been
    get() = p2

  @Test
  internal fun distantPressureMass() {
    startGame()
    keen.doTask("20 TerraformRating")
    been.doTask("20 TerraformRating")
    keen.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    been.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )

    keen.doTask("-6 ProjectCard<Selecting>")
    been.doTask("-6 ProjectCard<Selecting>")
    generation1()
    generation2()
    generation3()
    generation4()
    generation5()
    generation6()
    generation7()
    generation8()
    generation9()
    generation10()
    generation11()

    // _local/replays/Game20260824/player-keen.json; final Mars map.
    assertEquals(
        """
        |                      1     2     3     4     5     6     7     8     9
        |                     /     /     /     /     /     /     /     /     /
        |
        | 1 -             WPP   [G2]  [G2]  [C2]  [G2]
        |
        | 2 -          [O]   [G2]  [C2]  [G2]  [G2]  [C2]
        |
        | 3 -        WP   [C1]  [G1]  [C2]  [C2]  [S1]  [G2]
        |
        | 4 -     WP   [G1]  [G1]  [C1]  [G1]  [O]   [O]    LP
        |
        | 5 -  LC    L    [C2]  [G1]  [G1]  [O]   [O]   [O]    LP
        |
        | 6 -    [S2]  [G2]   LS   [G1]  [C1]  [O]   [O]    L
        |
        | 7 -       [O]   [G1]  [C1]  [G1]  [G1]  [G1]   LT
        |
        | 8 -           LS   [G1]  LHH   LHH   [C1]   LT
        |
        | 9 -              L    [G2]  [C2]  LHH    L
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
  }

  private fun generation1() {
    with(keen) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$SagittaFrontierServices>>",
          "$SagittaFrontierServices FROM CorporationCard",
          "31 MC",
          "PROD[Energy]",
          "PROD[2 MC]",
          "SearchForCard<NoTagsFilter>",
          "4 MC",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(been) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Polyphemos>>",
          "$Polyphemos FROM CorporationCard",
          "50 MC",
          "5 Titanium",
          "PROD[5 MC]",
          "20 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(keen) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$IndustrialComplex>>",
          "$IndustrialComplex FROM PreludeCard",
          "-18 MC",
          "EACH",
          "MC",
          "PROD[Titanium]",
          "PROD[Steel]",
          "PROD[Plant]",
          "PROD[Heat]",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AppliedScience>>",
          "$AppliedScience FROM PreludeCard",
          "6 Science<$AppliedScience>",
          "4 MC",
      )
    }

    with(been) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$StrategicBasePlanning>>",
          "$StrategicBasePlanning FROM PreludeCard",
          "-3 MC",
          "Colony<Ganymede>",
          "PROD[Plant]",
          "CityTile<Hellas_9_7>",
          "-6 MC",
          "OceanTile<Hellas_5_6>",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EarlySettlement>>",
          "$EarlySettlement FROM PreludeCard",
          "PROD[Plant]",
          "CityTile<Hellas_2_6>",
          "Plant",
      )
    }

    keen.turn {
      doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Titanium")
    }

    been.turn {
      doTasks(
          *playCardTasks("$LunarExports"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<> FROM MC",
          "$LunarExports FROM ProjectCard",
          "PROD[5 MC]",
      )
    }

    keen.doTask("Pass")

    been.turn {
      doTasks(
          *playCardTasks("$Flooding"),
          "7 Pay<> FROM MC",
          "$Flooding FROM ProjectCard",
          "OceanTile<Hellas_5_7>",
          "TerraformRating",
          "3 Heat",
          "2 MC",
      )
    }
    been.doTask("Pass")

    // Production.
    keen.doTasks("22 MC", "Energy", "Titanium", "Steel", "Plant", "Heat")
    been.doTasks("32 MC", "2 Plant")

    keen.doTask("VenusStep! BY Admin")
  }

  private fun generation2() {
    keen.doTask("4 ProjectCard<Selecting>")
    been.doTask("4 ProjectCard<Selecting>")

    keen.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    been.doTasks(
        "-1 ProjectCard<Selecting>",
        "15 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    been.turn {
      doTasks(
          *playCardTasks("$ExtractorBalloons"),
          "21 Pay<> FROM MC",
          "$ExtractorBalloons FROM ProjectCard",
          "3 Floater<$ExtractorBalloons>",
      )
    }

    keen.turn {
      doTasks(
          *playCardTasks("$CuttingEdgeTechnology"),
          "12 Pay<> FROM MC",
          "$CuttingEdgeTechnology FROM ProjectCard",
          "MC",
      )
    }

    been.turn {
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
    }

    keen.turn {
      doTasks(*playCardTasks("$DustSeals"), "$DustSeals FROM ProjectCard", "4 MC")
    }

    been.doTask("Pass")

    keen.turn {
      doTasks(
          *playCardTasks("$DirectedHeatUsage"),
          "1 Pay<> FROM MC",
          "$DirectedHeatUsage FROM ProjectCard",
          "4 MC",
      )
      doTasks(
          *playCardTasks("$CommunityServices"),
          "13 Pay<> FROM MC",
          "$CommunityServices FROM ProjectCard",
          "PROD[5 MC]",
          "4 MC",
      )
      doTasks(
          *playCardTasks("$BusinessNetwork"),
          "4 Pay<> FROM MC",
          "$BusinessNetwork FROM ProjectCard",
          "PROD[-MC]",
          "MC",
      )
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTasks(
          *playCardTasks("$SearchForLife"),
          "1 Pay<> FROM MC",
          "$SearchForLife FROM ProjectCard",
          "MC",
      )
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag on the revealed card.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Titanium")
    }
    keen.doTask("Pass")

    // Production.
    keen.doTasks("26 MC", "Energy", "Titanium", "Steel", "Plant", "Heat", "Heat FROM Energy")
    been.doTasks("33 MC", "2 Plant")

    been.doTask("OceanTile<Hellas_4_6>! BY Admin")
  }

  private fun generation3() {
    keen.doTask("4 ProjectCard<Selecting>")
    been.doTask("4 ProjectCard<Selecting>")

    keen.doTask("Ok") // Keep all four research cards.
    keen.doTasks("12 Pay<> FROM MC", "4 ProjectCard<Hand FROM Selecting>")

    // Screenshot evidence: generation-3-research.png, after Keen's purchase and before Been's.
    with(keen) {
      assertResources(m = 20, s = 2, t = 4, p = 2, e = 1, h = 3)
      assertProduction(m = 6, s = 1, t = 1, p = 1, e = 1, h = 1)
      assertDashMiddle(
          played = 9,
          tr = 20,
          hand = 6,
      )
      assertTags(but = 1, sct = 2, eat = 1)
      assertDashRight(events = 0, tagless = 5, colonies = 0)
    }
    with(been) {
      assertResources(m = 41, s = 0, t = 0, p = 5, e = 0, h = 3)
      assertProduction(m = 10, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertDashMiddle(
          played = 6,
          tr = 23,
          hand = 5,
      )
      assertTags(but = 2, spt = 2, eat = 1, vet = 1, cit = 2)
      assertDashRight(events = 1, tagless = 1, colonies = 1)
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 3, venus = 4)

    been.doTasks(
        "-2 ProjectCard<Selecting>",
        "10 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    keen.turn {
      doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Energy")
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Merchant")
    }

    been.turn {
      doTasks(
          *playCardTasks("$ForcedPrecipitation"),
          "8 Pay<> FROM MC",
          "$ForcedPrecipitation FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$MiningColony"),
          "20 Pay<> FROM MC",
          "$MiningColony FROM ProjectCard",
          "PROD[Titanium]",
          "Colony<Titan>",
          "3 Floater<$ForcedPrecipitation>",
      )
    }

    keen.turn {
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "2 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$MarsUniversity FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Researcher")
    }

    been.turn {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
    }

    keen.turn {
      doTasks(*cardActionTasks("$DirectedHeatUsage"), "3 Pay<Class<Heat>> FROM Heat", "4 MC")
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // Keep the offered card.
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    been.doTask("Pass")

    keen.doTask("Pass")

    // Production.
    keen.doTasks("26 MC", "Energy", "Titanium", "Steel", "Plant", "Heat", "2 Heat FROM Energy")
    been.doTasks("33 MC", "2 Plant", "Titanium")

    keen.doTask("OxygenStep! BY Admin")
  }

  private fun generation4() {
    keen.doTask("4 ProjectCard<Selecting>")
    been.doTask("4 ProjectCard<Selecting>")
    // Screenshot evidence: generation-4-research.png, before either player's purchase.
    with(keen) {
      assertResources(m = 27, s = 1, t = 5, p = 3, e = 1, h = 3)
      assertProduction(m = 6, s = 1, t = 1, p = 1, e = 1, h = 1)
      assertDashMiddle(
          played = 10,
          tr = 20,
          hand = 6,
      )
      assertTags(but = 2, sct = 3, eat = 1)
      assertDashRight(events = 0, tagless = 5, colonies = 0)
    }
    with(been) {
      assertResources(m = 34, s = 0, t = 1, p = 7, e = 0, h = 3)
      assertProduction(m = 10, s = 0, t = 1, p = 2, e = 0, h = 0)
      assertDashMiddle(
          played = 8,
          tr = 23,
          hand = 5,
      )
      assertTags(but = 2, spt = 3, eat = 1, vet = 2, cit = 2)
      assertDashRight(events = 1, tagless = 1, colonies = 2)
    }
    assertSidebar(gen = 4, temp = -30, oxygen = 1, oceans = 3, venus = 4)

    been.doTasks(
        "-3 ProjectCard<Selecting>",
        "5 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )

    keen.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    been.turn {
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
          "ProjectCard",
      )
    }

    keen.turn {
      doTasks(*cardActionTasks("$DirectedHeatUsage"), "3 Pay<Class<Heat>> FROM Heat", "4 MC")
    }

    been.turn {
      doTasks(
          "UseAction<TradeAction, Action1>",
          "9 Pay<> FROM MC",
          "Trade<Ganymede>",
          "5 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_7>",
          "OxygenStep",
          "Plant",
          "ProjectCard",
          "TerraformRating",
      )
    }

    keen.turn {
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
          "MC",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$MiningRights"),
          "9 Pay<> FROM MC",
          "$MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Hellas_6_2>",
          "Production<Class<Metal>(HAS PlacementBonus<LandArea(HAS MiningRights_SpecialTile)>)>",
          "Titanium",
      )
    }

    keen.turn {
      doTasks(
          *playCardTasks("$ResearchCoordination"),
          "2 Pay<> FROM MC",
          "$ResearchCoordination FROM ProjectCard",
          "4 MC",
      )
    }

    been.turn {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "Ok", // No microbe tag on the revealed card.
          "-ProjectCard<Revealed>",
      )
    }

    been.doTask("Pass")

    keen.turn {
      doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Titanium")
    }
    keen.doTask("Pass")

    // Production.
    keen.doTasks("26 MC", "Energy", "Titanium", "Steel", "Plant", "Heat", "Heat FROM Energy")
    been.doTasks("36 MC", "2 Plant", "2 Titanium", "Energy")

    been.doTask("VenusStep! BY Admin")
  }

  private fun generation5() {
    keen.doTask("4 ProjectCard<Selecting>")
    been.doTask("4 ProjectCard<Selecting>")

    been.doTasks(
        "-2 ProjectCard<Selecting>",
        "10 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    keen.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    keen.turn {
      doTasks(
          "UseAction<TradeAction, Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Ceres>",
          "8 Steel",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$CorporateStronghold"),
          "11 Pay<> FROM MC",
          "$CorporateStronghold FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Hellas_3_5>",
          "2 MC",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Mayor")
    }

    keen.turn {
      doTasks(
          *playCardTasks("$FueledGenerators"),
          "$FueledGenerators FROM ProjectCard",
          "PROD[-MC]",
          "PROD[Energy]",
      )
    }

    been.turn {
      doTasks(
          "UseAction<TradeAction, Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Titan>",
          "Floater<$ExtractorBalloons>",
          "3 Floater<$ForcedPrecipitation>",
      )
    }

    keen.turn {
      doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Titanium")
    }

    been.turn {
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
    }

    keen.turn {
      doTasks(
          *playCardTasks("$LunarMining"),
          "9 Pay<> FROM MC",
          "$LunarMining FROM ProjectCard",
          "PROD[2 Titanium]",
          "MC",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$SisterPlanetSupport"),
          "7 Pay<> FROM MC",
          "$SisterPlanetSupport FROM ProjectCard",
          "PROD[3 MC]",
      )
    }

    keen.turn {
      doTasks(
          *playCardTasks("$CupolaCity"),
          "6 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // No MC remainder.
          "$CupolaCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Hellas_4_4>",
          "2 Steel",
      )
    }

    been.turn {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
    }

    been.doTask("Pass")

    keen.turn {
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "Ok", // No microbe tag on the revealed card.
          "-ProjectCard<Revealed>",
      )
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "9 Pay<> FROM MC",
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
    }
    keen.doTask("Pass")

    // Production.
    keen.doTasks("28 MC", "Energy", "3 Titanium", "Steel", "3 Plant", "3 Heat", "Heat FROM Energy")
    been.doTasks("43 MC", "2 Plant", "2 Titanium", "Heat FROM Energy")

    keen.doTask("OxygenStep! BY Admin")
  }

  private fun generation6() {
    keen.doTask("4 ProjectCard<Selecting>")
    been.doTask("4 ProjectCard<Selecting>")

    keen.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    been.doTasks(
        "-2 ProjectCard<Selecting>",
        "10 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    been.turn {
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "2 TerraformRating",
      )
    }

    keen.turn {
      doTasks(
          "UseAction<TradeAction, Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Io>",
          "13 Heat",
      )
    }

    been.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_2_5>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // Keep the offered card.
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    been.turn {
      doTasks(*playCardTasks("$EarthOffice"), "1 Pay<> FROM MC", "$EarthOffice FROM ProjectCard")
    }

    keen.turn {
      doTasks(
          *playCardTasks("$RestrictedArea"),
          "9 Pay<> FROM MC",
          "$RestrictedArea FROM ProjectCard",
          "MC",
          "RestrictedArea_SpecialTile<Hellas_3_6>",
          "2 Plant",
          "2 MC",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    been.turn {
      doTasks(*playCardTasks("$LunaGovernor"), "$LunaGovernor FROM ProjectCard", "PROD[2 MC]")
    }

    keen.turn {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
    }

    been.turn {
      doTasks(
          *playCardTasks("$SolarReflectors"),
          "Ok", // Pay no titanium.
          "23 Pay<> FROM MC",
          "$SolarReflectors FROM ProjectCard",
          "PROD[5 Heat]",
      )
    }

    keen.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_4_5>",
          "OxygenStep",
          "Steel",
          "4 MC",
          "TerraformRating",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$SpaceMirrors"),
          "Ok", // Pay no titanium.
          "3 Pay<> FROM MC",
          "$SpaceMirrors FROM ProjectCard",
      )
    }

    keen.turn {
      doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Titanium")
    }

    been.turn {
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
    }

    keen.turn {
      doTasks(
          *playCardTasks("$LightningHarvest"),
          "4 Pay<> FROM MC",
          "$LightningHarvest FROM ProjectCard",
          "PROD[Energy]",
          "PROD[MC]",
          "MC",
      )
    }

    been.doTask("Pass")

    keen.turn {
      doTasks(
          *playCardTasks("$OptimalAerobraking"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$OptimalAerobraking FROM ProjectCard",
          "MC",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }
    keen.doTask("Pass")

    // Production.
    keen.doTasks(
        "31 MC",
        "3 Energy",
        "3 Titanium",
        "Steel",
        "3 Plant",
        "3 Heat",
        "Heat FROM Energy",
    )
    been.doTasks("49 MC", "2 Plant", "2 Titanium", "5 Heat", "Energy")

    been.doTask("VenusStep! BY Admin")
  }

  private fun generation7() {
    keen.doTask("4 ProjectCard<Selecting>")
    been.doTask("4 ProjectCard<Selecting>")

    been.doTasks(
        "-1 ProjectCard<Selecting>",
        "15 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    keen.doTask("Ok") // Keep all four research cards.
    keen.doTasks("12 Pay<> FROM MC", "4 ProjectCard<Hand FROM Selecting>")

    keen.turn {
      doTasks(
          *playCardTasks("$RadSuits"),
          "2 Pay<> FROM MC",
          "$RadSuits FROM ProjectCard",
          "PROD[MC]",
          "4 MC",
      )
      doTasks(
          *playCardTasks("$WaterSplittingPlant"),
          "4 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // No MC remainder.
          "$WaterSplittingPlant FROM ProjectCard",
          "MC",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$VenusianAnimals"),
          "15 Pay<> FROM MC",
          "$VenusianAnimals FROM ProjectCard",
          "Animal<$VenusianAnimals>",
      )
      doTasks(
          *playCardTasks("$InventionContest"),
          "2 Pay<> FROM MC",
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
          "Animal<$VenusianAnimals>",
      )
    }

    keen.turn {
      doTasks(
          *playCardTasks("$GiantIceAsteroid"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "25 Pay<> FROM MC",
          "$GiantIceAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "3 MC",
          "3 Heat",
          "2 TerraformRating",
          "PROD[Heat]",
          "-4 Plant<Been>",
          "OceanTile<Hellas_4_7>",
          "TerraformRating",
          "Plant",
          "4 MC",
          "OceanTile<Hellas_7_3>",
          "TerraformRating",
          "2 Titanium",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$BusinessContacts"),
          "4 Pay<> FROM MC",
          "$BusinessContacts FROM ProjectCard",
          "4 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }

    keen.turn {
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
          "PROD[Heat]",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$CorroderSuits"),
          "8 Pay<> FROM MC",
          "$CorroderSuits FROM ProjectCard",
          "PROD[2 MC]",
          "Animal<$VenusianAnimals>",
      )
    }

    keen.turn {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
    }

    been.turn {
      doTasks(
          "UseAction<TradeAction, Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Ganymede>",
          "5 Plant",
      )
    }

    keen.turn {
      doTasks(
          *playCardTasks("$SterlingVents"),
          "1 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$SterlingVents FROM ProjectCard",
          "PROD[-2 Heat]",
          "PROD[2 Energy]",
      )
    }

    been.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$FloaterPrototypes"),
          "2 Pay<> FROM MC",
          "$FloaterPrototypes FROM ProjectCard",
          "Animal<$VenusianAnimals>",
          "2 Floater<$ForcedPrecipitation>",
      )
    }

    keen.turn {
      doTasks(
          *playCardTasks("$RadChemFactory"),
          "3 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // No MC remainder.
          "$RadChemFactory FROM ProjectCard",
          "PROD[-Energy]",
          "2 TerraformRating",
          "MC",
      )
    }

    been.turn {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // Keep the offered card.
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    been.turn {
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "Ok", // No microbe tag on the revealed card.
          "-ProjectCard<Revealed>",
      )
    }

    been.doTask("Pass")

    keen.doTask("Pass")

    // Production.
    keen.doTasks("41 MC", "4 Energy", "3 Titanium", "Steel", "3 Plant", "3 Heat")
    been.doTasks("53 MC", "2 Plant", "2 Titanium", "5 Heat", "Energy", "Heat FROM Energy")

    keen.doTask("VenusStep! BY Admin")
  }

  private fun generation8() {
    keen.doTask("4 ProjectCard<Selecting>")
    been.doTask("4 ProjectCard<Selecting>")
    // Screenshot evidence: generation-8-research.png, before either player's purchase.
    with(keen) {
      assertResources(m = 42, s = 2, t = 5, p = 8, e = 4, h = 5)
      assertProduction(m = 10, s = 1, t = 3, p = 3, e = 4, h = 3)
      assertDashMiddle(
          played = 24,
          tr = 31,
          hand = 7,
      )
      assertTags(but = 7, spt = 2, sct = 4, pot = 3, eat = 3, jot = 1, cit = 1)
      assertDashRight(events = 1, tagless = 7, colonies = 0)
    }
    with(been) {
      assertResources(m = 57, s = 0, t = 4, p = 7, e = 1, h = 7)
      assertProduction(m = 20, s = 0, t = 2, p = 2, e = 1, h = 5)
      assertDashMiddle(
          played = 20,
          tr = 33,
          hand = 6,
      )
      assertTags(but = 4, spt = 5, sct = 1, pot = 1, eat = 5, vet = 5, ant = 1, cit = 3)
      assertDashRight(events = 4, tagless = 1, colonies = 2)
    }
    assertSidebar(gen = 8, temp = -18, oxygen = 6, oceans = 5, venus = 22)

    been.doTasks(
        "-3 ProjectCard<Selecting>",
        "5 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )

    keen.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    been.turn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Hellas_2_4>",
          "OxygenStep",
          "Plant",
          "Steel",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_5>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    keen.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_5_4>",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Scientist")
    }

    been.turn {
      doTasks(
          "UseAction<TradeAction, Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Titan>",
          "Floater<$ExtractorBalloons>",
          "3 Floater<$ForcedPrecipitation>",
      )
      doTasks(
          *playCardTasks("$SubterraneanReservoir"),
          "11 Pay<> FROM MC",
          "$SubterraneanReservoir FROM ProjectCard",
          "OceanTile<Hellas_5_8>",
          "TerraformRating",
          "4 MC",
      )
    }

    keen.turn {
      doTasks(
          *playCardTasks("$Atmoscoop"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$Atmoscoop FROM ProjectCard",
          "2 Floater",
          "2 VenusStep",
          "2 TerraformRating",
      )
    }

    been.turn {
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
    }

    keen.turn {
      doTasks(*playCardTasks("$ProductiveOutpost"), "$ProductiveOutpost FROM ProjectCard", "4 MC")
    }

    been.turn {
      doTasks(
          *playCardTasks("$AstraMechanica"),
          "7 Pay<> FROM MC",
          "$AstraMechanica FROM ProjectCard",
          "Animal<$VenusianAnimals>",
          "ProjectCard FROM PlayedEvent<Class<$InventionContest>>",
          "ProjectCard FROM PlayedEvent<Class<$Flooding>>",
      )
      doTasks(
          *playCardTasks("$Flooding"),
          "7 Pay<> FROM MC",
          "$Flooding FROM ProjectCard",
          "OceanTile<Hellas_6_8>",
          "TerraformRating",
          "Steel",
          "4 MC",
      )
    }

    keen.turn {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
    }

    been.turn {
      doTasks(
          *playCardTasks("$InventionContest"),
          "2 Pay<> FROM MC",
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
          "Animal<$VenusianAnimals>",
      )
      doTasks(
          *playCardTasks("$DeepWellHeating"),
          "2 Pay<Class<Steel>> FROM Steel",
          "9 Pay<> FROM MC",
          "$DeepWellHeating FROM ProjectCard",
          "PROD[Energy]",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // Keep the offered card.
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }

    been.doTask("Pass")

    keen.turn {
      doTasks(
          *cardActionTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$MolecularPrinting"),
          "9 Pay<> FROM MC",
          "$MolecularPrinting FROM ProjectCard",
          "7 MC",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "Ok", // No microbe tag on the revealed card.
          "-ProjectCard<Revealed>",
      )
      doTasks(
          *playCardTasks("$Hackers"),
          "1 Pay<> FROM MC",
          "$Hackers FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "4 MC",
          "PROD[-2 MC<Been>]",
      )
      doTasks(
          *playCardTasks("$BreathingFilters"),
          "7 Pay<> FROM MC",
          "$BreathingFilters FROM ProjectCard",
          "MC",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$KelpFarming"),
          "13 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[3 Plant]",
          "2 Plant",
          "MC",
      )
    }
    keen.doTask("Pass")

    // Production.
    keen.doTasks(
        "49 MC",
        "3 Energy",
        "3 Titanium",
        "Steel",
        "6 Plant",
        "3 Heat",
        "Heat FROM Energy",
    )
    been.doTasks("59 MC", "2 Plant", "2 Titanium", "5 Heat", "2 Energy", "Heat FROM Energy")

    been.doTask("OceanTile<Hellas_6_7>! BY Admin")
  }

  private fun generation9() {
    keen.doTask("4 ProjectCard<Selecting>")
    been.doTask("4 ProjectCard<Selecting>")

    been.doTasks(
        "-2 ProjectCard<Selecting>",
        "10 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    keen.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    keen.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_5_5>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
      )
    }

    // Screenshot evidence: generation-9-actions.png, after Keen's opening turn and before Been's.
    with(keen) {
      assertResources(m = 48, s = 5, t = 3, p = 0, e = 0, h = 9)
      assertProduction(m = 14, s = 1, t = 3, p = 6, e = 3, h = 3)
      assertDashMiddle(
          played = 30,
          tr = 37,
          hand = 8,
      )
      assertTags(but = 7, spt = 3, sct = 6, pot = 3, eat = 3, jot = 2, plt = 1, cit = 1)
      assertDashRight(events = 1, tagless = 9, colonies = 0)
    }
    with(been) {
      assertResources(m = 50, s = 0, t = 3, p = 3, e = 2, h = 13)
      assertProduction(m = 18, s = 0, t = 2, p = 2, e = 2, h = 5)
      assertDashMiddle(
          played = 23,
          tr = 41,
          hand = 7,
      )
      assertTags(but = 5, spt = 5, sct = 2, pot = 2, eat = 5, vet = 5, ant = 1, cit = 3)
      assertDashRight(events = 5, tagless = 1, colonies = 2)
    }
    assertSidebar(gen = 9, temp = -14, oxygen = 12, oceans = 8, venus = 30)

    been.turn {
      doTasks(
          *playCardTasks("$RimFreighters"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$RimFreighters FROM ProjectCard",
      )
      doTasks(
          "UseAction<TradeAction, Action3>",
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Ceres>",
          "6 Steel",
      )
    }

    keen.turn {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
    }

    been.turn {
      doTasks(
          *playCardTasks("$UndergroundCity"),
          "6 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$UndergroundCity FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[2 Steel]",
          "CityTile<Hellas_1_4>",
          "Plant",
          "Steel",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Highlander")
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTasks(
          *playCardTasks("$AdvancedAlloys"),
          "7 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
          "MC",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$LunarBeam"),
          "10 Pay<> FROM MC",
          "$LunarBeam FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[2 Heat]",
          "PROD[2 Energy]",
      )
    }

    // The archive pays Solar Probe entirely in M€ despite Keen retaining usable titanium.
    keen.turn {
      doTasks(
          *playCardTasks("$SolarProbe"),
          "Ok", // Pay no titanium.
          "7 Pay<> FROM MC",
          "$SolarProbe FROM ProjectCard",
          "3 ProjectCard",
          "3 MC",
          "3 Heat",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$AerialMappers"),
          "11 Pay<> FROM MC",
          "$AerialMappers FROM ProjectCard",
      )
    }

    keen.turn {
      doTasks(
          *playCardTasks("$AsteroidCard"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "0 Pay<> FROM MC", // No MC remainder.
          "$AsteroidCard FROM ProjectCard",
          "TemperatureStep",
          "2 Titanium",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "-3 Plant<Been>",
      )
      doTasks(
          *playCardTasks("$IceMoonColony"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "13 Pay<> FROM MC",
          "$IceMoonColony FROM ProjectCard",
          "MC",
          "Colony<Ganymede>",
          "PROD[Plant]",
          "OceanTile<Hellas_2_1>",
          "TerraformRating",
          "2 Plant",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$UrbanDecomposers"),
          "6 Pay<> FROM MC",
          "$UrbanDecomposers FROM ProjectCard",
          "PROD[Plant]",
          "Ok", // No card can receive microbes.
      )
    }

    keen.turn {
      doTasks(*playCardTasks("$Harvest"), "$Harvest FROM ProjectCard", "12 MC")
    }

    been.turn {
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$AerialMappers>")
    }

    keen.turn {
      doTasks(
          *playCardTasks("$VenusMagnetizer"),
          "3 Pay<> FROM MC",
          "$VenusMagnetizer FROM ProjectCard",
          "MC",
      )
    }

    been.turn {
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "Ok", // No microbe tag on the revealed card.
          "-ProjectCard<Revealed>",
      )
    }

    been.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    keen.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    been.doTask("Pass")

    keen.turn {
      doTasks(
          *playCardTasks("$SpecialDesign"),
          "2 Pay<> FROM MC",
          "$SpecialDesign FROM ProjectCard",
          "SpecialDesign_NextCardEffect",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$Trees"),
          "9 Pay<> FROM MC",
          "$Trees FROM ProjectCard",
          "PROD[3 Plant]",
          "Plant",
          "MC",
      )
      doTasks(
          *playCardTasks("$Algae"),
          "6 Pay<> FROM MC",
          "$Algae FROM ProjectCard",
          "Plant",
          "PROD[2 Plant]",
          "MC",
      )
      doTasks(
          *playCardTasks("$LocalHeatTrapping"),
          "$LocalHeatTrapping FROM ProjectCard",
          "-5 Heat",
          "MC",
          "4 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_3>",
          "OxygenStep",
          "Steel",
          "TerraformRating",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Hellas_4_3>",
          "OxygenStep",
          "Steel",
          "TerraformRating",
      )
    }
    keen.doTask("Pass")

    // Production.
    keen.doTasks("56 MC", "3 Energy", "3 Titanium", "Steel", "12 Plant", "3 Heat")
    been.doTasks(
        "58 MC",
        "3 Plant",
        "2 Titanium",
        "7 Heat",
        "2 Heat FROM Energy",
        "2 Steel",
        "2 Energy",
    )

    keen.doTask("TemperatureStep! BY Admin")
  }

  private fun generation10() {
    keen.doTask("4 ProjectCard<Selecting>")
    been.doTask("4 ProjectCard<Selecting>")

    been.doTasks(
        "-3 ProjectCard<Selecting>",
        "5 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )

    keen.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    been.turn {
      doTasks(
          *playCardTasks("$FieldCappedCity"),
          "3 Pay<Class<Steel>> FROM Steel",
          "23 Pay<> FROM MC",
          "$FieldCappedCity FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[Energy]",
          "3 Plant",
          "CityTile<Hellas_2_3>",
          "Plant",
      )
      doTasks(
          *playCardTasks("$UrbanizedArea"),
          "10 Pay<> FROM MC",
          "$UrbanizedArea FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "CityTile<Hellas_3_4>",
          "Steel",
      )
    }

    keen.turn {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Io>",
          "8 Heat",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_6_5>",
          "0 OxygenStep", // Oxygen is already maxed.
      )
    }

    been.turn {
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
      doTasks(
          "UseAction<TradeAction, Action3>",
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Ganymede>",
          "5 Plant",
      )
      giveTask(keen, "Plant")
    }

    keen.turn {
      doTasks(
          *playCardTasks("$Capital"),
          "7 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$Capital FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[5 MC]",
          "CapitalTile<Hellas_6_6>",
          "4 MC",
      )
    }

    been.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_9_6>",
          "OxygenStep",
          "2 Heat",
      )
      doTasks(
          *playCardTasks("$EcologyResearch"),
          "21 Pay<> FROM MC",
          "$EcologyResearch FROM ProjectCard",
          "PROD[2 Plant]",
          "2 Microbe",
      )
      doTask("Animal<$VenusianAnimals>", VenusianAnimals) // Science-tag reward.
      doTask("Animal<$VenusianAnimals>") // Ecology Research.
    }

    keen.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    been.turn {
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
          "Ok", // No ocean tiles remain.
      )
    }

    keen.turn {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
    }

    been.turn {
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // Keep the offered card.
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$Grass"),
          "7 Pay<> FROM MC",
          "$Grass FROM ProjectCard",
          "PROD[Plant]",
          "3 Plant",
          "MC",
      )
    }

    been.doTask("Pass")

    keen.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "NormalCityTile<Hellas_3_2>",
          "Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_4_2>",
          "OxygenStep",
          "Plant",
      )
      doTasks(
          *playCardTasks("$PeroxidePower"),
          "1 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$PeroxidePower FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Energy]",
      )
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "Ok", // No microbe tag on the revealed card.
          "-ProjectCard<Revealed>",
      )
      doTasks(
          *playCardTasks("$LuxuryFoods"),
          "4 Pay<> FROM MC",
          "$LuxuryFoods FROM ProjectCard",
          "4 MC",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    keen.doTask("Pass")

    // Production.
    keen.doTasks("63 MC", "3 Energy", "3 Titanium", "Steel", "13 Plant", "3 Heat")
    been.doTasks(
        "64 MC",
        "5 Plant",
        "2 Titanium",
        "7 Heat",
        "2 Steel",
        "2 Energy",
        "2 Heat FROM Energy",
    )

    been.doTask("TemperatureStep! BY Admin")
  }

  private fun generation11() {
    keen.doTask("4 ProjectCard<Selecting>")
    been.doTask("4 ProjectCard<Selecting>")

    been.doTasks(
        "-3 ProjectCard<Selecting>",
        "5 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )

    keen.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    keen.turn {
      doTasks(
          *playCardTasks("$SubZeroSaltFish"),
          "1 Pay<> FROM MC",
          "$SubZeroSaltFish FROM ProjectCard",
          "MC",
          "PROD[-Plant<Been>]",
      )
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "3 Animal<$SubZeroSaltFish>",
      )
    }

    been.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Hellas_5_3>",
      )
    }

    keen.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "NormalCityTile<Hellas_7_5>",
      )
    }

    been.turn {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "2 Pay<Class<Energy>> FROM Energy",
          "Trade<Ganymede>",
          "4 Plant",
      )
      giveTask(keen, "Plant")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_3>",
          "OxygenStep",
          "2 Plant",
      )
    }

    keen.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_7_6>",
          "OxygenStep",
          "ProjectCard",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "NormalCityTile<Hellas_8_8>",
          "Titanium",
      )
    }

    been.turn {
      doTasks(
          *playCardTasks("$Plantation"),
          "15 Pay<> FROM MC",
          "$Plantation FROM ProjectCard",
          "GreeneryTile<Hellas_1_2>",
          "OxygenStep",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_2_2>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
      )
    }

    keen.turn {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_7_7>",
          "OxygenStep",
          "2 MC",
      )
    }

    been.turn {
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Investor")
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$AerialMappers>")
    }

    keen.turn {
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "1 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
    }

    been.turn {
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
    }

    keen.turn {
      doTasks(*cardActionTasks("$SubZeroSaltFish"), "Animal<$SubZeroSaltFish>")
    }

    been.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    keen.turn {
      doTasks(
          *playCardTasks("$Greenhouses"),
          "1 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$Greenhouses FROM ProjectCard",
          "12 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_7_8>",
          "OxygenStep",
          "4 MC",
      )
    }

    // The archive records Been passing as a second action; defer it to Been's next legal turn.
    been.doTask("Pass")

    keen.turn {
      doTasks(
          *playCardTasks("$NitrogenFromTitan"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "0 Pay<> FROM MC", // No MC remainder.
          "$NitrogenFromTitan FROM ProjectCard",
          "2 TerraformRating",
          "Ok", // No Jovian card can receive floaters.
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    keen.turn {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "Ok", // No microbe tag on the revealed card.
          "-ProjectCard<Revealed>",
      )
      doTasks(
          *cardActionTasks("$VenusMagnetizer"),
          "PROD[-Energy]",
          "0 VenusStep", // Venus is already maxed.
      )
    }
    keen.doTask("Pass")

    // Production.
    keen.doTasks("67 MC", "2 Energy", "3 Titanium", "Steel", "13 Plant", "3 Heat")
    been.doTasks("66 MC", "4 Plant", "2 Titanium", "7 Heat", "2 Steel", "2 Energy")

    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    with(keen) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_7_4>",
          "2 MC",
      )

      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_8_5>",
          "ProjectCard",
      )

      doTask("Ok") // decline final greenery
    }

    with(been) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_6_3>",
          "2 MC",
      )

      doTask("Ok") // decline final greenery
    }
    keen.assertCounts(1 to "ProjectCard")
    been.assertCounts(0 to "ProjectCard")

    keen.assertCounts(47 to "TerraformRating", 117 to "VictoryPoint", 1 to "Victory")
    been.assertCounts(45 to "TerraformRating", 99 to "VictoryPoint", 0 to "Victory")

    keen.assertResources(m = 90, s = 1, t = 4, p = 1, e = 2, h = 3)
    keen.assertProduction(m = 20, s = 1, t = 3, p = 13, e = 2, h = 3)
    been.assertResources(m = 71, s = 5, t = 4, p = 0, e = 2, h = 8)
    been.assertProduction(m = 21, s = 2, t = 2, p = 4, e = 2, h = 7)
  }
}
