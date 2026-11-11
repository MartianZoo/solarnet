package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import kotlin.test.Test
import kotlin.test.assertEquals

/** Physical game played Tuesday and Wednesday, 2026-08-25–26. */
internal class OtbGame20260825Test : FullGameTest() {
  override val config =
      GameConfig(
          """
          CimmeriaMap
          VenusNextExpansion, PreludeExpansion, prelude2:cards, promos:cards, TerraLabsResearch
          fakes:cards

          Energizer, Farmer, Philantropist, Producer, RimSettler, Hoverlord
          Magnate, Manufacturer, Metropolist, SpaceBaron, Suburbian, Venuphile
          """,
          "Green",
          "Yellow",
      )

  private val green
    get() = player(1)

  private val yellow
    get() = player(2)

  @Test
  internal fun otbGame20260825() {
    startGame()
    green.doTask("20 TerraformRating")
    yellow.doTask("20 TerraformRating")
    // The physical game dealt three corporations and six Preludes. As before, this replay uses
    // the default offers and models the chosen cards; the unused offers remain unidentified.
    green.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    yellow.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    green.doTask("Ok") // Keep all ten starting projects.
    yellow.doTask("-5 ProjectCard<Selecting>")
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
  }

  private fun generation1() {
    with(green) {
      // 9:17:17 pm: "This is a two-player game on the Terra Cimmeria board. We're using
      // Venus, Prelude, Prelude 2, Promos, the following milestones and awards: Energizer,
      // Farmer, Philanthropist, Producer, Rim Settler, Hoverlord; Magnate, Manufacturer,
      // Metropolist, Space Baron, Suburbian, Venuphile."
      // "Terralabs research. I get 14 money and spend all 10 of it. Then I lose a TR."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$TerraLabsResearch>>",
          "$TerraLabsResearch FROM CorporationCard",
          "14 MC",
          "-TerraformRating",
          "10 Pay<> FROM MC",
          "10 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(yellow) {
      // 9:31:05 pm: "I can play Viron for 48 and I spend 15 on five cards."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Viron>>",
          "$Viron FROM CorporationCard",
          "48 MC",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(green) {
      // "Focused organization. Draw one card. And gain one standard resource." Green takes
      // titanium.
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$FocusedOrganization>>",
          "$FocusedOrganization FROM PreludeCard",
          "ProjectCard",
          "Titanium",
      )
      // "I had to experience what it might be like to have Terra Labs and have money at the same
      // time." Head Start gains two steel and 22 M€ for the eleven project cards in hand.
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$FakeHeadStart>>",
          "$FakeHeadStart FROM PreludeCard",
          "2 Steel",
          "22 MC",
      )
      // Head Start grants an action to reuse Focused Organization: discard Red Ships and 1 M€,
      // then draw a card and take titanium.
      doTasks(
          *cardActionTasks("$FocusedOrganization"),
          "-ProjectCard",
          "-MC",
          "ProjectCard",
          "Titanium",
      )
      // "For the other one. Advertising for 4."
      doTasks(*playCardTasks("$Advertising"), "4 Pay<> FROM MC", "$Advertising FROM ProjectCard")
    }

    with(yellow) {
      // "Supply drop, gain 3 titanium, 8 steel, and 3 plantas."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SupplyDrop>>",
          "$SupplyDrop FROM PreludeCard",
          "3 Titanium",
          "8 Steel",
          "3 Plant",
      )
      // "Terraforming deal is an effect prelude. Each step your TR is raised, you gain 2 money."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>",
          "$TerraformingDeal FROM PreludeCard",
      )
    }

    with(green) {
      // "I'm going to play mineral deposit for 5."
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "5 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
      // Green initially announces six steel, two titanium, and 9 M€. He later asks to retain two
      // steel, making the corrected payment 4 steel, 2 titanium, and 13 M€.
      doTasks(
          *playCardTasks("$SpaceElevator"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<Class<Steel>> FROM Steel",
          "13 Pay<> FROM MC",
          "$SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
          "PROD[MC]",
      )
    }
    // The phone entry immediately after Space Elevator records a titanium resource instead of the
    // production Space Elevator actually grants.
    green.exMachina("PROD[-Titanium], Titanium")
    with(yellow) {
      // Yellow's ledger records -12 M€ and +1 plant before Aquifer Pumping's payment, and the
      // subsequent oceans all invoke this card. The transcript omitted the play sentence.
      doTasks(
          *playCardTasks("$ArcticAlgae"),
          "12 Pay<> FROM MC",
          "$ArcticAlgae FROM ProjectCard",
          "Plant",
      )
      // "Aquifer pumping! I'll spend my 8 steel and 2 real."
      doTasks(
          *playCardTasks("$AquiferPumping"),
          "8 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$AquiferPumping FROM ProjectCard",
      )
    }
    with(green) {
      // "I'm gonna use a spa-che elevator, spend 1 steel and get 5 real."
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      // 9:37:18 pm: "I'll pay eight for rover construction."
      doTasks(
          *playCardTasks("$RoverConstruction"),
          "Ok", // No steel paid.
          "8 Pay<> FROM MC",
          "$RoverConstruction FROM ProjectCard",
      )
    }
    with(yellow) {
      // Ocean at 9,7: "one plant, then Arctic Algae gives me two more plants, and Terraforming
      // Deal gives me two money."
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Cimmeria_9_7>",
          "TerraformRating",
          "2 Plant",
          "Plant",
          "2 MC",
      )
      // "Vairon, to reuse aquifer pumping, spend eight." The second ocean goes at 9,6.
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$AquiferPumping, Action1>",
          "8 Pay<> FROM MC",
          "OceanTile<Cimmeria_9_6>",
          "TerraformRating",
          "2 Plant",
          "Plant",
          "2 MC",
          "2 MC",
      )
    }
    green.doTask("Pass")
    with(yellow) {
      // "I can do the plant forest action, spend eight plants on greenery." It goes at 8,7.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_8_7>",
          "OxygenStep",
          "ProjectCard",
          "2 MC",
          "TerraformRating",
          "2 MC",
      )
      // Mining Area costs 4 M€ and goes at 8,6, gaining titanium and titanium production.
      doTasks(
          *playCardTasks("$MiningArea"),
          "4 Pay<> FROM MC",
          "$MiningArea FROM ProjectCard",
          "MiningArea_SpecialTile<Cimmeria_8_6>",
          "PROD[Titanium]",
          "Titanium",
          "2 MC",
          "2 MC",
      )
    }
    with(yellow) {
      // "Now for six. I play sponsors."
      doTasks(
          *playCardTasks("$Sponsors"),
          "6 Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("Pass")
    }

    // Production.
    green.doTask("20 MC")
    yellow.doTasks("25 MC", "Titanium")
    // "I'm taking two one. So I took the two titanium spot, but I don't get the two titanium."
    green.doTask("OceanTile<Cimmeria_2_1>! BY Admin").expect("0 TerraformRating, 0 Titanium")
    yellow.doTask("2 Plant")
  }

  private fun generation2() {
    with(green) {
      assertProduction(m = 1, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 20, s = 2, t = 1, p = 0, e = 0, h = 0)
      assertCounts(19 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 0, t = 1, p = 0, e = 0, h = 0)
      assertResources(m = 32, s = 0, t = 5, p = 4, e = 0, h = 0) // ledger shows 2 plants, mistake
      assertCounts(23 to "TerraformRating")
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 3, venus = 0)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // 9:47:18 pm: Green's TerraLabs purchase buttons record four one-M€ purchases; Yellow's app
    // records two ordinary purchases for six M€.
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "4 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // The ocean at 9,5 gives "two money and two plants for placing, two money and two plants
      // for my card bonuses."
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Cimmeria_9_5>",
          "TerraformRating",
          "2 Plant",
          "2 Plant",
          "2 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // Viron repeats Aquifer Pumping and places the ocean at 8,4.
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$AquiferPumping, Action1>",
          "8 Pay<> FROM MC",
          "OceanTile<Cimmeria_8_4>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
          "2 MC",
      )
      // Yellow spends eight plants for a greenery at 8,5.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_8_5>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "2 MC",
          "TerraformRating",
          "2 MC",
      )
    }
    with(green) {
      // Focused Organization discards Physics Complex and 1 M€, then gains steel.
      doTasks(
          *cardActionTasks("$FocusedOrganization"),
          "-ProjectCard",
          "-MC",
          "ProjectCard",
          "Steel",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Methane from titan. That'll be, spend my five titanium is 15 and 13 real." It gains two
      // plant production and two heat production.
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "13 Pay<> FROM MC",
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // Sponsored Academies costs 9 M€: discard Urbanized Area, draw three, and Yellow draws one.
      doTasks(
          *playCardTasks("$SponsoredAcademies"),
          "9 Pay<> FROM MC",
          "$SponsoredAcademies FROM ProjectCard",
          "-ProjectCard",
          "EACH Other@Player(NOT Green) { ProjectCard<Other@Player, Hand>! }",
          "3 ProjectCard",
          "ProjectCard<Yellow, Hand>",
      )
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    green.doTask("Pass")

    // Production.
    green.doTask("20 MC")
    yellow.doTasks("28 MC", "Titanium", "2 Heat", "2 Plant")
    // "I'm going to world gov. Akigen. Akigen up to three."
    yellow.doTask("OxygenStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation3() {
    with(green) {
      assertProduction(m = 1, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 31, s = 2, t = 1, p = 0, e = 0, h = 0)
      assertCounts(19 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 0, t = 1, p = 2, e = 0, h = 2)
      assertResources(m = 41, s = 0, t = 1, p = 5, e = 0, h = 2)
      assertCounts(26 to "TerraformRating")
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 3, oceans = 5, venus = 0)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // "I'm buying four cards. For four money." Yellow buys three.
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "4 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // "Let's do focused organization first." Green discards Ganymede Colony and 1 M€, then takes
      // titanium.
      doTasks(
          *cardActionTasks("$FocusedOrganization"),
          "-ProjectCard",
          "-MC",
          "ProjectCard",
          "Titanium",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Neptunian power consultants. Would have been nice to have this much earlier, but not
      // bad." Yellow pays 14 M€.
      doTasks(
          *playCardTasks("$NeptunianPowerConsultants"),
          "14 Pay<> FROM MC",
          "$NeptunianPowerConsultants FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // Aquifer Pumping places the ocean at 9,9; Yellow pays 5 M€ for an energy production and a
      // hydroelectric resource.
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Cimmeria_9_9>",
          "TerraformRating",
          "2 Plant",
          "Plant",
          "2 MC",
          "UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>",
          "5 Pay<> FROM MC",
          "PROD[Energy]",
          "Hydroelectric<$NeptunianPowerConsultants>",
      )
      // Yellow immediately spends eight plants for a greenery at 9,8.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_9_8>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
          "2 MC",
      )
    }
    with(green) {
      // "This is probably not the move, but I just can't resist it. City!" Green pays for a city at
      // 8,8, gaining two plants, two ocean-adjacency M€, and two M€ from Rover Construction.
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Cimmeria_8_8>",
          "2 MC",
          "2 Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // Viron repeats Aquifer Pumping at 8,9; Yellow again pays 5 M€ for energy production and a
      // hydroelectric resource.
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$AquiferPumping, Action1>",
          "8 Pay<> FROM MC",
          "OceanTile<Cimmeria_8_9>",
          "TerraformRating",
          "2 Plant",
          "2 Plant",
          "2 MC",
          "2 MC",
          "UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>",
          "5 Pay<> FROM MC",
          "PROD[Energy]",
          "Hydroelectric<$NeptunianPowerConsultants>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Investment loan costs me three. Costs me a money production, gets me ten money."
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "3 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    with(green) {
      // "I'll just get my optimal arrow breaking down. For two titanium and one real."
      doTasks(
          *playCardTasks("$OptimalAerobraking"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$OptimalAerobraking FROM ProjectCard",
      )
      // Viral Enhancers costs 9 M€ and gives Green a plant.
      doTasks(
          *playCardTasks("$ViralEnhancers"),
          "9 Pay<> FROM MC",
          "$ViralEnhancers FROM ProjectCard",
          "Plant",
      )
    }
    with(green) {
      // "Let us sell one card." The sold card is Hermetic Order of Mars.
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      // "I do have three science tags now, so I can play lightning harvest." It raises M€ and
      // energy production.
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
    green.doTasks("21 MC", "Energy")
    yellow.doTasks("31 MC", "Titanium", "2 Heat", "2 Plant", "2 Energy")
    // "I actually want to raise Venus. I'm raising Venus to two percent."
    green.doTask("VenusStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation4() {
    with(green) {
      assertProduction(m = 2, s = 0, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 21, s = 1, t = 0, p = 3, e = 1, h = 0)
      assertCounts(19 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 0, t = 1, p = 2, e = 2, h = 2)
      assertResources(m = 35, s = 0, t = 2, p = 7, e = 2, h = 4)
      assertCounts(29 to "TerraformRating")
      assertCardResources(2 to NeptunianPowerConsultants)
    }
    assertSidebar(gen = 4, temp = -30, oxygen = 4, oceans = 7, venus = 2)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // Green buys four cards, leaving 17 M€. Yellow keeps three.
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "4 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // Aquifer Pumping places an ocean at 7,9, then Neptunian Power Consultants costs 5 M€ for
      // an energy production and a hydroelectric resource.
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Cimmeria_7_9>",
          "TerraformRating",
          "2 Plant",
          "2 Plant",
          "2 MC",
          "2 MC",
          "UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>",
          "5 Pay<> FROM MC",
          "PROD[Energy]",
          "Hydroelectric<$NeptunianPowerConsultants>",
      )
      // "Viar on. Oceans are now maxed." The final ocean goes at 1,5, followed by the last
      // Neptunian Power Consultants payment.
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$AquiferPumping, Action1>",
          "8 Pay<> FROM MC",
          "OceanTile<Cimmeria_1_5>",
          "TerraformRating",
          "2 Plant",
          "2 Plant",
          "2 MC",
          "UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>",
          "5 Pay<> FROM MC",
          "PROD[Energy]",
          "Hydroelectric<$NeptunianPowerConsultants>",
      )
    }

    with(green) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // The greenery at 7,6 gains a plant and two steel.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_7_6>",
          "OxygenStep",
          "Plant",
          "2 Steel",
          "TerraformRating",
          "2 MC",
      )
      // The second greenery goes at 7,4, gaining titanium and four M€.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_7_4>",
          "OxygenStep",
          "Titanium",
          "2 MC",
          "TerraformRating",
          "2 MC",
      )
    }
    with(green) {
      // Focused Organization discards Open City and 1 M€, draws the promo card, and takes
      // titanium.
      doTasks(
          *cardActionTasks("$FocusedOrganization"),
          "-ProjectCard",
          "-MC",
          "ProjectCard",
          "Titanium",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // Homeostasis Bureau gains two heat production. "If you forget, the computer will remind
      // us later. It's nice to be reminded."
      doTasks(
          *playCardTasks("$HomeostasisBureau"),
          "2 Pay<Class<Steel>> FROM Steel",
          "12 Pay<> FROM MC",
          "$HomeostasisBureau FROM ProjectCard",
          "PROD[2 Heat]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Spenducing asteroid, three worth of titanium and 13 real." The one titanium resource is
      // worth 3 M€; Venus rises twice, giving Green his first two TR plus three heat and three M€.
      doTasks(
          *playCardTasks("$SpinInducingAsteroid"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "13 Pay<> FROM MC",
          "$SpinInducingAsteroid FROM ProjectCard",
          "VenusStep",
          "VenusStep",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    with(green) {
      // "My grand plan was to play sulfur eating bacteria, and I finally can." It costs 6 M€;
      // Viral Enhancers adds one microbe and its action adds another.
      doTasks(
          *playCardTasks("$SulphurEatingBacteria"),
          "6 Pay<> FROM MC",
          "$SulphurEatingBacteria FROM ProjectCard",
          "Microbe<$SulphurEatingBacteria>",
      )
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTask("Pass")
    }

    // Production.
    green.doTasks("23 MC", "Energy", "Heat FROM Energy")
    yellow.doTasks("35 MC", "Titanium", "4 Heat", "2 Plant", "4 Energy", "2 Heat FROM Energy")
    // "I will world gov Venus. Venus to 8."
    yellow.doTask("VenusStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation5() {
    with(green) {
      assertProduction(m = 2, s = 0, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 28, s = 0, t = 0, p = 3, e = 1, h = 4)
      assertCounts(21 to "TerraformRating")
      assertCardResources(2 to SulphurEatingBacteria)
    }
    with(yellow) {
      assertProduction(m = 2, s = 0, t = 1, p = 2, e = 4, h = 4)
      assertResources(m = 35, s = 0, t = 4, p = 2, e = 4, h = 10)
      assertCounts(33 to "TerraformRating")
      assertCardResources(4 to NeptunianPowerConsultants)
    }
    assertSidebar(gen = 5, temp = -30, oxygen = 6, oceans = 9, venus = 8)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // Green buys four cards; Yellow buys one.
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "4 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // "Oh man, I don't have any money. I'm doing it. Am I? Let's just do it." Green pays 5 M€ for
      // Meat Industry and 10 M€ for Pets; its two animals return 4 M€ through Meat Industry.
      doTasks(*playCardTasks("$MeatIndustry"), "5 Pay<> FROM MC", "$MeatIndustry FROM ProjectCard")
      doTasks(*playCardTasks("$Pets"), "10 Pay<> FROM MC", "$Pets FROM ProjectCard")
      doTask("Animal<$Pets>", Pets)
      doTasks("2 MC", "Animal<$Pets>", "2 MC")
    }
    with(yellow) {
      // "I pay eight because I want my gahuga back."
      doTasks(
          *playCardTasks("$GhgProducingBacteria"),
          "8 Pay<> FROM MC",
          "$GhgProducingBacteria FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Might as well spend an energy honestly." Focused Organization discards Martian Rails
      // and the energy, gains steel, and Space Elevator immediately turns that steel into 5 M€.
      doTasks(
          *cardActionTasks("$FocusedOrganization"),
          "-ProjectCard",
          "-Energy",
          "ProjectCard",
          "Steel",
      )
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
    }
    with(yellow) {
      // "I use the back gahuga to add back to itself."
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *playCardTasks("$ExtremeColdFungus"),
          "13 Pay<> FROM MC",
          "$ExtremeColdFungus FROM ProjectCard",
          "Plant",
      )
      doTasks(
          *cardActionTasks("$ExtremeColdFungus", "Action2"),
          "2 Microbe<$SulphurEatingBacteria>",
      )
    }
    with(yellow) {
      // "Imported nutrients." "You are fucking kidding me."
      // Yellow gains four plants and adds four microbes to GHG Producing Bacteria.
      doTasks(
          *playCardTasks("$ImportedNutrients"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$ImportedNutrients FROM ProjectCard",
          "4 Plant",
          "4 Microbe<$GhgProducingBacteria>",
      )
      // Yellow spends 8 M€ to claim Farmer, the milestone Green had been preparing to claim.
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Farmer")
    }
    with(green) {
      // "Now that I destroyed your beautiful plan." Green removes all four Sulphur-Eating Bacteria
      // microbes for 12 M€.
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-4 Microbe<$SulphurEatingBacteria>",
          "12 MC",
      )
      // Green spends 8 M€ to claim Philanthropist.
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Philantropist")
    }
    with(yellow) {
      // Viron reuses GHG Producing Bacteria, removing two microbes to raise temperature.
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$GhgProducingBacteria, Action2>",
          "-2 Microbe<$GhgProducingBacteria>",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }
    green.doTask("Pass")
    with(yellow) {
      // "I do, actually. Kelp farming!" For 17 M€, Yellow gains two M€ production, three plant
      // production, and two plants.
      doTasks(
          *playCardTasks("$KelpFarming"),
          "17 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[3 Plant]",
          "2 Plant",
      )
      doTask("Pass")
    }

    // Production.
    green.doTasks("23 MC", "Energy")
    yellow.doTasks("38 MC", "Titanium", "4 Heat", "5 Plant", "4 Energy", "4 Heat FROM Energy")
    // "I know what I'm not doing. I'm not doing either of those, so Venus it is!" The game moves
    // to Generation 6 and adjourns before Research.
    green.doTask("VenusStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation6() {
    with(green) {
      assertProduction(m = 2, s = 0, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 32, s = 0, t = 0, p = 4, e = 1, h = 4)
      assertCounts(21 to "TerraformRating", 1 to "Philantropist")
      assertCardResources(2 to Pets, 0 to SulphurEatingBacteria)
    }
    with(yellow) {
      assertProduction(m = 4, s = 0, t = 1, p = 5, e = 4, h = 4)
      assertResources(m = 40, s = 0, t = 1, p = 13, e = 4, h = 18)
      assertCounts(34 to "TerraformRating", 1 to "Farmer")
      assertCardResources(3 to GhgProducingBacteria, 4 to NeptunianPowerConsultants)
    }
    assertSidebar(gen = 6, temp = -28, oxygen = 6, oceans = 9, venus = 10)

    green.assertCounts(
        14 to "ProjectCard",
        1 to "$TerraLabsResearch",
        1 to "$FakeHeadStart",
        1 to "$FocusedOrganization",
        1 to "$Advertising",
        1 to "$OptimalAerobraking",
        1 to "$ViralEnhancers",
        1 to "$RoverConstruction",
        1 to "$SulphurEatingBacteria",
        1 to "$SpaceElevator",
        1 to "$ExtremeColdFungus",
        1 to "$SponsoredAcademies",
        1 to "$LightningHarvest",
        1 to "$MeatIndustry",
        1 to "$Pets",
    )
    yellow.assertCounts(
        1 to "$Viron",
        1 to "$SupplyDrop",
        1 to "$TerraformingDeal",
        1 to "$HomeostasisBureau",
        1 to "$GhgProducingBacteria",
        1 to "$ArcticAlgae",
        1 to "$NeptunianPowerConsultants",
        1 to "$AquiferPumping",
        1 to "$MiningArea",
        1 to "$Sponsors",
        1 to "$MethaneFromTitan",
        1 to "$KelpFarming",
    )

    assertEquals(
        """
        |                      1     2     3     4     5     6     7     8     9
        |                     /     /     /     /     /     /     /     /     /
        |
        | 1 -              W     LP    VS    LP   [O]
        |
        | 2 -          [O]    L     L     L    LPS    WP
        |
        | 3 -        L     L     LX    L     L     LP    L
        |
        | 4 -     VS    L     LS    L    LSS    L    VTT    LC
        |
        | 5 -  L     L     L     LS    LS    LC    L    LSC    W
        |
        | 6 -    VCC    L    LTSS   L     LT   LSS    L    LSS
        |
        | 7 -       LPP   [G2]   L    [G2]  LPP    LP   [O]
        |
        | 8 -          [O]   [G2]  [S2]  [G2]  [C1]  [O]
        |
        | 9 -             [O]   [O]   [O]   [G2]  [O]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )

    // Before Generation 6 Research, Green's app entries 116–117 correct the Generation 1 entry to
    // titanium production and add the four net titanium resources it should have produced.
    green.exMachina("PROD[Titanium], 4 Titanium")
    with(green) {
      assertProduction(m = 2, s = 0, t = 1, p = 0, e = 1, h = 0)
      assertResources(m = 32, s = 0, t = 4, p = 4, e = 1, h = 4)
    }

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // 12:21:20 pm on Aug 26: Yellow buys two cards. TerraLabs lets Green buy all four for 1 M€
    // apiece.
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "4 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // "I've used the convert heat standard action twice, which gets up to minus 24, so I get a
      // heat production." Homeostasis Bureau and Terraforming Deal account for the 10 M€ correction
      // Yellow remembered at the end of the generation.
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
          "3 MC",
          "2 MC",
      )
    }
    // Yellow does not take either conversion's bonuses at the physical table. She reconstructs the
    // missing 6 M€ from Homeostasis Bureau and 4 M€ from Terraforming Deal at generation end.
    yellow.exMachina("-10 MC")
    with(green) {
      // "Asteroid mining consortium. I do have the titanium production that I need. You lose a
      // titanium production. I gain a titanium production. And I have to pay 13 for it."
      doTasks(
          *playCardTasks("$AsteroidMiningConsortium"),
          "13 Pay<> FROM MC",
          "$AsteroidMiningConsortium FROM ProjectCard",
          "PROD[Titanium]",
          "PROD[-Titanium<Yellow>]",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Pay 9 for mining rights... I'll place it here for 2 steel and 2 money. 6-9."
      doTasks(
          *playCardTasks("$MiningRights"),
          "9 Pay<> FROM MC",
          "$MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Cimmeria_6_9>",
          "PROD[Steel]",
          "2 Steel",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I'll play Solar Net... I pay 7. And I draw 2 useless cards."
      doTasks(
          *playCardTasks("$Solarnet"),
          "7 Pay<> FROM MC",
          "$Solarnet FROM ProjectCard",
          "2 ProjectCard",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Two steel and nine real to ... get the ore processor."
      doTasks(
          *playCardTasks("$OreProcessor"),
          "2 Pay<Class<Steel>> FROM Steel",
          "9 Pay<> FROM MC",
          "$OreProcessor FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(green) {
      // Focused Organization discards an unidentified card and an energy, then draws a card and
      // takes steel.
      doTasks(
          *cardActionTasks("$FocusedOrganization"),
          "-ProjectCard",
          "-Energy",
          "ProjectCard",
          "Steel",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Four money to build power infrastructure."
      doTasks(
          *playCardTasks("$PowerInfrastructure"),
          "4 Pay<> FROM MC",
          "$PowerInfrastructure FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Now I can use space elevator to spend one steel and get five real."
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Power Supply Consortium! ... I lose an energy production."
      doTasks(
          *playCardTasks("$PowerSupplyConsortium"),
          "5 Pay<> FROM MC",
          "$PowerSupplyConsortium FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Green>]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // Venus Orbital Survey costs four titanium and 6 M€.
      doTasks(
          *playCardTasks("$VenusOrbitalSurvey"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "6 Pay<> FROM MC",
          "$VenusOrbitalSurvey FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I will place a GHG back."
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
      doTask("PassBaton")
    }
    with(green) {
      // Neither surveyed card had a Venus tag, so Green bought both for 1 M€ apiece through
      // TerraLabs.
      // Neither offered card has a Venus tag.
      doTasks(
          *cardActionTasks("$VenusOrbitalSurvey"),
          "2 ProjectCard<Selecting>",
          "0 TakeSelectedCard<TagFilter<Class<VenusTag>>>",
          "Ok", // Keep both surveyed cards.
          "2 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Pay seven for Venus Magnetizer."
      doTasks(
          *playCardTasks("$VenusMagnetizer"),
          "7 Pay<> FROM MC",
          "$VenusMagnetizer FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    // The recorded game illegally played Law Suit against Yellow's 2 M€. Temporarily supply the
    // third M€ required by the card, then remove it from Green after reproducing the physical play.
    yellow.exMachina("MC")
    with(green) {
      doTasks(
          *playCardTasks("$LawSuit"),
          "2 Pay<> FROM MC",
          "$LawSuit FROM ProjectCard",
          "3 MC FROM MC<Yellow>",
      )
      doTask("PassBaton")
    }
    green.exMachina("-MC")
    with(yellow) {
      // "Now suppose I can decrease energy product one step, raise Venus one step. That is a TR
      // for me."
      doTasks(
          *cardActionTasks("$VenusMagnetizer"),
          "PROD[-Energy]",
          "VenusStep",
          "TerraformRating",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Use extreme cold fungus to drop two microbes onto sulfur eating bacteria."
      doTasks(
          *cardActionTasks("$ExtremeColdFungus", "Action2"),
          "2 Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // The greenery at 5,8 gains one steel and one card.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_5_8>",
          "OxygenStep",
          "Steel",
          "ProjectCard",
          "TerraformRating",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Sell one patent and then spend six real money to build windmills."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$Windmills"),
          "6 Pay<> FROM MC",
          "$Windmills FROM ProjectCard",
          "PROD[Energy]",
      )
    }
    with(yellow) {
      // Ore Processor raises oxygen to 8%, which also raises temperature. Viron then reuses GHG
      // Producing Bacteria to raise temperature once more.
      doTasks(
          *cardActionTasks("$OreProcessor"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Titanium",
          "OxygenStep",
          "TerraformRating",
          "TemperatureStep",
          "2 MC",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$GhgProducingBacteria, Action2>",
          "-2 Microbe<$GhgProducingBacteria>",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
          "3 MC",
          "2 MC",
      )
    }
    with(green) {
      // "Use sulfur-eating bacteria to add a microbe to sulfur-eating bacteria."
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTask("PassBaton")
    }
    with(yellow) {
      // "I can spend eight to claim the Producer."
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Producer")
      doTask("PassBaton")
    }
    green.doTask("Pass")
    yellow.doTask("Pass")

    // Production.
    green.doTasks("23 MC", "2 Titanium", "Energy")
    yellow.doTasks("45 MC", "6 Heat", "5 Plant", "4 Energy", "Steel")
    // "World government is you... increase the temp ... to minus eighteen."
    yellow.doTask("TemperatureStep! BY Admin").expect("0 TerraformRating")
    // "Way back in the generation, I did two other..." "Oh, I also think I forgot to give the
    // terraforming deal money. So that's a total of ten." Yellow takes the omitted 10 M€ now.
    yellow.exMachina("10 MC")
  }

  private fun generation7() {
    with(green) {
      assertProduction(m = 2, s = 0, t = 2, p = 0, e = 1, h = 0)
      assertResources(m = 23, s = 0, t = 2, p = 4, e = 1, h = 4)
      assertCounts(21 to "TerraformRating", 1 to "Philantropist")
      assertCardResources(2 to Pets, 3 to SulphurEatingBacteria)
    }
    with(yellow) {
      assertProduction(m = 4, s = 1, t = 0, p = 5, e = 4, h = 6)
      assertResources(m = 63, s = 2, t = 2, p = 10, e = 4, h = 8)
      assertCounts(41 to "TerraformRating", 1 to "Farmer", 1 to "Producer")
      assertCardResources(2 to GhgProducingBacteria, 4 to NeptunianPowerConsultants)
    }
    assertSidebar(gen = 7, temp = -18, oxygen = 8, oceans = 9, venus = 12)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // 12:45:28 pm: Green's TerraLabs research costs 1 M€ per card; Yellow buys all four at the
    // ordinary 3-M€ rate.
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "4 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // Atmoscoop raises Venus twice. Crossing 16% grants the third TR; no card can take its
      // floaters. Advertising gains one M€ production.
      doTasks(
          *playCardTasks("$Atmoscoop"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "16 Pay<> FROM MC",
          "$Atmoscoop FROM ProjectCard",
          "2 Floater",
          "PROD[MC]",
          "2 VenusStep",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // The city at 6,3 triggers Green's Pets, Meat Industry, and Rover Construction. The greenery
      // at 6,2 takes the two-card placement bonus and raises oxygen to 9%.
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Cimmeria_6_3>",
          "NOW Green { 2! MC<Green> }",
      )
      green.doTask("2 MC")
      doTask("NOW Green { Animal<$Pets<Green>>* }")
      green.doTask("Animal<$Pets>")
      doTask("NOW Green { 2! MC<Green> }")
      green.doTask("2 MC")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_6_2>",
          "OxygenStep",
          "2 ProjectCard",
          "TerraformRating",
          "2 MC",
      )
    }
    with(green) {
      doTasks(
          *cardActionTasks("$FocusedOrganization"),
          "-ProjectCard",
          "-Energy",
          "ProjectCard",
          "Steel",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Suburbian")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // Yellow explicitly preserved both steel while playing Rego Plastics.
      doTasks(
          *playCardTasks("$RegoPlastics"),
          "Ok", // No steel paid.
          "10 Pay<> FROM MC",
          "$RegoPlastics FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *cardActionTasks("$ExtremeColdFungus", "Action2"),
          "2 Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$FusionPower"),
          "2 Pay<Class<Steel>> FROM Steel",
          "8 Pay<> FROM MC",
          "$FusionPower FROM ProjectCard",
          "PROD[3 Energy]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // Fish and Ice Cap Melting have no Venus tag. Green buys Fish for 1 M€ and discards the
      // other.
      doTasks(
          *cardActionTasks("$VenusOrbitalSurvey"),
          "2 ProjectCard<Selecting>",
          "0 TakeSelectedCard<TagFilter<Class<VenusTag>>>",
          "-1 ProjectCard<Selecting>",
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-5 Microbe<$SulphurEatingBacteria>",
          "15 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
          *cardActionTasks("$OreProcessor"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Titanium",
          "OxygenStep",
          "TerraformRating",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$JetStreamMicroscrappers"),
          "12 Pay<> FROM MC",
          "$JetStreamMicroscrappers FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
          *cardActionTasks("$VenusMagnetizer"),
          "PROD[-Energy]",
          "VenusStep",
          "TerraformRating",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *playCardTasks("$AirScrappingExpedition"),
          "13 Pay<> FROM MC",
          "$AirScrappingExpedition FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "3 Floater<$JetStreamMicroscrappers>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
          *playCardTasks("$UnexpectedApplication"),
          "4 Pay<> FROM MC",
          "$UnexpectedApplication FROM ProjectCard",
          "-ProjectCard",
          "VenusStep",
          "TerraformRating",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *cardActionTasks("$JetStreamMicroscrappers", "Action2"),
          "-2 Floater<$JetStreamMicroscrappers>",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$VenusMagnetizer, Action1>",
          "PROD[-Energy]",
          "VenusStep",
          "TerraformRating",
          "2 MC",
      )
      doTask("PassBaton")
    }
    green.doTask("Pass")

    with(yellow) {
      doTasks(
          *cardActionTasks("$GhgProducingBacteria", "Action2"),
          "-2 Microbe<$GhgProducingBacteria>",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTasks(
          *playCardTasks("$CloudTourism"),
          "11 Pay<> FROM MC",
          "$CloudTourism FROM ProjectCard",
          "PROD[2 MC]",
      )
    }
    with(yellow) {
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTasks(*playCardTasks("$Mine"), "4 Pay<> FROM MC", "$Mine FROM ProjectCard", "PROD[Steel]")
    }
    with(yellow) {
      doTasks(
          *playCardTasks("$Shuttles"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$Shuttles FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
      )
    }
    yellow.doTask("Pass")

    // Production.
    green.doTasks("29 MC", "2 Titanium", "Energy")
    yellow.doTasks("57 MC", "6 Heat", "5 Plant", "4 Energy", "2 Steel")
    green.doTask("OxygenStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation8() {
    // 1:08 pm app-log checkpoint and 1:08:17 pm board photograph, before Generation 8 Research.
    with(green) {
      assertProduction(m = 3, s = 0, t = 2, p = 0, e = 1, h = 0)
      assertResources(m = 30, s = 0, t = 2, p = 4, e = 1, h = 4)
      assertCounts(26 to "TerraformRating", 1 to "Philantropist")
      assertCardResources(3 to Pets, 1 to JetStreamMicroscrappers)
    }
    with(yellow) {
      assertProduction(m = 9, s = 2, t = 0, p = 5, e = 4, h = 6)
      assertResources(m = 57, s = 2, t = 0, p = 7, e = 4, h = 6)
      assertCounts(
          48 to "TerraformRating",
          1 to "Farmer",
          1 to "Producer",
          1 to "Suburbian",
      )
      assertCardResources(0 to GhgProducingBacteria)
    }
    assertSidebar(gen = 8, temp = -14, oxygen = 11, oceans = 9, venus = 26)

    assertEquals(
        """
        |                      1     2     3     4     5     6     7     8     9
        |                     /     /     /     /     /     /     /     /     /
        |
        | 1 -              W     LP    VS    LP   [O]
        |
        | 2 -          [O]    L     L     L    LPS    WP
        |
        | 3 -        L     L     LX    L     L     LP    L
        |
        | 4 -     VS    L     LS    L    LSS    L    VTT    LC
        |
        | 5 -  L     L     L     LS    LS    LC    L    [G2]   W
        |
        | 6 -    [G2]  [C2]  LTSS   L     LT   LSS    L    [S2]
        |
        | 7 -       LPP   [G2]   L    [G2]  LPP    LP   [O]
        |
        | 8 -          [O]   [G2]  [S2]  [G2]  [C1]  [O]
        |
        | 9 -             [O]   [O]   [O]   [G2]  [O]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )

    // 5:35:25 pm on Aug 26: "We just did a reconciliation where I took one more money and you
    // gave up one money. To set things right."
    green.exMachina("MC")
    yellow.exMachina("-MC")

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // "It's gen eight, which would make it my start ... let me get my absolute barrage of cards."
    // Green's TerraLabs research costs one M€ per card; Yellow's app records two cards bought for 6
    // M€.
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "4 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // Player-supplied transcription of the audio omitted by transcript-finished.md: "I will Venus
      // Magnetize, and Viron Venus Magnetize. I get 4 from my special effect."
      doTasks(
          *cardActionTasks("$VenusMagnetizer"),
          "PROD[-Energy]",
          "VenusStep",
          "TerraformRating",
          "2 MC",
      )
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$VenusMagnetizer, Action1>",
          "PROD[-Energy]",
          "VenusStep",
          "TerraformRating",
          "2 MC",
      )
    }
    with(green) {
      // "I spend 13 on Immigrant City. I lose 2 money production and 1 energy production." Its
      // city at 7,5 restores one M€ production and triggers Rover Construction, Pets, and Meat
      // Industry.
      doTasks(
          *playCardTasks("$ImmigrantCity"),
          "13 Pay<> FROM MC",
          "$ImmigrantCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[-2 MC]",
          "CityTile<Cimmeria_7_5>",
          "2 MC",
          "Animal<$Pets>",
          "PROD[MC]",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "Pay 25 for a city standard project. It'll go right here in 6,6."
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Cimmeria_6_6>",
          "NOW Green { 2! MC<Green> }",
      )
      green.doTask("2 MC")
      doTask("NOW Green { Animal<$Pets<Green>>* }")
      green.doTask("Animal<$Pets>")
      doTask("NOW Green { Production<Green, Class<MC>>! }")
      green.doTask("PROD[MC]")
      doTasks("Titanium", "NOW Green { 2! MC<Green> }")
      green.doTask("2 MC")
      // "Second will be to ore process. Spend four energy, gain titanium, oxygen raise, TR, two
      // money." Oxygen reaches 12%.
      doTasks(
          *cardActionTasks("$OreProcessor"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Titanium",
          "OxygenStep",
          "TerraformRating",
          "2 MC",
      )
    }
    with(green) {
      // "Goodbye, Regular Theaters. And a standard research, obviously I'm using my Focused
      // Organization. And I'll discard a piece of energy ... and a piece of steel. For my second
      // trick, I'll use the steel with Space Elevator to get five money."
      doTasks(
          *cardActionTasks("$FocusedOrganization"),
          "-ProjectCard",
          "-Energy",
          "ProjectCard",
          "Steel",
      )
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
    }
    with(yellow) {
      // "I will greenery standard project. And I will put the first one here for ... wait, no,
      // four money and two plants. Seven, three."
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Cimmeria_7_3>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "TerraformRating",
          "2 MC",
      )
      // "I'll go 6,7. Yes, two steel. Placing a greenery by converting plants."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_6_7>",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
          "2 MC",
      )
    }
    with(green) {
      // "First I'll Extreme Cold Fungus to put two microbes onto Sulphur-Eating Bacteria."
      doTasks(
          *cardActionTasks("$ExtremeColdFungus", "Action2"),
          "2 Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I will use GHG-Producing Bacteria. Boop."
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
      doTask("PassBaton")
    }
    with(green) {
      // "Earth Catapult ... cost me 23." Advertising raises M€ production.
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Right, I will add Clearism [sic]."
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }
    with(green) {
      // "I will use Venus Orbital Survey to reveal Research Outpost and OmniCorp [sic] and pay two
      // money for them."
      // Neither offered card has a Venus tag.
      doTasks(
          *cardActionTasks("$VenusOrbitalSurvey"),
          "2 ProjectCard<Selecting>",
          "0 TakeSelectedCard<TagFilter<Class<VenusTag>>>",
          "Ok", // Keep both surveyed cards.
          "2 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Ecological Zone! Pay 12." It goes at 7,8 and gains two animals from its own two tags.
      doTasks(
          *playCardTasks("$EcologicalZone"),
          "12 Pay<> FROM MC",
          "$EcologicalZone FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "Animal<$EcologicalZone>",
          "EcologicalZone_SpecialTile<Cimmeria_7_8>",
          "Plant",
          "2 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I'm going to play Symbiotic Fungus, which costs two money. It has a microbial tag, so
      // that means it would give me a microbe, but it doesn't actually take microbes. So I get a
      // plant. And then I'll use its action to add a microbe to Sulphur-Eating Bacteria."
      doTasks(
          *playCardTasks("$SymbioticFungus"),
          "2 Pay<> FROM MC",
          "$SymbioticFungus FROM ProjectCard",
          "Plant",
      )
      doTasks(*cardActionTasks("$SymbioticFungus"), "Microbe<$SulphurEatingBacteria>")
    }
    with(yellow) {
      // "I will pay two steel for Martian Lumber Corp." Its plant tag adds an Ecological Zone
      // animal.
      doTasks(
          *playCardTasks("$MartianLumberCorp"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$MartianLumberCorp FROM ProjectCard",
          "PROD[Plant]",
          "Animal<$EcologicalZone>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Stratospheric Expedition costs ten. I pay six worth of titanium. How was I going to pay
      // for the other four? Because I was going to take this first. Right. Take nine money and
      // then spend four of it." Its floaters go to Jet Stream Microscrappers.
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-3 Microbe<$SulphurEatingBacteria>",
          "9 MC",
      )
      doTasks(
          *playCardTasks("$StratosphericExpedition"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<> FROM MC",
          "$StratosphericExpedition FROM ProjectCard",
      )
      doTask("2 SearchForCard<TagFilter<Class<VenusTag>>>").expect("2 ProjectCard")
      doTasks("3 MC", "3 Heat", "2 Floater<$JetStreamMicroscrappers>")
    }
    // "Actually, I think I'm quite out of stuff to do, so I pass."
    yellow.doTask("Pass")
    with(green) {
      // "I'll sell 6 cards ... play Predators, which costs me 12." Viral Enhancers and the action
      // each add an animal; the action removes one of Yellow's Ecological Zone animals.
      doTasks(*standardProjectTasks("SellPatentsProject"), "6 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$Predators"),
          "12 Pay<> FROM MC",
          "$Predators FROM ProjectCard",
          "Animal<$Predators>",
          "2 MC",
      )
      doTasks(
          *cardActionTasks("$Predators"),
          "-Animal<Yellow, $EcologicalZone<Yellow>>",
          "Animal<$Predators>",
          "2 MC",
      )
      // "Let's sell four more cards ... and spend all six of my money to build Stratobirds. That
      // means that I lose a floater from Jet Stream. It has an animal tag so it gets an animal and
      // two money, and then I take the action to get an animal and two money."
      doTasks(*standardProjectTasks("SellPatentsProject"), "4 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$StratosphericBirds"),
          "10 Pay<> FROM MC",
          "$StratosphericBirds FROM ProjectCard",
          "-Floater<$JetStreamMicroscrappers>",
          "Animal<$StratosphericBirds>",
          "2 MC",
      )
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>", "2 MC")
      // "I have four money, so let's build Heather. That costs all four of my money ... Viral
      // Enhancers gives me a plant, and then it gives me a plant production and a plant."
      doTasks(
          *playCardTasks("$Heather"),
          "4 Pay<> FROM MC",
          "$Heather FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("Plant", Heather)
      doTask("Plant")
      doTask("Pass")
    }

    // Production.
    green.doTasks("30 MC", "2 Titanium", "Plant")
    yellow.doTasks("63 MC", "6 Heat", "6 Plant", "2 Energy", "2 Steel")
    // "Production. Your World Government choice." The following exchange and app checkpoint
    // identify the chosen temperature raise to -12 C; World Government grants no TR.
    yellow.doTask("TemperatureStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation9() {
    // 5:58:31 pm app-log checkpoint, before Generation 9 Research.
    with(green) {
      assertProduction(m = 4, s = 0, t = 2, p = 1, e = 0, h = 0)
      assertResources(m = 30, s = 0, t = 2, p = 8, e = 0, h = 7)
      assertCounts(26 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 10, s = 2, t = 0, p = 6, e = 2, h = 6)
      assertResources(m = 69, s = 4, t = 2, p = 8, e = 2, h = 12)
      assertCounts(53 to "TerraformRating")
    }
    assertSidebar(gen = 9, temp = -12, oxygen = 14, oceans = 9, venus = 30)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // "I'm buying four cards." "I'll buy two cards." Green is first in Generation 9.
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "4 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // "I'm going to use Predators to eat your ecomal. And take two money."
      doTasks(
          *cardActionTasks("$Predators"),
          "-Animal<Yellow, $EcologicalZone<Yellow>>",
          "Animal<$Predators>",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I'll put it right here. Five, seven." "Are you standard project king or are you playing a
      // card?" "Yes, standard project." Green's three city effects then trigger.
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Cimmeria_5_7>",
          "NOW Green { 2! MC<Green> }",
      )
      green.doTask("2 MC")
      doTask("NOW Green { Animal<$Pets<Green>>* }")
      green.doTask("Animal<$Pets>")
      doTask("NOW Green { Production<Green, Class<MC>>! }")
      green.doTask("PROD[MC]")
      doTask("NOW Green { 2! MC<Green> }")
      green.doTask("2 MC")
      // "Greenery to Apollinaris Mons for two titaniums." Yellow converts plants at 4,7.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_4_7>",
          "OxygenStep",
          "2 Titanium",
      )
    }
    with(green) {
      // "I'm going to Focused Organization to discard Subterranean Reservoir and also discard ...
      // a money. To draw this card and get a steel."
      doTasks(
          *cardActionTasks("$FocusedOrganization"),
          "-ProjectCard",
          "-MC",
          "ProjectCard",
          "Steel",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "My Space Shuttles gives me two off the space tags, so 16 Psyche for four titanium and 17
      // real monies."
      doTasks(
          *playCardTasks("$SixteenPsyche"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "17 Pay<> FROM MC",
          "$SixteenPsyche FROM ProjectCard",
          "PROD[2 Titanium]",
          "3 Titanium",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I'm going to use Space Elevator, spend one steel to get five real."
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      assertCounts(21 to "MC")

      // "I will... oh yeah, pay 11, which will be... I think it's kind of a hard question of
      // which to pay. Damn, how dare. Three steel. Lose energy product.Okay."
      doTasks(
          *playCardTasks("$AsteroidDeflectionSystem"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // No titanium paid.
          "2 Pay<> FROM MC",
          "$AsteroidDeflectionSystem FROM ProjectCard",
          "PROD[-Energy]",
      )
      doTask("PassBaton")
    }
    // Yellow simply forgot to pay the remaining 2 M€.
    yellow.exMachina("2 MC")

    with(green) {
      // "I will Venus Orbital Survey. I will look at these two cards, which are Comet and Rad
      // Suits. And then I might as well buy them."
      // Neither offered card has a Venus tag.
      doTasks(
          *cardActionTasks("$VenusOrbitalSurvey"),
          "2 ProjectCard<Selecting>",
          "0 TakeSelectedCard<TagFilter<Class<VenusTag>>>",
          "Ok", // Keep both surveyed cards.
          "2 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I might as well spend 14 to fund the Manufacturer."
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Manufacturer")
      doTask("PassBaton")
    }
    with(green) {
      // "I'm going to play Boozness Network for two and lose a money production. And then let's
      // just go ahead and use it. Use it tonight. Look at this card ... I might as well buy it."
      doTasks(
          *playCardTasks("$BusinessNetwork"),
          "2 Pay<> FROM MC",
          "$BusinessNetwork FROM ProjectCard",
          "PROD[-MC]",
      )
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // Keep the offered card.
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    with(yellow) {
      // "May as well use Asteroid Deflection System ... use it tonight. You flip. Greenhouses,
      // which is not a space tag."
      doTasks(
              *cardActionTasks("$AsteroidDeflectionSystem"),
              "ProjectCard<Revealed>",
              "0 ClaimCardReward<TagFilter<Class<SpaceTag>>, $AsteroidDeflectionSystem>",
              "-ProjectCard<Revealed>",
          )
          .expect("0 TerraformRating")
      doTask("PassBaton")
    }
    with(green) {
      // "Damn, now I wish I'd played it at the ... going to go ahead and use Aqueduct Systems for
      // seven." The three kept cards are Heat Trappers, Biomass Combustors, and Development Center.
      doTasks(
          *playCardTasks("$AqueductSystems"),
          "7 Pay<> FROM MC",
          "$AqueductSystems FROM ProjectCard",
      )
      doTask("3 SearchForCard<TagFilter<Class<BuildingTag>>>").expect("3 ProjectCard")
      doTask("PassBaton")
    }
    with(yellow) {
      // "I suppose I'll spend five monies on Venusian Insects."
      doTasks(
          *playCardTasks("$VenusianInsects"),
          "5 Pay<> FROM MC",
          "$VenusianInsects FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I think I'll plant boop ... to plant a forest. And I'll get a titanium and two steel ...
      // by going on six, four." Immigrant City at 7,5 makes this legally adjacent.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_6_4>",
          "OxygenStep",
          "Titanium",
          "2 Steel",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I will add a Cloud Tourism."
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }
    with(green) {
      // "First I'm gonna play Freyja Biodomes ... which costs twelve. I can't play it. Okay, I
      // have to play Biomass Combustors as well, which costs two. You lose a plant production. I
      // gain two energy production and then I lose one of them again ... for Freyja Biodomes."
      doTasks(
          *playCardTasks("$BiomassCombustors"),
          "Ok", // No steel paid.
          "2 Pay<> FROM MC",
          "$BiomassCombustors FROM ProjectCard",
          "PROD[2 Energy]",
          "PROD[-Plant<Yellow>]",
      )
      doTasks(
          *playCardTasks("$FreyjaBiodomes"),
          "12 Pay<> FROM MC",
          "$FreyjaBiodomes FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "Plant",
          "2 Animal<$StratosphericBirds>",
          "4 MC",
      )
    }
    with(yellow) {
      // "I add a Venusian insect."
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>")
      doTask("PassBaton")
    }
    with(green) {
      // "I'm gonna use Extremely Cold Fungus and put two microbials on Sulphur-Eating Bacteria."
      doTasks(
          *cardActionTasks("$ExtremeColdFungus", "Action2"),
          "2 Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Use GHG-Producing ..."
      doTasks(*cardActionTasks("$GhgProducingBacteria"), "Microbe<$GhgProducingBacteria>")
      doTask("PassBaton")
    }
    with(green) {
      // "... and I'm gonna use Symbiotic Fungus to put one more on Sulphur-Eating."
      doTasks(*cardActionTasks("$SymbioticFungus"), "Microbe<$SulphurEatingBacteria>")
      doTask("PassBaton")
    }
    with(yellow) {
      // "I Viron Venusian Insects ... yeah, I'll just Viron Venusian Insects. Hope it doesn't kill
      // me."
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$VenusianInsects, Action1>",
          "Microbe<$VenusianInsects>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I think I'm gonna play Floating Refinery for five. I get one floater for each Venus tag.
      // One, two, three, four, five, six. Woo!"
      doTasks(
          *playCardTasks("$FloatingRefinery"),
          "5 Pay<> FROM MC",
          "$FloatingRefinery FROM ProjectCard",
          "6 Floater<$FloatingRefinery>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Standard heat boop, so up to minus ten." Homeostasis Bureau pays 5 M€.
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I just realized I have not run Stratobirds yet, so I get a silver Stratobird and two
      // money."
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>", "2 MC")
      doTask("PassBaton")
    }
    // "I think I ... pass."
    yellow.doTask("Pass")
    with(green) {
      // "I will use Floating Refinery to remove two floaters from Floating Refinery and get one
      // titanium and two money."
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
      // "I will play Heat Trappers for four money. You lose two heat production ... and I gain an
      // energy production."
      doTasks(
          *playCardTasks("$HeatTrappers"),
          "Ok", // No steel paid.
          "4 Pay<> FROM MC",
          "$HeatTrappers FROM ProjectCard",
          "PROD[-2 Heat<Yellow>]",
          "PROD[Energy]",
      )
    }
    with(green) {
      // "And then I'll play Magnetic Field Dome for three. I lose two energy production and I gain
      // a plant production and a TR."
      doTasks(
          *playCardTasks("$MagneticFieldDome"),
          "Ok", // No steel paid.
          "3 Pay<> FROM MC",
          "$MagneticFieldDome FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[Plant]",
          "TerraformRating",
      )
      // "I could have played Omnicourt for all my money ... this cost nine for five plus four worth
      // of steel. And I get two more TR. So I'm dangerous now."
      doTasks(
          *playCardTasks("$Omnicourt"),
          "2 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$Omnicourt FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("Pass")
    }

    // Production.
    green.doTasks("35 MC", "2 Titanium", "2 Plant")
    yellow.doTasks(
        "65 MC",
        "4 Heat",
        "5 Plant",
        "Energy",
        "2 Steel",
        "2 Heat FROM Energy",
        "2 Titanium",
    )
    // "I think I'll do temp up to minus eight. Shocker." World Government grants no TR.
    green.doTask("TemperatureStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation10() {
    // 6:19:26 pm app-log checkpoint, before Generation 10 Research.
    with(green) {
      assertProduction(m = 6, s = 0, t = 2, p = 2, e = 0, h = 0)
      assertResources(m = 36, s = 0, t = 6, p = 3, e = 0, h = 7)
      assertCounts(29 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 11, s = 2, t = 2, p = 5, e = 1, h = 4)
      assertResources(m = 72, s = 3, t = 5, p = 5, e = 1, h = 10)
      assertCounts(54 to "TerraformRating")
    }
    assertSidebar(gen = 10, temp = -8, oxygen = 14, oceans = 9, venus = 30)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // "I believe you begin." "Yeah, I'll also buy four cards. Boop."
    yellow.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "4 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // "I will play Large Convoy ... 5 is 15. So 19 real."
      // All nine oceans are already placed. Choose five plants instead of animals.
      doTasks(
          *playCardTasks("$LargeConvoy"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "19 Pay<> FROM MC",
          "$LargeConvoy FROM ProjectCard",
          "OceanTile<WaterArea(HAS MAX 0 Tile)>",
          "2 ProjectCard",
          "5 Plant",
      )
      // "Will play Equatorial Magnetizer for three steel and two real. Because you have steel.
      // Worth three, yeah." Shuttles and Rego Plastics make that payment exact.
      doTasks(
          *playCardTasks("$EquatorialMagnetizer"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // No plants paid.
          "2 Pay<> FROM MC",
          "$EquatorialMagnetizer FROM ProjectCard",
      )
    }

    with(green) {
      // "I'm going to Sabotage for free. You lose seven money."
      doTasks(*playCardTasks("$Sabotage"), "$Sabotage FROM ProjectCard", "-7 MC<Yellow>")
      // "Then you're going to lose three plants as well." "Asteroid Deflection System. You're not
      // going to lose three plants as well. Hee hee hee." "I'll do it anyway. It costs 19 and I
      // use 18 worth of titanium." The last ocean is already placed.
      doTasks(
          *playCardTasks("$Comet"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$Comet FROM ProjectCard",
          "TemperatureStep",
          "OceanTile<WaterArea(HAS MAX 0 Tile)>",
          "PROD[MC]",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "-0 Plant<Anyone>",
      )
    }
    with(yellow) {
      // "I play Lava Flows for 18. Doot doot. Temp is at minus two ... I place the volcano tile up
      // here. One, three for a steel. What a steel."
      doTasks(
          *playCardTasks("$LavaFlows"),
          "18 Pay<> FROM MC",
          "$LavaFlows FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "TerraformRating",
          "3 MC",
          "2 MC",
          "2 MC",
          "LavaFlows_SpecialTile<Cimmeria_1_3>",
          "Steel",
      )
      // "Oh, I'm going to remember my ten money for ..."
      // "I also do my plant forest ... five, six" for a card.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_5_6>",
          "OxygenStep",
          "ProjectCard",
      )
    }
    with(green) {
      // "Let's use the stupid Venus Orbital Survey to reveal Comet for Venus, which is not a Venus
      // card, and Jovian Embassy. I'll buy them both."
      // Neither offered card has a Venus tag.
      doTasks(
          *cardActionTasks("$VenusOrbitalSurvey"),
          "2 ProjectCard<Selecting>",
          "0 TakeSelectedCard<TagFilter<Class<VenusTag>>>",
          "Ok", // Keep both surveyed cards.
          "2 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I will remove two GHGs, which will be a temp raise up to zero ... the TR and five monies."
      doTasks(
          *cardActionTasks("$GhgProducingBacteria", "Action2"),
          "-2 Microbe<$GhgProducingBacteria>",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<WaterArea(HAS MAX 0 Tile)>",
          "3 MC",
          "2 MC",
      )
      // "That was my first action, right? ... I'm also going to knock out a heat boop for eight."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
    }
    with(green) {
      // "And then I also do a heat boop, so it's at four now."
      doTasks(
              "UseAction<ConvertHeatAction, Action1>",
              "8 Pay<Class<Heat>> FROM Heat",
              "TemperatureStep",
              "TerraformRating",
          )
          .expect("0 MC")
      // "Hold on. Might still be my turn. It's kind of weird, but I'm going to play Comet for
      // Venus. Cost me nine ... I remove four money from you."
      doTasks(
          *playCardTasks("$CometForVenus"),
          "9 Pay<> FROM MC",
          "$CometForVenus FROM ProjectCard",
          "VenusStep",
          "3 MC",
          "3 Heat",
          "-4 MC<Yellow>",
      )
    }
    with(yellow) {
      // "I use Equimag, lose an energy product, gain a TR, gain two monies."
      doTasks(*cardActionTasks("$EquatorialMagnetizer"), "PROD[-Energy]", "TerraformRating", "2 MC")
      doTask("PassBaton")
    }
    with(green) {
      // "Business Network to look at this card and buy it for one money."
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // Keep the offered card.
          "Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Asteroid Deflection System. Reveal a card. No space tag. It is Venus Governor."
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<SpaceTag>>, $AsteroidDeflectionSystem>",
          "-ProjectCard<Revealed>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I will use Extreme Cold Fungus to put two microbes on Sulphur-Eating Bacteria."
      doTasks(
          *cardActionTasks("$ExtremeColdFungus", "Action2"),
          "2 Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Cloud Tourism. Add a floater to self."
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }
    with(green) {
      // "I'm going to spend 14 to raise the temp."
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    // It appears that Yellow gave herself a TR for Green's temp raise (entry 376)
    yellow.exMachina("TerraformRating")

    // "I guess I just gotta take that last temp raise. And I gain five moolahs. Temp is maxed."
    with(yellow) {
      assertCounts(1 to "GameEndBarrier")

      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      assertCounts(0 to "GameEndBarrier")
      doTask("PassBaton")
    }
    with(green) {
      // "I need to place a microbe onto Sulphur-Eating using Symbiotic Fungus ... so I'm just
      // going to use Sulphur-Eating Bacteria to get 18 money."
      doTasks(*cardActionTasks("$SymbioticFungus"), "Microbe<$SulphurEatingBacteria>")
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-6 Microbe<$SulphurEatingBacteria>",
          "18 MC",
      )
    }
    with(yellow) {
      // "For the first time I will use Power Infrastructure: sell one energy, gain one money."
      doTasks(*cardActionTasks("$PowerInfrastructure"), "-Energy", "MC")
      doTask("PassBaton")
    }
    with(green) {
      // "I'm going to use Focused Organization and get rid of a money and this card, Mohole Area.
      // I'm going to draw this card ... I think I'll take steel."
      doTasks(
          *cardActionTasks("$FocusedOrganization"),
          "-ProjectCard",
          "-MC",
          "ProjectCard",
          "Steel",
      )
      // "My second one is to use the steel to get the five money. That's the bada bing and the
      // bada boom."
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
    }
    with(yellow) {
      // "All right, I use Venusian Insects. Add a thing to itself."
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>")
      doTask("PassBaton")
    }
    with(green) {
      // "I'll spend four on Rad Suits, get a money production. And then I'll spend 20 on ...
      // Magnate."
      doTasks(
          *playCardTasks("$RadSuits"),
          "4 Pay<> FROM MC",
          "$RadSuits FROM ProjectCard",
          "PROD[MC]",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Magnate")
    }
    with(yellow) {
      // "I will sell a card."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(green) {
      // "I will do the thing where I use Floating Refinery to remove two floaters and take a
      // titanium and two money. Back to you."
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Sell a card."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(green) {
      // "Well, I'll sell a card then. Back to you."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Sell a card."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(green) {
      // "I'll use Predators to predate on your last ecomal. And that gives me two money."
      doTasks(
          *cardActionTasks("$Predators"),
          "-Animal<Yellow, $EcologicalZone<Yellow>>",
          "Animal<$Predators>",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "What was it? Sell a card."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(green) {
      // "Well, then I'll sell a card too. I can beat you at the sell-a-card stalling game."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Use Viron to add a Venusian insect."
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$VenusianInsects, Action1>",
          "Microbe<$VenusianInsects>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Use Stratobirds to add a Stratobird and get two money."
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>", "2 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // "I have to sell a card."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(green) {
      // "Sell a card."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Standard greenery. Way up at one four for two money and a plant."
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Cimmeria_1_4>",
          "OxygenStep",
          "Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Wait a minute! We're fish. You lose a plant production. I pay seven. But I receive one
      // fish from Viral Enhancers ... I get two money for that one fish."
      doTasks(
          *playCardTasks("$Fish"),
          "7 Pay<> FROM MC",
          "$Fish FROM ProjectCard",
          "PROD[-Plant<Yellow>]",
          "Animal<$Fish>",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I do perfectly have eight money for Luxury Foods. Nice."
      doTasks(*playCardTasks("$LuxuryFoods"), "8 Pay<> FROM MC", "$LuxuryFoods FROM ProjectCard")
      doTask("PassBaton")
    }
    with(green) {
      // "I will use Fish to take a fish and two money."
      doTasks(*cardActionTasks("$Fish"), "Animal<$Fish>", "2 MC")
      doTask("PassBaton")
    }
    // "I believe I pass."
    yellow.doTask("Pass")
    with(green) {
      // "I will use Local Heat Trapping for free to spend five heat and get two fish. Blue fish,
      // red fish, blue fish. Yes. And four money."
      doTasks(
          *playCardTasks("$LocalHeatTrapping"),
          "$LocalHeatTrapping FROM ProjectCard",
          "-5 Heat",
          "2 Animal<$Fish>",
          "4 MC",
      )
      // "I'll sell at least three cards ... sell one more."
      doTasks(*standardProjectTasks("SellPatentsProject"), "4 MC FROM ProjectCard")
    }
    // board-18-47-04.jpg shows three cubes on Fish and the fourth knocked above the card beside the
    // deck, corroborating all four narrated fish.
    with(green) {
      // "All right, let's have some fun. Let's do Bushes of Love. It'll also help me with the
      // bigger lead on Magnate. Bushes of Love cost me eight, and it gives me a planta."
      doTasks(
          *playCardTasks("$Bushes"),
          "8 Pay<> FROM MC",
          "$Bushes FROM ProjectCard",
          "PROD[2 Plant]",
          "2 Plant",
          "Plant",
      )
      // "Robot Pollinators cost me seven ... it gives me a plant production, and since I have
      // three plant tags now, it also gives me three plants."
      doTasks(
          *playCardTasks("$RobotPollinators"),
          "7 Pay<> FROM MC",
          "$RobotPollinators FROM ProjectCard",
          "PROD[Plant]",
          "3 Plant",
      )
      // "And I will plant forest, whatever that means, on this spot for two more plants. I made it
      // to the next one." The final photograph identifies the adjacent space.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_7_7>",
          "OxygenStep",
          "2 Plant",
      )
      // Green says "now I'll sell two more cards" while working out whether another greenery is
      // possible, but does not complete that sale: the app goes from 8 M€ to the
      // House Printing payment, and the sourced card ledger leaves only the later two-card sale.
      // "I cannot quite make it. So I'm just going to do stupid House Printing for stupid all of
      // my money, and technically I get a steel production from that."
      doTasks(
          *playCardTasks("$HousePrinting"),
          "8 Pay<> FROM MC",
          "$HousePrinting FROM ProjectCard",
          "PROD[Steel]",
      )
      // "And then I'll sell two cards. And that is the end."
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTask("Pass")
    }

    // Production.
    green.doTasks("40 MC", "2 Titanium", "5 Plant", "Steel")
    yellow.doTasks("72 MC", "4 Heat", "4 Plant", "2 Steel", "2 Titanium")
    // Both app logs after final production and before final greenery.
    with(green) {
      assertProduction(m = 8, s = 1, t = 2, p = 5, e = 0, h = 0)
      assertResources(m = 42, s = 1, t = 3, p = 8, e = 0, h = 0)
      assertCounts(32 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 11, s = 2, t = 2, p = 4, e = 0, h = 4)
      assertCounts(61 to "TerraformRating")
    }
    assertSidebar(gen = 10, temp = 8, oxygen = 14, oceans = 9, venus = 30)
    // "Do you get to plant a forest?" "I do not because you took me down to seven."
    yellow.doTask("PassBaton")

    with(green) {
      // "Okay, I get to plant a forest. And there's not that many places I can put it. I think
      // literally this is the only ... oh, I guess that also works. But I get a steel for that."
      // The replacement photograph identifies the legal adjacent space at 5,4.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Cimmeria_5_4>",
          "Steel",
      )
    }
    green.doTask("PassBaton")
    // board-18-47-04.jpg is the replacement final photograph after this greenery. It also confirms
    // Asteroid Deflection System in Yellow's tableau, directly below Shuttles.

    green.assertResources(m = 42, s = 2, t = 3, p = 0, e = 0, h = 0)
    yellow.assertResources(m = 72, s = 3, t = 2, p = 7, e = 0, h = 6)

    assertEquals(
        """
        |                      1     2     3     4     5     6     7     8     9
        |                     /     /     /     /     /     /     /     /     /
        |
        | 1 -              W     LP   [S2]  [G2]  [O]
        |
        | 2 -          [O]    L     L     L    LPS    WP
        |
        | 3 -        L     L     LX    L     L     LP    L
        |
        | 4 -     VS    L     LS    L    LSS    L    [G2]   LC
        |
        | 5 -  L     L     L    [G1]   LS   [G2]  [C2]  [G2]   W
        |
        | 6 -    [G2]  [C2]  [G1]   L    [C2]  [G2]   L    [S2]
        |
        | 7 -       [G2]  [G2]  [C1]  [G2]  [G1]  [S2]  [O]
        |
        | 8 -          [O]   [G2]  [S2]  [G2]  [C1]  [O]
        |
        | 9 -             [O]   [O]   [O]   [G2]  [O]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )

    // "You will agree, I assume, that I have three, six, four, and three. That's sixteen. And I
    // have fifteen out here." "All right, so I got 122." "What was your score?" "83 points." The
    // loose fourth Fish cube in the final photograph makes Green's rule-correct score 84; the
    // spoken tally evidently missed the displaced cube.
    val score = Summarizer(game)
    assertEquals(5, score.net("Milestone", "VictoryPoint<Green>"))
    assertEquals(10, score.net("Milestone", "VictoryPoint<Yellow>"))
    assertEquals(5, score.net("FirstPlace", "VictoryPoint<Green>"))
    assertEquals(10, score.net("FirstPlace", "VictoryPoint<Yellow>"))
    assertEquals(10, score.net("Tile", "VictoryPoint<Green>"))
    assertEquals(
        24,
        score.net("Tile", "VictoryPoint<Yellow>"),
    )
    assertEquals(32, score.net("Card", "VictoryPoint<Green>"))
    assertEquals(17, score.net("Card", "VictoryPoint<Yellow>"))
    green.assertCounts(84 to "VictoryPoint", 0 to "Victory")
    yellow.assertCounts(122 to "VictoryPoint", 1 to "Victory")
  }
}
