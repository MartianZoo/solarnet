package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import kotlin.test.Test

internal class FirstPartialGameTest : AbstractFullGameTest() {
  override val config =
      GameConfig(
          "ElysiumMap, PreludeExpansion, LakefrontResorts",
          "Player1",
          "Player2",
      )

  @Test
  internal fun fourWholeGenerations() {
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE
    admin.startGame()
    p1.doTask("20 TerraformRating")
    p2.doTask("20 TerraformRating")
    p1.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-7 ProjectCard<Selecting>",
    )
    p2.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-2 ProjectCard<Selecting>",
    )
    p1.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$LakefrontResorts>>",
          "$LakefrontResorts FROM CorporationCard",
          "54 MC",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
    }
    p2.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$InterplanetaryCinematics>>",
          "$InterplanetaryCinematics FROM CorporationCard",
          "30 MC",
          "20 Steel",
          "24 Pay<> FROM MC",
          "8 ProjectCard<Hand FROM Selecting>",
      )
    }

    p1.turn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MartianIndustries>>",
          "$MartianIndustries FROM PreludeCard",
          "PROD[Energy]",
          "PROD[Steel]",
          "6 MC",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$GalileanMining>>",
          "$GalileanMining FROM PreludeCard",
          "PROD[2 Titanium]",
          "-5 MC",
      )
    }
    p2.turn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MiningOperations>>",
          "$MiningOperations FROM PreludeCard",
          "PROD[2 Steel]",
          "4 Steel",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>",
          "$UnmiContractor FROM PreludeCard",
          "3 TerraformRating",
          "ProjectCard",
      )
    }

    // Generation 1 (Player1 first)
    p1.turn {
      doTasks(
          *playCardTasks("$AsteroidMining"),
          "30 Pay<> FROM MC",
          "$AsteroidMining FROM ProjectCard",
          "PROD[2 Titanium]",
      )
    }
    p2.turn {
      doTasks(
          *playCardTasks("$NaturalPreserve"),
          "4 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$NaturalPreserve FROM ProjectCard",
          "PROD[MC]",
          "NaturalPreserve_SpecialTile<Elysium_3_7>",
          "3 ProjectCard",
      )
    }
    p1.doTask("Pass")
    p2.turn {
      doTasks(
          *playCardTasks("$SpaceElevator"),
          "13 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTasks(*cardActionTasks("$SpaceElevator"), "1 Pay<Class<Steel>> FROM Steel", "5 MC")
      doTasks(
          *playCardTasks("$InventionContest"),
          "2 Pay<> FROM MC",
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$GreatEscarpmentConsortium"),
          "6 Pay<> FROM MC",
          "$GreatEscarpmentConsortium FROM ProjectCard",
          "PROD[Steel]",
          "PROD[-Steel<Player1>]",
      )
    }
    p2.doTask("Pass")
    p1.doTasks("-5 MC", "5 MC", "20 MC", "Energy", "4 Titanium")
    p2.doTasks("6 MC", "-5 MC", "23 MC", "3 Steel", "Titanium")

    // Generation 2 (Player2 first)
    p1.doTasks(
        "4 ProjectCard<Selecting>",
        "Ok", // keep all four Research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "4 ProjectCard<Selecting>",
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    p2.turn {
      doTasks(*cardActionTasks("$SpaceElevator"), "1 Pay<Class<Steel>> FROM Steel", "5 MC")
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$TitaniumMine"),
          "7 Pay<> FROM MC",
          "$TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTasks(
          *playCardTasks("$RoboticWorkforce"),
          "9 Pay<> FROM MC",
          "$RoboticWorkforce FROM ProjectCard",
          "CopyProductionBox<$MartianIndustries>",
          "PROD[Energy]",
          "PROD[Steel]",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$IndustrialMicrobes"),
          "5 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$IndustrialMicrobes FROM ProjectCard",
          "PROD[Energy]",
          "PROD[Steel]",
      )
      doTasks(
          *playCardTasks("$TechnologyDemonstration"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$TechnologyDemonstration FROM ProjectCard",
          "2 ProjectCard",
          "2 MC",
      )
    }

    p1.turn {
      doTasks(
          *playCardTasks("$Sponsors"),
          "6 Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "1 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Player1>]",
      )
      doTasks(
          *playCardTasks("$BuildingIndustries"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$BuildingIndustries FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 Steel]",
      )
    }

    p1.doTask("Pass")
    p2.doTask("Pass")
    p1.doTasks("7 MC", "-5 MC", "20 MC", "Energy", "5 Titanium", "Heat FROM Energy", "Steel")
    p2.doTasks("-5 MC", "6 MC", "23 MC", "6 Steel", "Titanium", "Energy")

    // Generation 3 (Player1 first)
    p1.doTasks(
        "4 ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "4 ProjectCard<Selecting>",
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    p1.turn {
      doTasks(
          *playCardTasks("$Mine"),
          "1 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$Mine FROM ProjectCard",
          "PROD[Steel]",
      )
    }

    p2.turn {
      doTasks(*cardActionTasks("$SpaceElevator"), "1 Pay<Class<Steel>> FROM Steel", "5 MC")
      doTasks(
          *playCardTasks("$ElectroCatapult"),
          "5 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$ElectroCatapult FROM ProjectCard",
          "PROD[-Energy]",
      )
    }

    p1.doTask("Pass")

    p2.turn {
      doTasks(
          *cardActionTasks("$ElectroCatapult", "Action2"),
          "1 Pay<Class<Steel>> FROM Steel",
          "7 MC",
      )
      doTasks(
          *playCardTasks("$SpaceHotels"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$SpaceHotels FROM ProjectCard",
          "PROD[4 MC]",
      )
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "6 Pay<> FROM MC",
          "$MarsUniversity FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$ArtificialPhotosynthesis"),
          "10 Pay<> FROM MC",
          "$ArtificialPhotosynthesis FROM ProjectCard",
          "PROD[2 Energy]",
          "Ok", // decline Mars University's optional discard and draw
      )
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "5 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
          "2 MC",
      )
      doTask("Pass")
    }

    p1.doTasks("-5 MC", "7 MC", "20 MC", "Energy", "5 Titanium", "2 Steel", "Heat FROM Energy")
    p2.doTasks("-5 MC", "10 MC", "25 MC", "6 Steel", "Titanium", "Heat FROM Energy", "2 Energy")

    // Generation 4 (Player2 first)
    p1.doTasks(
        "4 ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "4 ProjectCard<Selecting>",
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    p2.turn {
      doTasks(
          *cardActionTasks("$ElectroCatapult", "Action2"),
          "1 Pay<Class<Steel>> FROM Steel",
          "7 MC",
      )
      doTasks(*cardActionTasks("$SpaceElevator"), "1 Pay<Class<Steel>> FROM Steel", "5 MC")
    }

    p1.turn {
      doTasks(
          *playCardTasks("$ResearchOutpost"),
          "2 Pay<Class<Steel>> FROM Steel",
          "14 Pay<> FROM MC",
          "$ResearchOutpost FROM ProjectCard",
          "CityTile<Elysium_5_6>",
          "3 Plant",
          "0 MC", // Lakefront Resorts: no neighboring oceans
      )
      doTasks(
          *playCardTasks("$IoMiningIndustries"),
          "13 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$IoMiningIndustries FROM ProjectCard",
          "PROD[2 Titanium]",
          "PROD[2 MC]",
      )
    }

    p2.turn {
      doTasks(
          *playCardTasks("$TransNeptuneProbe"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$TransNeptuneProbe FROM ProjectCard",
          "Ok", // decline Mars University's optional discard and draw
      )
      doTasks(
          *playCardTasks("$Hackers"),
          "1 Pay<> FROM MC",
          "$Hackers FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "PROD[-2 MC<Player1>]",
      )
    }

    p1.turn { doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard") }

    p2.turn {
      doTasks(
          *playCardTasks("$SolarPower"),
          "4 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$SolarPower FROM ProjectCard",
          "PROD[Energy]",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Elysium_6_5>",
          "Plant",
      )
    }

    p1.doTask("Pass")
    p2.doTask("Pass")
    p1.doTasks("-5 MC", "7 MC", "20 MC", "Energy", "7 Titanium", "2 Steel", "Heat FROM Energy")
    p2.doTasks("-5 MC", "13 MC", "25 MC", "6 Steel", "Titanium", "2 Energy", "2 Heat FROM Energy")
    p1.doTask("4 ProjectCard<Selecting>")
    p2.doTask("4 ProjectCard<Selecting>")

    // Four generations are complete; the game now offers generation 5 research.
    admin.assertCounts(5 to "Generation")
    admin.assertCounts(0 to "OceanTile", 0 to "OxygenStep", 0 to "TemperatureStep")

    with(p1) {
      assertCounts(20 to "TerraformRating")

      assertCounts(
          34 to "MC",
          2 to "Steel",
          8 to "Titanium",
          3 to "Plant",
          1 to "Energy",
          3 to "Heat",
      )
      assertProds(
          2 to "MC",
          2 to "Steel",
          7 to "Titanium",
          0 to "Plant",
          1 to "Energy",
          0 to "Heat",
      )

      assertCounts(
          19 to "Card",
          5 to "ProjectCard",
          4 to "ProjectCard<Selecting>",
          10 to "CardFront",
      )
      assertCounts(1 to "ActiveCard", 6 to "AutomatedCard", 0 to "PlayedEvent")

      assertTags(but = 5, spt = 2, sct = 2, eat = 1, jot = 3, cit = 1)

      assertCounts(1 to "CityTile", 0 to "GreeneryTile", 0 to "SpecialTile")
    }

    with(p2) {
      assertCounts(25 to "TerraformRating")

      assertCounts(
          47 to "MC",
          6 to "Steel",
          1 to "Titanium",
          1 to "Plant",
          2 to "Energy",
          3 to "Heat",
      )
      assertProds(
          8 to "MC",
          6 to "Steel",
          1 to "Titanium",
          0 to "Plant",
          2 to "Energy",
          0 to "Heat",
      )

      assertCounts(
          27 to "Card",
          3 to "ProjectCard",
          4 to "ProjectCard<Selecting>",
          17 to "CardFront",
      )
      assertCounts(4 to "ActiveCard", 10 to "AutomatedCard", 3 to "PlayedEvent")

      assertTags(but = 9, spt = 3, sct = 4, pot = 2, eat = 3, mit = 1)

      assertCounts(1 to "CityTile", 0 to "GreeneryTile", 1 to "SpecialTile")
    }
    expectPendingTasksAtEnd(4) // generation 5 Research offers and deferred phase work
  }
}
