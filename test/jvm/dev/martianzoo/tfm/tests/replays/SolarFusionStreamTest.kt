package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Source: _local/replays/Game20260819/ (game log, player records, and generation-6 screenshot)
// Complete archive replay: Solar Fusion Stream (g4ce040d78bb6)
// https://terraforming-mars.herokuapp.com/the-end?id=pc2de3208e4ca
internal class SolarFusionStreamTest : FullGameTest() {
  // Player-record evidence: Elysium, Corporate Era, Prelude, promo cards, drafting, fast mode,
  // three players, and these limited-synergy milestone and award pools.
  // Unsupported component: unclaimed Terraformer substitutes for unclaimed Hydrologist.
  // The thresholds select the archived Builder and Terraformer variants.
  override val config =
      GameConfig(
          """
          ElysiumMap
          PreludeExpansion, promos:cards
          fakes:cards

          Builder, Philantropist, Spacefarer, Terraformer, Energizer
          Incorporator, Botanist, Founder, Benefactor, Banker
          """,
          "JR",
          "KB",
          "ER",
      )

  private val JR
    get() = p1

  private val KB
    get() = p2

  private val ER
    get() = p3

  @Test
  internal fun game20260819() {
    startGame()
    JR.doTask("20 TerraformRating")
    KB.doTask("20 TerraformRating")
    ER.doTask("20 TerraformRating")
    JR.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    KB.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    ER.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )

    admin.assertCounts(1 to "Generation")
    JR.doTask("-6 ProjectCard<Selecting>")
    KB.doTask("-5 ProjectCard<Selecting>")
    ER.doTask("-5 ProjectCard<Selecting>")
    generation1()
    generation2()
    generation3()
    generation4()
    generation5()
    generation6()
    generation7()
    generation8()
    generation9()
  }

  private fun generation1() {
    with(JR) {
      // Player-record evidence: JR rejected Teractor and one unsupported corporation; UNMI
      // Contractor and Acquired Space Agency; Crash Site Cleanup, Outdoor Sports, Interstellar
      // Colony Ship, Tropical Resort, Physics Complex, and Weather Balloons.
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$TharsisRepublic>>",
          "$TharsisRepublic FROM CorporationCard",
          "40 MC",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(KB) {
      // Mons Insurance: each opponent loses 2 MC production.
      // Player-record evidence: KB rejected Utopia Invest and Recyclon; Great Aquifer and Polar
      // Industries; Asteroid Deflection System, Decomposers, Black Polar Dust, Comet Aiming, and
      // Astra Mechanica.
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$MonsInsurance>>",
          "$MonsInsurance FROM CorporationCard",
          "48 MC",
          "PROD[4 MC]",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
          "EACH Other@Player(NOT KB) { -2! Production<Other@Player, Class<MC>> BY Other@Player }",
          "PROD[-2 MC<JR>] BY JR",
      )
      JR.doTask("PROD[-2 MC]")
      doTask("PROD[-2 MC<ER>] BY ER")
      ER.doTask("PROD[-2 MC]")
    }

    with(ER) {
      // Player-record evidence: ER rejected Factorum and Viron; Mohole and Huge Asteroid; and Meat
      // Industry, Supercapacitors, Orbital Cleanup, Ice Cap Melting, and Towing A Comet.
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$TychoMagnetics>>",
          "$TychoMagnetics FROM CorporationCard",
          "42 MC",
          "PROD[Energy]",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(JR) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SmeltingPlant>>",
          "$SmeltingPlant FROM PreludeCard",
          "OxygenStep",
          "OxygenStep",
          "5 Steel",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SelfSufficientSettlement>>",
          "$SelfSufficientSettlement FROM PreludeCard",
          "PROD[2 MC]",
          "CityTile<Elysium_3_7>",
          "PROD[MC]",
          "3 MC",
          "3 ProjectCard",
      )
    }

    with(KB) {
      doTask("PlayCard<Class<PreludeCard>, Class<$ResearchNetwork>>")
      doTask("$ResearchNetwork FROM PreludeCard").expect("WildTag")
      doTasks("PROD[MC]", "3 ProjectCard")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EstablishedMethods>>",
          "$EstablishedMethods FROM PreludeCard",
          "30 MC",
          "UseAction<PowerPlantProject, Action1>",
          "11 Pay<> FROM MC",
          "PROD[Energy]",
          "UseAction<PowerPlantProject, Action1>",
          "11 Pay<> FROM MC",
          "PROD[Energy]",
      )
    }

    with(ER) {
      // Payment reconstruction: ER spent five.
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$GalileanMining>>",
          "$GalileanMining FROM PreludeCard",
          "PROD[2 Titanium]",
          "-5 MC",
      )
      // Payment reconstruction: ER spent three.
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AquiferTurbines>>",
          "$AquiferTurbines FROM PreludeCard",
          "PROD[2 Energy]",
          "-3 MC",
          "OceanTile<Elysium_4_7>",
          "TerraformRating",
          "Plant",
      )
    }

    with(JR) {
      doTasks("DoRequiredActions", "CityTile<Elysium_5_6>", "PROD[MC]", "3 MC", "3 Plant")
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "28 Pay<> FROM MC",
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
    }

    with(KB) {
      doTasks(
          *playCardTasks("$ResearchOutpost"),
          "18 Pay<> FROM MC",
          "$ResearchOutpost FROM ProjectCard",
          "CityTile<Elysium_5_3>",
          "NOW JR { PROD[MC<JR>] }",
      )
      JR.doTask("PROD[MC]")
      doTask("2 Plant")
      doTasks(
          *playCardTasks("$AcquiredCompany"),
          "9 Pay<> FROM MC",
          "$AcquiredCompany FROM ProjectCard",
          "PROD[3 MC]",
      )
    }

    with(ER) {
      doTasks(
          *playCardTasks("$IndustrialCenter"),
          "4 Pay<> FROM MC",
          "$IndustrialCenter FROM ProjectCard",
          "IndustrialCenter_SpecialTile<Elysium_4_8>",
          "Plant",
          "Steel",
          "2 MC",
      )
      doTasks(*cardActionTasks("$IndustrialCenter"), "7 Pay<> FROM MC", "PROD[Steel]")
    }

    with(JR) {
      doTasks(
          *playCardTasks("$TechnologyDemonstration"),
          "5 Pay<> FROM MC",
          "$TechnologyDemonstration FROM ProjectCard",
          "2 ProjectCard",
      )
      doTasks(
          *playCardTasks("$FueledGenerators"),
          "Ok", // Pay no steel.
          "1 Pay<> FROM MC",
          "$FueledGenerators FROM ProjectCard",
          "PROD[-MC]",
          "PROD[Energy]",
      )
    }

    with(KB) {
      doTasks(
          *playCardTasks("$RestrictedArea"),
          "10 Pay<> FROM MC",
          "$RestrictedArea FROM ProjectCard",
          "RestrictedArea_SpecialTile<Elysium_9_8>",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
    }

    with(ER) {
      doTask("Pass")
    }

    with(JR) {
      doTask("Pass")
    }

    with(KB) {
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "2 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<ER>]",
          "0 MC<ER> FROM MC",
      )
      doTask("Pass")
    }

    // Production.
    JR.doTasks("24 MC", "2 Heat", "2 Plant", "Energy")
    KB.doTasks("28 MC", "3 Energy")
    ER.doTasks("19 MC", "2 Energy", "2 Titanium", "Steel")
  }

  private fun generation2() {
    JR.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")
    ER.doTask("4 ProjectCard<Selecting>")
    ER.doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "1 ProjectCard<Hand FROM Selecting>")
    KB.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
    JR.doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")

    with(KB) {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          *playCardTasks("$PeroxidePower"),
          "6 Pay<> FROM MC",
          "$PeroxidePower FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Energy]",
      )
    }

    with(ER) {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-2 Energy",
          "2 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "9 Pay<> FROM MC",
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
      )
    }

    with(JR) {
      // Consequence reconstruction: JR lost one energy production and two money production.
      doTasks(
          *playCardTasks("$ImmigrantCity"),
          "5 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$ImmigrantCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[-2 MC]",
          "CityTile<Elysium_3_3>",
          "PROD[MC]",
          "3 MC",
          "PROD[MC]",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$Pets"),
          "10 Pay<> FROM MC",
          "$Pets FROM ProjectCard",
          "Animal<$Pets>",
      )
    }

    with(KB) {
      doTasks(
          *playCardTasks("$StaticHarvesting"),
          "4 Pay<> FROM MC",
          "$StaticHarvesting FROM ProjectCard",
          "PROD[Energy]",
          "3 MC",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Energizer")
    }

    with(ER) {
      doTasks(*cardActionTasks("$IndustrialCenter"), "7 Pay<> FROM MC", "PROD[Steel]")
      doTask("PassBaton")
    }

    with(JR) {
      doTasks(
          *playCardTasks("$SterlingVents"),
          "5 Pay<> FROM MC",
          "$SterlingVents FROM ProjectCard",
          "PROD[-2 Heat]",
          "PROD[2 Energy]",
      )
      doTask("PassBaton")
    }

    with(KB) {
      doTask("Pass")
    }

    // Chronology: Heroku records ER's and JR's illegal second-action passes here. Decline those
    // second actions in Solarnet, then perform the no-effect passes at the next legal first-action
    // checkpoint.
    with(ER) {
      doTask("Pass")
    }

    with(JR) {
      doTask("Pass")
    }

    // Production.
    JR.doTasks("24 MC", "2 Plant", "Heat FROM Energy", "2 Energy")
    KB.doTasks("27 MC", "6 Energy", "3 Heat FROM Energy")
    ER.doTasks("19 MC", "2 Energy", "3 Titanium", "2 Steel")
  }

  private fun generation3() {
    JR.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")
    ER.doTask("4 ProjectCard<Selecting>")
    JR.doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "1 ProjectCard<Hand FROM Selecting>")
    ER.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
    KB.doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")

    with(ER) {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-2 Energy",
          "2 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(*cardActionTasks("$IndustrialCenter"), "7 Pay<> FROM MC", "PROD[Steel]")
    }

    with(JR) {
      doTasks(
          *playCardTasks("$NaturalPreserve"),
          "9 Pay<> FROM MC",
          "$NaturalPreserve FROM ProjectCard",
          "PROD[MC]",
          "NaturalPreserve_SpecialTile<Elysium_8_4>",
          "2 Steel",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Builder")
    }

    with(KB) {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          *playCardTasks("$OlympusConference"),
          "9 Pay<> FROM MC",
          "$OlympusConference FROM ProjectCard",
          "Science<$OlympusConference>",
      )
    }

    with(ER) {
      doTask("Pass")
    }

    with(JR) {
      doTasks(*playCardTasks("$EnergyMarket"), "3 Pay<> FROM MC", "$EnergyMarket FROM ProjectCard")
      doTask("PassBaton")
    }

    with(KB) {
      doTasks(
          *playCardTasks("$QuantumExtractor"),
          "12 Pay<> FROM MC",
          "$QuantumExtractor FROM ProjectCard",
          "PROD[4 Energy]",
          "ProjectCard FROM Science",
      )
      doTasks(*playCardTasks("$EarthOffice"), "$EarthOffice FROM ProjectCard")
    }

    with(JR) {
      // Chronology: Heroku records JR's pass as a second action; defer it to this legal point.
      doTask("Pass")
    }

    with(KB) {
      doTask("Pass")
    }

    // Production.
    JR.doTasks("25 MC", "2 Plant", "2 Energy", "2 Heat FROM Energy")
    KB.doTasks("27 MC", "10 Energy", "6 Heat FROM Energy")
    ER.doTasks("19 MC", "2 Energy", "3 Titanium", "3 Steel")
  }

  private fun generation4() {
    JR.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")
    ER.doTask("4 ProjectCard<Selecting>")
    KB.doTask("Ok") // Keep all four research cards.
    KB.doTasks("12 Pay<> FROM MC", "4 ProjectCard<Hand FROM Selecting>")
    JR.doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "1 ProjectCard<Hand FROM Selecting>")
    ER.doTask("Ok") // Keep all four research cards.
    ER.doTasks("12 Pay<> FROM MC", "4 ProjectCard<Hand FROM Selecting>")

    with(JR) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_2_6>",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$KaguyaTech"),
          "10 Pay<> FROM MC",
          "$KaguyaTech FROM ProjectCard",
          "PROD[2 MC]",
          "ProjectCard",
          "CityTile<Elysium_2_6> FROM GreeneryTile<Elysium_2_6>",
          "PROD[MC]",
          "3 MC",
          "PROD[MC]",
          "Animal<$Pets>",
          "2 Steel",
      )
    }

    with(KB) {
      doTasks(
          *playCardTasks("$MassConverter"),
          "7 Pay<> FROM MC",
          "$MassConverter FROM ProjectCard",
          "PROD[6 Energy]",
          "Science<$OlympusConference>",
      )
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
    }

    with(ER) {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-2 Energy",
          "2 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$InventorsGuild"),
          "9 Pay<> FROM MC",
          "$InventorsGuild FROM ProjectCard",
      )
    }

    with(JR) {
      doTasks(
          *playCardTasks("$LavaTubeSettlement"),
          "6 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$LavaTubeSettlement FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "CityTile<Elysium_3_1>",
          "PROD[MC]",
          "3 MC",
          "PROD[MC]",
          "Animal<$Pets>",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$HermeticOrderOfMars"),
          "10 Pay<> FROM MC",
          "$HermeticOrderOfMars FROM ProjectCard",
          "PROD[2 MC]",
          "22 MC",
      )
    }

    with(KB) {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          *playCardTasks("$StandardTechnology"),
          "5 Pay<> FROM MC",
          "$StandardTechnology FROM ProjectCard",
          "ProjectCard FROM Science",
      )
    }

    with(ER) {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTasks(
          *playCardTasks("$DuskLaserMining"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$DuskLaserMining FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "4 Titanium",
      )
    }

    with(JR) {
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "21 Pay<> FROM MC",
          "$InterplanetaryTrade FROM ProjectCard",
          "EACH @WildTagElection<JR> { Tag<@WildTagElection>? }",
          "PROD[9 MC]",
      )
      doTask("PassBaton")
    }

    with(KB) {
      doTasks(
          *playCardTasks("$AdaptedLichen"),
          "8 Pay<> FROM MC",
          "$AdaptedLichen FROM ProjectCard",
          "PROD[Plant]",
      )
      doTasks(
          *playCardTasks("$Hackers"),
          "2 Pay<> FROM MC",
          "$Hackers FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "PROD[-2 MC<JR>]",
          "MC<JR> FROM MC",
      )
    }

    with(ER) {
      // User recollection: ER used all eight titanium and retained 11 M€ after this play.
      doTasks(
          *playCardTasks("$WaterImportFromEuropa"),
          "8 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$WaterImportFromEuropa FROM ProjectCard",
      )
      ER.assertCounts(11 to "MC", 0 to "Titanium")
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    with(JR) {
      doTask("Pass")
    }

    with(KB) {
      doTask("Pass")
    }

    with(ER) {
      doTasks(
          *cardActionTasks("$WaterImportFromEuropa"),
          "12 Pay<> FROM MC",
          "OceanTile<Elysium_4_4>",
          "TerraformRating",
          "2 Plant",
      )
      doTask("Pass")
    }

    // Production.
    JR.doTasks("43 MC", "2 Plant", "Energy", "2 Heat FROM Energy")
    KB.doTasks("28 MC", "15 Energy", "10 Heat FROM Energy", "Plant")
    ER.doTasks("20 MC", "Energy", "4 Titanium", "3 Steel")
  }

  private fun generation5() {
    JR.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")
    ER.doTask("4 ProjectCard<Selecting>")
    JR.doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
    KB.doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
    ER.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

    with(KB) {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(
          *playCardTasks("$BusinessContacts"),
          "3 Pay<> FROM MC",
          "$BusinessContacts FROM ProjectCard",
          "4 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(ER) {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-Energy",
          "ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
    }

    with(JR) {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Philantropist")
      doTasks(
          *playCardTasks("$SpaceElevator"),
          "27 Pay<> FROM MC",
          "$SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
    }

    with(KB) {
      doTasks(
          *playCardTasks("$HiredRaiders"),
          "$HiredRaiders FROM ProjectCard",
          "2 Steel FROM Steel<ER>",
          "3 MC<ER> FROM MC",
      )
      doTasks(
          *playCardTasks("$TitaniumMine"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
      )
    }

    with(ER) {
      doTasks(*playCardTasks("$DustSeals"), "2 Pay<> FROM MC", "$DustSeals FROM ProjectCard")
      doTasks(
          *playCardTasks("$Sponsors"),
          "6 Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
      )
    }

    with(JR) {
      doTasks(
          *playCardTasks("$Archaebacteria"),
          "6 Pay<> FROM MC",
          "$Archaebacteria FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("PassBaton")
    }

    with(KB) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$EquatorialMagnetizer"),
          "10 Pay<> FROM MC",
          "$EquatorialMagnetizer FROM ProjectCard",
      )
    }

    with(ER) {
      doTasks(
          *cardActionTasks("$WaterImportFromEuropa"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No MC remainder.
          "OceanTile<Elysium_5_4>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$CommercialDistrict"),
          "8 Pay<Class<Steel>> FROM Steel",
          "Ok", // No MC remainder.
          "$CommercialDistrict FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 MC]",
          "CommercialDistrict_SpecialTile<Elysium_4_3>",
          "Plant",
          "2 MC",
          "2 MC",
      )
    }

    with(JR) {
      doTask("Pass")
    }

    with(KB) {
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
      doTask("PassBaton")
    }

    with(ER) {
      doTask("Pass")
    }

    with(KB) {
      doTask("Pass")
    }

    // Production.
    JR.doTasks("43 MC", "3 Plant", "Energy", "Heat FROM Energy", "Titanium")
    KB.doTasks("29 MC", "14 Energy", "15 Heat FROM Energy", "Plant", "Titanium")
    ER.doTasks("27 MC", "4 Titanium", "3 Steel")
  }

  private fun generation6() {
    JR.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")
    ER.doTask("4 ProjectCard<Selecting>")
    ER.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
    KB.doTask("Ok") // Keep all four research cards.
    KB.doTasks("12 Pay<> FROM MC", "4 ProjectCard<Hand FROM Selecting>")
    JR.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

    with(ER) {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTasks(
          *playCardTasks("$GiantSpaceMirror"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$GiantSpaceMirror FROM ProjectCard",
          "PROD[3 Energy]",
      )
    }

    with(JR) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$FieldCappedCity"),
          "29 Pay<> FROM MC",
          "$FieldCappedCity FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[Energy]",
          "3 Plant",
          "CityTile<Elysium_6_5>",
          "PROD[MC]",
          "3 MC",
          "PROD[MC]",
          "Animal<$Pets>",
          "Plant",
          "2 MC",
      )
    }

    with(KB) {
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

    with(ER) {
      doTasks(
          *cardActionTasks("$WaterImportFromEuropa"),
          "12 Pay<> FROM MC",
          "OceanTile<Elysium_4_6>",
          "TerraformRating",
          "Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_8>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "TerraformRating",
      )
    }

    with(JR) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_5>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )

      // Screenshot: Game20260819-dashboard-gen6.png, after JR's greenery and before Zeppelins.
      assertSidebar(gen = 6, temp = -24, oxygen = 5, oceans = 4)
      ER.assertResources(m = 23, s = 3, t = 0, p = 2, e = 0, h = 0)
      ER.assertProduction(m = 4, s = 3, t = 4, p = 0, e = 3, h = 0)
      JR.assertResources(m = 18, s = 0, t = 1, p = 4, e = 1, h = 0)
      JR.assertProduction(m = 24, s = 0, t = 1, p = 3, e = 2, h = 0)
      KB.assertResources(m = 17, s = 0, t = 1, p = 4, e = 14, h = 18)
      KB.assertProduction(m = 8, s = 0, t = 1, p = 1, e = 14, h = 1)
      ER.assertCounts(25 to "TerraformRating")
      JR.assertCounts(25 to "TerraformRating")
      KB.assertCounts(23 to "TerraformRating")
      doTasks(
          *playCardTasks("$Zeppelins"),
          "13 Pay<> FROM MC",
          "$Zeppelins FROM ProjectCard",
          "PROD[7 MC]",
      )
    }

    with(KB) {
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

    with(ER) {
      doTasks(*playCardTasks("$RedShips"), "2 Pay<> FROM MC", "$RedShips FROM ProjectCard")
      doTasks(*cardActionTasks("$RedShips"), "7 MC")
    }

    with(JR) {
      doTasks(
          *playCardTasks("$SpaceMirrors"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No MC remainder.
          "$SpaceMirrors FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(KB) {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
    }

    with(ER) {
      doTasks(
          *playCardTasks("$Greenhouses"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // No MC remainder.
          "$Greenhouses FROM ProjectCard",
          "7 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_9>",
          "OxygenStep",
          "Plant",
          "Titanium",
          "TerraformRating",
      )
    }

    with(JR) {
      doTask("Pass")
    }

    with(KB) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$IceAsteroid"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "15 Pay<> FROM MC",
          "$IceAsteroid FROM ProjectCard",
          "OceanTile<Elysium_3_5>",
          "TerraformRating",
          "Plant",
          "2 MC",
          "OceanTile<Elysium_3_6>",
          "TerraformRating",
          "2 MC",
          "2 MC",
          "2 MC",
      )
    }

    with(ER) {
      doTasks(
          *playCardTasks("$StripMine"),
          "25 Pay<> FROM MC",
          "$StripMine FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[2 Steel]",
          "PROD[Titanium]",
          "OxygenStep",
          "OxygenStep",
          "TerraformRating",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(KB) {
      doTasks(
          *playCardTasks("$NoctisFarming"),
          "9 Pay<> FROM MC",
          "$NoctisFarming FROM ProjectCard",
          "PROD[MC]",
          "2 Plant",
      )
      doTask("PassBaton")
    }

    with(ER) {
      doTask("Pass")
    }

    with(KB) {
      doTask("Pass")
    }

    // Production.
    JR.doTasks("56 MC", "3 Plant", "2 Energy", "Heat FROM Energy", "Titanium")
    KB.doTasks("37 MC", "13 Energy", "14 Heat FROM Energy", "Plant", "Titanium", "2 Heat")
    ER.doTasks("33 MC", "5 Titanium", "5 Steel", "Energy")
  }

  private fun generation7() {
    JR.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")
    ER.doTask("4 ProjectCard<Selecting>")
    JR.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
    ER.doTask("Ok") // Keep all four research cards.
    ER.doTasks("12 Pay<> FROM MC", "4 ProjectCard<Hand FROM Selecting>")
    KB.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

    with(JR) {
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Banker")
    }

    with(KB) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_2>",
          "OxygenStep",
          "2 Plant",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
    }

    with(ER) {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-Energy",
          "ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
    }

    with(JR) {
      doTasks(*playCardTasks("$Mine"), "4 Pay<> FROM MC", "$Mine FROM ProjectCard", "PROD[Steel]")
      doTasks(
          *playCardTasks("$Supermarkets"),
          "9 Pay<> FROM MC",
          "$Supermarkets FROM ProjectCard",
          "PROD[2 MC]",
      )
    }

    with(KB) {
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    with(ER) {
      doTasks(
          *playCardTasks("$AdvancedAlloys"),
          "9 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$WaterImportFromEuropa"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No MC remainder.
          "OceanTile<Elysium_1_3>",
          "TerraformRating",
          "ProjectCard",
      )
    }

    with(JR) {
      doTasks(
          *playCardTasks("$MagneticShield"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "21 Pay<> FROM MC",
          "$MagneticShield FROM ProjectCard",
          "4 TerraformRating",
      )
      doTask("PassBaton")
    }

    with(KB) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    with(ER) {
      doTasks(*playCardTasks("$MediaGroup"), "6 Pay<> FROM MC", "$MediaGroup FROM ProjectCard")
      doTasks(
          *playCardTasks("$AsteroidCard"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$AsteroidCard FROM ProjectCard",
          "TemperatureStep",
          "2 Titanium",
          "3 MC",
          "TerraformRating",
          "-3 Plant<JR>",
          "NOW KB { 3* MC<JR FROM KB> }",
      )
      KB.doTask("3 MC<JR> FROM MC")
    }

    with(JR) {
      doTask("Pass")
    }
    admin.assertCounts(8 to "TemperatureStep")

    with(KB) {
      doTasks(
          *playCardTasks("$DesignedMicroorganisms"),
          "15 Pay<> FROM MC",
          "$DesignedMicroorganisms FROM ProjectCard",
          "PROD[2 Plant]",
          "Science<$OlympusConference>",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    with(ER) {
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "2 Titanium",
      )
      doTasks(*cardActionTasks("$RedShips"), "8 MC")
    }

    with(KB) {
      // Consequence reconstruction: KB lost two plants.
      doTasks(
          *playCardTasks("$Potatoes"),
          "1 Pay<> FROM MC",
          "$Potatoes FROM ProjectCard",
          "-2 Plant",
          "PROD[2 MC]",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    with(ER) {
      doTasks(
          *playCardTasks("$SubterraneanReservoir"),
          "11 Pay<> FROM MC",
          "$SubterraneanReservoir FROM ProjectCard",
          "3 MC",
          "OceanTile<Elysium_2_5>",
          "TerraformRating",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$MirandaResort"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<> FROM MC",
          "$MirandaResort FROM ProjectCard",
          "PROD[2 MC]",
      )
    }

    with(KB) {
      doTask("Pass")
    }

    with(ER) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$LightningHarvest"),
          "8 Pay<> FROM MC",
          "$LightningHarvest FROM ProjectCard",
          "PROD[Energy]",
          "PROD[MC]",
      )
      doTask("Pass")
    }

    // Production.
    JR.doTasks("62 MC", "3 Plant", "3 Energy", "2 Heat FROM Energy", "Titanium", "Steel")
    KB.doTasks("43 MC", "12 Energy", "13 Heat FROM Energy", "3 Plant", "Titanium", "2 Heat")
    ER.doTasks("39 MC", "5 Titanium", "5 Steel", "3 Energy")
  }

  private fun generation8() {
    JR.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")
    ER.doTask("4 ProjectCard<Selecting>")
    ER.doTask("Ok") // Keep all four research cards.
    ER.doTasks("12 Pay<> FROM MC", "4 ProjectCard<Hand FROM Selecting>")
    JR.doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
    KB.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

    with(KB) {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
    }

    with(ER) {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-3 Energy",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *cardActionTasks("$WaterImportFromEuropa"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No MC remainder.
          "OceanTile<Elysium_1_4>",
          "TerraformRating",
          "Steel",
          "2 MC",
          "2 MC",
      )
    }

    with(JR) {
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
    }

    with(KB) {
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "19 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$BigAsteroid"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "14 Pay<> FROM MC",
          "$BigAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "4 Titanium",
          "TerraformRating",
          "TerraformRating",
          "Ok", // Leave the opponents' plants.
      )
    }

    with(ER) {
      doTasks(
          *playCardTasks("$GreatDamPromo"),
          "5 Pay<Class<Steel>> FROM Steel",
          "Ok", // No MC remainder.
          "$GreatDamPromo FROM ProjectCard",
          "PROD[2 Energy]",
          "GreatDamPromo_SpecialTile<Elysium_1_5>",
          "ProjectCard",
          "2 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$SolarLogistics"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "12 Pay<> FROM MC",
          "$SolarLogistics FROM ProjectCard",
          "2 Titanium",
      )
    }

    with(JR) {
      doTasks(
              *playCardTasks("$Algae"),
              "10 Pay<> FROM MC",
              "$Algae FROM ProjectCard",
              "Plant",
              "PROD[2 Plant]",
          )
          .expect("0 ProjectCard<ER>")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_4_5>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
    }

    with(KB) {
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

    with(ER) {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$MarsUniversity FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
    }

    with(JR) {
      doTasks(
          *playCardTasks("$RobotPollinators"),
          "9 Pay<> FROM MC",
          "$RobotPollinators FROM ProjectCard",
          "PROD[Plant]",
          "2 Plant",
      )
      doTasks(
          *playCardTasks("$MedicalLab"),
          "13 Pay<> FROM MC",
          "$MedicalLab FROM ProjectCard",
          "PROD[6 MC]",
      )
    }

    with(KB) {
      doTasks(
          *playCardTasks("$NitrogenRichAsteroid"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "12 Pay<> FROM MC",
          "$NitrogenRichAsteroid FROM ProjectCard",
          "2 TerraformRating",
          "TemperatureStep",
          "NOW ER { ProjectCard<ER, Hand> }",
      )
      ER.doTask("ProjectCard")
      doTasks("TerraformRating", "PROD[4 Plant]")
      doTasks(
          *playCardTasks("$SmallAsteroid"),
          "3 Pay<> FROM MC",
          "$SmallAsteroid FROM ProjectCard",
          "TemperatureStep",
          "NOW ER { ProjectCard<ER, Hand> }",
      )
      ER.doTask("ProjectCard")
      doTask("TerraformRating")
      doTask("OceanTile<WaterArea(HAS MAX 0 Tile)>") // All nine oceans are already placed.
      doTasks("-2 Plant<JR>", "0 MC<JR> FROM MC")
    }

    with(ER) {
      // Payment reconstruction: ER spent a fourth steel for the remaining two so the later Gene
      // Repair payment and final dashboard can both be reproduced.
      doTasks(
          *playCardTasks("$GhgFactories"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // No MC remainder.
          "$GhgFactories FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 Heat]",
      )
      doTasks(
          *playCardTasks("$RoboticWorkforce"),
          "9 Pay<> FROM MC",
          "$RoboticWorkforce FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
          "CopyProductionBox<$GhgFactories>",
          "PROD[-Energy]",
          "PROD[4 Heat]",
      )
    }

    with(JR) {
      doTasks(
          *playCardTasks("$PhobosSpaceHaven"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "22 Pay<> FROM MC",
          "$PhobosSpaceHaven FROM ProjectCard",
          "PROD[Titanium]",
          "NormalCityTile<PhobosSpaceHaven_RemoteArea>",
          "3 MC",
          "PROD[MC]",
          "Animal<$Pets>",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Founder")
    }

    with(KB) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*playCardTasks("$Tardigrades"), "1 Pay<> FROM MC", "$Tardigrades FROM ProjectCard")
    }

    with(ER) {
      doTasks(
          *playCardTasks("$GeneRepair"),
          "12 Pay<> FROM MC",
          "$GeneRepair FROM ProjectCard",
          "PROD[2 MC]",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(*cardActionTasks("$RedShips"), "9 MC")
    }

    with(JR) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(KB) {
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
      doTask("PassBaton")
    }

    with(ER) {
      doTasks(
          *playCardTasks("$Shuttles"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$Shuttles FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
      )
      doTask("PassBaton")
    }

    with(JR) {
      doTask("Pass")
    }

    with(KB) {
      doTask("Pass")
    }

    with(ER) {
      doTask("Pass")
    }

    // Production.
    JR.doTasks("70 MC", "6 Plant", "4 Energy", "3 Heat FROM Energy", "2 Titanium", "Steel")
    KB.doTasks("52 MC", "11 Energy", "12 Heat FROM Energy", "7 Plant", "Titanium", "2 Heat")
    ER.doTasks("44 MC", "5 Titanium", "5 Steel", "2 Energy", "8 Heat")
  }

  private fun generation9() {
    JR.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")
    ER.doTask("4 ProjectCard<Selecting>")
    KB.doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
    ER.doTasks("-1 ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
    JR.doTasks("-3 ProjectCard<Selecting>", "3 Pay<> FROM MC", "ProjectCard<Hand FROM Selecting>")

    with(ER) {
      doTasks(
          *playCardTasks("$DeimosDownPromo"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "9 Pay<> FROM MC",
          "$DeimosDownPromo FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TemperatureStep",
          "4 Steel",
          "3 MC",
          "ProjectCard",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
          "DeimosDownPromo_SpecialTile<Elysium_9_7>",
          "ProjectCard",
          "-6 Plant<JR>",
          "NOW KB { 3* MC<JR FROM KB> }",
      )
      KB.doTask("3 MC<JR> FROM MC")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    with(JR) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
    }

    with(KB) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_6_3>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
    }

    with(ER) {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "Ok", // Keep the offered card.
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$SmallAnimals"),
          "6 Pay<> FROM MC",
          "$SmallAnimals FROM ProjectCard",
          "PROD[-Plant<KB>]",
      )
    }

    with(JR) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Elysium_6_6>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Elysium_3_2>",
          "OxygenStep",
          "TerraformRating",
      )
    }

    with(KB) {
      doTasks(
          *playCardTasks("$Research"),
          "8 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
          "ProjectCard FROM Science",
          "Science<$OlympusConference>",
      )
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating")
    }

    with(ER) {
      doTasks(
          *playCardTasks("$BioPrintingFacility"),
          "2 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$BioPrintingFacility FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "Animal<$SmallAnimals>",
      )
    }

    with(JR) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Elysium_4_2>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(KB) {
      doTasks(
          *playCardTasks("$InventionContest"),
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
          "ProjectCard FROM Science",
      )
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>")
    }

    with(ER) {
      // Payment reconstruction: ER spent a second steel for the remaining two so the later Callisto
      // Penal Mines payment and final dashboard can both be reproduced.
      doTasks(
          *playCardTasks("$MagneticFieldDome"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // No MC remainder.
          "$MagneticFieldDome FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[Plant]",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
    }

    with(JR) {
      // Chronology: Heroku records JR's pass as a second action; defer it to this legal point.
      doTask("Pass")
    }

    with(KB) {
      // Source log: 14 cards revealed and 14 M€ gained.
      doTasks(
          *playCardTasks("$PublicPlans"),
          "4 Pay<> FROM MC",
          "$PublicPlans FROM ProjectCard",
          "14 ProjectCard<Revealed FROM Hand>",
          "14 ProjectCard<Hand FROM Revealed>",
          "14 MC",
      )
      doTasks(*playCardTasks("$Ants"), "6 Pay<> FROM MC", "$Ants FROM ProjectCard")
    }

    with(ER) {
      doTasks(*cardActionTasks("$RedShips"), "9 MC")
      doTasks(
          *playCardTasks("$PublicBaths"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // No MC remainder.
          "$PublicBaths FROM ProjectCard",
          "6 MC",
      )
    }

    with(KB) {
      doTasks(*cardActionTasks("$Ants"), "-Microbe<$Tardigrades>", "Microbe<$Ants>")
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Benefactor")
    }

    with(ER) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    with(KB) {
      doTasks(
          *playCardTasks("$BactoviralResearch"),
          "7 Pay<> FROM MC",
          "$BactoviralResearch FROM ProjectCard",
          "ProjectCard",
          "Science<$OlympusConference>",
          "11 Microbe<$Ants>",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    with(ER) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    with(KB) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    with(ER) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      // Consequence reconstruction: ER gained one money production.
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Elysium_6_2>",
          "NOW JR { PROD[MC<JR>] }",
      )
      JR.doTask("PROD[MC]")
      doTask("NOW JR { PROD[MC<JR>] }")
      JR.doTask("PROD[MC]")
      doTask("NOW JR { Animal<$Pets<JR>>* }")
      JR.doTask("Animal<$Pets>")
      doTask("Steel")
    }

    with(KB) {
      doTasks(
          *playCardTasks("$Worms"),
          "5 Pay<> FROM MC",
          "$Worms FROM ProjectCard",
          "PROD[3 Plant]",
      )
      doTasks(
          *playCardTasks("$ImportedNutrients"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$ImportedNutrients FROM ProjectCard",
          "4 Plant",
          "NOW ER { ProjectCard<ER, Hand> }",
      )
      ER.doTask("ProjectCard")
      doTask("4 Microbe<$Ants>")
    }

    with(ER) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$CallistoPenalMines"),
          "22 Pay<> FROM MC",
          "$CallistoPenalMines FROM ProjectCard",
          "PROD[3 MC]",
      )
    }

    with(KB) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }

    with(ER) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(KB) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "4 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$SoilFactory"),
          "6 Pay<> FROM MC",
          "$SoilFactory FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Plant]",
      )
    }

    with(ER) {
      doTask("Pass")
    }

    with(KB) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      assertSidebar(gen = 9, temp = 8, oxygen = 14, oceans = 9)
      doTask("Pass")
    }

    // Production.
    JR.doTasks("75 MC", "6 Plant", "5 Energy", "4 Heat FROM Energy", "2 Titanium", "Steel")
    KB.doTasks("54 MC", "9 Energy", "11 Heat FROM Energy", "10 Plant", "Titanium", "2 Heat")
    ER.doTasks("53 MC", "5 Titanium", "5 Steel", "8 Heat", "Plant")

    with(ER) {
      // Decline ER's final greenery placement.
      doTask("PassBaton")
    }

    with(JR) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_5_7>",
          "2 Plant",
          "2 MC",
          "2 MC",
      )
      // Decline another final greenery placement for JR.
      doTask("PassBaton")
    }

    with(KB) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_7_4>",
          "Steel",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Elysium_8_5>",
      )
      // Decline another final greenery placement for KB.
      doTask("PassBaton")
    }
    JR.assertCounts(1 to "ProjectCard")
    KB.assertCounts(0 to "ProjectCard")
    ER.assertCounts(0 to "ProjectCard")
    JR.assertResources(m = 81, s = 1, t = 4, p = 3, e = 5, h = 10)
    JR.assertProduction(m = 42, s = 1, t = 2, p = 6, e = 5, h = 0)
    KB.assertResources(m = 59, s = 1, t = 1, p = 1, e = 9, h = 28)
    KB.assertProduction(m = 11, s = 0, t = 1, p = 10, e = 9, h = 2)
    ER.assertResources(m = 54, s = 9, t = 5, p = 3, e = 0, h = 8)
    ER.assertProduction(m = 15, s = 5, t = 5, p = 1, e = 0, h = 8)

    JR.count("EVAL Founder.metric") shouldBe 5
    KB.count("EVAL Founder.metric") shouldBe 4
    ER.count("EVAL Founder.metric") shouldBe 3
    JR.count("EVAL Benefactor.metric") shouldBe 33
    KB.count("EVAL Benefactor.metric") shouldBe 43
    ER.count("EVAL Benefactor.metric") shouldBe 38

    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<JR>") shouldBe 10
    score.net("Milestone", "VictoryPoint<KB>") shouldBe 5
    score.net("Milestone", "VictoryPoint<ER>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<JR>") shouldBe 10
    score.net("SecondPlace", "VictoryPoint<JR>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<KB>") shouldBe 5
    score.net("SecondPlace", "VictoryPoint<KB>") shouldBe 2
    score.net("FirstPlace", "VictoryPoint<ER>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<ER>") shouldBe 4
    score.net("GreeneryTile", "VictoryPoint<JR>") shouldBe 6
    score.net("GreeneryTile", "VictoryPoint<KB>") shouldBe 4
    score.net("GreeneryTile", "VictoryPoint<ER>") shouldBe 2
    score.net("CityTile", "VictoryPoint<JR>") shouldBe 9
    score.net("CityTile", "VictoryPoint<KB>") shouldBe 3
    score.net("CityTile", "VictoryPoint<ER>") shouldBe 2
    score.net("Card", "VictoryPoint<JR>") shouldBe 16
    score.net("Card", "VictoryPoint<KB>") shouldBe 13
    score.net("Card", "VictoryPoint<ER>") shouldBe 21

    JR.assertCounts(33 to "TerraformRating")
    KB.assertCounts(43 to "TerraformRating")
    ER.assertCounts(38 to "TerraformRating")
    JR.assertCounts(84 to "VictoryPoint")
    KB.assertCounts(75 to "VictoryPoint")
    ER.assertCounts(67 to "VictoryPoint")
    JR.assertCounts(1 to "Victory")
    KB.assertCounts(0 to "Victory")
    ER.assertCounts(0 to "Victory")
  }
}
