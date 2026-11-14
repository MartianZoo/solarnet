package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Source: _local/replays/Game20260820/ (game log, player record, and dashboard screenshots)
// Complete archive replay: Synthetic Magnet Burst (ga5237bd2fb08)
// https://terraforming-mars.herokuapp.com/the-end?id=pa9f45e80d897
internal class SyntheticMagnetBurstTest : FullGameTest() {
  // Player-record evidence: Hellas, Corporate Era, Venus, Prelude, Prelude 2, drafting, World
  // Government, two players, and these full-random milestone and award pools.
  // Setup retains Solarnet's two-corporation offer; the archive offered three. The rejected-card
  // comments below preserve source identities, but the extra corporation rejection is not modeled.
  // This replay still uses Builder and Sponsor stand-ins for Thawer and Briber.
  // Player-record evidence: Merger was dealt despite promo cards being disabled, so it is included
  // individually without enabling promos:cards.
  override val config =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, prelude2:cards, Merger, SagittaFrontierServices, Unsafe
          fakes:cards

          Energizer, Builder, Generalist, Diversifier, Terraformer, Sponsor
          Scientist, Landscaper, Founder, Contractor, Forecaster, Incorporator
          """,
          "Pink",
          "Green",
      )

  private val pink
    get() = p1

  private val green
    get() = p2

  @Test
  internal fun game20260820() {
    startGame()
    pink.doTask("20 TerraformRating")
    green.doTask("20 TerraformRating")
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
    pink.doTask("-3 ProjectCard<Selecting>")
    green.doTask("-6 ProjectCard<Selecting>")
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
  }

  private fun generation1() {
    admin.assertCounts(1 to "Generation")

    with(pink) {
      // Pink rejected CrediCor and Ecotec; Venus Contract and Focused Organization; and Windmills,
      // Open City, and Energy Saving.
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$TharsisRepublic>>",
          "$TharsisRepublic FROM CorporationCard",
          "40 MC",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(green) {
      // Green rejected Ecoline and Morning Star Inc.; Project Eden and Floating Trade Hub; and
      // Magnetic Field Dome, Heather, Insulation, Cartel, Caretaker Contract, and Protected
      // Habitats.
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$NirgalEnterprises>>",
          "$NirgalEnterprises FROM CorporationCard",
          "30 MC",
          "PROD[Energy]",
          "PROD[Plant]",
          "PROD[Steel]",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(pink) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Recession>>",
          "$Recession FROM PreludeCard",
          "10 MC",
          "EACH Other@Player(NOT Pink) { -5* MC<Other@Player>, -Production<Other@Player, Class<MC>>! }",
          "-5 MC<Green>",
          "PROD[-MC<Green>]",
      )
      // You drew Interplanetary Cinematics,Inventrix,Sagitta Frontier Services,Teractor
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Merger>>",
          "$Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
          "PlayCard<Class<CorporationCard>, Class<$SagittaFrontierServices>>",
          "$SagittaFrontierServices FROM CorporationCard<Selecting>",
          "31 MC",
          "-42 MC",
          "PROD[Energy]",
          "PROD[2 MC]",
      )
      // Pink drew Micro-Mills
      doTask("SearchForCard<NoTagsFilter>").expect("ProjectCard")
      doTask("4 MC")
    }

    with(green) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$NobelPrize>>",
          "$NobelPrize FROM PreludeCard",
          "5 MC",
      )
      // Green drew Insects,Stratopolis
      doTask("2 SearchForCard<RequirementFilter>").expect("2 ProjectCard")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>",
          "$SuitableInfrastructure FROM PreludeCard",
          "5 Steel",
      )
    }

    with(pink) {
      // You drew Industrial Center
      doTasks(
          "DoRequiredActions",
          "CityTile<Hellas_9_7>",
          "PROD[MC]",
          "3 MC",
          "-6 MC",
          "OceanTile<Hellas_5_6>",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$MicroMills"),
          "3 Pay<> FROM MC",
          "$MicroMills FROM ProjectCard",
          "PROD[Heat]",
          "4 MC",
      )
    }

    with(green) {
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "5 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$Mine"),
          "4 Pay<> FROM MC",
          "$Mine FROM ProjectCard",
          "PROD[Steel]",
          "MC",
      )
      doTasks(
          *playCardTasks("$GreatEscarpmentConsortium"),
          "6 Pay<> FROM MC",
          "$GreatEscarpmentConsortium FROM ProjectCard",
          "PROD[Steel]",
          "4 MC",
          "PROD[-Steel<Green>]",
      )
    }

    with(green) {
      // The later Aquifer Pumping action requires four of Green's ten steel to remain available.
      doTasks(
          *playCardTasks("$AquiferPumping"),
          "6 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$AquiferPumping FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$Sponsors"),
          "6 Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
          "MC",
      )
      // Copy Mine's production without creating another Mine.
      doTasks(
              *playCardTasks("$RoboticWorkforce"),
              "9 Pay<> FROM MC",
              "$RoboticWorkforce FROM ProjectCard",
              "MC",
              "CopyProductionBox<$Mine>",
              "PROD[Steel]",
          )
          .expect("0 Mine")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // No MC remainder.
          "OceanTile<Hellas_7_3>",
          "TerraformRating",
          "2 Titanium",
      )
      doTask("PassBaton")
    }

    with(pink) {
      // Green declared this pass early after one action; Solarnet executes both passes in turn
      // order.
      doTask("Pass")
    }

    with(green) {
      doTask("Pass")
    }

    // Production.
    pink.doTasks("26 MC", "Energy", "Heat", "3 Steel")
    green.doTasks("20 MC", "Energy", "Plant")

    // World Government.
    pink.doTask("OceanTile<Hellas_2_1>! BY Admin")
  }

  private fun generation2() {
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    // You bought Giant Ice Asteroid,Investment Loan,Earth Office,Power Supply Consortium
    green.doTasks(
        "Ok", // Keep all four offered cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    // Game20260820-dashboards-gen2.png was taken after Green's generation 2 purchase and
    // before Pink's.
    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 3, venus = 0)
    pink.assertResources(m = 28, s = 3, t = 0, p = 0, e = 1, h = 1)
    pink.assertProduction(m = 5, s = 3, t = 0, p = 0, e = 1, h = 1)
    green.assertResources(m = 15, s = 0, t = 2, p = 1, e = 1, h = 0)
    green.assertProduction(m = -1, s = 0, t = 0, p = 1, e = 1, h = 0)
    // You bought Invention Contest,Decomposers,Colonizer Training Camp,Imported Hydrogen
    pink.doTasks(
        "Ok", // Keep all four offered cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      doTasks(*playCardTasks("$EarthOffice"), "1 Pay<> FROM MC", "$EarthOffice FROM ProjectCard")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$DesignedMicroorganisms"),
          "16 Pay<> FROM MC",
          "$DesignedMicroorganisms FROM ProjectCard",
          "PROD[2 Plant]",
      )
      doTask("PassBaton")
    }

    with(green) {
      // Power Supply Consortium requires Nobel Prize's wild icon to count as Green's second power
      // tag.
      doTasks(
          *playCardTasks("$PowerSupplyConsortium"),
          "5 Pay<> FROM MC",
          "$PowerSupplyConsortium FROM ProjectCard",
          "PROD[Energy]",
          "2 MC",
          "PROD[-Energy<Pink>]",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTask("Pass")
    }

    with(green) {
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Hellas_5_7>",
          "TerraformRating",
          "3 Heat",
          "2 MC",
      )
    }

    with(green) {
      doTasks(
          *playCardTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No MC remainder.
          "$RotatorImpacts FROM ProjectCard",
      )
      doTasks(*cardActionTasks("$RotatorImpacts"), "6 Pay<> FROM MC", "Asteroid<$RotatorImpacts>")
    }

    with(green) {
      doTask("Pass")
    }

    // Production.
    pink.doTasks("26 MC", "Heat", "3 Steel", "Heat FROM Energy", "2 Plant")
    green.doTasks("20 MC", "2 Energy", "Plant", "Heat FROM Energy")

    // World Government.
    green.doTask("OxygenStep! BY Admin")
  }

  private fun generation3() {
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    // You bought Mars University,Neutralizer Factory,Great Dam
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    // You bought Hackers,ArchaeBacteria
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    // Game20260820-dashboards-gen3.png was taken after generation 3 purchases and before
    // the first action.
    assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 4, venus = 0)
    pink.assertResources(m = 20, s = 6, t = 0, p = 2, e = 0, h = 3)
    pink.assertProduction(m = 5, s = 3, t = 0, p = 2, e = 0, h = 1)
    green.assertResources(m = 20, s = 0, t = 0, p = 2, e = 2, h = 4)
    green.assertProduction(m = -2, s = 0, t = 0, p = 1, e = 2, h = 0)

    with(pink) {
      // You drew Ishtar Mining
      doTasks(
          *playCardTasks("$InventionContest"),
          "2 Pay<> FROM MC",
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }

    with(green) {
      // Green discarded Stratopolis
      // You drew House Printing
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "8 Pay<> FROM MC",
          "$MarsUniversity FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$Archaebacteria"),
          "6 Pay<> FROM MC",
          "$Archaebacteria FROM ProjectCard",
          "PROD[Plant]",
          "MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTask("Pass")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Hellas_4_6>",
          "TerraformRating",
          "Plant",
          "2 MC",
          "2 MC",
      )
    }

    with(green) {
      doTask("Pass")
    }

    // Production.
    pink.doTasks("26 MC", "Heat", "3 Steel", "3 Plant")
    green.doTasks("22 MC", "2 Energy", "Plant", "2 Heat FROM Energy")

    // World Government.
    pink.doTask("VenusStep! BY Admin")
  }

  private fun generation4() {
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    // You bought Titanium Mine,Ecological Zone
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // You bought Media Group,Ironworks,Research,Miranda Resort
    green.doTasks(
        "Ok", // Keep all four offered cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      doTasks(*playCardTasks("$MediaGroup"), "3 Pay<> FROM MC", "$MediaGroup FROM ProjectCard")
      doTask("PassBaton")
    }

    with(pink) {
      // You drew Water Splitting Plant
      doTasks(
          *playCardTasks("$OrbitalReflectors"),
          "26 Pay<> FROM MC",
          "$OrbitalReflectors FROM ProjectCard",
          "VenusStep",
          "VenusStep",
          "PROD[2 Heat]",
          "TerraformRating",
          "TerraformRating",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$IshtarMining"),
          "5 Pay<> FROM MC",
          "$IshtarMining FROM ProjectCard",
          "PROD[Titanium]",
          "MC",
      )
    }

    with(green) {
      // Miranda Resort counted Nobel Prize's wild icon as Green's third earth tag.
      doTasks(
          *playCardTasks("$MirandaResort"),
          "12 Pay<> FROM MC",
          "$MirandaResort FROM ProjectCard",
          "PROD[3 MC]",
          "2 MC",
      )
      // Nobel Prize's wild icon counts as Green's eighth distinct tag.
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "Diversifier",
      ) // Free with Nirgal Enterprises.
    }

    with(pink) {
      doTasks(
          *playCardTasks("$TitaniumMine"),
          "3 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
          "MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTask("Pass")
    }

    with(pink) {
      doTask("Pass")
    }

    // Production.
    pink.doTasks("28 MC", "3 Heat", "3 Steel", "3 Plant", "2 Titanium")
    green.doTasks("25 MC", "2 Energy", "Plant", "2 Heat FROM Energy")

    // World Government.
    green.doTask("OxygenStep! BY Admin")
  }

  private fun generation5() {
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // Game20260820-dashboards-gen5.png was taken in generation 5 drafting, before purchases.
    assertSidebar(gen = 5, temp = -30, oxygen = 2, oceans = 5, venus = 8)
    pink.assertResources(m = 31, s = 9, t = 2, p = 8, e = 0, h = 7)
    pink.assertProduction(m = 5, s = 3, t = 2, p = 3, e = 0, h = 3)
    green.assertResources(m = 30, s = 0, t = 0, p = 5, e = 2, h = 8)
    green.assertProduction(m = 1, s = 0, t = 0, p = 1, e = 2, h = 0)
    // You bought Thermophiles,Rad-Chem Factory,GHG Factories
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    // You bought Floating Habs,Nuclear Zone,Space Elevator
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(pink) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_8_7>",
          "OxygenStep",
          "2 Heat",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$GreatDam"),
          "12 Pay<> FROM MC",
          "$GreatDam FROM ProjectCard",
          "PROD[2 Energy]",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(pink) {
      // The later Space Elevator action requires one of Pink's nine steel to remain available.
      doTasks(
          *playCardTasks("$SpaceElevator"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$RotatorImpacts"), "6 Pay<> FROM MC", "Asteroid<$RotatorImpacts>")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }

    with(green) {
      doTask("Pass")
    }

    with(pink) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      // You drew Earth Catapult
      doTasks(
          *playCardTasks("$NuclearZone"),
          "10 Pay<> FROM MC",
          "$NuclearZone FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "MC",
          "TerraformRating",
          "TerraformRating",
          "PROD[Heat]",
          "NuclearZone_SpecialTile<Hellas_3_7>",
          "Plant",
          "ProjectCard",
      )
    }

    with(pink) {
      doTask("Pass")
    }

    // Production.
    pink.doTasks("32 MC", "4 Heat", "3 Steel", "3 Plant", "3 Titanium")
    green.doTasks("25 MC", "4 Energy", "Plant", "2 Heat FROM Energy")

    // World Government.
    pink.doTask("OceanTile<Hellas_4_7>! BY Admin")
  }

  private fun generation6() {
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    // You bought Solar Wind Power,Aerobraked Ammonia Asteroid
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // You bought Asteroid,Nitrite Reducing Bacteria
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$AsteroidCard"),
          "14 Pay<> FROM MC",
          "$AsteroidCard FROM ProjectCard",
          "TemperatureStep",
          "2 Titanium",
          "3 MC",
          "TerraformRating",
          "PROD[Heat]",
          "2 MC",
          "-3 Plant<Pink>",
      )
    }

    with(pink) {
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
          "MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*playCardTasks("$Ironworks"), "11 Pay<> FROM MC", "$Ironworks FROM ProjectCard")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No MC remainder.
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "2 Titanium",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Generalist")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$Ironworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$Hackers"),
          "1 Pay<> FROM MC",
          "$Hackers FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "4 MC",
          "PROD[-2 MC<Green>]",
      )
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$Decomposers"),
          "3 Pay<> FROM MC",
          "$Decomposers FROM ProjectCard",
          "MC",
          "Microbe<$Decomposers>",
      )
      // You drew Zeppelins
      doTasks(
          *playCardTasks("$EcologicalZone"),
          "10 Pay<> FROM MC",
          "$EcologicalZone FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "Microbe<$Decomposers>",
          "Animal<$EcologicalZone>",
          "Microbe<$Decomposers>",
          "EcologicalZone_SpecialTile<Hellas_7_6>",
          "ProjectCard",
      )
    }

    with(green) {
      doTasks("UseAction<FundAwardAction, Action1>", "Contractor") // Free with Nirgal Enterprises.
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$FloatingHabs"),
          "3 Pay<> FROM MC",
          "$FloatingHabs FROM ProjectCard",
          "MC",
      )
      doTasks(
          *playCardTasks("$ColonizerTrainingCamp"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$ColonizerTrainingCamp FROM ProjectCard",
      )
    }

    with(green) {
      doTask("Pass")
    }

    with(pink) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$FloatingHabs>")
    }

    with(pink) {
      doTask("Pass")
    }

    // Production.
    pink.doTasks("34 MC", "4 Heat", "3 Steel", "3 Plant", "3 Titanium")
    green.doTasks("27 MC", "4 Energy", "Plant", "Heat")

    // World Government.
    green.doTask("TemperatureStep! BY Admin")
  }

  private fun generation7() {
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    // You bought Lagrange Observatory,Advanced Ecosystems,Deimos Down
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    // You bought Terraforming Contract,Sponsored Academies
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    // Game20260820-dashboards-gen7.png was taken after generation 7 purchases and before actions.
    assertSidebar(gen = 7, temp = -18, oxygen = 4, oceans = 6, venus = 10)
    pink.assertResources(m = 25, s = 3, t = 5, p = 4, e = 0, h = 9)
    pink.assertProduction(m = 7, s = 3, t = 3, p = 3, e = 0, h = 4)
    green.assertResources(m = 25, s = 1, t = 2, p = 7, e = 4, h = 3)
    green.assertProduction(m = -1, s = 0, t = 0, p = 1, e = 4, h = 1)

    with(pink) {
      doTasks(
          *playCardTasks("$DeimosDown"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "14 Pay<> FROM MC",
          "$DeimosDown FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TemperatureStep",
          "4 Steel",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
          "-7 Plant<Green>",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Terraformer")
    }

    with(green) {
      doTasks(
          *playCardTasks("$TerraformingContract"),
          "5 Pay<> FROM MC",
          "$TerraformingContract FROM ProjectCard",
          "PROD[4 MC]",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No MC remainder.
          "Asteroid<$RotatorImpacts>",
      )
      doTask("PassBaton")
    }

    with(pink) {
      // You drew Gyropolis
      doTasks(
          *playCardTasks("$LagrangeObservatory"),
          "7 Pay<> FROM MC",
          "$LagrangeObservatory FROM ProjectCard",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$Ironworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "2 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "OceanTile<Hellas_5_8>",
          "TerraformRating",
          "2 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTask("Pass")
    }

    with(green) {
      // Nobel Prize's wild icon counts as Green's second science tag.
      doTasks(
          *playCardTasks("$TectonicStressPower"),
          "18 Pay<> FROM MC",
          "$TectonicStressPower FROM ProjectCard",
          "PROD[3 Energy]",
          "2 MC",
      )
      doTask("Pass")
    }

    // Production.
    pink.doTasks("38 MC", "4 Heat", "3 Steel", "3 Plant", "3 Titanium")
    green.doTasks("33 MC", "7 Energy", "Plant", "Heat")

    // World Government.
    pink.doTask("TemperatureStep! BY Admin")
  }

  private fun generation8() {
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // Game20260820-dashboards-gen8.png was taken during generation 8 drafting, before purchases.
    assertSidebar(gen = 8, temp = -8, oxygen = 5, oceans = 7, venus = 10)
    pink.assertResources(m = 39, s = 9, t = 3, p = 7, e = 0, h = 5)
    pink.assertProduction(m = 7, s = 3, t = 3, p = 3, e = 0, h = 4)
    green.assertResources(m = 39, s = 0, t = 0, p = 1, e = 7, h = 4)
    green.assertProduction(m = 3, s = 0, t = 0, p = 1, e = 7, h = 1)
    // You bought Ishtar Expedition,Lava Tube Settlement
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // You bought Rad-Suits,Geothermal Power,Gene Repair
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // Green drew Stratospheric Birds,Floating Refinery
      doTasks(
          *playCardTasks("$IshtarExpedition"),
          "6 Pay<> FROM MC",
          "$IshtarExpedition FROM ProjectCard",
          "3 Titanium",
      )
      doTask("2 SearchForCard<TagFilter<Class<VenusTag>>>").expect("2 ProjectCard")
      doTask("3 MC")
      doTasks(
          *playCardTasks("$GiantIceAsteroid"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "27 Pay<> FROM MC",
          "$GiantIceAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "3 MC",
          "TerraformRating",
          "TerraformRating",
          "OceanTile<Hellas_6_7>",
          "TerraformRating",
          "2 MC",
          "2 MC",
          "OceanTile<Hellas_6_8>",
          "TerraformRating",
          "Steel",
          "2 MC",
          "2 MC",
          "2 MC",
          "-6 Plant<Pink>",
      )
    }

    with(pink) {
      doTasks(
          *playCardTasks("$GeothermalPower"),
          "4 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$GeothermalPower FROM ProjectCard",
          "PROD[2 Energy]",
      )
      // The later Space Elevator action requires one of Pink's five steel to remain available.
      doTasks(
          *playCardTasks("$ImmigrantCity"),
          "4 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$ImmigrantCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[-2 MC]",
          "CityTile<Hellas_2_5>",
          "PROD[MC]",
          "3 MC",
          "PROD[MC]",
          "Plant",
      )
    }

    with(green) {
      doTasks(
          *cardActionTasks("$Ironworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTasks(
          *playCardTasks("$RadSuits"),
          "4 Pay<> FROM MC",
          "$RadSuits FROM ProjectCard",
          "PROD[MC]",
          "4 MC",
      )
    }

    with(green) {
      doTasks(
          *playCardTasks("$SponsoredAcademies"),
          "6 Pay<> FROM MC",
          "$SponsoredAcademies FROM ProjectCard",
      )
      // Both cards offer a discard; identify Sponsored Academies explicitly.
      doTask("-ProjectCard", SponsoredAcademies)
      // The context also avoids matching EACH against Mars University's unrelated THEN task.
      doTask(
          "EACH Other@Player(NOT Green) { ProjectCard<Other@Player, Hand>! }",
          SponsoredAcademies,
      )
      doTasks(
          "3 ProjectCard", // Green drew Commercial District, Cloud Seeding, and Solarnet.
          "ProjectCard<Pink, Hand>", // Pink drew GHG Producing Bacteria.
          "-ProjectCard", // Mars University: Green discarded Floating Refinery.
          "ProjectCard", // Green drew Space Mirrors.
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$GeneRepair"),
          "10 Pay<> FROM MC",
          "$GeneRepair FROM ProjectCard",
          "PROD[2 MC]",
          "MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$GhgProducingBacteria"),
          "6 Pay<> FROM MC",
          "$GhgProducingBacteria FROM ProjectCard",
          "Microbe<$Decomposers>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$RadChemFactory"),
          "2 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$RadChemFactory FROM ProjectCard",
          "PROD[-Energy]",
          "2 TerraformRating",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$FloatingHabs>")
      doTask("PassBaton")
    }

    with(green) {
      doTask("Pass")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$AerobrakedAmmoniaAsteroid"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "15 Pay<> FROM MC",
          "$AerobrakedAmmoniaAsteroid FROM ProjectCard",
          "PROD[3 Heat]",
          "PROD[Plant]",
          "2 Microbe<$GhgProducingBacteria>",
      )
      doTasks(
          *cardActionTasks("$GhgProducingBacteria", "Action2"),
          "-2 Microbe<$GhgProducingBacteria>",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("Pass")
    }

    // Production.
    pink.doTasks("42 MC", "7 Heat", "3 Steel", "4 Plant", "3 Titanium", "Energy")
    green.doTasks("41 MC", "6 Energy", "Plant", "Heat", "3 Heat FROM Energy")

    // World Government.
    green.doTask("TemperatureStep! BY Admin")
    // All nine oceans are already placed, so the temperature bonus adds no ocean.
    green.doTask("OceanTile<WaterArea(HAS MAX 0 Tile)>* BY Admin")
  }

  private fun generation9() {
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    // You bought Land Claim,Bribed Committee
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // You bought Herbivores,Dawn City,Beam From A Thorium Asteroid
    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(pink) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Hellas_3_6>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_8_6>",
          "OxygenStep",
          "2 Heat",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    with(green) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
    }

    // Game20260820-dashboards-gen9-early.png was taken after Space Elevator and before Ironworks.
    assertSidebar(gen = 9, temp = 6, oxygen = 8, oceans = 9, venus = 12)
    pink.assertResources(m = 21, s = 2, t = 3, p = 0, e = 1, h = 6)
    pink.assertProduction(m = 10, s = 3, t = 3, p = 4, e = 1, h = 7)
    green.assertResources(m = 41, s = 0, t = 0, p = 2, e = 6, h = 0)
    green.assertProduction(m = 3, s = 0, t = 0, p = 1, e = 6, h = 1)

    with(green) {
      doTasks(
          *cardActionTasks("$Ironworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$IndustrialCenter"),
          "1 Pay<Class<Steel>> FROM Steel",
          "Ok", // No MC remainder.
          "$IndustrialCenter FROM ProjectCard",
          "MC",
          "IndustrialCenter_SpecialTile<Hellas_9_8>",
          "2 Heat",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    with(green) {
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$NeutralizerFactory"),
          "7 Pay<> FROM MC",
          "$NeutralizerFactory FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
      )
    }

    with(pink) {
      doTasks(
          *playCardTasks("$DawnCity"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<> FROM MC",
          "$DawnCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "NormalCityTile<DawnCity_RemoteArea>",
          "3 MC",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$RotatorImpacts"), "6 Pay<> FROM MC", "Asteroid<$RotatorImpacts>")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "4 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
          "3 MC",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$FloatingHabs>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$CloudSeeding"),
          "11 Pay<> FROM MC",
          "$CloudSeeding FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
          "2 MC",
          "PROD[-Heat<Pink>]",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$Zeppelins"),
          "11 Pay<> FROM MC",
          "$Zeppelins FROM ProjectCard",
          "PROD[2 MC]",
          "4 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$LandClaim"),
          "1 Pay<> FROM MC",
          "$LandClaim FROM ProjectCard",
          "3 MC",
          "Community<Hellas_2_4>",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTask("Pass")
    }

    with(green) {
      // You drew Water to Venus,Noctis City
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$Solarnet"),
          "7 Pay<> FROM MC",
          "$Solarnet FROM ProjectCard",
          "2 ProjectCard",
      )
      doTask("Pass")
    }

    // Production.
    pink.doTasks("50 MC", "6 Heat", "3 Steel", "4 Plant", "4 Titanium", "Heat FROM Energy")
    green.doTasks("47 MC", "6 Energy", "3 Plant", "Heat", "2 Heat FROM Energy")

    // Game20260820-dashboards-gen9-late.png was taken after production and before World Government.
    assertSidebar(gen = 9, temp = 8, oxygen = 9, oceans = 9, venus = 16)
    pink.assertResources(m = 62, s = 4, t = 4, p = 4, e = 0, h = 7)
    pink.assertProduction(m = 13, s = 3, t = 4, p = 4, e = 0, h = 6)
    green.assertResources(m = 47, s = 1, t = 0, p = 5, e = 6, h = 3)
    green.assertProduction(m = 2, s = 0, t = 0, p = 3, e = 6, h = 1)

    // World Government.
    pink.doTask("OxygenStep! BY Admin")
  }

  private fun generation10() {
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    // You bought Business Network
    green.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    // You bought CEO's Favorite Project,Fusion Power
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      doTasks(
          *cardActionTasks("$Ironworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      // You drew Venusian Insects
      doTasks(
          *playCardTasks("$LavaTubeSettlement"),
          "2 Pay<Class<Steel>> FROM Steel",
          "11 Pay<> FROM MC",
          "$LavaTubeSettlement FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "2 MC",
          "CityTile<Hellas_8_5>",
          "NOW Pink { Production<Pink, Class<MC>>! }",
      )
      pink.doTask("PROD[MC]")
      doTask("NOW Pink { Production<Pink, Class<MC>>! }")
      pink.doTask("PROD[MC]")
      doTask("ProjectCard")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$FusionPower"),
          "4 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$FusionPower FROM ProjectCard",
          "PROD[3 Energy]",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$BusinessNetwork"),
          "1 Pay<> FROM MC",
          "$BusinessNetwork FROM ProjectCard",
          "PROD[-MC]",
      )
    }

    with(pink) {
      doTasks(
          *playCardTasks("$Herbivores"),
          "10 Pay<> FROM MC",
          "$Herbivores FROM ProjectCard",
          "Animal<$Herbivores>",
          "MC",
          "Animal<$EcologicalZone>",
          "Microbe<$Decomposers>",
          "PROD[-Plant<Green>]",
      )
      doTasks(
          *playCardTasks("$Gyropolis"),
          "18 Pay<> FROM MC",
          "$Gyropolis FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[6 MC]", // Three Earth tags and three Venus tags.
          "CityTile<Hellas_1_3>",
          "PROD[MC]",
          "3 MC",
          "PROD[MC]",
          "2 Plant",
      )
    }

    with(green) {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>", // Decline the revealed Business Network card.
      )
      doTasks(
          *playCardTasks("$Research"),
          "11 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard", // Energy Tapping and Ice Cap Melting.
          "-ProjectCard", // Mars University: discard Ice Cap Melting.
          "ProjectCard", // Venus Shuttles.
          "-ProjectCard", // Mars University: discard Venusian Insects.
          "ProjectCard", // Venus Soils.
      )
    }

    with(pink) {
      doTasks(
          *playCardTasks("$ImportedHydrogen"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$ImportedHydrogen FROM ProjectCard",
          "OceanTile<WaterArea(HAS MAX 0 Tile)>", // All nine oceans are already placed.
          "3 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_4>",
          "OxygenStep",
          "Animal<$Herbivores>",
          "Plant",
          "Steel",
          "TerraformRating",
      )
    }

    with(green) {
      doTasks(
          *playCardTasks("$HousePrinting"),
          "10 Pay<> FROM MC",
          "$HousePrinting FROM ProjectCard",
          "PROD[Steel]",
          "2 MC",
      )
      // Nobel Prize's wild icon counts as Green's second plant tag.
      doTasks(
          *playCardTasks("$Insects"),
          "9 Pay<> FROM MC",
          "$Insects FROM ProjectCard",
          "PROD[2 Plant]",
          "2 MC",
      )
    }

    with(pink) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$WaterToVenus"),
          "9 Pay<> FROM MC",
          "$WaterToVenus FROM ProjectCard",
          "VenusStep",
          "3 MC",
          "TerraformRating",
      )
    }

    with(pink) {
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Landscaper")
      doTask("PassBaton")
    }

    with(green) {
      doTask("Pass")
    }

    with(pink) {
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$FloatingHabs>")
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
      doTask("Pass")
    }

    // Production.
    pink.doTasks("61 MC", "6 Heat", "3 Steel", "4 Plant", "4 Titanium", "Energy")
    green.doTasks("51 MC", "5 Energy", "4 Plant", "Heat", "2 Heat FROM Energy", "Steel")

    // Game20260820-dashboards-gen10.png was taken after production and before World Government.
    assertSidebar(gen = 10, temp = 8, oxygen = 12, oceans = 9, venus = 20)
    pink.assertResources(m = 76, s = 3, t = 4, p = 6, e = 1, h = 13)
    pink.assertProduction(m = 23, s = 3, t = 4, p = 4, e = 1, h = 6)
    green.assertResources(m = 54, s = 1, t = 0, p = 9, e = 5, h = 6)
    green.assertProduction(m = 3, s = 1, t = 0, p = 4, e = 5, h = 1)

    // World Government.
    green.doTask("VenusStep! BY Admin")
  }

  private fun generation11() {
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    // You bought Technology Demonstration,Birds
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // You bought Giant Solar Shade,Algae
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(pink) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Hellas_1_2>",
          "OxygenStep",
          "Animal<$Herbivores>",
          "2 Plant",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_2_6>",
          "OxygenStep",
          "Animal<$Herbivores>",
          "Plant",
          "TerraformRating",
      )
    }

    with(green) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_8_4>",
          "OxygenStep",
          "Steel",
          "2 MC",
      )
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>", // Decline the revealed Business Network card.
      )
    }

    with(pink) {
      // You drew Toll Station,Fueled Generators
      doTasks(
          *playCardTasks("$TechnologyDemonstration"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No MC remainder.
          "$TechnologyDemonstration FROM ProjectCard",
          "2 ProjectCard",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Founder")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$Ironworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Steel",
          "0 OxygenStep",
      )
      doTasks(
          *playCardTasks("$CommercialDistrict"),
          "3 Pay<Class<Steel>> FROM Steel",
          "10 Pay<> FROM MC",
          "$CommercialDistrict FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 MC]",
          "2 MC",
          "CommercialDistrict_SpecialTile<Hellas_2_4>",
          "Plant",
          "Steel",
      )
    }

    with(pink) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$AdvancedEcosystems"),
          "9 Pay<> FROM MC",
          "$AdvancedEcosystems FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "Microbe<$Decomposers>",
          "Microbe<$Decomposers>",
          "Animal<$EcologicalZone>",
          "Microbe<$Decomposers>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$GiantSolarShade"),
          "27 Pay<> FROM MC",
          "$GiantSolarShade FROM ProjectCard",
          "VenusStep",
          "VenusStep",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$NoctisCity"),
          "1 Pay<Class<Steel>> FROM Steel",
          "16 Pay<> FROM MC",
          "$NoctisCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "2 MC",
          "CityTile<Hellas_2_2>",
          "NOW Pink { Production<Pink, Class<MC>>! }",
      )
      pink.doTask("PROD[MC]")
      doTask("NOW Pink { Production<Pink, Class<MC>>! }")
      pink.doTask("PROD[MC]")
      doTasks("2 Plant", "2 MC")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(green) {
      // Green declared this pass early after one action; Solarnet executes it on Green's next turn.
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$Birds"),
          "8 Pay<> FROM MC",
          "$Birds FROM ProjectCard",
          "MC",
          "Animal<$EcologicalZone>",
          "Microbe<$Decomposers>",
          "PROD[-2 Plant<Green>]",
      )
      doTasks(*cardActionTasks("$Birds"), "Animal<$Birds>")
    }

    with(green) {
      doTask("Pass")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$CeosFavoriteProject"),
          "$CeosFavoriteProject FROM ProjectCard",
          "MC",
          "Animal<$Birds>",
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("Pass")
    }

    // Production.
    pink.doTasks(
        "66 MC",
        "6 Heat",
        "3 Steel",
        "4 Plant",
        "4 Titanium",
        "Energy",
        "Heat FROM Energy",
    )
    green.doTasks("61 MC", "3 Energy", "2 Plant", "Heat", "Heat FROM Energy", "Steel")

    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    with(pink) {
      // Final greenery placement
      // Pink declines the final greenery placement.
      doTask("PassBaton")
    }

    with(green) {
      // Green declines the final greenery placement.
      doTask("PassBaton")
    }
    pink.assertCounts(0 to "ProjectCard")
    green.assertCounts(0 to "ProjectCard")

    admin.assertCounts(1 to "End", 1 to "Phase", 0 to "GameScope")

    pink.assertCounts(
        41 to "TerraformRating",
        100 to "VictoryPoint",
        1 to "Victory",
    )
    green.assertCounts(
        51 to "TerraformRating",
        75 to "VictoryPoint",
        0 to "Victory",
    )

    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<Pink>") shouldBe 10
    score.net("Milestone", "VictoryPoint<Green>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Pink>") shouldBe 10
    score.net("SecondPlace", "VictoryPoint<Pink>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<Green>") shouldBe 5
    score.net("SecondPlace", "VictoryPoint<Green>") shouldBe 0
    score.net("GreeneryTile", "VictoryPoint<Pink>") shouldBe 6
    score.net("GreeneryTile", "VictoryPoint<Green>") shouldBe 1
    score.net("CityTile", "VictoryPoint<Pink>") shouldBe 7
    score.net("CityTile", "VictoryPoint<Green>") shouldBe 3
    score.net("Card", "VictoryPoint<Pink>") shouldBe 26
    score.net("Card", "VictoryPoint<Green>") shouldBe 10

    pink.assertCardResources(
        9 to Decomposers,
        6 to EcologicalZone,
        4 to FloatingHabs,
        3 to GhgProducingBacteria,
        4 to Herbivores,
        2 to Birds,
    )
    pink.assertResources(m = 71, s = 5, t = 7, p = 5, e = 1, h = 20)
    pink.assertProduction(m = 25, s = 3, t = 4, p = 4, e = 1, h = 6)
    green.assertResources(m = 69, s = 1, t = 0, p = 6, e = 3, h = 8)
    green.assertProduction(m = 10, s = 1, t = 0, p = 2, e = 3, h = 1)
  }
}
