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

// Source: _local/replays/Game20260811/ (game log, player records, and dashboard screenshots)
// Complete archive replay: Synthetic Proton Fragment (g9ea8656f1c7e)
// https://terraforming-mars.herokuapp.com/the-end?id=p9d6d3ff25b39
internal class SyntheticProtonFragmentTest : FullGameTest() {
  // Player-record evidence: Hellas, Corporate Era, Prelude, promo cards, drafting, fast mode,
  // three players, no Venus/Colonies/Turmoil, and these full-random milestone and award pools.
  override val config =
      GameConfig(
          """
          HellasMap
          PreludeExpansion, promos:cards

          Mayor, Diversifier, Trader, Sponsor, Tycoon
          Biologist, SpaceBaron, Forecaster, Botanist, Collector
          """
      )
  // Player-record evidence: Purple has a five-TR handicap.
  override val playerClassPets =
      """
      CLASS Player1 : Player { SetupPhase: 5 TerraformRating<This> }
      CLASS Player2 : Player
      CLASS Player3 : Player
      """
          .trimIndent()

  private val purple
    get() = p1

  private val pink
    get() = p2

  private val green
    get() = p3

  @Test
  internal fun game20260811() {
    startGame()
    purple.doTasks("25 TerraformRating")
    pink.doTask("20 TerraformRating")
    green.doTask("20 TerraformRating")
    purple.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    pink.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    green.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    purple.doTask("-6 ProjectCard<Selecting>")
    pink.doTask("-5 ProjectCard<Selecting>")
    green.doTask("-3 ProjectCard<Selecting>")

    admin.assertCounts(1 to "Generation")
    generation1()
    generation2()
    generation3()
    generation4()
    generation5()
    generation6()
    generation7()
    generation8()
    generation9()

    // _local/replays/Game20260811/Game20260811-herokuapp-player.json; final Mars map.
    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -              [G1]  [G1]  [S1]  [G3]  [C1]
        |
        | 2 -           [O]   [S2]  [S1]  [C3]  [G3]  [G1]
        |
        | 3 -        [O]   [G3]  [G1]   LS   [G3]  [G1]  [S1]
        |
        | 4 -     [O]   [G3]  [C3]  [G1]  [C1]  [O]   [O]    LP
        |
        | 5 -  [S2]   L    [G3]  LSS   [G1]  [O]   [O]   [O]    LP
        |
        | 6 -      LT    L     LS   [C1]  [G1]   W     WS    L
        |
        | 7 -        [O]    L     L    [G1]  [C2]  [G2]   LT
        |
        | 8 -            LS    LC   LHH   [G2]  [S2]   LT
        |
        | 9 -               L    LHH   LO6   LHH    L
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
  }

  private fun generation1() {
    with(purple) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Recyclon>>",
          "$Recyclon FROM CorporationCard",
          "38 MC",
          "PROD[Steel]",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
          "Microbe<$Recyclon>",
      )
    }

    with(pink) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$RobinsonIndustries>>",
          "$RobinsonIndustries FROM CorporationCard",
          "47 MC",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(green) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$SpliceTacticalGenomics>>",
          "$SpliceTacticalGenomics FROM CorporationCard",
          "44 MC",
      )
      doTask("2 MC", SpliceTacticalGenomics)
      doTasks("21 Pay<> FROM MC", "7 ProjectCard<Hand FROM Selecting>", "2 MC")
    }

    with(purple) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AlbedoPlants>>",
          "$AlbedoPlants FROM PreludeCard",
          "PROD[Plant]",
          "Plant",
          "3 Heat",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SocietySupport>>",
          "$SocietySupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[Plant]",
          "PROD[Energy]",
          "PROD[Heat]",
      )
    }

    with(pink) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
          "$MetalsCompany FROM PreludeCard",
          "PROD[MC]",
          "PROD[Steel]",
          "PROD[Titanium]",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Biolab>>",
          "$Biolab FROM PreludeCard",
          "PROD[Plant]",
          "3 ProjectCard",
      )
    }

    with(green) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Merger>>",
          "$Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
          "PlayCard<Class<CorporationCard>, Class<$Inventrix>>",
          "$Inventrix FROM CorporationCard<Selecting>",
          "45 MC",
          "-42 MC",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MoholeExcavation>>",
          "$MoholeExcavation FROM PreludeCard",
          "PROD[Steel]",
          "PROD[2 Heat]",
          "2 Heat",
      )
    }

    purple.turn {
      doTasks(
          *playCardTasks("$DeepWellHeating"),
          "13 Pay<> FROM MC",
          "$DeepWellHeating FROM ProjectCard",
          "PROD[Energy]",
          "TemperatureStep",
          "Microbe<$Recyclon>",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC")
      doTask("PROD[Heat]")
    }

    green.turn {
      doTask("DoRequiredActions")
      doTask("SearchForCard<TagFilter<Class<MicrobeTag>>>")
      doTask("3 ProjectCard")
      doTasks(
          *playCardTasks("$Lichen"),
          "7 Pay<> FROM MC",
          "$Lichen FROM ProjectCard",
          "PROD[Plant]",
      )
    }

    // (Purple already passed early)
    purple.doTask("Pass")

    pink.doTask("Pass")

    green.turn {
      doTasks(
          *playCardTasks("$SolarLogistics"),
          "20 Pay<> FROM MC",
          "$SolarLogistics FROM ProjectCard",
          "2 Titanium",
      )
    }
    green.doTask("Pass")

    // Production.
    purple.doTasks("25 MC", "Steel", "2 Plant", "2 Energy", "Heat")
    pink.doTasks("21 MC", "Steel", "Titanium", "Plant", "Heat")
    green.doTasks("20 MC", "Steel", "2 Heat", "Plant")
  }

  private fun generation2() {
    purple.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // Game20260811-dashboards-gen2.png was taken before cards were bought.
    purple.assertResources(m = 38, s = 1, t = 0, p = 3, e = 2, h = 4)
    purple.assertProduction(m = -1, s = 1, t = 0, p = 2, e = 2, h = 1)
    pink.assertResources(m = 26, s = 1, t = 1, p = 1, e = 0, h = 1)
    pink.assertProduction(m = 1, s = 1, t = 1, p = 1, e = 0, h = 1)
    green.assertResources(m = 23, s = 1, t = 2, p = 1, e = 0, h = 4)
    green.assertProduction(m = 0, s = 1, t = 0, p = 1, e = 0, h = 2)
    pink.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    pink.turn {
      doTasks(*playCardTasks("$MediaGroup"), "4 Pay<> FROM MC", "$MediaGroup FROM ProjectCard")
      doTasks(*playCardTasks("$Sabotage"), "$Sabotage FROM ProjectCard", "3 MC", "-7 MC<Player1>")
    }

    green.turn {
      doTasks(
          *playCardTasks("$AsteroidRights"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$AsteroidRights FROM ProjectCard",
          "2 Asteroid<$AsteroidRights>",
      )
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
    }

    purple.turn {
      doTasks(
          *playCardTasks("$ReleaseOfInertGases"),
          "14 Pay<> FROM MC",
          "$ReleaseOfInertGases FROM ProjectCard",
          "2 TerraformRating",
      )
      doTasks(
          *playCardTasks("$MagneticFieldDome"),
          "1 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$MagneticFieldDome FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[Plant]",
          "TerraformRating",
          "-2 Microbe<$Recyclon>",
          "PROD[Plant]",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "6 Pay<> FROM MC",
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$HiredRaiders"),
          "$HiredRaiders FROM ProjectCard",
          "3 MC",
          "3 MC FROM MC<Player3>",
      )
    }

    green.turn {
      doTasks(
          *playCardTasks("$Archaebacteria"),
          "6 Pay<> FROM MC",
          "$Archaebacteria FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("2 MC", SpliceTacticalGenomics)
      doTask("2 MC")
    }

    purple.doTask("Pass")

    pink.turn {
      doTasks(
          *playCardTasks("$LagrangeObservatory"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$LagrangeObservatory FROM ProjectCard",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Titanium]")
    }

    // (Green already passed early)
    green.doTask("Pass")

    pink.turn {
      doTasks(
          *playCardTasks("$MiningRights"),
          "1 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Hellas_8_8>",
          "PROD[Titanium]",
          "Titanium",
      )
      doTasks(
          *playCardTasks("$DirectedImpactors"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$DirectedImpactors FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "6 Pay<> FROM MC",
          "Asteroid<$DirectedImpactors>",
      )
    }
    pink.doTask("Pass")

    // Production.
    purple.doTasks("28 MC", "Steel", "4 Plant", "Heat", "2 Heat FROM Energy")
    pink.doTasks("21 MC", "Steel", "3 Titanium", "Plant", "Heat", "Energy")
    green.doTasks("20 MC", "Steel", "2 Heat", "2 Plant")
  }

  private fun generation3() {
    purple.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // Game20260811-dashboards-gen3.png was taken before cards were bought.
    purple.assertResources(m = 36, s = 1, t = 0, p = 7, e = 0, h = 7)
    purple.assertProduction(m = -1, s = 1, t = 0, p = 4, e = 0, h = 1)
    pink.assertResources(m = 24, s = 1, t = 3, p = 2, e = 1, h = 2)
    pink.assertProduction(m = 1, s = 1, t = 3, p = 1, e = 1, h = 1)
    green.assertResources(m = 27, s = 2, t = 2, p = 3, e = 0, h = 6)
    green.assertProduction(m = 0, s = 1, t = 0, p = 2, e = 0, h = 2)
    purple.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    green.turn {
      doTasks(
          *playCardTasks("$FueledGenerators"),
          "Ok", // Pay no steel.
          "1 Pay<> FROM MC",
          "$FueledGenerators FROM ProjectCard",
          "PROD[-MC]",
          "PROD[Energy]",
      )
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
    }

    purple.turn {
      doTasks(
          *playCardTasks("$PeroxidePower"),
          "1 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$PeroxidePower FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Energy]",
          "Microbe<$Recyclon>",
      )
      doTasks(
          *playCardTasks("$Hospitals"),
          "8 Pay<> FROM MC",
          "$Hospitals FROM ProjectCard",
          "PROD[-Energy]",
          "Microbe<$Recyclon>",
      )
    }

    pink.turn {
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Steel]")
      doTasks(
          *playCardTasks("$GhgFactories"),
          "1 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$GhgFactories FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 Heat]",
      )
    }

    green.turn {
      doTasks(
          *playCardTasks("$CorporateStronghold"),
          "2 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$CorporateStronghold FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Hellas_2_4>",
      )
      giveTask(purple, "Disease<$Hospitals>")
      doTasks("Plant", "Steel")
      purple.assertCounts(7 to "Plant")
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Diversifier")
    }

    purple.turn {
      doTasks(
          *playCardTasks("$IndustrialCenter"),
          "4 Pay<> FROM MC",
          "$IndustrialCenter FROM ProjectCard",
          "IndustrialCenter_SpecialTile<Hellas_2_3>",
          "Plant",
          "-2 Microbe<$Recyclon>",
          "PROD[Plant]",
      )
      purple.assertCounts(8 to "Plant")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_2>",
          "OxygenStep",
          "2 Plant",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "0 Pay<> FROM MC", // No MC remainder.
          "Asteroid<$DirectedImpactors>",
      )
    }

    green.doTask("Pass")

    purple.turn {
      doTasks(
          *playCardTasks("$EcologicalZone"),
          "12 Pay<> FROM MC",
          "$EcologicalZone FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "Animal<$EcologicalZone>",
          "3 Heat",
          "EcologicalZone_SpecialTile<Hellas_1_3>",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    // (Pink already passed early)
    pink.doTask("Pass")

    purple.doTask("Pass")

    // Production.
    purple.doTasks("29 MC", "Steel", "5 Plant", "Heat", "Energy")
    pink.doTasks("21 MC", "2 Steel", "3 Titanium", "Plant", "5 Heat", "Heat FROM Energy")
    green.doTasks("22 MC", "Steel", "2 Heat", "2 Plant")
  }

  private fun generation4() {
    purple.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // Game20260811-dashboards-gen4.png was taken before cards were bought.
    purple.assertResources(m = 30, s = 1, t = 0, p = 9, e = 1, h = 3)
    purple.assertProduction(m = -2, s = 1, t = 0, p = 5, e = 1, h = 1)
    pink.assertResources(m = 25, s = 2, t = 4, p = 3, e = 0, h = 8)
    pink.assertProduction(m = 1, s = 2, t = 3, p = 1, e = 0, h = 5)
    green.assertResources(m = 24, s = 2, t = 4, p = 6, e = 0, h = 8)
    green.assertProduction(m = 2, s = 1, t = 0, p = 2, e = 0, h = 2)
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    purple.turn {
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Hellas_2_1>",
          "TerraformRating",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_3>",
          "OxygenStep",
          "Steel",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      // Source log: six cards revealed and 6 M€ gained.
      doTasks(
          *playCardTasks("$PublicPlans"),
          "5 Pay<> FROM MC",
          "$PublicPlans FROM ProjectCard",
          "3 MC",
          "6 ProjectCard<Revealed FROM Hand>",
          "6 ProjectCard<Hand FROM Revealed>",
          "6 MC",
      )
    }

    green.turn {
      doTasks(
          *playCardTasks("$HeatTrappers"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$HeatTrappers FROM ProjectCard",
          "PROD[Energy]",
      )
      doTask("PROD[-2 Heat<Player2>]")
      doTasks(
          *playCardTasks("$RoboticWorkforce"),
          "9 Pay<> FROM MC",
          "$RoboticWorkforce FROM ProjectCard",
          "CopyProductionBox<$HeatTrappers>",
          "PROD[Energy]",
          "PROD[-2 Heat<Player2>]",
      )
    }

    purple.turn {
      doTasks(
          *playCardTasks("$NaturalPreserve"),
          "2 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$NaturalPreserve FROM ProjectCard",
          "PROD[MC]",
          "Microbe<$Recyclon>",
          "NaturalPreserve_SpecialTile<Hellas_3_7>",
          "Plant",
          "ProjectCard",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    pink.turn {
      doTasks(
          *playCardTasks("$ProtectedGrowth"),
          "$ProtectedGrowth FROM ProjectCard",
          "Plant",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$Soletta"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "21 Pay<> FROM MC",
          "$Soletta FROM ProjectCard",
          "PROD[7 Heat]",
      )
    }

    green.turn {
      doTasks(*cardActionTasks("$AsteroidRights"), "Pay<> FROM MC", "Asteroid<$AsteroidRights>")
      // Mercurian Alloys makes Green's retained titanium worth more after this payment; the
      // next authoritative dashboard confirms that he retained all four.
      doTasks(
          *playCardTasks("$MercurianAlloys"),
          "Ok", // Pay no titanium.
          "3 Pay<> FROM MC",
          "$MercurianAlloys FROM ProjectCard",
      )
    }

    purple.turn {
      doTasks(*cardActionTasks("$Hospitals"), "-Disease<$Hospitals>", "MC")
      doTasks(
          *playCardTasks("$Psychrophiles"),
          "2 Pay<> FROM MC",
          "$Psychrophiles FROM ProjectCard",
      )
      giveTask(green, "2 MC")
      doTask("Microbe<$Psychrophiles>")
    }

    pink.turn {
      doTasks(
          *playCardTasks("$Mine"),
          "1 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // No MC remainder.
          "$Mine FROM ProjectCard",
          "PROD[Steel]",
      )
    }

    green.doTask("Pass")

    purple.turn {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
    }

    // (Pink already passed early)
    pink.doTask("Pass")

    purple.doTask("Pass")

    // Production.
    purple.doTasks("32 MC", "Steel", "5 Plant", "Heat", "Energy", "Heat FROM Energy")
    pink.doTasks("22 MC", "3 Steel", "3 Titanium", "Plant", "9 Heat")
    green.doTasks("22 MC", "Steel", "2 Heat", "2 Plant", "2 Energy")
  }

  private fun generation5() {
    purple.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // Game20260811-dashboards-gen5.png was taken before cards were bought.
    purple.assertResources(m = 33, s = 1, t = 0, p = 9, e = 1, h = 5)
    purple.assertProduction(m = -1, s = 1, t = 0, p = 5, e = 1, h = 1)
    pink.assertResources(m = 24, s = 4, t = 3, p = 5, e = 0, h = 9)
    pink.assertProduction(m = 1, s = 3, t = 3, p = 1, e = 0, h = 9)
    green.assertResources(m = 27, s = 1, t = 4, p = 8, e = 2, h = 10)
    green.assertProduction(m = 2, s = 1, t = 0, p = 2, e = 2, h = 2)
    purple.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    pink.turn {
      doTasks(
          *playCardTasks("$HermeticOrderOfMars"),
          "8 Pay<> FROM MC",
          "$HermeticOrderOfMars FROM ProjectCard",
          "PROD[2 MC]",
          "6 MC",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Tycoon")
    }

    green.turn {
      doTasks(*playCardTasks("$Ants"), "9 Pay<> FROM MC", "$Ants FROM ProjectCard")
      doTask("2 MC", SpliceTacticalGenomics)
      doTask("2 MC")
      doTasks(*cardActionTasks("$Ants"), "-Microbe<Player1, $Recyclon<Player1>>", "Microbe<$Ants>")
    }

    purple.turn {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }

    pink.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$DirectedImpactors", "Action2"),
          "-Asteroid<$DirectedImpactors>",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
    }

    green.turn {
      doTasks(
          *playCardTasks("$WeatherBalloons"),
          "11 Pay<> FROM MC",
          "$WeatherBalloons FROM ProjectCard",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("$WeatherBalloons"), "Floater<$WeatherBalloons>")
    }

    purple.turn {
      doTasks(
          *playCardTasks("$TollStation"),
          "12 Pay<> FROM MC",
          "$TollStation FROM ProjectCard",
          "PROD[7 MC]",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_4_4>",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "5 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
          "3 MC",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    green.turn {
      doTasks(
          *playCardTasks("$DiversitySupport"),
          "1 Pay<> FROM MC",
          "$DiversitySupport FROM ProjectCard",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
    }

    purple.turn {
      doTasks(*cardActionTasks("$IndustrialCenter"), "7 Pay<> FROM MC", "PROD[Steel]")
    }

    pink.turn {
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Energy]")
    }

    green.turn {
      doTasks(
          *playCardTasks("$CallistoPenalMines"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "0 Pay<> FROM MC", // No MC remainder.
          "$CallistoPenalMines FROM ProjectCard",
          "PROD[3 MC]",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    // (Purple already passed early)
    purple.doTask("Pass")

    // (Pink already passed early)
    pink.doTask("Pass")

    green.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_4>",
          "OxygenStep",
          "Plant",
          "Steel",
          "TerraformRating",
      )
    }
    green.doTask("Pass")

    // Production.
    purple.doTasks("40 MC", "2 Steel", "5 Plant", "Heat", "2 Energy", "Heat FROM Energy")
    pink.doTasks("28 MC", "3 Steel", "3 Titanium", "Plant", "10 Heat", "Energy")
    green.doTasks("28 MC", "Steel", "2 Heat", "2 Plant", "2 Energy", "2 Heat FROM Energy")
  }

  private fun generation6() {
    purple.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // Game20260811-dashboards-gen6.png was taken before cards were bought.
    purple.assertResources(m = 40, s = 5, t = 0, p = 6, e = 2, h = 7)
    purple.assertProduction(m = 6, s = 2, t = 0, p = 5, e = 2, h = 1)
    pink.assertResources(m = 28, s = 7, t = 6, p = 6, e = 1, h = 11)
    pink.assertProduction(m = 3, s = 3, t = 3, p = 1, e = 1, h = 10)
    green.assertResources(m = 29, s = 3, t = 0, p = 3, e = 2, h = 6)
    green.assertProduction(m = 5, s = 1, t = 0, p = 2, e = 2, h = 2)
    purple.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    green.turn {
      doTasks(
          *playCardTasks("$CupolaCity"),
          "3 Pay<Class<Steel>> FROM Steel",
          "10 Pay<> FROM MC",
          "$CupolaCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Hellas_4_3>",
      )
      giveTask(purple, "Disease<$Hospitals>")
      doTask("Steel")
      doTasks(
          *cardActionTasks("$Ants"),
          "-Microbe<Player1, $Psychrophiles<Player1>>",
          "Microbe<$Ants>",
      )
    }

    purple.turn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Trader")
      doTasks(
          *playCardTasks("$ProtectedValley"),
          "2 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>",
          "5 Pay<Class<Steel>> FROM Steel",
          "9 Pay<> FROM MC",
          "$ProtectedValley FROM ProjectCard",
          "PROD[2 MC]",
          "Animal<$EcologicalZone>",
          "3 Heat",
          "Microbe<$Recyclon>",
          "GreeneryTile<Hellas_1_1>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$LavaFlows"),
          "16 Pay<> FROM MC",
          "$LavaFlows FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "3 MC",
          "2 TerraformRating",
          "LavaFlows_SpecialTile<Hellas_2_2>",
          "2 Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_8_7>",
          "OxygenStep",
          "2 Heat",
          "TerraformRating",
      )
    }

    green.turn {
      doTasks(*cardActionTasks("$AsteroidRights"), "Pay<> FROM MC", "Asteroid<$AsteroidRights>")
      doTasks(
          *playCardTasks("$Hackers"),
          "3 Pay<> FROM MC",
          "$Hackers FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "PROD[-2 MC<Player2>]",
      )
    }

    purple.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Hellas_4_7>",
          "TerraformRating",
          "Plant",
      )
    }

    pink.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Plant]")
    }

    green.turn {
      // Viral Enhancers reacts to its own tag.
      doTasks(
          *playCardTasks("$ViralEnhancers"),
          "9 Pay<> FROM MC",
          "$ViralEnhancers FROM ProjectCard",
      )
      doTask("2 MC", SpliceTacticalGenomics)
      doTasks("Plant", "2 MC")
      doTasks(*cardActionTasks("$WeatherBalloons", "Action2"), "-Floater<$WeatherBalloons>", "2 MC")
    }

    purple.turn {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_6>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *cardActionTasks("$DirectedImpactors", "Action2"),
          "-Asteroid<$DirectedImpactors>",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$RadChemFactory"),
          "3 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // No MC remainder.
          "$RadChemFactory FROM ProjectCard",
          "PROD[-Energy]",
          "2 TerraformRating",
      )
    }

    green.doTask("Pass")

    purple.doTask("Pass")

    pink.doTask("Pass")

    // Production.
    purple.doTasks("46 MC", "2 Steel", "5 Plant", "Heat", "2 Energy", "2 Heat FROM Energy")
    pink.doTasks("33 MC", "3 Steel", "3 Titanium", "2 Plant", "10 Heat", "Heat FROM Energy")
    green.doTasks("33 MC", "Steel", "2 Heat", "2 Plant", "2 Heat FROM Energy")
  }

  private fun generation7() {
    purple.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // We have no screencap for generation 7, so these only assert what the actual values happen to
    // be.
    purple.assertResources(m = 49, s = 2, t = 0, p = 8, e = 2, h = 5)
    purple.assertProduction(m = 8, s = 2, t = 0, p = 5, e = 2, h = 1)
    pink.assertResources(m = 37, s = 7, t = 9, p = 2, e = 0, h = 16)
    pink.assertProduction(m = 1, s = 3, t = 3, p = 2, e = 0, h = 10)
    green.assertResources(m = 39, s = 2, t = 0, p = 6, e = 0, h = 10)
    green.assertProduction(m = 10, s = 1, t = 0, p = 2, e = 0, h = 2)
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    purple.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Hellas_4_5>",
          "Disease<$Hospitals>",
          "Steel",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_5_5>",
          "OxygenStep",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Energy]")
      doTasks(
          *playCardTasks("$DuskLaserMining"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "0 Pay<> FROM MC", // No MC remainder.
          "$DuskLaserMining FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "4 Titanium",
      )
    }

    green.turn {
      doTasks(*cardActionTasks("$Ants"), "-Microbe<Player1, $Recyclon<Player1>>", "Microbe<$Ants>")
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
    }

    purple.turn {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTasks(
          *playCardTasks("$BioPrintingFacility"),
          "3 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$BioPrintingFacility FROM ProjectCard",
          "Microbe<$Recyclon>",
      )
    }

    pink.turn {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "SpaceBaron")
      doTasks(
          *playCardTasks("$AsteroidCard"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "0 Pay<> FROM MC", // No MC remainder.
          "$AsteroidCard FROM ProjectCard",
          "TemperatureStep",
          "2 Titanium",
      )
      giveTask(green, "ProjectCard")
      doTasks("3 MC", "TerraformRating", "-3 Plant<Player3>")
    }

    green.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Hellas_3_1>",
          "TerraformRating",
          "Plant",
          "2 MC",
      )
    }

    purple.turn {
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "Animal<$EcologicalZone>",
      )
      doTasks(
          *playCardTasks("$ImportedNutrients"),
          "14 Pay<> FROM MC",
          "$ImportedNutrients FROM ProjectCard",
          "4 Plant",
      )
      giveTask(green, "ProjectCard")
      doTask("4 Microbe<$Recyclon>")
    }

    pink.turn {
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

    green.turn {
      doTasks(
          *playCardTasks("$SterlingVents"),
          "2 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$SterlingVents FROM ProjectCard",
          "PROD[-2 Heat]",
          "PROD[2 Energy]",
      )
      doTasks(*playCardTasks("$Algae"), "10 Pay<> FROM MC", "$Algae FROM ProjectCard")
      doTask("Plant", Algae)
      doTasks("PROD[2 Plant]", "Plant")
    }

    purple.turn {
      doTasks(
          *playCardTasks("$Supercapacitors"),
          "4 Pay<> FROM MC",
          "$Supercapacitors FROM ProjectCard",
          "PROD[MC]",
          "-2 Microbe<$Recyclon>",
          "PROD[Plant]",
      )
      doTasks(*cardActionTasks("$Hospitals"), "-Disease<$Hospitals>", "3 MC")
    }

    pink.turn {
      doTasks(
          *playCardTasks("$IoMiningIndustries"),
          "9 Pay<Class<Titanium>> FROM Titanium",
          "12 Pay<> FROM MC",
          "$IoMiningIndustries FROM ProjectCard",
          "PROD[2 Titanium]",
          "PROD[2 MC]",
      )
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "6 Pay<> FROM MC",
          "Asteroid<$DirectedImpactors>",
      )
    }

    green.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$Grass"),
          "11 Pay<> FROM MC",
          "$Grass FROM ProjectCard",
          "PROD[Plant]",
          "3 Plant",
          "Plant", // choose Viral Enhancers' plant
      )
    }

    purple.doTask("Pass")

    pink.doTask("Pass")

    green.turn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_2>",
          "OxygenStep",
          "Plant",
          "4 MC",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$WeatherBalloons"), "Floater<$WeatherBalloons>")
    }
    green.doTask("Pass")

    // Production.
    purple.doTasks("50 MC", "2 Steel", "6 Plant", "Heat", "2 Energy")
    pink.doTasks("38 MC", "3 Steel", "6 Titanium", "2 Plant", "10 Heat")
    green.doTasks("37 MC", "Steel", "5 Plant", "2 Energy")
  }

  private fun generation8() {
    purple.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // Game20260811-dashboards-gen8.png was taken before cards were bought.
    purple.assertResources(m = 55, s = 2, t = 0, p = 10, e = 2, h = 6)
    purple.assertProduction(m = 10, s = 2, t = 0, p = 6, e = 2, h = 1)
    pink.assertResources(m = 39, s = 10, t = 6, p = 4, e = 0, h = 10)
    pink.assertProduction(m = 3, s = 3, t = 6, p = 2, e = 0, h = 10)
    green.assertResources(m = 41, s = 1, t = 2, p = 8, e = 2, h = 2)
    green.assertProduction(m = 10, s = 1, t = 0, p = 5, e = 2, h = 0)
    green.assertCounts(9 to "ProjectCard")
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    pink.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$DirectedImpactors", "Action2"),
          "-Asteroid<$DirectedImpactors>",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    green.turn {
      doTasks(*cardActionTasks("$Ants"), "-Microbe<Player1, $Recyclon<Player1>>", "Microbe<$Ants>")
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Forecaster")
    }

    purple.turn {
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Hellas_5_6>",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_6_6>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$LakeMarineris"),
          "16 Pay<> FROM MC",
          "$LakeMarineris FROM ProjectCard",
          "OceanTile<Hellas_4_6>",
          "TerraformRating",
          "Plant",
          "4 MC",
          "OceanTile<Hellas_5_7>",
          "TerraformRating",
          "3 Heat",
          "6 MC",
      )
      doTasks(
          *playCardTasks("$MiningExpedition"),
          "10 Pay<> FROM MC",
          "$MiningExpedition FROM ProjectCard",
          "OxygenStep",
          "2 Steel",
          "3 MC",
          "TerraformRating",
          "-2 Plant<Player3>",
      )
    }

    green.turn {
      doTasks(
          *playCardTasks("$IceAsteroid"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "15 Pay<> FROM MC",
          "$IceAsteroid FROM ProjectCard",
          "ProjectCard",
          "OceanTile<Hellas_7_3>",
          "TerraformRating",
          "2 Titanium",
          "OceanTile<Hellas_4_1>",
          "TerraformRating",
          "Plant",
          "2 MC",
      )
      doTasks(*cardActionTasks("$AsteroidRights"), "Pay<> FROM MC", "Asteroid<$AsteroidRights>")
    }

    purple.turn {
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "Animal<$EcologicalZone>",
      )
      doTasks(
          *playCardTasks("$FieldCappedCity"),
          "2 Pay<Class<Steel>> FROM Steel",
          "25 Pay<> FROM MC",
          "$FieldCappedCity FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[Energy]",
          "3 Plant",
          "CityTile<Hellas_6_5>",
          "Disease<$Hospitals>",
          "-2 Microbe<$Recyclon>",
          "PROD[Plant]",
      )
    }

    pink.turn {
      doTasks(
          *playCardTasks("$ConvoyFromEuropa"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$ConvoyFromEuropa FROM ProjectCard",
          "ProjectCard",
      )
      giveTask(green, "ProjectCard")
      doTasks("3 MC", "OceanTile<Hellas_5_8>", "TerraformRating", "4 MC")
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
      )
    }

    green.turn {
      doTasks(*cardActionTasks("$WeatherBalloons", "Action2"), "-Floater<$WeatherBalloons>", "4 MC")
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    purple.turn {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTasks(*cardActionTasks("$Hospitals"), "-Disease<$Hospitals>", "4 MC")
    }

    pink.turn {
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Energy]")
      doTasks(
          *playCardTasks("$SaturnSurfing"),
          "11 Pay<> FROM MC",
          "$SaturnSurfing FROM ProjectCard",
          "3 Floater<$SaturnSurfing>",
      )
    }

    green.turn {
      doTasks(
          *playCardTasks("$RobotPollinators"),
          "9 Pay<> FROM MC",
          "$RobotPollinators FROM ProjectCard",
          "PROD[Plant]",
          "3 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_4_2>",
          "OxygenStep",
          "Plant",
          "4 MC",
          "TerraformRating",
      )
    }

    purple.turn {
      doTasks(
          *playCardTasks("$InventionContest"),
          "2 Pay<> FROM MC",
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$ProjectInspection"),
          "$ProjectInspection FROM ProjectCard",
          "UseAction<$Hospitals, Action1>",
          "-Disease<$Hospitals>",
          "4 MC",
      )
    }

    pink.turn {
      doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "3 MC")
    }

    green.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$EquatorialMagnetizer"),
          "1 Pay<Class<Steel>> FROM Steel",
          "9 Pay<> FROM MC",
          "$EquatorialMagnetizer FROM ProjectCard",
      )
    }

    purple.turn {
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "3 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Player3>]",
      )
    }

    // (Pink already passed early)
    pink.doTask("Pass")

    green.turn {
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
    }

    // (Purple already passed early)
    purple.doTask("Pass")

    // (Green already passed early)
    green.doTask("Pass")

    // Production.
    purple.doTasks("54 MC", "2 Steel", "7 Plant", "Heat", "4 Energy")
    pink.doTasks("44 MC", "3 Steel", "7 Titanium", "2 Plant", "10 Heat", "Energy")
    green.doTasks("41 MC", "Steel", "6 Plant", "2 Heat FROM Energy")
  }

  private fun generation9() {
    purple.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // Game20260811-dashboards-gen9.png was taken before cards were bought.
    purple.assertResources(m = 62, s = 2, t = 0, p = 12, e = 4, h = 7)
    purple.assertProduction(m = 12, s = 2, t = 0, p = 7, e = 4, h = 1)
    pink.assertResources(m = 48, s = 15, t = 7, p = 7, e = 1, h = 15)
    pink.assertProduction(m = 3, s = 3, t = 7, p = 2, e = 1, h = 10)
    green.assertResources(m = 41, s = 1, t = 2, p = 9, e = 0, h = 4)
    green.assertProduction(m = 10, s = 1, t = 0, p = 6, e = 0, h = 0)
    green.assertCounts(5 to "ProjectCard")
    green.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    green.turn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Hellas_2_5>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_5_3>",
          "OxygenStep",
          "TerraformRating",
      )
    }

    purple.turn {
      doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Hellas_7_6>",
              "OxygenStep",
              "ProjectCard",
          )
          .expect("0 TerraformRating")
      doTasks(
          *playCardTasks("$ProtectedHabitats"),
          "5 Pay<> FROM MC",
          "$ProtectedHabitats FROM ProjectCard",
      )
    }

    pink.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Hellas_7_7>",
      )
      giveTask(purple, "Disease<$Hospitals>")
      doTasks(
          *playCardTasks("$HousePrinting"),
          "4 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // No MC remainder.
          "$HousePrinting FROM ProjectCard",
          "PROD[Steel]",
      )
    }

    green.turn {
      doTasks(
          *playCardTasks("$TechnologyDemonstration"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$TechnologyDemonstration FROM ProjectCard",
          "2 ProjectCard",
          "ProjectCard",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    purple.turn {
      doTasks(
          *playCardTasks("$AdaptedLichen"),
          "3 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>",
          "3 Pay<> FROM MC",
          "$AdaptedLichen FROM ProjectCard",
          "PROD[Plant]",
          "Animal<$EcologicalZone>",
          "3 Heat",
      )
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "Animal<$EcologicalZone>",
      )
    }

    pink.turn {
      doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>")
      doTask("2 MC") // The fixed and floater-based gains are both one MC.
      doTasks(
          *playCardTasks("$BeamFromAThoriumAsteroid"),
          "7 Pay<Class<Titanium>> FROM Titanium",
          "9 Pay<> FROM MC",
          "$BeamFromAThoriumAsteroid FROM ProjectCard",
          "PROD[3 Heat]",
          "PROD[3 Energy]",
      )
    }

    green.turn {
      doTasks(
          *playCardTasks("$Windmills"),
          "1 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$Windmills FROM ProjectCard",
          "PROD[Energy]",
      )
      doTasks(
          *playCardTasks("$BactoviralResearch"),
          "10 Pay<> FROM MC",
          "$BactoviralResearch FROM ProjectCard",
          "ProjectCard",
      )
      doTask("2 MC", SpliceTacticalGenomics)
      doTasks("Plant", "2 MC", "5 Microbe<$Ants>")
    }

    purple.turn {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTasks(*cardActionTasks("$Hospitals"), "-Disease<$Hospitals>", "5 MC")
    }

    pink.turn {
      doTasks(
          *playCardTasks("$MagneticFieldGeneratorsPromo"),
          "10 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // No MC remainder.
          "$MagneticFieldGeneratorsPromo FROM ProjectCard",
          "PROD[-4 Energy]",
          "PROD[2 Plant]",
          "3 TerraformRating",
          "MagneticFieldGeneratorsPromo_SpecialTile<Hellas_5_1>",
          "ProjectCard",
          "2 MC",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    green.turn {
      doTasks(*cardActionTasks("$Ants"))
      doTasks("-Microbe<$Ants>", "Microbe<$Ants>")
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
    }

    purple.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Hellas_1_5>",
          "Disease<$Hospitals>",
          "Plant",
      )
    }

    pink.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    green.turn {
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      doTasks(*cardActionTasks("$WeatherBalloons"), "Floater<$WeatherBalloons>")
    }

    purple.turn {
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Botanist")
      doTasks(*cardActionTasks("$IndustrialCenter"), "7 Pay<> FROM MC", "PROD[Steel]")
    }

    pink.doTask("Pass")

    green.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "6 MC FROM ProjectCard")
    }

    purple.doTask("Pass")

    green.doTask("Pass")

    // Production.
    purple.doTasks("55 MC", "3 Steel", "8 Plant", "Heat", "4 Energy", "2 Heat FROM Energy")
    purple.doTask("Ok") // Supercapacitors: retain no energy.
    pink.doTasks("48 MC", "4 Steel", "7 Titanium", "4 Plant", "13 Heat", "Heat FROM Energy")
    green.doTasks("44 MC", "Steel", "6 Plant")

    with(green) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_5>",
          "2 MC",
      )
      // Decline another final greenery placement for Green.
      doTask("Ok") // decline final greenery
    }

    with(purple) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_2_6>",
          "Plant",
      )
      // Decline another final greenery placement for Purple.
      doTask("Ok") // decline final greenery
    }

    with(pink) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_7_8>",
      )
      // Decline another final greenery placement for Pink.
      doTask("Ok") // decline final greenery
    }
    purple.assertCounts(0 to "ProjectCard")
    pink.assertCounts(0 to "ProjectCard")
    green.assertCounts(0 to "ProjectCard")
    with(purple) {
      assertResources(m = 56, s = 5, t = 0, p = 6, e = 4, h = 13)
      assertProduction(m = 13, s = 3, t = 0, p = 8, e = 4, h = 1)
    }
    with(pink) {
      assertResources(m = 63, s = 5, t = 7, p = 3, e = 0, h = 29)
      assertProduction(m = 4, s = 4, t = 7, p = 4, e = 0, h = 13)
    }
    with(green) {
      assertResources(m = 57, s = 1, t = 3, p = 1, e = 0, h = 4)
      assertProduction(m = 10, s = 1, t = 0, p = 6, e = 0, h = 0)
    }

    purple.assertCounts(1 to "FirstPlace<Player1>", 0 to "SecondPlace<Player1>")
    pink.assertCounts(1 to "FirstPlace<Player2>", 1 to "SecondPlace<Player2>")
    green.assertCounts(1 to "FirstPlace<Player3>", 2 to "SecondPlace<Player3>")

    purple.assertCounts(42 to "TerraformRating")
    pink.assertCounts(44 to "TerraformRating")
    green.assertCounts(34 to "TerraformRating")
    purple.assertCounts(74 to "VictoryPoint")
    pink.assertCounts(74 to "VictoryPoint")
    green.assertCounts(67 to "VictoryPoint")
    purple.assertCounts(0 to "Victory")
    pink.assertCounts(1 to "Victory")
    green.assertCounts(0 to "Victory")
  }
}
