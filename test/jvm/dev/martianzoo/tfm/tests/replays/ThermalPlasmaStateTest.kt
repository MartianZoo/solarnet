package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

// Sources: _local/replays/Game20260817/full-log-pb64886c6e682.txt and
// _local/replays/Game20260817/player-pb64886c6e682.json. The complete Research offers come from the
// earlier clone of the same source game at /the-end?id=pe59a6b631bd6.
internal class ThermalPlasmaStateTest : FullGameTest() {
  override val config =
      GameConfig(
          """
          ElysiumMap
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, TerraLabsResearch, promos:cards
          Tr63SoloObjective
          Ganymede, Luna, Miranda, Pluto
          """,
          "Me",
      )

  // Drew and discarded Orbital Reflectors to place a 2
  // Drew and discarded Lake Marineris to place a 0
  // Drew and discarded Topsoil Contract to place a 2
  // Drew and discarded Space Station to place a 0
  override val soloTileLocations =
      listOf("Elysium_6_4", "Elysium_7_5", "Elysium_8_7", "Elysium_9_8")

  private val me
    get() = p1

  @Test
  internal fun thermalPlasmaState() {
    startGame()
    with(me) {
      doTasks(
          "14 TerraformRating",
          "2 CorporationCard",
          "-CorporationCard",
          "10 ProjectCard<Selecting>",
          "4 PreludeCard",
          "-2 PreludeCard",
          "-SelectedColonyTile<Class<Miranda>>",
          "-2 ProjectCard<Selecting>",
      )
    }
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
    generation12()

    // _local/replays/Game20260817/player-pb64886c6e682.json; final Mars map.
    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -               W     WT   [O]    WS    LC
        |
        | 2 -            VT    L     L    [O]   [O]   LSS
        |
        | 3 -        [S1]   L     LC   [G1]  [G1]  [O]   [S1]
        |
        | 4 -      LP    LP    LP   [O]   [C1]  [O]   [O]   [G1]
        |
        | 5 -  LPP   LPP   LPP   [O]   [G1]  [G1]  [G1]  [G1]  VPT
        |
        | 6 -      LS    LP   [C]    LP    LP    LP    LP    L
        |
        | 7 -         LT    LS   [G]    L     LS    L     L
        |
        | 8 -           LSS    L     L    [C]   LSS    L
        |
        | 9 -               LS    L     LC   [G]   LSS
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
  }

  private fun generation1() {
    with(me) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$TerraLabsResearch>>",
          "14 MC",
          "-TerraformRating",
          "8 Pay<> FROM MC",
          "8 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BusinessEmpire>>",
          "PROD[6 MC]",
          "-6 MC",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
          "PROD[MC]",
          "PROD[Steel]",
          "PROD[Titanium]",
      )

      assertCounts(0 to "MC")
      doTask("Pass")
      doTasks(
          "18 MC",
          "Steel",
          "Titanium",
          "VenusStep! BY Admin",
      )
    }
  }

  private fun generation2() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-3 ProjectCard<Selecting>",
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )

      assertCounts(17 to "MC")
      doTask("Pass")
      doTasks(
          "18 MC",
          "Steel",
          "Titanium",
          "VenusStep! BY Admin",
      )
    }
  }

  private fun generation3() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "2 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(
            *playCardTasks("$FueledGenerators"),
            "Ok", // no steel paid
            "1 Pay<> FROM MC",
            "PROD[-MC]",
            "PROD[Energy]",
        )
        doTasks(
            *playCardTasks("$Research"),
            "11 Pay<> FROM MC",
            "2 ProjectCard",
        )
        doTasks(
            *playCardTasks("$AiCentral"),
            "2 Pay<Class<Steel>> FROM Steel",
            "17 Pay<> FROM MC",
            "PROD[-Energy]",
        )
        doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
        doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$VestaShipyard"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "9 Pay<> FROM MC",
            "PROD[Titanium]",
        )

        assertCounts(0 to "MC")
      }
      doTask("Pass")
      doTasks(
          "17 MC",
          "Steel",
          "2 Titanium",
          "VenusStep! BY Admin",
      )
    }
  }

  private fun generation4() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
        doTasks(
            *playCardTasks("$SolarWindPower"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "5 Pay<> FROM MC",
            "PROD[Energy]",
            "2 Titanium",
        )
        doTasks(
            *playCardTasks("$GeothermalPower"),
            "1 Pay<Class<Steel>> FROM Steel",
            "9 Pay<> FROM MC",
            "PROD[2 Energy]",
        )

        assertCounts(0 to "MC")
      }
      doTask("Pass")
      doTasks(
          "17 MC",
          "Steel",
          "2 Titanium",
          "3 Energy",
          "TemperatureStep! BY Admin",
      )
    }
  }

  private fun generation5() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
        doTasks(*playCardTasks("$MediaGroup"), "6 Pay<> FROM MC")
        doTasks(
            *playCardTasks("$ProjectInspection"),
            "3 MC",
            "UseAction<$AiCentral, Action1>",
            "2 ProjectCard",
        )
        doTasks(
            *playCardTasks("$RotatorImpacts"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "0 Pay<> FROM MC", // no MC remainder
        )
        doTasks(
            *cardActionTasks("$RotatorImpacts"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "0 Pay<> FROM MC", // no MC remainder
            "Asteroid<$RotatorImpacts>",
        )
        doTasks(
            "UseAction<TradeAction, Action2>",
            "3 Pay<Class<Energy>> FROM Energy",
            "Trade<Luna>",
            "13 MC",
        )
        doTasks(
            *playCardTasks("$RestrictedArea"),
            "11 Pay<> FROM MC",
            "RestrictedArea_SpecialTile<Elysium_3_7>",
            "3 ProjectCard",
        )
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$Conscription"),
            "5 Pay<> FROM MC",
            "Conscription_NextCardEffect",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$LunarBeam"),
            "PROD[-2 MC]",
            "PROD[2 Heat]",
            "PROD[2 Energy]",
        )
        doTasks(
            *playCardTasks("$ArtificialPhotosynthesis"),
            "12 Pay<> FROM MC",
            "PROD[2 Energy]",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$Extremophiles"),
            "3 Pay<> FROM MC",
        )
        doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$Extremophiles>")

        assertCounts(0 to "MC")
      }
      doTask("Pass")
      doTasks(
          "15 MC",
          "Steel",
          "2 Titanium",
          "7 Energy",
          "2 Heat",
          "TemperatureStep! BY Admin",
      )
    }
  }

  private fun generation6() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "Ok", // keep all offered cards
          "4 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
        doTasks(
            *cardActionTasks("$RotatorImpacts", "Action2"),
            "-Asteroid<$RotatorImpacts>",
            "VenusStep",
            "TerraformRating",
            "ProjectCard",
        )
        doTasks(
            "UseAction<TradeAction, Action2>",
            "3 Pay<Class<Energy>> FROM Energy",
            "Trade<Pluto>",
            "4 ProjectCard",
        )
        doTasks(
            *playCardTasks("$IndenturedWorkers"),
            "IndenturedWorkers_NextCardEffect",
            "3 MC",
        )
        doTasks(*playCardTasks("$ForcedPrecipitation"))
        doTasks(
            *cardActionTasks("$ForcedPrecipitation"),
            "2 Pay<> FROM MC",
            "Floater<$ForcedPrecipitation>",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$Steelworks"),
            "2 Pay<Class<Steel>> FROM Steel",
            "11 Pay<> FROM MC",
        )
        doTasks(
            *cardActionTasks("$Steelworks"),
            "4 Pay<Class<Energy>> FROM Energy",
            "2 Steel",
            "OxygenStep",
            "TerraformRating",
        )
        doTasks(
            *playCardTasks("$ImportOfAdvancedGhg"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "3 Pay<> FROM MC",
            "PROD[2 Heat]",
            "3 MC",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$SulphurEatingBacteria"),
            "6 Pay<> FROM MC",
        )
        doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
        doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$SulphurEatingBacteria>")

        assertCounts(0 to "MC")
      }
      doTask("Pass")
      doTasks(
          "17 MC",
          "Steel",
          "2 Titanium",
          "7 Energy",
          "4 Heat",
          "OceanTile<Elysium_3_6>! BY Admin",
      )
    }
  }

  private fun generation7() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "Ok", // keep all offered cards
          "4 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            *cardActionTasks("$Steelworks"),
            "4 Pay<Class<Energy>> FROM Energy",
            "2 Steel",
            "OxygenStep",
            "TerraformRating",
        )
        doTasks(
            "UseAction<TradeAction, Action2>",
            "3 Pay<Class<Energy>> FROM Energy",
            "Trade<Ganymede>",
            "6 Plant",
        )
        doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$SulphurEatingBacteria>")
        doTasks(
            *cardActionTasks("$ForcedPrecipitation"),
            "2 Pay<> FROM MC",
            "Floater<$ForcedPrecipitation>",
        )
        doTasks(
            *playCardTasks("$InvestmentLoan"),
            "3 Pay<> FROM MC",
            "PROD[-MC]",
            "13 MC",
        )
        doTasks(
            *playCardTasks("$Solarnet"),
            "7 Pay<> FROM MC",
            "2 ProjectCard",
        )
        doTasks(
            *cardActionTasks("$SulphurEatingBacteria", "Action2"),
            "-3 Microbe<$SulphurEatingBacteria>",
            "9 MC",
        )
        doTasks(
            *playCardTasks("$BigAsteroid"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "21 Pay<> FROM MC",
            "TemperatureStep",
            "TemperatureStep",
            "4 Titanium",
            "3 MC",
            "TerraformRating",
            "PROD[Heat]",
            "TerraformRating",
            "Ok", // decline removing an opponent's plants
        )
        doTasks(
            *cardActionTasks("$RotatorImpacts"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "0 Pay<> FROM MC", // no MC remainder
            "Asteroid<$RotatorImpacts>",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$LunaGovernor"),
            "4 Pay<> FROM MC",
            "PROD[2 MC]",
        )

        assertCounts(2 to "MC")
      }
      doTask("Pass")
      doTasks(
          "21 MC",
          "Steel",
          "2 Titanium",
          "7 Energy",
          "5 Heat",
          "OceanTile<Elysium_2_4>! BY Admin",
      )
    }
  }

  private fun generation8() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "2 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
            "PROD[Heat]",
        )
        doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            *cardActionTasks("$ForcedPrecipitation", "Action2"),
            "-2 Floater<$ForcedPrecipitation>",
            "VenusStep",
            "TerraformRating",
        )
        doTasks(
            *cardActionTasks("$RotatorImpacts", "Action2"),
            "-Asteroid<$RotatorImpacts>",
            "VenusStep",
            "TerraformRating",
        )
        doTasks(
            *cardActionTasks("$Steelworks"),
            "4 Pay<Class<Energy>> FROM Energy",
            "2 Steel",
            "OxygenStep",
            "TerraformRating",
        )
        doTasks(
            "UseAction<TradeAction, Action2>",
            "3 Pay<Class<Energy>> FROM Energy",
            "Trade<Luna>",
            "7 MC",
        )
        doTasks(
            *playCardTasks("$OlympusConference"),
            "5 Pay<Class<Steel>> FROM Steel",
            "0 Pay<> FROM MC", // no MC remainder
            "Science<$OlympusConference>",
        )
        doTasks(
            *playCardTasks("$StaticHarvesting"),
            "5 Pay<> FROM MC",
            "PROD[Energy]",
            "5 MC",
        )
        doTasks(
            *playCardTasks("$ProtectedGrowth"),
            "2 Pay<> FROM MC",
            "5 Plant",
            "3 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Elysium_4_8>",
            "OxygenStep",
            "Plant",
            "Steel",
            "TerraformRating",
        )
        doTasks(
            *playCardTasks("$AtalantaPlanitiaLab"),
            "10 Pay<> FROM MC",
            "2 ProjectCard",
            "ProjectCard FROM Science",
        )
        doTasks(
            *playCardTasks("$ImportedGhg"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "1 Pay<> FROM MC",
            "PROD[Heat]",
            "3 Heat",
            "3 MC",
        )
        doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
        doTasks(
            *playCardTasks("$RegolithEaters"),
            "13 Pay<> FROM MC",
            "Science<$OlympusConference>",
        )
        doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
        doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$RegolithEaters>")
        doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$Worms"),
            "8 Pay<> FROM MC",
            "PROD[2 Plant]",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")

        assertCounts(2 to "MC")
      }
      doTask("Pass")
      doTasks(
          "26 MC",
          "Steel",
          "2 Titanium",
          "8 Energy",
          "7 Heat",
          "2 Plant",
          "TemperatureStep! BY Admin",
      )
    }
  }

  private fun generation9() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            *cardActionTasks("$Steelworks"),
            "4 Pay<Class<Energy>> FROM Energy",
            "2 Steel",
            "OxygenStep",
            "TerraformRating",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$BusinessNetwork"),
            "4 Pay<> FROM MC",
            "PROD[-MC]",
        )
        doTasks(
            *cardActionTasks("$BusinessNetwork"),
            "ProjectCard<Selecting>",
            "Ok", // keep all offered cards
            "Pay<> FROM MC",
            "ProjectCard<Hand FROM Selecting>",
        )
        doTasks(
            *cardActionTasks("$RotatorImpacts"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "0 Pay<> FROM MC", // no MC remainder
            "Asteroid<$RotatorImpacts>",
        )
        doTasks(
            *playCardTasks("$AirScrappingExpedition"),
            "13 Pay<> FROM MC",
            "VenusStep",
            "3 MC",
            "TerraformRating",
            "3 Floater<$ForcedPrecipitation>",
        )
        doTasks(
            *cardActionTasks("$ForcedPrecipitation", "Action2"),
            "-2 Floater<$ForcedPrecipitation>",
            "VenusStep",
            "2 TerraformRating",
        )
        doTasks(
            *cardActionTasks("$RegolithEaters", "Action2"),
            "-2 Microbe<$RegolithEaters>",
            "OxygenStep",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$RegolithEaters>")
        doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
        doTasks(
            *playCardTasks("$SoilEnrichment"),
            "6 Pay<> FROM MC",
            "5 Plant",
            "3 MC",
            "-Microbe<$Extremophiles>",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Elysium_5_8>",
            "OxygenStep",
            "2 Plant",
            "TerraformRating",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$Thermophiles"),
            "9 Pay<> FROM MC",
        )
        doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")

        assertCounts(0 to "MC")
      }
      doTask("Pass")
      doTasks(
          "32 MC",
          "Steel",
          "2 Titanium",
          "8 Energy",
          "7 Heat",
          "2 Plant",
          "4 Heat FROM Energy",
          "TemperatureStep! BY Admin",
      )
    }
  }

  private fun generation10() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "Ok", // keep all offered cards
          "4 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
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
        doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            *cardActionTasks("$BusinessNetwork"),
            "ProjectCard<Selecting>",
            "Ok", // keep all offered cards
            "Pay<> FROM MC",
            "ProjectCard<Hand FROM Selecting>",
        )
        doTasks(
            *cardActionTasks("$RotatorImpacts", "Action2"),
            "-Asteroid<$RotatorImpacts>",
            "VenusStep",
            "TerraformRating",
        )
        doTasks(
            *cardActionTasks("$Steelworks"),
            "4 Pay<Class<Energy>> FROM Energy",
            "2 Steel",
            "OxygenStep",
            "TerraformRating",
            "TemperatureStep",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$RegolithEaters>")
        doTasks(
            *cardActionTasks("$RegolithEaters", "Action2"),
            "-2 Microbe<$RegolithEaters>",
            "OxygenStep",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
        doTasks(
            *cardActionTasks("$ForcedPrecipitation"),
            "2 Pay<> FROM MC",
            "Floater<$ForcedPrecipitation>",
        )
        doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$Thermophiles>")
        doTasks(
            *playCardTasks("$CarbonateProcessing"),
            "3 Pay<Class<Steel>> FROM Steel",
            "0 Pay<> FROM MC", // no MC remainder
            "PROD[-Energy]",
            "PROD[3 Heat]",
        )
        doTasks(
            "UseAction<TradeAction, Action2>",
            "3 Pay<Class<Energy>> FROM Energy",
            "Trade<Pluto>",
            "3 ProjectCard",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "6 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$BlackPolarDust"),
            "15 Pay<> FROM MC",
            "PROD[-2 MC]",
            "PROD[3 Heat]",
            "OceanTile<Elysium_1_3>",
            "TerraformRating",
            "ProjectCard",
            "2 MC",
        )
        doTasks(
            *playCardTasks("$BribedCommittee"),
            "7 Pay<> FROM MC",
            "2 TerraformRating",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$BusinessContacts"),
            "7 Pay<> FROM MC",
            "4 ProjectCard<Selecting>",
            "2 ProjectCard<Hand FROM Selecting>",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$JovianEmbassy"),
            "7 Pay<Class<Steel>> FROM Steel",
            "0 Pay<> FROM MC", // no MC remainder
            "TerraformRating",
        )
        doTasks(
            *playCardTasks("$MineralDeposit"),
            "5 Pay<> FROM MC",
            "5 Steel",
            "3 MC",
        )
        doTasks(
            *playCardTasks("$Windmills"),
            "3 Pay<Class<Steel>> FROM Steel",
            "0 Pay<> FROM MC", // no MC remainder
            "PROD[Energy]",
        )

        assertCounts(6 to "MC")
      }
      doTask("Pass")
      doTasks(
          "40 MC",
          "Steel",
          "2 Titanium",
          "8 Energy",
          "13 Heat",
          "2 Plant",
          "Heat FROM Energy",
          "VenusStep! BY Admin",
      )
    }
  }

  private fun generation11() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-3 ProjectCard<Selecting>",
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(
            "UseAction<TradeAction, Action2>",
            "3 Pay<Class<Energy>> FROM Energy",
            "Trade<Luna>",
            "7 MC",
        )
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
        doTasks(
            *playCardTasks("$InventionContest"),
            "2 Pay<> FROM MC",
            "3 ProjectCard<Selecting>",
            "ProjectCard<Hand FROM Selecting>",
            "3 MC",
            "ProjectCard FROM Science",
        )
        doTasks(
            *playCardTasks("$TowingAComet"),
            "6 Pay<Class<Titanium>> FROM Titanium",
            "5 Pay<> FROM MC",
            "2 Plant",
            "OxygenStep",
            "3 MC",
            "TerraformRating",
            "OceanTile<Elysium_4_6>",
            "TerraformRating",
            "Plant",
            "2 MC",
        )
        doTasks(
            *playCardTasks("$LavaFlows"),
            "18 Pay<> FROM MC",
            "TemperatureStep",
            "TemperatureStep",
            "3 MC",
            "2 TerraformRating",
            "LavaFlows_SpecialTile<Elysium_3_1>",
            "2 Titanium",
        )
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "OceanTile<Elysium_4_7>",
            "TerraformRating",
            "Plant",
            "4 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Elysium_5_7>",
            "OxygenStep",
            "2 Plant",
            "4 MC",
            "TerraformRating",
        )
        doTasks(
            *cardActionTasks("$BusinessNetwork"),
            "ProjectCard<Selecting>",
            "Ok", // keep all offered cards
            "Pay<> FROM MC",
            "ProjectCard<Hand FROM Selecting>",
        )
        doTasks(
            *cardActionTasks("$ForcedPrecipitation", "Action2"),
            "-2 Floater<$ForcedPrecipitation>",
            "VenusStep",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
        doTasks(
            *cardActionTasks("$Thermophiles", "Action2"),
            "-2 Microbe<$Thermophiles>",
            "VenusStep",
            "TerraformRating",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
        doTasks(
            *cardActionTasks("$RotatorImpacts"),
            "2 Pay<Class<Titanium>> FROM Titanium",
            "0 Pay<> FROM MC", // no MC remainder
            "Asteroid<$RotatorImpacts>",
        )
        doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$Thermophiles>")
        doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
        doTasks(
            *playCardTasks("$ExtractorBalloons"),
            "21 Pay<> FROM MC",
            "3 Floater<$ExtractorBalloons>",
        )
        doTasks(
            *playCardTasks("$Airliners"),
            "11 Pay<> FROM MC",
            "PROD[2 MC]",
            "2 Floater<$ForcedPrecipitation>",
        )
        doTasks(
            *cardActionTasks("$ExtractorBalloons", "Action2"),
            "-2 Floater<$ExtractorBalloons>",
            "VenusStep",
            "TerraformRating",
        )

        assertCounts(0 to "MC")
      }
      doTask("Pass")
      doTasks(
          "53 MC",
          "Steel",
          "2 Titanium",
          "8 Energy",
          "13 Heat",
          "2 Plant",
          "5 Heat FROM Energy",
          "OceanTile<Elysium_4_4>! BY Admin",
      )
    }
  }

  private fun generation12() {
    with(me) {
      doTasks(
          "4 ProjectCard<Selecting>",
          "-3 ProjectCard<Selecting>",
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )

      me.turn {
        doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Elysium_5_6>",
            "OxygenStep",
            "3 Plant",
            "2 MC",
            "TerraformRating",
        )
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
        doTasks(*cardActionTasks("$AiCentral"), "2 ProjectCard")
        doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
        doTasks(
            *cardActionTasks("$BusinessNetwork"),
            "ProjectCard<Selecting>",
            "Ok", // keep all offered cards
            "Pay<> FROM MC",
            "ProjectCard<Hand FROM Selecting>",
        )
        doTasks(
            *cardActionTasks("$ForcedPrecipitation", "Action2"),
            "-2 Floater<$ForcedPrecipitation>",
            "VenusStep",
            "TerraformRating",
        )
        doTasks(
            *cardActionTasks("$RotatorImpacts", "Action2"),
            "-Asteroid<$RotatorImpacts>",
            "VenusStep",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
        doTasks(
            *cardActionTasks("$Steelworks"),
            "4 Pay<Class<Energy>> FROM Energy",
            "2 Steel",
            "OxygenStep",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$RegolithEaters>")
        doTasks(
            *cardActionTasks("$RegolithEaters", "Action2"),
            "-2 Microbe<$RegolithEaters>",
            "OxygenStep",
            "TerraformRating",
        )
        doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$SulphurEatingBacteria>")
        doTasks(
            *playCardTasks("$IoSulphurResearch"),
            "17 Pay<> FROM MC",
            "Science<$OlympusConference>",
            "3 ProjectCard",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
        doTasks(
            *playCardTasks("$FusionPower"),
            "6 Pay<Class<Steel>> FROM Steel",
            "2 Pay<> FROM MC",
            "PROD[3 Energy]",
            "ProjectCard FROM Science",
        )
        doTasks(
            *playCardTasks("$InventorsGuild"),
            "9 Pay<> FROM MC",
            "Science<$OlympusConference>",
        )
        doTasks(
            *cardActionTasks("$InventorsGuild"),
            "ProjectCard<Selecting>",
            "Ok", // keep all offered cards
            "Pay<> FROM MC",
            "ProjectCard<Hand FROM Selecting>",
        )
        // The app omitted Air Raid's 5 M€ transfer from the log.
        doTasks(
            *playCardTasks("$AirRaid"),
            "-Floater",
            "3 MC",
            "5 MC FROM MC<SoloOpponent>",
        )
        doTasks(
            *playCardTasks("$BactoviralResearch"),
            "10 Pay<> FROM MC",
            "ProjectCard",
            "ProjectCard FROM Science",
            "14 Microbe<$SulphurEatingBacteria>",
        )
        doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
        doTasks(
            *cardActionTasks("$SulphurEatingBacteria", "Action2"),
            "-19 Microbe<$SulphurEatingBacteria>",
            "57 MC",
        )
        doTasks(
            *playCardTasks("$ReleaseOfInertGases"),
            "14 Pay<> FROM MC",
            "2 TerraformRating",
            "3 MC",
        )
        doTasks(
            *standardProjectTasks("AquiferProject"),
            "18 Pay<> FROM MC",
            "OceanTile<Elysium_2_5>",
            "TerraformRating",
            "4 MC",
        )
        doTasks(
            *playCardTasks("$Mangrove"),
            "12 Pay<> FROM MC",
            "GreeneryTile<Elysium_3_5>",
            "OxygenStep",
            "Plant",
            "8 MC",
        )
        doTasks(
            *standardProjectTasks("CityProject"),
            "25 Pay<> FROM MC",
            "PROD[MC]",
            "CityTile<Elysium_4_5>",
            "Plant",
            "4 MC",
        )
        doTasks(
            *playCardTasks("$Algae"),
            "10 Pay<> FROM MC",
            "Plant",
            "PROD[2 Plant]",
        )
        doTasks(
            "UseAction<TradeAction, Action2>",
            "3 Pay<Class<Energy>> FROM Energy",
            "Trade<Ganymede>",
            "5 Plant",
        )
        doTasks(
            *standardProjectTasks("AquiferProject"),
            "18 Pay<> FROM MC",
            "OceanTile<Elysium_5_4>",
            "TerraformRating",
            "2 Plant",
            "2 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Elysium_5_5>",
            "OxygenStep",
            "2 Plant",
            "4 MC",
        )
        doTasks(
            "UseAction<ConvertPlantsAction, Action1>",
            "8 Pay<Class<Plant>> FROM Plant",
            "GreeneryTile<Elysium_3_4>",
            "OxygenStep",
            "4 MC",
        )
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
        )
        assertCounts(3 to "MC")
      }
      doTask("Pass")
      doTasks("67 MC", "Steel", "2 Titanium", "11 Energy", "13 Heat", "4 Plant", "Heat FROM Energy")
      // Decline the final greenery placement.
      doTask("Ok") // decline final greenery
      assertCounts(0 to "ProjectCard")
      // Final state and score come from /api/player?id=pb64886c6e682.
      assertProduction(m = 3, s = 1, t = 2, p = 4, e = 11, h = 13)
      assertResources(m = 70, s = 1, t = 4, p = 4, e = 11, h = 14)
      assertCounts(64 to "TerraformRating", 0 to "ProjectCard", 18 to "PlayedEvent")
      assertSidebar(gen = 12, temp = 8, oxygen = 14, oceans = 8, venus = 30)
      has("Victory") shouldBe true

      doTask("84 VictoryPoint")

      assertCounts(84 to "VictoryPoint")
    }
  }
}

/*
 * Implementation note: complete Research deck order from the earlier source-game clone:
 *
 * RadSuits, SoilFactory, FueledGenerators, NeptunianPowerConsultants, AiCentral, Research,
 * SecurityFleet, ImportOfAdvancedGhg, UrbanDecomposers, FreyjaBiodomes, ProductiveOutpost,
 * Solarnet, InterplanetaryTrade, MolecularPrinting, LunarBeam, LawSuit, NoctisFarming,
 * VestaShipyard, SolarWindPower, ArtificialPhotosynthesis, Steelworks, Heather,
 * AcquiredCompany, LavaTubeSettlement, FoodFactory, Conscription, SulphurEatingBacteria,
 * IndenturedWorkers, Harvest, StaticHarvesting, LunaGovernor, JetStreamMicroscrappers,
 * InvestmentLoan, TundraFarming, TradingColony, RegolithEaters, Herbivores, Satellites,
 * MineralDeposit, FuelFactory, BusinessNetwork, Potatoes, DirectedHeatUsage,
 * CarbonateProcessing, PioneerSettlement, InterstellarColonyShip, Archaebacteria,
 * OutdoorSports, MiningRights, Airliners, CrashSiteCleanup, QuantumExtractor,
 * AsteroidHollowing, InventorsGuild
 */
