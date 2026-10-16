package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Replay of the 2025 WSBG Terraforming Mars Ring Final, through final scoring.
 *
 * Player tasks are explicit; Admin retains normal autoexecution. Uncertain purchases, payments, and
 * turn grouping are labeled as provisional choices; exMachina records the Miranda mistake and
 * refund. Passing the replay does not independently establish those choices as physical game facts.
 *
 * Sources:
 * - Primary recording: https://www.youtube.com/watch?v=31VxDkthyOk
 * - Corroborating commentary: https://www.youtube.com/watch?v=RbTR59QRVEc
 * - Local evidence inventory: _local/replays/Game20250919/sources.md
 */
internal class Wsbg2025Test : AbstractFullGameTest() {
  override val config =
      GameConfig(
          """
          ElysiumMap
          PreludeExpansion
          fakes:cards
          EcologyExperts, Unsafe
          """,
          "Blue",
          "Red",
          "Green",
          "Black",
      )

  @Test
  internal fun wsbg2025() {
    val blue = player(1)
    val red = player(2)
    val green = player(3)
    val black = player(4)
    val players = listOf(blue, red, green, black)
    players.forEach { it.autoExecPolicy = NONE }
    admin.startGame()
    players.forEach { it.doTasks("20 TerraformRating") }
    players.forEach {
      it.doTasks(
          "2 CorporationCard",
          "-CorporationCard",
          "10 ProjectCard<Selecting>",
          "4 PreludeCard",
          "-2 PreludeCard",
      )
    }
    blue.doTasks("-4 ProjectCard<Selecting>")
    red.doTasks("-3 ProjectCard<Selecting>")
    green.doTasks("-2 ProjectCard<Selecting>")
    black.doTasks("-3 ProjectCard<Selecting>")

    // Generation 1
    blue.turn {
      doTasks(
          "PlayCard<Class<$CrediCor>>",
          "$CrediCor FROM CorporationCard",
          "57 MC",
          "18 Pay<> FROM MC",
          "6 ProjectCard<Hand FROM Selecting>",
      )
    }
    red.turn {
      doTasks(
          "PlayCard<Class<$ValleyTrust>>",
          "$ValleyTrust FROM CorporationCard",
          "37 MC",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
    }
    green.turn {
      doTasks(
          "PlayCard<Class<$RobinsonIndustries>>",
          "$RobinsonIndustries FROM CorporationCard",
          "47 MC",
          "24 Pay<> FROM MC",
          "8 ProjectCard<Hand FROM Selecting>",
      )
    }
    black.turn {
      doTasks(
          "PlayCard<Class<$CheungShingMars>>",
          "$CheungShingMars FROM CorporationCard",
          "44 MC",
          "PROD[3 MC]",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
    }
    blue.turn {
      doTasks(
          "PlayCard<Class<$UnmiContractor>>",
          "$UnmiContractor FROM PreludeCard",
          "3 TerraformRating",
          "ProjectCard",
      )
      doTasks(
          "PlayCard<Class<$EarlySettlement>>",
          "$EarlySettlement FROM PreludeCard",
          "PROD[Plant]",
          "CityTile<Elysium_3_7>",
          "3 ProjectCard",
      )
      assertCounts(1 to "$EarlySettlement") // visual confirmation
    }
    red.turn {
      doTasks(
          "PlayCard<Class<$Biofuels>>",
          "$Biofuels FROM PreludeCard",
          "PROD[Plant]",
          "PROD[Energy]",
          "2 Plant",
      )
      doTasks(
          "PlayCard<Class<$ResearchNetwork>>",
          "$ResearchNetwork FROM PreludeCard",
          "PROD[MC]",
          "3 ProjectCard",
      )
    }
    green.turn {
      doTasks(
          "PlayCard<Class<$SelfSufficientSettlement>>",
          "$SelfSufficientSettlement FROM PreludeCard",
          "PROD[2 MC]",
          "CityTile<Elysium_8_8>",
          "2 Steel",
      )
      doTasks(
          "PlayCard<Class<$EcologyExperts>>",
          "$EcologyExperts FROM PreludeCard",
          "PROD[Plant]",
          "PlayCard<Class<$Predators>>",
      )
      // Hand the global-requirement waiver to Admin before playing Predators.
      selectTask("-Required* / Required")
      doTasks("14 Pay<> FROM MC", "$Predators FROM ProjectCard")
    }
    black.turn {
      doTasks(
          "PlayCard<Class<$AlliedBank>>",
          "$AlliedBank FROM PreludeCard",
          "PROD[4 MC]",
          "3 MC",
      )
      doTasks(
          "PlayCard<Class<$AquiferTurbines>>",
          "$AquiferTurbines FROM PreludeCard",
          "PROD[2 Energy]",
          "-3 MC",
          "OceanTile<Elysium_5_4>",
          "TerraformRating",
          "2 Plant",
      )
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$EarthCatapult>>",
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
          "4 MC",
      )
      doTasks(
          "PlayProject<Class<$ArcticAlgae>>",
          "10 Pay<> FROM MC",
          "$ArcticAlgae FROM ProjectCard",
          "Plant",
      )
    }
    red.turn {
      doTasks(
          "RequiredActionsSignal",
          "3 PreludeCard<Selecting>",
          "PlayCard<Class<$MoholeExcavation>>",
          "$MoholeExcavation FROM PreludeCard<Selecting>",
          "PROD[Steel]",
          "PROD[2 Heat]",
          "2 Heat",
      )
      doTasks(
          "PlayProject<Class<$LandClaim>>",
          "1 Pay<> FROM MC",
          "$LandClaim FROM ProjectCard",
          "Community<Elysium_5_6>",
          "PlayedEvent<Class<$LandClaim>> FROM $LandClaim",
      )
    }
    green.turn {
      doTasks("UseCardAction<$RobinsonIndustries, Action1>", "4 Pay<> FROM MC", "PROD[Titanium]")
    }
    black.turn {
      doTasks(
          "PlayProject<Class<$GhgFactories>>",
          "9 Pay<> FROM MC",
          "$GhgFactories FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 Heat]",
      )
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$EnergyTapping>>",
          "1 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Red>]",
      )
    }
    red.turn {
      // Provisional Sabotage target/payment: Blue loses 7 MC.
      doTasks(
          "PlayProject<Class<$Sabotage>>",
          "1 Pay<> FROM MC",
          "$Sabotage FROM ProjectCard",
          "-7 MC<Blue>",
          "PlayedEvent<Class<$Sabotage>> FROM $Sabotage",
      )
      doTasks(
          "PlayProject<Class<$ResearchCoordination>>",
          "4 Pay<> FROM MC",
          "$ResearchCoordination FROM ProjectCard",
      )
    }
    green.turn { doTasks("Pass") }
    black.turn { doTasks("Pass") }
    blue.turn { doTasks("Pass") }
    red.turn { doTasks("Pass") }

    // Generation 2
    blue.doTasks("23 MC", "Plant", "Energy")
    red.doTasks("21 MC", "Plant", "Steel", "2 Heat")
    green.doTasks("22 MC", "Plant", "Titanium")
    black.doTasks("28 MC", "Energy", "4 Heat")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>") }

    blue.assertCounts(2 to "Generation", 23 to "TerraformRating", 1 to "OwnedTile")
    blue.assertProduction(m = 0, s = 0, t = 0, p = 1, e = 1, h = 0)
    blue.assertResources(m = 25, s = 0, t = 0, p = 2, e = 1, h = 0)

    red.assertCounts(20 to "TerraformRating", 1 to "Community")
    red.assertProduction(m = 1, s = 1, t = 0, p = 1, e = 0, h = 2)
    red.assertResources(m = 31, s = 1, t = 0, p = 3, e = 0, h = 4)

    green.assertCounts(20 to "TerraformRating", 1 to "OwnedTile")
    green.assertProduction(m = 2, s = 0, t = 1, p = 1, e = 0, h = 0)
    green.assertResources(m = 27, s = 2, t = 1, p = 1, e = 0, h = 0)

    black.assertCounts(21 to "TerraformRating", 0 to "OwnedTile")
    black.assertProduction(m = 7, s = 0, t = 0, p = 0, e = 1, h = 4)
    black.assertResources(m = 42, s = 0, t = 0, p = 2, e = 1, h = 4)

    red.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    black.doTasks("-4 ProjectCard<Selecting>")
    // Provisional Research count: three cards.
    blue.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    red.turn {
      doTasks(
          "PlayProject<Class<$Archaebacteria>>",
          "6 Pay<> FROM MC",
          "$Archaebacteria FROM ProjectCard",
          "PROD[Plant]",
      )
      doTasks("ClaimMilestone<Class<Ecologist>>", "8 Pay<> FROM MC", "Ecologist")
    }
    green.turn {
      doTasks(
          "PlayProject<Class<$IndustrialCenter>>",
          "Ok", // no steel paid; payment allocation is provisional
          "4 Pay<> FROM MC",
          "$IndustrialCenter FROM ProjectCard",
          "IndustrialCenter_SpecialTile<Elysium_4_8>",
          "Plant",
          "Steel",
      )
      doTasks(
          "PlayProject<Class<$IndustrialMicrobes>>",
          "3 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$IndustrialMicrobes FROM ProjectCard",
          "PROD[Energy]",
          "PROD[Steel]",
      )
    }
    black.turn {
      doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
      doTasks(
          "PlayProject<Class<$TowingAComet>>",
          "23 Pay<> FROM MC",
          "$TowingAComet FROM ProjectCard",
          "2 Plant",
          "OxygenStep",
          "TerraformRating",
          "OceanTile<Elysium_4_4>",
          "TerraformRating",
          "2 Plant",
      )
      assertCounts(20 to "MC") // visual confirmation
      doTasks("2 MC")
      blue.doTasks("2 Plant") // Arctic Algae
      doTasks("PlayedEvent<Class<$TowingAComet>> FROM $TowingAComet")
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$ElectroCatapult>>",
          "15 Pay<> FROM MC",
          "$ElectroCatapult FROM ProjectCard",
          "PROD[-Energy]",
      )
      doTasks("UseCardAction<$ElectroCatapult, Action1>", "Pay<Class<Plant>> FROM Plant", "7 MC")
    }
    red.turn { doTasks("Pass") }
    green.turn {
      doTasks("UseCardAction<$RobinsonIndustries, Action1>", "4 Pay<> FROM MC", "PROD[Heat]")
    }
    black.turn {
      doTasks(
          "PlayProject<Class<$DomedCrater>>",
          "22 Pay<> FROM MC",
          "$DomedCrater FROM ProjectCard",
          "3 Plant",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Elysium_5_3>",
          "2 Plant",
          "2 MC",
      )
    }
    blue.turn { doTasks("Pass") }
    green.turn { doTasks("Pass") }
    black.turn {
      // Greenery timing inferred between 00:38:30 and 00:43:30.
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_4_3>", "OxygenStep", "TerraformRating", "Plant", "4 MC")
      assertCounts(6 to "MC", 24 to "TerraformRating") // visual confirmation
      doTasks("Pass")
    }

    // Generation 3
    blue.doTasks("Heat FROM Energy", "23 MC", "Plant")
    red.doTasks("21 MC", "2 Plant", "Steel", "2 Heat")
    green.doTasks("22 MC", "Plant", "Titanium", "Energy", "Steel", "Heat")
    black.doTasks("Heat FROM Energy", "34 MC", "4 Heat")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>") }

    blue.assertCounts(3 to "Generation", 23 to "TerraformRating", 1 to "OwnedTile")
    blue.assertProduction(m = 0, s = 0, t = 0, p = 1, e = 0, h = 0)
    blue.assertResources(m = 31, s = 0, t = 0, p = 4, e = 0, h = 1)

    red.assertCounts(20 to "TerraformRating", 1 to "Community")
    red.assertProduction(m = 1, s = 1, t = 0, p = 2, e = 0, h = 2)
    red.assertResources(m = 32, s = 2, t = 0, p = 5, e = 0, h = 6)

    green.assertCounts(20 to "TerraformRating", 2 to "OwnedTile")
    green.assertProduction(m = 2, s = 1, t = 1, p = 1, e = 1, h = 1)
    green.assertResources(m = 26, s = 1, t = 2, p = 3, e = 1, h = 1)

    black.assertCounts(24 to "TerraformRating", 40 to "MC") // visual confirmation
    black.assertCounts(2 to "OwnedTile")
    black.assertCounts(
        (10 + black.count("ProdOffset<Class<MC>>")) to "PROD[MC]"
    ) // visual confirmation
    black.assertProduction(m = 10, s = 0, t = 0, p = 0, e = 0, h = 4)
    black.assertResources(m = 40, s = 0, t = 0, p = 4, e = 0, h = 9)

    red.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks("-4 ProjectCard<Selecting>")
    black.doTasks("-4 ProjectCard<Selecting>")
    blue.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    green.turn {
      doTasks("ClaimMilestone<Class<Generalist>>", "8 Pay<> FROM MC", "Generalist")
      doTasks("UseCardAction<$RobinsonIndustries, Action1>", "4 Pay<> FROM MC", "PROD[Titanium]")
      assertCounts(1 to "Generalist", 2 to "PROD[Titanium]") // visual confirmation
    }
    black.turn {
      doTasks("ClaimMilestone<Class<Specialist>>", "8 Pay<> FROM MC", "Specialist")
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
    }
    blue.turn {
      doTasks("UseCardAction<$ElectroCatapult, Action1>", "Pay<Class<Plant>> FROM Plant", "7 MC")
      doTasks(
          "PlayProject<Class<$OptimalAerobraking>>",
          "5 Pay<> FROM MC",
          "$OptimalAerobraking FROM ProjectCard",
      )
    }
    red.turn {
      // Provisional patent count: two.
      doTasks("UseStandardProject<SellPatentsProject>", "2 MC FROM ProjectCard")
      doTasks(
          "UseStandardProject<CityProject>",
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Elysium_5_6>",
          "3 Plant",
      )
    }
    green.turn {
      doTasks(
          "PlayProject<Class<$MartianSurvey>>",
          "9 Pay<> FROM MC",
          "$MartianSurvey FROM ProjectCard",
          "2 ProjectCard",
          "PlayedEvent<Class<$MartianSurvey>> FROM $MartianSurvey",
      )
      doTasks(
          "PlayProject<Class<$MiningArea>>",
          "Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$MiningArea FROM ProjectCard",
          "MiningArea_SpecialTile<Elysium_5_9>",
          "PROD[Titanium]",
          "Plant",
          "Titanium",
      )
      // Secondary 00:27:48 identifies Mining Area; the primary commentary calls it Mining Rights.
    }
    black.turn {
      doTasks(
          "PlayProject<Class<$LunarBeam>>",
          "13 Pay<> FROM MC",
          "$LunarBeam FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[2 Heat]",
          "PROD[2 Energy]",
      )
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$ConvoyFromEuropa>>",
          "13 Pay<> FROM MC",
          "$ConvoyFromEuropa FROM ProjectCard",
          "ProjectCard",
          "3 MC",
          "3 Heat",
          "OceanTile<Elysium_3_6>",
          "TerraformRating",
          "2 Plant",
          "PlayedEvent<Class<$ConvoyFromEuropa>> FROM $ConvoyFromEuropa",
      )
      doTasks(
          "PlayProject<Class<$ImportedHydrogen>>",
          "14 Pay<> FROM MC",
          "$ImportedHydrogen FROM ProjectCard",
          "3 MC",
          "3 Heat",
          "3 Plant",
          "OceanTile<Elysium_2_5>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
          "PlayedEvent<Class<$ImportedHydrogen>> FROM $ImportedHydrogen",
      )
    }
    red.turn {
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks(
          "GreeneryTile<Elysium_5_5>",
          "OxygenStep",
          "2 Plant",
          "4 MC",
          "TerraformRating",
      )
    }
    green.turn {
      doTasks(
          "PlayProject<Class<$LagrangeObservatory>>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "0 Pay<> FROM MC",
          "$LagrangeObservatory FROM ProjectCard",
          "ProjectCard",
      )
    }
    black.turn { doTasks("Pass") }
    blue.turn {
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks(
          "GreeneryTile<Elysium_2_6>",
          "OxygenStep",
          "2 Steel",
          "4 MC",
          "TerraformRating",
      )
      doTasks(
          "PlayProject<Class<$NitrophilicMoss>>",
          "6 Pay<> FROM MC",
          "$NitrophilicMoss FROM ProjectCard",
          "PROD[2 Plant]",
          "-2 Plant",
      )
    }
    red.turn { doTasks("Pass") }
    green.turn { doTasks("Pass") }
    blue.turn { doTasks("Pass") }

    // Generation 4
    blue.doTasks("26 MC", "3 Plant")
    red.doTasks("23 MC", "2 Plant", "Steel", "2 Heat")
    green.doTasks("Heat FROM Energy", "22 MC", "Plant", "3 Titanium", "Energy", "Steel", "Heat")
    black.doTasks("33 MC", "6 Heat", "2 Energy")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>") }

    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -               W     WT    WC    WS    LC
        |
        | 2 -            VT    L     L     W    [O]   [G1]
        |
        | 3 -        VTT    L     LC    L     WP   [O]   [C1]
        |
        | 4 -      LP    LP   [G4]  [O]    LP    WP    WP   [S3]
        |
        | 5 -  LPP   LPP   [C4]  [O]   [G2]  [C2]  LPP   LPP   [S3]
        |
        | 6 -      LS    LP    LP    LP    LP    LP    LP    L
        |
        | 7 -         LT    LS    L     L     LS    L     L
        |
        | 8 -           LSS    L     L     L    [C3]   L
        |
        | 9 -               LS    L     LC    LC   LSS
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )

    // Research counts for Blue, Red, and Green remain tentative.
    blue.assertCounts(29 to "MC") // visual confirmation
    black.doTasks(
        "Ok", // discard no Research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    red.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    black.turn {
      doTasks(
          "PlayProject<Class<$MarsUniversity>>",
          "6 Pay<> FROM MC",
          "$MarsUniversity FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
    }
    blue.turn {
      assertCounts(26 to "MC") // visual confirmation
      doTasks("UseCardAction<$ElectroCatapult, Action2>", "Pay<Class<Steel>> FROM Steel", "7 MC")
    }
    blue.assertCounts(33 to "MC") // visual confirmation
    red.turn {
      doTasks(
          "PlayProject<Class<$ProtectedHabitats>>",
          "5 Pay<> FROM MC",
          "$ProtectedHabitats FROM ProjectCard",
      )
      doTasks(
          "PlayProject<Class<$Decomposers>>",
          "5 Pay<> FROM MC",
          "$Decomposers FROM ProjectCard",
          "Microbe<$Decomposers>",
      )
    }
    green.turn {
      doTasks(
          "PlayProject<Class<$ColonizerTrainingCamp>>",
          "Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$ColonizerTrainingCamp FROM ProjectCard",
      )
      doTasks("UseCardAction<$RobinsonIndustries, Action1>", "4 Pay<> FROM MC", "PROD[Steel]")
    }
    black.turn {
      // Robotic Workforce copies Cheung Shing Mars; commentary misidentifies it as Dome Farming.
      doTasks(
          "PlayProject<Class<$RoboticWorkforce>>",
          "9 Pay<> FROM MC",
          "$RoboticWorkforce FROM ProjectCard",
          "CopyProductionBox<$CheungShingMars>",
          "PROD[3 MC]",
          "-ProjectCard",
          "ProjectCard",
      )
    }
    black.assertProduction(m = 11, s = 0, t = 0, p = 0, e = 2, h = 6)
    blue.turn {
      doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    }
    blue.assertCounts(34 to "MC") // visual confirmation

    red.turn {
      // Provisional grouping: Reservoir followed by heat conversion.
      doTasks(
          "PlayProject<Class<$SubterraneanReservoir>>",
          "11 Pay<> FROM MC",
          "$SubterraneanReservoir FROM ProjectCard",
          "OceanTile<Elysium_3_5>",
          "TerraformRating",
          "Plant",
          "4 MC",
      )
      blue.doTasks("2 Plant")
      doTasks("PlayedEvent<Class<$SubterraneanReservoir>> FROM $SubterraneanReservoir")
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
    }
    green.turn { doTasks("Pass") }
    black.turn {
      doTasks(
          "PlayProject<Class<$Algae>>",
          "10 Pay<> FROM MC",
          "$Algae FROM ProjectCard",
          "Plant",
          "PROD[2 Plant]",
      )
    }
    blue.turn {
      // 01:00:30: ocean order is inferred.
      doTasks(
          "PlayProject<Class<$GiantIceAsteroid>>",
          "34 Pay<> FROM MC",
          "$GiantIceAsteroid FROM ProjectCard",
          "7 MC",
          "3 Heat",
          "2 TemperatureStep",
          "2 TerraformRating",
          "PROD[Heat]",
          "OceanTile<Elysium_2_4>",
          "TerraformRating",
          "2 Plant",
          "4 MC",
          "OceanTile<Elysium_1_4>",
          "TerraformRating",
          "2 Plant",
          "Steel",
          "4 MC",
          "-5 Plant<Black>",
          "PlayedEvent<Class<$GiantIceAsteroid>> FROM $GiantIceAsteroid",
      )
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_1_5>", "OxygenStep", "TerraformRating", "ProjectCard", "4 MC")
    }
    blue.assertCounts(31 to "TerraformRating") // 01:01:50 track
    red.turn {
      doTasks(
          "PlayProject<Class<$Greenhouses>>",
          "3 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC", // steel allocation inferred from Mohole Excavation's production
          "$Greenhouses FROM ProjectCard",
          "4 Plant",
          "Microbe<$Decomposers>",
      )
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_4_5>", "OxygenStep", "TerraformRating", "Plant", "4 MC")
    }
    black.turn {
      doTasks(
          "PlayProject<Class<$Sponsors>>",
          "6 Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
      )
    }
    blue.turn {
      // Steel allocation inferred.
      doTasks(
          "PlayProject<Class<$GeothermalPower>>",
          "2 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$GeothermalPower FROM ProjectCard",
          "PROD[2 Energy]",
      )
    }
    red.turn {
      doTasks(
          "PlayProject<Class<$SmallAnimals>>",
          "6 Pay<> FROM MC",
          "$SmallAnimals FROM ProjectCard",
          "PROD[-Plant<Blue>]",
          "Microbe<$Decomposers>",
      )
      doTasks("UseCardAction<$SmallAnimals, Action1>", "Animal<$SmallAnimals>")
    }
    black.turn {
      // Search reveal unidentified; declining Mars University is provisional.
      doTasks(
          "PlayProject<Class<$SearchForLife>>",
          "3 Pay<> FROM MC",
          "$SearchForLife FROM ProjectCard",
          "Ok", // provisionally decline the Mars University exchange
      )
      doTasks(
          "UseCardAction<$SearchForLife, Action1>",
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<MicrobeTag>>, $SearchForLife>",
          "-ProjectCard<Revealed>",
      )
    }
    blue.turn { doTasks("Pass") }
    red.turn { doTasks("Pass") }
    black.turn { doTasks("Pass") }

    // Production, about 01:03:30.
    blue.doTasks("31 MC", "2 Plant", "2 Energy", "Heat")
    red.doTasks("26 MC", "2 Plant", "Steel", "2 Heat")
    green.doTasks("Heat FROM Energy", "22 MC", "Plant", "3 Titanium", "Energy", "2 Steel", "Heat")
    black.doTasks("2 Heat FROM Energy", "38 MC", "2 Plant", "6 Heat", "2 Energy")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>") }
    blue.assertCounts(
        5 to "Generation",
        7 to "OceanTile",
        6 to "OxygenStep",
        4 to "TemperatureStep",
    )
    blue.assertCounts(1 to "GreeneryTile<Elysium_1_5>")
    red.assertCounts(
        1 to "GreeneryTile<Elysium_4_5>",
        1 to "Animal<$SmallAnimals>",
        3 to "Microbe<$Decomposers>",
    )
    black.assertCounts(0 to "Science<$SearchForLife>")

    // Generation 5. Blue/Green Research counts are provisional.
    blue.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    red.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    black.doTasks(
        "Ok", // keep all four Research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    blue.turn {
      doTasks(
          "ConvertHeat",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTasks(
          "PlayProject<Class<$Capital>>",
          "24 Pay<> FROM MC",
          "$Capital FROM ProjectCard",
          "4 MC",
          "CapitalTile<Elysium_3_4>",
          "6 MC",
          "PROD[-2 Energy]",
          "PROD[5 MC]",
      )
    }
    red.turn {
      doTasks("FundAward<Class<DesertSettler>>", "8 Pay<> FROM MC", "DesertSettler")
      doTasks(
          "PlayProject<Class<$Worms>>",
          "8 Pay<> FROM MC",
          "$Worms FROM ProjectCard",
          "PROD[3 Plant]",
          "Microbe<$Decomposers>",
      )
    }
    // Large Convoy payment inferred: six titanium and 18 MC.
    green.turn {
      doTasks(
          "PlayProject<Class<$LargeConvoy>>",
          "6 Pay<Class<Titanium>> FROM Titanium",
          "18 Pay<> FROM MC",
          "$LargeConvoy FROM ProjectCard",
          "2 ProjectCard",
          "4 Animal<$Predators>",
          "OceanTile<Elysium_1_3>",
          "TerraformRating",
          "ProjectCard",
          "4 MC",
      )
      blue.doTasks("2 Plant")
      doTasks("PlayedEvent<Class<$LargeConvoy>> FROM $LargeConvoy")
      doTasks(
          "PlayProject<Class<$NuclearZone>>",
          "10 Pay<> FROM MC",
          "$NuclearZone FROM ProjectCard",
          "NuclearZone_SpecialTile<Elysium_2_3>",
          "4 MC",
          "2 TemperatureStep",
          "2 TerraformRating",
      )
    }
    black.turn {
      // Guild discard and Mars University exchanges are provisional.
      doTasks(
          "PlayProject<Class<$InventorsGuild>>",
          "9 Pay<> FROM MC",
          "$InventorsGuild FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          "UseCardAction<$InventorsGuild, Action1>",
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
    }
    blue.turn {
      // Catapult timing and plant payment inferred.
      doTasks("UseCardAction<$ElectroCatapult, Action1>", "Pay<Class<Plant>> FROM Plant", "7 MC")
      doTasks(
          "PlayProject<Class<$AsteroidMining>>",
          "28 Pay<> FROM MC",
          "$AsteroidMining FROM ProjectCard",
          "4 MC",
          "PROD[2 Titanium]",
      )
    }
    red.turn {
      // Steel allocation and animal-action timing are provisional.
      doTasks(
          "PlayProject<Class<$RoverConstruction>>",
          "Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$RoverConstruction FROM ProjectCard",
      )
      doTasks("UseCardAction<$SmallAnimals, Action1>", "Animal<$SmallAnimals>")
    }
    green.turn {
      // Robinson timing inferred from later plant production.
      doTasks("UseCardAction<$RobinsonIndustries, Action1>", "4 Pay<> FROM MC", "PROD[Plant]")
    }
    black.turn {
      // Heat-conversion timing inferred from later 26 TR.
      doTasks(
          "PlayProject<Class<$DesignedMicroorganisms>>",
          "16 Pay<> FROM MC",
          "$DesignedMicroorganisms FROM ProjectCard",
          "PROD[2 Plant]",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
    }
    blue.turn { doTasks("Pass") }
    red.turn { doTasks("Pass") }
    green.turn { doTasks("Pass") }
    black.turn {
      // Search outcome provisionally a miss.
      doTasks(
          "UseCardAction<$SearchForLife, Action1>",
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<MicrobeTag>>, $SearchForLife>",
          "-ProjectCard<Revealed>",
      )
      doTasks("Pass")
    }

    // Generation 6 production, about 01:18.
    blue.doTasks("2 Heat FROM Energy", "37 MC", "2 Titanium", "2 Plant", "2 Heat")
    red.doTasks("26 MC", "Steel", "5 Plant", "2 Heat")
    green.doTasks("Heat FROM Energy", "25 MC", "2 Steel", "3 Titanium", "2 Plant", "Energy", "Heat")
    black.doTasks("2 Heat FROM Energy", "39 MC", "4 Plant", "2 Energy", "6 Heat")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>") }
    blue.assertCounts(6 to "Generation", 32 to "TerraformRating", 8 to "OceanTile")
    green.assertCounts(4 to "Animal<$Predators>")
    red.assertCounts(2 to "Animal<$SmallAnimals>")

    // Research counts are inferred, including Green keeping two before his confirmed patent sale.
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    red.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    black.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    red.turn {
      // Southern greenery placement order is provisional.
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_6_6>", "OxygenStep", "TerraformRating", "Plant")
      doTasks(
          "UseStandardProject<GreeneryProject>",
          "23 Pay<> FROM MC",
          "GreeneryTile<Elysium_6_7>",
          "OxygenStep",
          "TerraformRating",
          "Plant",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    green.turn {
      doTasks(
          "PlayProject<Class<$Herbivores>>",
          "12 Pay<> FROM MC",
          "$Herbivores FROM ProjectCard",
          "Animal<$Herbivores>",
          "PROD[-Plant<Blue>]",
      )
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks(
          "GreeneryTile<Elysium_9_9>",
          "OxygenStep",
          "TerraformRating",
          "2 Steel",
          "Animal<$Herbivores>",
      )
    }
    black.turn {
      // Guild purchase and Mars University card identities remain unknown.
      doTasks(
          "UseCardAction<$InventorsGuild, Action1>",
          "ProjectCard<Selecting>",
          "Ok", // keep the Guild's card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "PlayProject<Class<$DevelopmentCenter>>",
          "9 Pay<> FROM MC",
          "$DevelopmentCenter FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$ArtificialPhotosynthesis>>",
          "10 Pay<> FROM MC",
          "$ArtificialPhotosynthesis FROM ProjectCard",
          "PROD[2 Energy]",
      )
      doTasks("PlayProject<Class<$CupolaCity>>", "14 Pay<> FROM MC", "$CupolaCity FROM ProjectCard")
      doTasks("NormalCityTile<Elysium_6_5>", "Plant", "2 MC", "PROD[-Energy]", "PROD[3 MC]")
      red.doTasks("2 MC")
    }
    red.turn {
      // Small Animals turn grouping is provisional.
      doTasks("UseCardAction<$SmallAnimals, Action1>", "Animal<$SmallAnimals>")
    }
    green.turn {
      assertCounts(6 to "Steel") // visual confirmation
      doTasks(
          "PlayProject<Class<$HousePrinting>>",
          "5 Pay<Class<Steel>> FROM Steel",
          "0 Pay<> FROM MC",
          "$HousePrinting FROM ProjectCard",
          "PROD[Steel]",
      )
      assertCounts(1 to "Steel")
    }
    black.turn {
      doTasks(
          "UseCardAction<$DevelopmentCenter, Action1>",
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$KelpFarming>>",
          "15 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[3 Plant]",
          "2 Plant",
      )
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_3_3>", "OxygenStep", "TerraformRating", "ProjectCard", "2 MC")
    }
    red.turn {
      // Investment Loan / Noctis Farming turn grouping is provisional.
      doTasks(
          "PlayProject<Class<$InvestmentLoan>>",
          "3 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "10 MC",
          "PROD[-MC]",
          "PlayedEvent<Class<$InvestmentLoan>> FROM $InvestmentLoan",
      )
    }
    green.turn {
      doTasks(
          "PlayProject<Class<$SfMemorial>>",
          "Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$SfMemorial FROM ProjectCard",
          "ProjectCard",
      )
    }
    black.turn {
      doTasks(
          "PlayProject<Class<$MassConverter>>",
          "8 Pay<> FROM MC",
          "$MassConverter FROM ProjectCard",
          "PROD[6 Energy]",
          "-ProjectCard",
          "ProjectCard",
      )
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$Shuttles>>",
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$Shuttles FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
      )
      // Catapult plant payment inferred.
      doTasks("UseCardAction<$ElectroCatapult, Action1>", "Pay<Class<Plant>> FROM Plant", "7 MC")
    }
    red.turn {
      doTasks(
          "PlayProject<Class<$NoctisFarming>>",
          "Pay<Class<Steel>> FROM Steel",
          "8 Pay<> FROM MC",
          "$NoctisFarming FROM ProjectCard",
          "PROD[MC]",
          "2 Plant",
          "Microbe<$Decomposers>",
      )
    }
    green.turn {
      assertCounts(3 to "MC")
      doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
      doTasks("UseCardAction<$RobinsonIndustries, Action1>", "4 Pay<> FROM MC", "PROD[Energy]")
      assertCounts(0 to "MC")
    }
    black.turn {
      doTasks(
          "PlayProject<Class<$Mine>>",
          "2 Pay<> FROM MC",
          "$Mine FROM ProjectCard",
          "PROD[Steel]",
      )
      doTasks(
          "PlayProject<Class<$TitaniumMine>>",
          "5 Pay<> FROM MC",
          "$TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$Satellites>>",
          "6 Pay<> FROM MC",
          "$Satellites FROM ProjectCard",
          "PROD[4 MC]",
      )
    }
    red.turn { doTasks("Pass") }
    green.turn {
      // Predators target inferred: his own Herbivores.
      doTasks("UseCardAction<$Predators, Action1>", "-Animal<$Herbivores>", "Animal<$Predators>")
    }
    black.turn {
      doTasks(
          "UseCardAction<$SearchForLife, Action1>",
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<MicrobeTag>>, $SearchForLife>",
          "-ProjectCard<Revealed>",
      )
    }
    blue.turn { doTasks("Pass") }
    green.turn { doTasks("Pass") }
    blue.assertCounts(33 to "TerraformRating")
    blue.assertCounts(6 to "MC", 0 to "Plant") // visual confirmation
    red.assertCounts(27 to "TerraformRating")
    red.assertCounts(4 to "MC", 5 to "Plant") // visual confirmation
    green.assertCounts(24 to "TerraformRating")
    green.assertCounts(3 to "Titanium", 0 to "MC") // visual confirmation
    black.assertCounts(26 to "TerraformRating")
    black.assertCounts(7 to "MC") // visual confirmation

    black.turn { doTasks("Pass") }

    // Generation 7 production, about 01:31:24.
    blue.assertCounts(
        (16 + blue.count("ProdOffset<Class<MC>>")) to "PROD[MC]",
        0 to "PROD[Steel]",
        2 to "PROD[Titanium]",
        4 to "PROD[Plant]",
        0 to "PROD[Energy]",
        2 to "PROD[Heat]",
    ) // visual confirmation
    red.assertCounts(
        (2 + red.count("ProdOffset<Class<MC>>")) to "PROD[MC]",
        1 to "PROD[Steel]",
        0 to "PROD[Titanium]",
        5 to "PROD[Plant]",
        0 to "PROD[Energy]",
        2 to "PROD[Heat]",
    ) // visual confirmation
    green.assertCounts(
        (2 + green.count("ProdOffset<Class<MC>>")) to "PROD[MC]",
        3 to "PROD[Steel]",
        3 to "PROD[Titanium]",
        2 to "PROD[Plant]",
        2 to "PROD[Energy]",
        1 to "PROD[Heat]",
    ) // visual confirmation
    black.assertCounts(
        (13 + black.count("ProdOffset<Class<MC>>")) to "PROD[MC]",
        1 to "PROD[Steel]",
        1 to "PROD[Titanium]",
        4 to "PROD[Plant]",
        8 to "PROD[Energy]",
        6 to "PROD[Heat]",
    ) // visual confirmation
    blue.doTasks("49 MC", "2 Titanium", "4 Plant", "2 Heat")
    red.doTasks("29 MC", "Steel", "5 Plant", "2 Heat")
    green.doTasks(
        "Heat FROM Energy",
        "26 MC",
        "3 Steel",
        "3 Titanium",
        "2 Plant",
        "2 Energy",
        "Heat",
    )
    black.doTasks(
        "Heat FROM Energy",
        "39 MC",
        "Steel",
        "Titanium",
        "4 Plant",
        "8 Energy",
        "6 Heat",
    )
    players.forEach { it.doTasks("4 ProjectCard<Selecting>") }
    blue.assertCounts(
        7 to "Generation",
        8 to "OceanTile",
        10 to "OxygenStep",
        9 to "TemperatureStep",
    )

    // Blue Research count is provisional.
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    red.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "Ok", // discard no Research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    black.doTasks(
        "Ok", // discard no Research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    green.turn {
      doTasks(
          "PlayProject<Class<$MineralDeposit>>",
          "5 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
          "PlayedEvent<Class<$MineralDeposit>> FROM $MineralDeposit",
      )
      assertCounts(9 to "MC", 8 to "Steel") // visual confirmation
      doTasks(
          "PlayProject<Class<$UndergroundCity>>",
          "8 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$UndergroundCity FROM ProjectCard",
          "NormalCityTile<Elysium_6_8>",
          "Plant",
          "PROD[-2 Energy]",
          "PROD[2 Steel]",
      )
      assertCounts(7 to "MC", 0 to "Steel") // visual confirmation
      red.doTasks("2 MC")
    }
    black.turn {
      // 01:40:05 shows a greenery, despite commentary calling it a city spot.
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_6_4>", "OxygenStep", "TerraformRating", "Plant", "2 MC")
      doTasks(
          "UseCardAction<$InventorsGuild, Action1>",
          "ProjectCard<Selecting>",
          "Ok", // keep the Guild's card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    blue.turn {
      doTasks("FundAward<Class<Celebrity>>", "14 Pay<> FROM MC", "Celebrity")
      doTasks("FundAward<Class<EstateDealer>>", "20 Pay<> FROM MC", "EstateDealer")
    }
    red.turn {
      // Immigrant City, corrected by primary commentary at 01:43:40.
      doTasks(
          "PlayProject<Class<$PeroxidePower>>",
          "Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$PeroxidePower FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Energy]",
      )
      doTasks(
          "PlayProject<Class<$ImmigrantCity>>",
          "13 Pay<> FROM MC",
          "$ImmigrantCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[-2 MC]",
          "NormalCityTile<Elysium_7_4>",
          "Steel",
          "2 MC",
          "PROD[MC]",
      )
    }
    green.turn {
      doTasks("UseStandardProject<SellPatentsProject>", "2 MC FROM ProjectCard")
      doTasks(
          "PlayProject<Class<$MiningRights>>",
          "9 Pay<> FROM MC",
          "$MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Elysium_7_3>",
          "Titanium",
          "PROD[Titanium]",
      )
    }
    black.turn {
      doTasks("PlayProject<Class<$MediaGroup>>", "6 Pay<> FROM MC", "$MediaGroup FROM ProjectCard")
      doTasks(
          "PlayProject<Class<$IndenturedWorkers>>",
          "$IndenturedWorkers FROM ProjectCard",
          "IndenturedWorkers_NextCardEffect",
          "3 MC",
          "PlayedEvent<Class<$IndenturedWorkers>> FROM $IndenturedWorkers",
      )
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$CallistoPenalMines>>",
          "2 Pay<Class<Titanium>> FROM Titanium",
          "14 Pay<> FROM MC",
          "$CallistoPenalMines FROM ProjectCard",
          "4 MC",
          "PROD[3 MC]",
      )
      // Heat conversion / Bushes turn grouping is inferred.
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
    }
    red.turn {
      // Small Animals timing inferred.
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_8_4>", "OxygenStep", "TerraformRating", "2 Steel")
      doTasks("UseCardAction<$SmallAnimals, Action1>", "Animal<$SmallAnimals>")
    }
    green.turn {
      doTasks("UseCardAction<$Predators, Action1>", "-Animal<$Herbivores>", "Animal<$Predators>")
    }
    black.turn {
      // Power Plant timing inferred before Medical Lab from building tags and energy production.
      doTasks(
          "PlayProject<Class<$ProtectedValley>>",
          "Pay<Class<Steel>> FROM Steel",
          "11 Pay<> FROM MC",
          "$ProtectedValley FROM ProjectCard",
          "PROD[2 MC]",
          "GreeneryTile<Elysium_1_2>",
          "OxygenStep",
          "TerraformRating",
          "Titanium",
          "2 MC",
      )
      doTasks(
          "PlayProject<Class<$PowerPlant>>",
          "2 Pay<> FROM MC",
          "$PowerPlant FROM ProjectCard",
          "PROD[Energy]",
      )
    }
    blue.turn {
      // Catapult plant payment inferred.
      doTasks("UseCardAction<$ElectroCatapult, Action1>", "Pay<Class<Plant>> FROM Plant", "7 MC")
      doTasks(
          "PlayProject<Class<$Bushes>>",
          "8 Pay<> FROM MC",
          "$Bushes FROM ProjectCard",
          "PROD[2 Plant]",
          "2 Plant",
      )
    }
    red.turn {
      doTasks(
          "PlayProject<Class<$Insects>>",
          "9 Pay<> FROM MC",
          "$Insects FROM ProjectCard",
          "PROD[4 Plant]",
          "Microbe<$Decomposers>",
      )
    }
    green.turn { doTasks("Pass") }
    black.turn {
      doTasks(
          "PlayProject<Class<$MedicalLab>>",
          "11 Pay<> FROM MC",
          "$MedicalLab FROM ProjectCard",
          "PROD[5 MC]",
          "-ProjectCard",
          "ProjectCard",
      )
      // Development Center timing is provisional.
      doTasks(
          "UseCardAction<$DevelopmentCenter, Action1>",
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }
    blue.turn { doTasks("Pass") }
    red.turn { doTasks("Pass") }
    black.turn {
      // Both heat conversions grouped here provisionally.
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
      doTasks(
          "PlayProject<Class<$MirandaResort>>",
          "2 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<> FROM MC",
          "$MirandaResort FROM ProjectCard",
          "PROD[4 MC]",
      )
      doTasks(
          "UseCardAction<$SearchForLife, Action1>",
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<MicrobeTag>>, $SearchForLife>",
          "-ProjectCard<Revealed>",
      )
    }
    // Intentional Miranda overpayment: Black misses Mass Converter's 2 MC discount.
    black.exMachina("-2 MC")
    blue.assertCounts(34 to "TerraformRating")
    blue.assertCounts(4 to "MC", 5 to "Plant") // visual confirmation
    red.assertCounts(28 to "TerraformRating")
    red.assertCounts(1 to "MC", 3 to "Steel", 2 to "Plant") // visual confirmation
    black.assertCounts(30 to "TerraformRating")
    black.assertCounts(1 to "MC") // visual confirmation
    green.assertCounts(0 to "MC") // visual confirmation
    black.turn { doTasks("Pass") }

    // Generation 8 production. Secondary 01:10:51 corrects Black to 24 MC / 1 steel production.
    blue.doTasks("53 MC", "2 Titanium", "6 Plant", "2 Heat")
    red.doTasks("28 MC", "Steel", "9 Plant", "Energy", "2 Heat")
    green.doTasks("2 Heat FROM Energy", "26 MC", "5 Steel", "4 Titanium", "2 Plant", "Heat")
    black.doTasks(
        "7 Heat FROM Energy",
        "54 MC",
        "Steel",
        "Titanium",
        "4 Plant",
        "9 Energy",
        "6 Heat",
    )
    black.exMachina("2 MC") // 01:50:42-01:50:57: table agrees to refund Miranda's missed discount
    players.forEach { it.doTasks("4 ProjectCard<Selecting>") }
    blue.assertCounts(
        8 to "Generation",
        8 to "OceanTile",
        13 to "OxygenStep",
        12 to "TemperatureStep",
    )

    // 01:50:41 production checkpoint; uncertain resource readings remain in the local notes.
    blue.assertCounts(
        (19 + blue.count("ProdOffset<Class<MC>>")) to "PROD[MC]",
        0 to "PROD[Steel]",
        2 to "PROD[Titanium]",
        6 to "PROD[Plant]",
        0 to "PROD[Energy]",
        2 to "PROD[Heat]",
    ) // visual confirmation
    red.assertCounts(
        red.count("ProdOffset<Class<MC>>") to "PROD[MC]",
        1 to "PROD[Steel]",
        0 to "PROD[Titanium]",
        9 to "PROD[Plant]",
        1 to "PROD[Energy]",
        2 to "PROD[Heat]",
    ) // visual confirmation
    green.assertCounts(
        (2 + green.count("ProdOffset<Class<MC>>")) to "PROD[MC]",
        5 to "PROD[Steel]",
        4 to "PROD[Titanium]",
        2 to "PROD[Plant]",
        0 to "PROD[Energy]",
        1 to "PROD[Heat]",
    ) // visual confirmation
    black.assertProds(
        24 to "MC",
        1 to "Steel",
        1 to "Titanium",
        4 to "Plant",
        9 to "Energy",
        6 to "Heat",
    )
    // 01:50:41 map.
    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -               W    [G4]  [O]   [O]   [G1]
        |
        | 2 -            VT    L    [S3]  [O]   [O]   [G1]
        |
        | 3 -        VTT    L    [G1]  [C1]  [O]   [O]   [C1]
        |
        | 4 -      LP    LP   [G4]  [O]   [G2]   WP    WP   [S3]
        |
        | 5 -  LPP   LPP   [C4]  [O]   [G2]  [C2]  LPP   LPP   [S3]
        |
        | 6 -      LS    LP   [G4]  [C1]  [G2]  [G2]  [C3]   L
        |
        | 7 -        [S3]  [C2]   L     L     LS    L     L
        |
        | 8 -           [G2]   L     L     L    [C3]   L
        |
        | 9 -               LS    L     LC    LC   [G3]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )

    // Research counts are provisional; Green keeps a single card to fund Open City after the theft.
    blue.doTasks("-4 ProjectCard<Selecting>")
    red.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    black.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    black.turn {
      // Mars University exchange unknown; retaining the hand is provisional.
      doTasks(
          "PlayProject<Class<$ViralEnhancers>>",
          "9 Pay<> FROM MC",
          "$ViralEnhancers FROM ProjectCard",
          "Plant",
          "Ok", // provisionally decline the Mars University exchange
      )
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_5_2>", "2 Plant", "OxygenStep", "TerraformRating")
    }
    blue.turn {
      // Catapult timing/payment inferred.
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_7_6>", "0 OxygenStep")
      doTasks("UseCardAction<$ElectroCatapult, Action1>", "Pay<Class<Plant>> FROM Plant", "7 MC")
    }
    red.turn {
      doTasks(
          "PlayProject<Class<$HiredRaiders>>",
          "Pay<> FROM MC",
          "$HiredRaiders FROM ProjectCard",
          "2 Steel<Red FROM Green>",
          "PlayedEvent<Class<$HiredRaiders>> FROM $HiredRaiders",
      )
      assertCounts(1 to "PlayedEvent<Class<$HiredRaiders>>") // visual confirmation
      // Commercial District payment inferred: six steel and 4 MC.
      doTasks(
          "PlayProject<Class<$CommercialDistrict>>",
          "6 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$CommercialDistrict FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 MC]",
          "CommercialDistrict_SpecialTile<Elysium_7_8>",
      )
    }
    green.turn {
      // Robinson energy increase inferred to fund Open City.
      doTasks("UseCardAction<$RobinsonIndustries, Action1>", "4 Pay<> FROM MC", "PROD[Energy]")
      doTasks(
          "PlayProject<Class<$OpenCity>>",
          "3 Pay<Class<Steel>> FROM Steel",
          "17 Pay<> FROM MC",
          "$OpenCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 MC]",
          "2 Plant",
          "NormalCityTile<Elysium_8_6>",
      )
      red.doTasks("2 MC", "PROD[MC]")
    }
    black.turn {
      doTasks(
          "UseCardAction<$DevelopmentCenter, Action1>",
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$StandardTechnology>>",
          "4 Pay<> FROM MC",
          "$StandardTechnology FROM ProjectCard",
      )
    }
    red.turn {
      // Final Small Animals activation timing inferred.
      doTasks("UseCardAction<$SmallAnimals, Action1>", "Animal<$SmallAnimals>")
    }
    green.turn {
      // Immigration Shuttles titanium payment inferred.
      doTasks(
          "PlayProject<Class<$ImmigrationShuttles>>",
          "10 Pay<Class<Titanium>> FROM Titanium",
          "Pay<> FROM MC",
          "$ImmigrationShuttles FROM ProjectCard",
          "PROD[5 MC]",
      )
    }
    black.turn {
      // Guild discard is provisional.
      doTasks(
          "UseCardAction<$InventorsGuild, Action1>",
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
    }
    blue.turn {
      doTasks(
          "UseStandardProject<AsteroidProject>",
          "14 Pay<> FROM MC",
          "3 MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    red.turn {
      // 02:00:06: heat conversion then paid asteroid inferred from audio and bonus ocean.
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
      doTasks(
          "UseStandardProject<AsteroidProject>",
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Elysium_4_6>",
          "TerraformRating",
          "Plant",
          "4 MC",
      )
      blue.doTasks("2 Plant")
    }
    green.turn {
      // Heat conversion evidenced by 24 -> 25 TR; grouping with patent sale is provisional.
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
      doTasks("UseStandardProject<SellPatentsProject>", "3 MC FROM ProjectCard")
    }
    black.turn {
      // Two heat conversions; turn grouping provisional.
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
    }
    blue.turn {
      doTasks(
          "UseStandardProject<AsteroidProject>",
          "14 Pay<> FROM MC",
          "3 MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    red.turn {
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_5_7>", "2 Plant", "2 MC", "0 OxygenStep")
    }
    green.turn { doTasks("Pass") }
    black.turn {
      // Search for Life timing inferred.
      doTasks(
          "UseCardAction<$SearchForLife, Action1>",
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<MicrobeTag>>, $SearchForLife>",
          "-ProjectCard<Revealed>",
      )
    }
    blue.turn {
      doTasks(
          "PlayProject<Class<$TerraformingGanymede>>",
          "2 Pay<Class<Titanium>> FROM Titanium",
          "23 Pay<> FROM MC",
          "$TerraformingGanymede FROM ProjectCard",
          "4 MC",
          "3 TerraformRating",
      )
    }
    red.turn {
      green.assertCounts(3 to "PlayedEvent") // visual confirmation
      black.assertCounts(2 to "PlayedEvent") // visual confirmation
      blue.assertCounts(3 to "PlayedEvent") // visual confirmation
      assertCounts(5 to "PlayedEvent") // visual confirmation
      doTasks(
          "PlayProject<Class<$MediaArchives>>",
          "8 Pay<> FROM MC",
          "$MediaArchives FROM ProjectCard",
          "13 MC",
      )
      doTasks(
          "PlayProject<Class<$Trees>>",
          "13 Pay<> FROM MC",
          "$Trees FROM ProjectCard",
          "PROD[3 Plant]",
          "Plant",
          "Microbe<$Decomposers>",
      )
    }
    black.turn {
      doTasks(
          "PlayProject<Class<$EcologicalZone>>",
          "12 Pay<> FROM MC",
          "$EcologicalZone FROM ProjectCard",
          "EcologicalZone_SpecialTile<Elysium_3_2>",
      )
      repeat(2) {
        selectTask("Animal<$EcologicalZone>*")
      }
      repeat(2) {
        selectTask("Plant! OR Animal<$EcologicalZone>* OR Ok")
        doTasks("Animal<$EcologicalZone>")
      }
      doTasks(
          "PlayProject<Class<$Farming>>",
          "16 Pay<> FROM MC",
          "$Farming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[2 Plant]",
          "2 Plant",
          "Plant",
          "Animal<$EcologicalZone>",
      )
    }
    blue.turn {
      assertCounts(19 to "MC") // visual confirmation
      doTasks(
          "PlayProject<Class<$ImportedNitrogen>>",
          "19 Pay<> FROM MC",
      )
      assertCounts(0 to "MC") // visual confirmation
      doTasks(
          "$ImportedNitrogen FROM ProjectCard",
          "4 MC",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "4 Plant",
      )
      selectTask("3* Microbe")
      selectTask("2* Animal")
      doTasks("PlayedEvent<Class<$ImportedNitrogen>> FROM $ImportedNitrogen")
      assertCounts(7 to "MC") // visual confirmation
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      selectTask("DefaultGreeneryTile")
      doTasks("GreeneryTile<Elysium_7_5>", "0 OxygenStep")
    }
    red.turn { doTasks("Pass") }
    black.turn {
      // Virus removes no animals; missed Viral Enhancers rebate recovered at 02:08:38.
      doTasks(
          "PlayProject<Class<$Virus>>",
          "Pay<> FROM MC",
          "$Virus FROM ProjectCard",
          "-0 Plant<Anyone>",
          "Plant",
          "3 MC",
          "PlayedEvent<Class<$Virus>> FROM $Virus",
      )
      doTasks(
          "PlayProject<Class<$Zeppelins>>",
          "13 Pay<> FROM MC",
          "$Zeppelins FROM ProjectCard",
          "PROD[9 MC]",
      )
    }
    blue.turn {
      // Patent sale before Probe is provisional.
      doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
      doTasks(
          "PlayProject<Class<$TransNeptuneProbe>>",
          "2 Pay<> FROM MC",
          "$TransNeptuneProbe FROM ProjectCard",
      )
    }
    black.turn { doTasks("Pass") }
    blue.turn { doTasks("Pass") }
    blue.assertCounts(
        40 to "TerraformRating",
        9 to "OceanTile",
        14 to "OxygenStep",
        19 to "TemperatureStep",
    )
    red.assertCounts(31 to "TerraformRating")
    green.assertCounts(25 to "TerraformRating", 6 to "Animal<$Predators>")
    black.assertCounts(33 to "TerraformRating", 5 to "Animal<$EcologicalZone>")

    // Final production and greenery, 02:10:45–02:12:25.
    blue.doTasks("59 MC", "2 Titanium", "6 Plant", "2 Heat")
    red.doTasks("Heat FROM Energy", "36 MC", "Steel", "12 Plant", "2 Heat")
    green.doTasks("36 MC", "5 Steel", "4 Titanium", "2 Plant", "Heat")
    black.doTasks(
        "8 Heat FROM Energy",
        "68 MC",
        "Steel",
        "Titanium",
        "6 Plant",
        "9 Energy",
        "6 Heat",
    )
    black.doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
    black.selectTask("DefaultGreeneryTile")
    black.doTasks("GreeneryTile<Elysium_4_2>", "Plant", "Done")
    blue.doTasks("Done")
    red.doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
    red.selectTask("DefaultGreeneryTile")
    red.doTasks("GreeneryTile<Elysium_6_3>", "Plant")
    red.doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
    red.selectTask("DefaultGreeneryTile")
    red.doTasks("GreeneryTile<Elysium_8_5>", "Done")
    green.doTasks("Done")

    // 02:12:25 final map.
    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -               W    [G4]  [O]   [O]   [G1]
        |
        | 2 -            VT    L    [S3]  [O]   [O]   [G1]
        |
        | 3 -        VTT   [S4]  [G1]  [C1]  [O]   [O]   [C1]
        |
        | 4 -      LP   [G4]  [G4]  [O]   [G2]  [O]    WP   [S3]
        |
        | 5 -  LPP   [G4]  [C4]  [O]   [G2]  [C2]  [G2]  LPP   [S3]
        |
        | 6 -      LS   [G2]  [G4]  [C1]  [G2]  [G2]  [C3]   L
        |
        | 7 -        [S3]  [C2]  [G1]  [G1]   LS   [S2]   L
        |
        | 8 -           [G2]  [G2]  [C3]   L    [C3]   L
        |
        | 9 -               LS    L     LC    LC   [G3]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
    val summary = Summarizer(game)
    assertEquals(6, summary.net("CityTile<Green>", "VictoryPoint<Green>"))
    assertEquals(6, summary.net("$Predators", "VictoryPoint<Green>"))
    assertEquals(3, summary.net("$ImmigrationShuttles", "VictoryPoint<Green>"))
    blue.assertCounts(77 to "VictoryPoint", 1 to "Victory")
    red.assertCounts(70 to "VictoryPoint", 0 to "Victory")
    green.assertCounts(57 to "VictoryPoint", 0 to "Victory")
    black.assertCounts(60 to "VictoryPoint", 0 to "Victory")
    assertEquals(true, game.isIdle())
    blue.assertCounts(0 to "ProjectCard<Hand>")
    red.assertCounts(2 to "ProjectCard<Hand>")
    green.assertCounts(6 to "ProjectCard<Hand>")
    black.assertCounts(1 to "ProjectCard<Hand>")
  }
}
