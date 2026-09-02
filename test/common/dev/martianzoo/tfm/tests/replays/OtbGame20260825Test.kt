package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test
import kotlin.test.assertEquals

/** Physical game played Tuesday and Wednesday, 2026-08-25–26. */
internal class OtbGame20260825Test : AbstractFullGameTest() {
  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  override val config =
      GameConfig(
          """
          CimmeriaMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, PromoCardPack, TurmoilCardPack

          Energizer, Farmer, Philantropist, Producer, RimSettler, Hoverlord
          Magnate, Manufacturer, Metropolist, SpaceBaron, Suburbian, Venuphile
          """,
          "Dad",
          "Ellie",
      )

  @Test
  internal fun otbGame20260825() {
    TfmWorkflow.Auto(game).launch()
    val dad = game.tfm(Player.PLAYER1)
    val ellie = game.tfm(Player.PLAYER2)

    // 9:17:17 pm: "This is a two-player game on the Terra Cimmeria board. We're using
    // Venus, Prelude, Prelude 2, Promos, the following milestones and awards: Energizer,
    // Farmer, Philanthropist, Producer, Rim Settler, Hoverlord; Magnate, Manufacturer,
    // Metropolist, Space Baron, Suburbian, Venuphile."
    // "Terralabs research. I get 14 money and spend all 10 of it. Then I lose a TR."
    dad.playCorp(TerraLabsResearch, 10).expect("4 MC, 10 ProjectCard, -TerraformRating")
    // 9:31:05 pm: "I can play Viron for 48 and I spend 15 on five cards."
    ellie.playCorp(Viron, 5).expect("33 MC")

    dad.turn {
      // "Focused organization. Draw one card. And gain one standard resource." Dad takes titanium.
      playPrelude(FocusedOrganization) { doTask("Titanium") }
      // "I had to experience what it might be like to have Terra Labs and have money at the same
      // time." Head Start gains two steel and 22 M€ for the eleven project cards in hand.
      playPrelude(HeadStart) {
        // The first immediate action reuses Focused Organization: discard Red Ships and 1 M€,
        // then draw a card and take titanium.
        doTask("UseAction<UseCardAction, Action1>", 1)
        doTask("ActionUsedMarker<$FocusedOrganization>")
        cardAction1(FocusedOrganization) {
          doTask("-MC", 2)
          doTask("Titanium", 2)
        }

        // "For the other one. Advertising for 4."
        doTask("UseAction<PlayCardFromHand, Action1>")
        doTask("PlayCard<Class<ProjectCard>, Class<$Advertising>>")
        pay(4)
      }
    }

    ellie.turn {
      // "Supply drop, gain 3 titanium, 8 steel, and 3 plantas."
      playPrelude(SupplyDrop)
      // "Terraforming deal is an effect prelude. Each step your TR is raised, you gain 2 money."
      playPrelude(TerraformingDeal)
    }

    dad.turn {
      // "I'm going to play mineral deposit for 5."
      playProject(MineralDeposit, 5)
      // Dad initially announces six steel, two titanium, and 9 M€. He later asks to retain two
      // steel, making the corrected payment 4 steel, 2 titanium, and 13 M€.
      playProject(SpaceElevator, 13, steel = 4, titanium = 2)
    }
    // The phone entry immediately after Space Elevator records a titanium resource instead of the
    // production Space Elevator actually grants.
    dad.exMachina("PROD[-Titanium], Titanium")
    ellie.turn {
      // Ellie's ledger records -12 M€ and +1 plant before Aquifer Pumping's payment, and the
      // subsequent oceans all invoke this card. The transcript omitted the play sentence.
      playProject(ArcticAlgae, 12)
      // "Aquifer pumping! I'll spend my 8 steel and 2 real."
      playProject(AquiferPumping, 2, steel = 8)
    }
    dad.turn {
      // "I'm gonna use a spa-che elevator, spend 1 steel and get 5 real."
      cardAction1(SpaceElevator)
      // 9:37:18 pm: "I'll pay eight for rover construction."
      playProject(RoverConstruction, 8)
    }
    ellie.turn {
      // Ocean at 9,7: "one plant, then Arctic Algae gives me two more plants, and Terraforming
      // Deal gives me two money."
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(9, 7)
      }
      // "Vairon, to reuse aquifer pumping, spend eight." The second ocean goes at 9,6.
      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, Action1>")
        pay(8)
        placeTile(9, 6)
      }
    }
    dad.pass()
    ellie.turn {
      // "I can do the plant forest action, spend eight plants on greenery." It goes at 8,7.
      convertPlants { placeTile(8, 7) }
      // Mining Area costs 4 M€ and goes at 8,6, gaining titanium and titanium production.
      playProject(MiningArea, 4) { placeTile(8, 6) }
    }
    ellie.turn {
      // "Now for six. I play sponsors."
      playProject(Sponsors, 6)
      pass()
    }
    // "I'm taking two one. So I took the two titanium spot, but I don't get the two titanium."
    dad.wgt("OceanTile<Cimmeria_2_1>").expect("0 TerraformRating, 0 Titanium")

    with(dad) {
      assertProduction(m = 1, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 20, s = 2, t = 1, p = 0, e = 0, h = 0)
      assertCounts(19 to "TerraformRating")
    }
    with(ellie) {
      assertProduction(m = 2, s = 0, t = 1, p = 0, e = 0, h = 0)
      assertResources(m = 32, s = 0, t = 5, p = 4, e = 0, h = 0) // ledger shows 2 plants, mistake
      assertCounts(23 to "TerraformRating")
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 3, venus = 0)

    // 9:47:18 pm: Dad's direct Terralabs buttons record four one-M€ purchases; Ellie's app
    // records two ordinary purchases for six M€.
    ellie.buyCards(2)
    dad.buyCards(4)

    ellie.turn {
      // The ocean at 9,5 gives "two money and two plants for placing, two money and two plants
      // for my card bonuses."
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(9, 5)
      }
    }
    dad.turn { cardAction1(SpaceElevator) }
    ellie.turn {
      // Viron repeats Aquifer Pumping and places the ocean at 8,4.
      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, Action1>")
        pay(8)
        placeTile(8, 4)
      }
      // Ellie spends eight plants for a greenery at 8,5.
      convertPlants { placeTile(8, 5) }
    }
    dad.turn {
      // Focused Organization discards Physics Complex and 1 M€, then gains steel.
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Steel")
      }
    }
    ellie.turn {
      // "Methane from titan. That'll be, spend my five titanium is 15 and 13 real." It gains two
      // plant production and two heat production.
      playProject(MethaneFromTitan, 13, titanium = 5)
    }
    dad.turn {
      // Sponsored Academies costs 9 M€: discard Urbanized Area, draw three, and Ellie draws one.
      playProject(SponsoredAcademies, 9)
    }
    ellie.pass()
    dad.pass()
    // "I'm going to world gov. Akigen. Akigen up to three."
    ellie.wgt("OxygenStep").expect("0 TerraformRating")
    with(dad) {
      assertProduction(m = 1, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 31, s = 2, t = 1, p = 0, e = 0, h = 0)
      assertCounts(19 to "TerraformRating")
    }
    with(ellie) {
      assertProduction(m = 2, s = 0, t = 1, p = 2, e = 0, h = 2)
      assertResources(m = 41, s = 0, t = 1, p = 5, e = 0, h = 2)
      assertCounts(26 to "TerraformRating")
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 3, oceans = 5, venus = 0)

    // "I'm buying four cards. For four money." Ellie buys three.
    dad.buyCards(4)
    ellie.buyCards(3)

    dad.turn {
      // "Let's do focused organization first." Dad discards Ganymede Colony and 1 M€, then takes
      // titanium.
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Titanium")
      }
    }
    ellie.turn {
      // "Neptunian power consultants. Would have been nice to have this much earlier, but not
      // bad." Ellie pays 14 M€.
      playProject(NeptunianPowerConsultants, 14)
    }
    dad.turn { cardAction1(SpaceElevator) }
    ellie.turn {
      // Aquifer Pumping places the ocean at 9,9; Ellie pays 5 M€ for an energy production and a
      // hydroelectric resource.
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(9, 9)
        doTask("UseAction<NeptunianOption, Action1>")
        pay(5)
      }
      // Ellie immediately spends eight plants for a greenery at 9,8.
      convertPlants { placeTile(9, 8) }
    }
    dad.turn {
      // "This is probably not the move, but I just can't resist it. City!" Dad pays for a city at
      // 8,8, gaining two plants, two ocean-adjacency M€, and two M€ from Rover Construction.
      stdProject("CitySP") { placeTile(8, 8) }
    }
    ellie.turn {
      // Viron repeats Aquifer Pumping at 8,9; Ellie again pays 5 M€ for energy production and a
      // hydroelectric resource.
      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, Action1>")
        pay(8)
        placeTile(8, 9)
        doTask("UseAction<NeptunianOption, Action1>")
        pay(5)
      }
    }
    dad.turn {
      // "Investment loan costs me three. Costs me a money production, gets me ten money."
      playProject(InvestmentLoan, 3)
    }
    ellie.pass()
    dad.turn {
      // "I'll just get my optimal arrow breaking down. For two titanium and one real."
      playProject(OptimalAerobraking, 1, titanium = 2)
      // Viral Enhancers costs 9 M€ and gives Dad a plant.
      playProject(ViralEnhancers, 9)
    }
    dad.turn {
      // "Let us sell one card." The sold card is Hermetic Order of Mars.
      sellPatents(1)
      // "I do have three science tags now, so I can play lightning harvest." It raises M€ and
      // energy production.
      playProject(LightningHarvest, 8)
      pass()
    }
    // "I actually want to raise Venus. I'm raising Venus to two percent."
    dad.wgt("VenusStep").expect("0 TerraformRating")

    with(dad) {
      assertProduction(m = 2, s = 0, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 21, s = 1, t = 0, p = 3, e = 1, h = 0)
      assertCounts(19 to "TerraformRating")
    }
    with(ellie) {
      assertProduction(m = 2, s = 0, t = 1, p = 2, e = 2, h = 2)
      assertResources(m = 35, s = 0, t = 2, p = 7, e = 2, h = 4)
      assertCounts(29 to "TerraformRating")
      assertCardResources(2 to NeptunianPowerConsultants)
    }
    assertSidebar(gen = 4, temp = -30, oxygen = 4, oceans = 7, venus = 2)

    // Dad buys four cards, leaving 17 M€. Ellie keeps three.
    dad.buyCards(4)
    ellie.buyCards(3)

    ellie.turn {
      // Aquifer Pumping places an ocean at 7,9, then Neptunian Power Consultants costs 5 M€ for
      // an energy production and a hydroelectric resource.
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(7, 9)
        doTask("UseAction<NeptunianOption, Action1>")
        pay(5)
      }
      // "Viar on. Oceans are now maxed." The final ocean goes at 1,5, followed by the last
      // Neptunian Power Consultants payment.
      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, Action1>")
        pay(8)
        placeTile(1, 5)
        doTask("UseAction<NeptunianOption, Action1>")
        pay(5)
      }
    }
    dad.turn { cardAction1(SpaceElevator) }
    ellie.turn {
      // The greenery at 7,6 gains a plant and two steel.
      convertPlants { placeTile(7, 6) }
      // The second greenery goes at 7,4, gaining titanium and four M€.
      convertPlants { placeTile(7, 4) }
    }
    dad.turn {
      // Focused Organization discards Open City and 1 M€, draws the promo card, and takes
      // titanium.
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Titanium")
      }
    }
    ellie.turn {
      // Homeostasis Bureau gains two heat production. "If you forget, the computer will remind
      // us later. It's nice to be reminded."
      playProject(HomeostasisBureau, 12, steel = 2)
    }
    dad.turn {
      // "Spenducing asteroid, three worth of titanium and 13 real." The one titanium resource is
      // worth 3 M€; Venus rises twice, giving Dad his first two TR plus three heat and three M€.
      playProject(SpinInducingAsteroid, 13, titanium = 1)
    }
    ellie.pass()
    dad.turn {
      // "My grand plan was to play sulfur eating bacteria, and I finally can." It costs 6 M€;
      // Viral Enhancers adds one microbe and its action adds another.
      playProject(SulphurEatingBacteria, 6) { addCardResources(SulphurEatingBacteria) }
      cardAction1(SulphurEatingBacteria)
      pass()
    }
    // "I will world gov Venus. Venus to 8."
    ellie.wgt("VenusStep").expect("0 TerraformRating")

    with(dad) {
      assertProduction(m = 2, s = 0, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 28, s = 0, t = 0, p = 3, e = 1, h = 4)
      assertCounts(21 to "TerraformRating")
      assertCardResources(2 to SulphurEatingBacteria)
    }
    with(ellie) {
      assertProduction(m = 2, s = 0, t = 1, p = 2, e = 4, h = 4)
      assertResources(m = 35, s = 0, t = 4, p = 2, e = 4, h = 10)
      assertCounts(33 to "TerraformRating")
      assertCardResources(4 to NeptunianPowerConsultants)
    }
    assertSidebar(gen = 5, temp = -30, oxygen = 6, oceans = 9, venus = 8)

    // Dad buys four cards; Ellie buys one.
    dad.buyCards(4)
    ellie.buyCards(1)

    dad.turn {
      // "Oh man, I don't have any money. I'm doing it. Am I? Let's just do it." Dad pays 5 M€ for
      // Meat Industry and 10 M€ for Pets; its two animals return 4 M€ through Meat Industry.
      playProject(MeatIndustry, 5)
      playProject(Pets, 10) { addCardResources(Pets) }
    }
    ellie.turn {
      // "I pay eight because I want my gahuga back."
      playProject(GhgProducingBacteria, 8)
    }
    dad.turn {
      // "Might as well spend an energy honestly." Focused Organization discards Martian Rails
      // and the energy, gains steel, and Space Elevator immediately turns that steel into 5 M€.
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
      cardAction1(SpaceElevator)
    }
    ellie.turn {
      // "I use the back gahuga to add back to itself."
      cardAction1(GhgProducingBacteria)
    }
    dad.turn {
      playProject(ExtremeColdFungus, 13)
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      // "Imported nutrients." "You are fucking kidding me."
      // Ellie gains four plants and adds four microbes to GHG Producing Bacteria.
      playProject(ImportedNutrients, 2, titanium = 4) { addCardResources(GhgProducingBacteria) }
      // Ellie spends 8 M€ to claim Farmer, the milestone Dad had been preparing to claim.
      claimMilestone(cn("Farmer"))
    }
    dad.turn {
      // "Now that I destroyed your beautiful plan." Dad removes all four Sulphur-Eating Bacteria
      // microbes for 12 M€.
      cardAction2(SulphurEatingBacteria) {
        doTask("-4 Microbe<$SulphurEatingBacteria> THEN 12 MC")
      }
      // Dad spends 8 M€ to claim Philanthropist.
      claimMilestone(cn("Philantropist"))
    }
    ellie.turn {
      // Viron reuses GHG Producing Bacteria, removing two microbes to raise temperature.
      cardAction1(Viron) {
        doTask("UseAction<$GhgProducingBacteria, Action2>")
      }
    }
    dad.pass()
    ellie.turn {
      // "I do, actually. Kelp farming!" For 17 M€, Ellie gains two M€ production, three plant
      // production, and two plants.
      playProject(KelpFarming, 17)
      pass()
    }
    // "I know what I'm not doing. I'm not doing either of those, so Venus it is!" The game moves
    // to Generation 6 and adjourns before Research.
    dad.wgt("VenusStep").expect("0 TerraformRating")

    with(dad) {
      assertProduction(m = 2, s = 0, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 32, s = 0, t = 0, p = 4, e = 1, h = 4)
      assertCounts(21 to "TerraformRating", 1 to "Philantropist")
      assertCardResources(2 to Pets, 0 to SulphurEatingBacteria)
    }
    with(ellie) {
      assertProduction(m = 4, s = 0, t = 1, p = 5, e = 4, h = 4)
      assertResources(m = 40, s = 0, t = 1, p = 13, e = 4, h = 18)
      assertCounts(34 to "TerraformRating", 1 to "Farmer")
      assertCardResources(3 to GhgProducingBacteria, 4 to NeptunianPowerConsultants)
    }
    assertSidebar(gen = 6, temp = -28, oxygen = 6, oceans = 9, venus = 10)

    dad.assertCounts(
        14 to "ProjectCard",
        1 to "$TerraLabsResearch",
        1 to "$HeadStart",
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
    ellie.assertCounts(
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

    // Before Generation 6 Research, Dad's app entries 116–117 correct the Generation 1 entry to
    // titanium production and add the four net titanium resources it should have produced.
    dad.exMachina("PROD[Titanium], 4 Titanium")
    with(dad) {
      assertProduction(m = 2, s = 0, t = 1, p = 0, e = 1, h = 0)
      assertResources(m = 32, s = 0, t = 4, p = 4, e = 1, h = 4)
    }

    // 12:21:20 pm on Aug 26: Ellie buys two cards. Terralabs lets Dad buy all four for 1 M€
    // apiece.
    ellie.buyCards(2)
    dad.buyCards(4)

    ellie.turn {
      // "I've used the convert heat standard action twice, which gets up to minus 24, so I get a
      // heat production." Homeostasis Bureau and Terraforming Deal account for the 10 M€ correction
      // Ellie remembered at the end of the generation.
      convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating, 5 MC")
      convertHeat().expect("-8 Heat, TemperatureStep, PROD[Heat], TerraformRating, 5 MC")
    }
    // Ellie does not take either conversion's bonuses at the physical table. She reconstructs the
    // missing 6 M€ from Homeostasis Bureau and 4 M€ from Terraforming Deal at generation end.
    ellie.exMachina("-10 MC")
    dad.turn {
      // "Asteroid mining consortium. I do have the titanium production that I need. You lose a
      // titanium production. I gain a titanium production. And I have to pay 13 for it."
      playProject(AsteroidMiningConsortium, 13) {
            doTask("PROD[-Titanium<Ellie>]")
          }
          .expect("PROD[Titanium<Dad>, -Titanium<Ellie>]")
    }
    ellie.turn {
      // "Pay 9 for mining rights... I'll place it here for 2 steel and 2 money. 6-9."
      playProject(MiningRights, 9) { placeTile(6, 9) }.expect("2 Steel, PROD[Steel]")
    }
    dad.turn {
      // "I'll play Solar Net... I pay 7. And I draw 2 useless cards."
      playProject(Solarnet, 7).expect("ProjectCard")
    }
    ellie.turn {
      // "Two steel and nine real to ... get the ore processor."
      playProject(OreProcessor, 9, steel = 2)
    }
    dad.turn {
      // Focused Organization discards an unidentified card and an energy, then draws a card and
      // takes steel.
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
    }
    ellie.turn {
      // "Four money to build power infrastructure."
      playProject(PowerInfrastructure, 4)
    }
    dad.turn {
      // "Now I can use space elevator to spend one steel and get five real."
      cardAction1(SpaceElevator)
    }
    ellie.turn {
      // "Power Supply Consortium! ... I lose an energy production."
      playProject(PowerSupplyConsortium, 5) { doTask("PROD[-Energy<Dad>]") }
          .expect("PROD[-Energy<Dad>, Energy<Ellie>]")
    }
    dad.turn {
      // Venus Orbital Survey costs four titanium and 6 M€.
      playProject(VenusOrbitalSurvey, 6, titanium = 4)
    }
    ellie.turn {
      // "I will place a GHG back."
      cardAction1(GhgProducingBacteria)
    }
    dad.turn {
      // Venus Orbital Survey reveals Magnetic Field Dome and Energy Saving. Neither has a Venus
      // tag, so Dad buys both for 1 M€ apiece through Terralabs.
      cardAction1(VenusOrbitalSurvey) {
            doTask("Ok")
            doTask("Ok")
            dad.pay(mc = 2)
          }
          .expect("2 ProjectCard")
    }
    ellie.turn {
      // "Pay seven for Venus Magnetizer."
      playProject(VenusMagnetizer, 7)
    }
    dad.turn {
      // Law Suit follows Power Supply Consortium's attack and takes the 2 M€ Ellie has at the time.
      playProject(LawSuit, 2) { doTask("3 MC<Dad> FROM MC<Ellie>.") }
    }
    ellie.turn {
      // "Now suppose I can decrease energy product one step, raise Venus one step. That is a TR
      // for me."
      cardAction1(VenusMagnetizer).expect("PROD[-Energy], VenusStep, TerraformRating, 2 MC")
    }
    dad.turn {
      // "Use extreme cold fungus to drop two microbes onto sulfur eating bacteria."
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      // The greenery at 5,8 gains one steel and one card.
      convertPlants { placeTile(5, 8) }
          .expect("-8 Plant, Steel, ProjectCard, OxygenStep, TerraformRating, 2 MC")
    }
    dad.turn {
      // "Sell one patent and then spend six real money to build windmills."
      sellPatents(1)
      playProject(Windmills, 6)
    }
    ellie.turn {
      // Ore Processor raises oxygen to 8%, which also raises temperature. Viron then reuses GHG
      // Producing Bacteria to raise temperature once more.
      cardAction1(OreProcessor)
          .expect("-4 Energy, Titanium, OxygenStep, TemperatureStep, 2 TerraformRating, 7 MC")
      cardAction1(Viron) { doTask("UseAction<$GhgProducingBacteria, Action2>") }
          .expect("-2 Microbe, TemperatureStep, PROD[Heat], TerraformRating, 5 MC")
    }
    dad.turn {
      // "Use sulfur-eating bacteria to add a microbe to sulfur-eating bacteria."
      cardAction1(SulphurEatingBacteria)
    }
    ellie.turn {
      // "I can spend eight to claim the Producer."
      claimMilestone(cn("Producer"))
    }
    dad.pass()
    ellie.pass()
    // "World government is you... increase the temp ... to minus eighteen."
    ellie.wgt("TemperatureStep").expect("0 TerraformRating")
    // "Way back in the generation, I did two other..." "Oh, I also think I forgot to give the
    // terraforming deal money. So that's a total of ten." Ellie takes the omitted 10 M€ now.
    ellie.exMachina("10 MC")

    with(dad) {
      assertProduction(m = 2, s = 0, t = 2, p = 0, e = 1, h = 0)
      assertResources(m = 23, s = 0, t = 2, p = 4, e = 1, h = 4)
      assertCounts(21 to "TerraformRating", 1 to "Philantropist")
      assertCardResources(2 to Pets, 3 to SulphurEatingBacteria)
    }
    with(ellie) {
      assertProduction(m = 4, s = 1, t = 0, p = 5, e = 4, h = 6)
      assertResources(m = 63, s = 2, t = 2, p = 10, e = 4, h = 8)
      assertCounts(41 to "TerraformRating", 1 to "Farmer", 1 to "Producer")
      assertCardResources(2 to GhgProducingBacteria, 4 to NeptunianPowerConsultants)
    }
    assertSidebar(gen = 7, temp = -18, oxygen = 8, oceans = 9, venus = 12)

    // 12:45:28 pm: Dad's Terralabs research costs 1 M€ per card; Ellie buys all four at the
    // ordinary 3-M€ rate.
    dad.buyCards(4)
    ellie.buyCards(4)

    dad.turn {
      // Atmoscoop raises Venus twice. Crossing 16% grants the third TR; no card can take its
      // floaters. Advertising gains one M€ production.
      playProject(Atmoscoop, 16, titanium = 2) { doTask("2 VenusStep") }
          .expect("2 VenusStep, 3 TerraformRating, PROD[MC]")
    }
    ellie.turn {
      // The city at 6,3 triggers Dad's Pets, Meat Industry, and Rover Construction. The greenery
      // at 6,2 takes the two-card placement bonus and raises oxygen to 9%.
      stdProject("CitySP") { placeTile(6, 3) }
      convertPlants { placeTile(6, 2) }
          .expect("-8 Plant, 2 ProjectCard, OxygenStep, TerraformRating, 2 MC")
    }
    dad.turn {
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
    }
    ellie.turn { fundAward(cn("Suburbian"), 8) }
    dad.turn { cardAction1(SpaceElevator) }
    ellie.turn {
      // Ellie explicitly preserved both steel while playing Rego Plastics.
      intentionalUnderpay()
      playProject(RegoPlastics, 10)
    }
    dad.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn { playProject(FusionPower, 8, steel = 2) }
    dad.turn {
      // Fish and Ice Cap Melting have no Venus tag. Dad buys Fish for 1 M€ and discards the other.
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        dad.buyCards(1)
      }
    }
    ellie.turn {
      convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating, 5 MC")
    }
    dad.turn {
      cardAction2(SulphurEatingBacteria, x = 5)
    }
    ellie.turn {
      cardAction1(OreProcessor).expect("-4 Energy, Titanium, OxygenStep, TerraformRating, 2 MC")
    }
    dad.turn { playProject(JetStreamMicroscrappers, 12) }
    ellie.turn {
      cardAction1(VenusMagnetizer).expect("PROD[-Energy], VenusStep, TerraformRating, 2 MC")
    }
    dad.turn {
      playProject(AirScrappingExpedition, 13) {
        addCardResources(JetStreamMicroscrappers)
      }
    }
    ellie.turn {
      playProject(UnexpectedApplication, 4)
          .expect("-2 ProjectCard, VenusStep, TerraformRating, -2 MC")
    }
    dad.turn {
      cardAction2(JetStreamMicroscrappers).expect("-2 Floater, VenusStep, TerraformRating")
    }
    ellie.turn {
      cardAction1(Viron) { cardAction1(VenusMagnetizer) }
          .expect("PROD[-Energy], VenusStep, TerraformRating, 2 MC")
    }
    dad.pass()

    ellie.turn {
      cardAction2(GhgProducingBacteria).expect("-2 Microbe, TemperatureStep, TerraformRating, 5 MC")
      playProject(CloudTourism, 11).expect("PROD[2 MC]")
    }
    ellie.turn {
      cardAction1(CloudTourism)
      playProject(Mine, 4)
    }
    ellie.turn {
      playProject(Shuttles, 1, titanium = 3).expect("PROD[-Energy, 2 MC]")
    }
    ellie.pass()
    dad.wgt("OxygenStep").expect("0 TerraformRating")

    // 1:08 pm app-log checkpoint and 1:08:17 pm board photograph, before Generation 8 Research.
    with(dad) {
      assertProduction(m = 3, s = 0, t = 2, p = 0, e = 1, h = 0)
      assertResources(m = 30, s = 0, t = 2, p = 4, e = 1, h = 4)
      assertCounts(26 to "TerraformRating", 1 to "Philantropist")
      assertCardResources(3 to Pets, 1 to JetStreamMicroscrappers)
    }
    with(ellie) {
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
    dad.exMachina("MC")
    ellie.exMachina("-MC")

    // "It's gen eight, which would make it my start ... let me get my absolute barrage of cards."
    // Dad's Terralabs research costs one M€ per card; Ellie's app records two cards bought for 6
    // M€.
    dad.buyCards(4)
    ellie.buyCards(2)

    ellie.turn {
      // Player-supplied transcription of the audio omitted by transcript-finished.md: "I will Venus
      // Magnetize, and Viron Venus Magnetize. I get 4 from my special effect."
      cardAction1(VenusMagnetizer).expect("PROD[-Energy], VenusStep, TerraformRating, 2 MC")
      cardAction1(Viron) { cardAction1(VenusMagnetizer) }
          .expect("PROD[-Energy], VenusStep, TerraformRating, 2 MC")
    }
    dad.turn {
      // "I spend 13 on Immigrant City. I lose 2 money production and 1 energy production." Its
      // city at 7,5 restores one M€ production and triggers Rover Construction, Pets, and Meat
      // Industry.
      playProject(ImmigrantCity, 13) { placeTile(7, 5) }.expect("PROD[-MC, -Energy], Animal<$Pets>")
    }

    ellie.turn {
      // "Pay 25 for a city standard project. It'll go right here in 6,6."
      stdProject("CitySP") { placeTile(6, 6) }
          .expect("Titanium<Ellie>, PROD[MC<Dad>], 4 MC<Dad>, Animal<Dad, $Pets<Dad>>")
      // "Second will be to ore process. Spend four energy, gain titanium, oxygen raise, TR, two
      // money." Oxygen reaches 12%.
      cardAction1(OreProcessor).expect("-4 Energy, Titanium, OxygenStep, TerraformRating, 2 MC")
    }
    dad.turn {
      // "Goodbye, Regular Theaters. And a standard research, obviously I'm using my Focused
      // Organization. And I'll discard a piece of energy ... and a piece of steel. For my second
      // trick, I'll use the steel with Space Elevator to get five money."
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
      cardAction1(SpaceElevator)
    }
    ellie.turn {
      // "I will greenery standard project. And I will put the first one here for ... wait, no,
      // four money and two plants. Seven, three."
      stdProject("GreenerySP") { placeTile(7, 3) }.expect("2 Plant, OxygenStep, TerraformRating")
      // "I'll go 6,7. Yes, two steel. Placing a greenery by converting plants."
      convertPlants { placeTile(6, 7) }.expect("2 Steel, OxygenStep, TerraformRating, 2 MC")
    }
    dad.turn {
      // "First I'll Extreme Cold Fungus to put two microbes onto Sulphur-Eating Bacteria."
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      // "I will use GHG-Producing Bacteria. Boop."
      cardAction1(GhgProducingBacteria)
    }
    dad.turn {
      // "Earth Catapult ... cost me 23." Advertising raises M€ production.
      playProject(EarthCatapult, 23).expect("PROD[MC]")
    }
    ellie.turn {
      // "Right, I will add Clearism [sic]."
      cardAction1(CloudTourism).expect("Floater<$CloudTourism>")
    }
    dad.turn {
      // "I will use Venus Orbital Survey to reveal Research Outpost and OmniCorp [sic] and pay two
      // money for them."
      cardAction1(VenusOrbitalSurvey) {
            doTask("Ok")
            doTask("Ok")
            pay(2)
          }
          .expect("2 ProjectCard")
    }
    ellie.turn {
      // "Ecological Zone! Pay 12." It goes at 7,8 and gains two animals from its own two tags.
      playProject(EcologicalZone, 12) { placeTile(7, 8) }.expect("2 Animal<$EcologicalZone>")
    }
    dad.turn {
      // "I'm going to play Symbiotic Fungus, which costs two money. It has a microbial tag, so
      // that means it would give me a microbe, but it doesn't actually take microbes. So I get a
      // plant. And then I'll use its action to add a microbe to Sulphur-Eating Bacteria."
      playProject(SymbioticFungus, 2).expect("Plant")
      cardAction1(SymbioticFungus) { doTask("Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      // "I will pay two steel for Martian Lumber Corp." Its plant tag adds an Ecological Zone
      // animal.
      playProject(MartianLumberCorp, steel = 2).expect("PROD[Plant], Animal<$EcologicalZone>")
    }
    dad.turn {
      // "Stratospheric Expedition costs ten. I pay six worth of titanium. How was I going to pay
      // for the other four? Because I was going to take this first. Right. Take nine money and
      // then spend four of it." Its floaters go to Jet Stream Microscrappers.
      cardAction2(SulphurEatingBacteria, x = 3).expect("-3 Microbe<$SulphurEatingBacteria>, 9 MC")
      playProject(StratosphericExpedition, 4, titanium = 2) {
            addCardResources(JetStreamMicroscrappers)
          }
          .expect("ProjectCard, 3 Heat, 2 Floater<$JetStreamMicroscrappers>")
    }
    // "Actually, I think I'm quite out of stuff to do, so I pass."
    ellie.pass()
    dad.turn {
      // "I'll sell 6 cards ... play Predators, which costs me 12." Viral Enhancers and the action
      // each add an animal; the action removes one of Ellie's Ecological Zone animals.
      sellPatents(6)
      playProject(Predators, 12) { addCardResources(Predators) }.expect("Animal<$Predators>")
      cardAction1(Predators) { doTask("-Animal<Ellie, $EcologicalZone<Ellie>>") }
          .expect("Animal<$Predators>, -Animal<Ellie, $EcologicalZone<Ellie>>, 2 MC")

      // "Let's sell four more cards ... and spend all six of my money to build Stratobirds. That
      // means that I lose a floater from Jet Stream. It has an animal tag so it gets an animal and
      // two money, and then I take the action to get an animal and two money."
      sellPatents(4)
      playProject(StratosphericBirds, 10) {
            addCardResources(StratosphericBirds)
          }
          .expect("-Floater<$JetStreamMicroscrappers>, Animal<$StratosphericBirds>")
      cardAction1(StratosphericBirds).expect("Animal<$StratosphericBirds>, 2 MC")

      // "I have four money, so let's build Heather. That costs all four of my money ... Viral
      // Enhancers gives me a plant, and then it gives me a plant production and a plant."
      playProject(Heather, 4).expect("PROD[Plant], 2 Plant")
      pass()
    }
    // "Production. Your World Government choice." The following exchange and app checkpoint
    // identify the chosen temperature raise to -12 C; World Government grants no TR.
    ellie.wgt("TemperatureStep").expect("0 TerraformRating")

    // 5:58:31 pm app-log checkpoint, before Generation 9 Research.
    with(dad) {
      assertProduction(m = 4, s = 0, t = 2, p = 1, e = 0, h = 0)
      assertResources(m = 30, s = 0, t = 2, p = 8, e = 0, h = 7)
      assertCounts(26 to "TerraformRating")
    }
    with(ellie) {
      assertProduction(m = 10, s = 2, t = 0, p = 6, e = 2, h = 6)
      assertResources(m = 69, s = 4, t = 2, p = 8, e = 2, h = 12)
      assertCounts(53 to "TerraformRating")
    }
    assertSidebar(gen = 9, temp = -12, oxygen = 14, oceans = 9, venus = 30)

    // "I'm buying four cards." "I'll buy two cards." Dad is first in Generation 9.
    dad.buyCards(4)
    ellie.buyCards(2)

    dad.turn {
      // "I'm going to use Predators to eat your ecomal. And take two money."
      cardAction1(Predators) { doTask("-Animal<Ellie, $EcologicalZone<Ellie>>") }
          .expect("Animal<$Predators>, -Animal<Ellie, $EcologicalZone<Ellie>>, 2 MC")
    }
    ellie.turn {
      // "I'll put it right here. Five, seven." "Are you standard project king or are you playing a
      // card?" "Yes, standard project." Dad's three city effects then trigger.
      stdProject("CitySP") { placeTile(5, 7) }
          .expect("PROD[MC<Dad>], 4 MC<Dad>, Animal<Dad, $Pets<Dad>>, -25 MC")
      // "Greenery to Apollinaris Mons for two titaniums." Ellie converts plants at 4,7.
      convertPlants { placeTile(4, 7) }.expect("2 Titanium")
    }
    dad.turn {
      // "I'm going to Focused Organization to discard Subterranean Reservoir and also discard ...
      // a money. To draw this card and get a steel."
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Steel")
      }
    }
    ellie.turn {
      // "My Space Shuttles gives me two off the space tags, so 16 Psyche for four titanium and 17
      // real monies."
      playProject(SixteenPsyche, 17, titanium = 4).expect("PROD[2 Titanium], -Titanium")
    }
    dad.turn {
      // "I'm going to use Space Elevator, spend one steel to get five real."
      cardAction1(SpaceElevator)
    }
    ellie.turn {
      assertCounts(21 to "MC")

      // "I will... oh yeah, pay 11, which will be... I think it's kind of a hard question of
      // which to pay. Damn, how dare. Three steel. Lose energy product.Okay."
      playProject(AsteroidDeflectionSystem, 2, steel = 3).expect("PROD[-Energy], -2 MC")
    }
    // Ellie simply forgot to pay the remaining 2 M€.
    ellie.exMachina("2 MC")

    dad.turn {
      // "I will Venus Orbital Survey. I will look at these two cards, which are Comet and Rad
      // Suits. And then I might as well buy them."
      cardAction1(VenusOrbitalSurvey) {
            doTask("Ok")
            doTask("Ok")
            pay(2)
          }
          .expect("2 ProjectCard")
    }
    ellie.turn {
      // "I might as well spend 14 to fund the Manufacturer."
      fundAward(cn("Manufacturer"), 14).expect("-14 MC")
    }
    dad.turn {
      // "I'm going to play Boozness Network for two and lose a money production. And then let's
      // just go ahead and use it. Use it tonight. Look at this card ... I might as well buy it."
      playProject(BusinessNetwork, 2).expect("PROD[-MC]")
      cardAction1(BusinessNetwork) { buyCards(1) }.expect("ProjectCard")
    }
    ellie.turn {
      // "May as well use Asteroid Deflection System ... use it tonight. You flip. Greenhouses,
      // which is not a space tag."
      cardAction1(AsteroidDeflectionSystem) { doTask("Ok") }.expect("0 TerraformRating")
    }
    dad.turn {
      // "Damn, now I wish I'd played it at the ... going to go ahead and use Aqueduct Systems for
      // seven." The three kept cards are Heat Trappers, Biomass Combustors, and Development Center.
      playProject(AqueductSystems, 7).expect("2 ProjectCard")
    }
    ellie.turn {
      // "I suppose I'll spend five monies on Venusian Insects."
      playProject(VenusianInsects, 5).expect("-5 MC")
    }
    dad.turn {
      // "I think I'll plant boop ... to plant a forest. And I'll get a titanium and two steel ...
      // by going on six, four." Immigrant City at 7,5 makes this legally adjacent.
      convertPlants { placeTile(6, 4) }.expect("Titanium, 2 Steel")
    }
    ellie.turn {
      // "I will add a Cloud Tourism."
      cardAction1(CloudTourism).expect("Floater<$CloudTourism>")
    }
    dad.turn {
      // "First I'm gonna play Freyja Biodomes ... which costs twelve. I can't play it. Okay, I
      // have to play Biomass Combustors as well, which costs two. You lose a plant production. I
      // gain two energy production and then I lose one of them again ... for Freyja Biodomes."
      playProject(BiomassCombustors, 2) { doTask("PROD[-Plant<Ellie>]") }
          .expect("PROD[2 Energy, -Plant<Ellie>]")
      playProject(FreyjaBiodomes, 12) { addCardResources(StratosphericBirds) }
          .expect("PROD[-Energy, 2 MC], Plant, 2 Animal<$StratosphericBirds>")
    }
    ellie.turn {
      // "I add a Venusian insect."
      cardAction1(VenusianInsects)
    }
    dad.turn {
      // "I'm gonna use Extremely Cold Fungus and put two microbials on Sulphur-Eating Bacteria."
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      // "Use GHG-Producing ..."
      cardAction1(GhgProducingBacteria)
    }
    dad.turn {
      // "... and I'm gonna use Symbiotic Fungus to put one more on Sulphur-Eating."
      cardAction1(SymbioticFungus) { doTask("Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      // "I Viron Venusian Insects ... yeah, I'll just Viron Venusian Insects. Hope it doesn't kill
      // me."
      cardAction1(Viron) { cardAction1(VenusianInsects) }.expect("Microbe<$VenusianInsects>")
    }
    dad.turn {
      // "I think I'm gonna play Floating Refinery for five. I get one floater for each Venus tag.
      // One, two, three, four, five, six. Woo!"
      playProject(FloatingRefinery, 5).expect("6 Floater<$FloatingRefinery>")
    }
    ellie.turn {
      // "Standard heat boop, so up to minus ten." Homeostasis Bureau pays 5 M€.
      convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating, 5 MC")
    }
    dad.turn {
      // "I just realized I have not run Stratobirds yet, so I get a silver Stratobird and two
      // money."
      cardAction1(StratosphericBirds)
    }
    // "I think I ... pass."
    ellie.pass()
    dad.turn {
      // "I will use Floating Refinery to remove two floaters from Floating Refinery and get one
      // titanium and two money."
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }
      // "I will play Heat Trappers for four money. You lose two heat production ... and I gain an
      // energy production."
      playProject(HeatTrappers, 4).expect("PROD[-2 Heat<Ellie>]")
    }
    dad.turn {
      // "And then I'll play Magnetic Field Dome for three. I lose two energy production and I gain
      // a plant production and a TR."
      playProject(MagneticFieldDome, 3)
      // "I could have played Omnicourt for all my money ... this cost nine for five plus four worth
      // of steel. And I get two more TR. So I'm dangerous now."
      playProject(Omnicourt, 5, steel = 2)
      pass()
    }
    // "I think I'll do temp up to minus eight. Shocker." World Government grants no TR.
    dad.wgt("TemperatureStep").expect("0 TerraformRating")

    // 6:19:26 pm app-log checkpoint, before Generation 10 Research.
    with(dad) {
      assertProduction(m = 6, s = 0, t = 2, p = 2, e = 0, h = 0)
      assertResources(m = 36, s = 0, t = 6, p = 3, e = 0, h = 7)
      assertCounts(29 to "TerraformRating")
    }
    with(ellie) {
      assertProduction(m = 11, s = 2, t = 2, p = 5, e = 1, h = 4)
      assertResources(m = 72, s = 3, t = 5, p = 5, e = 1, h = 10)
      assertCounts(54 to "TerraformRating")
    }
    assertSidebar(gen = 10, temp = -8, oxygen = 14, oceans = 9, venus = 30)

    // "I believe you begin." "Yeah, I'll also buy four cards. Boop."
    ellie.buyCards(4)
    dad.buyCards(4)

    ellie.turn {
      // "I will play Large Convoy ... 5 is 15. So 19 real."
      playProject(LargeConvoy, 19, titanium = 5) {
            doTask("5 Plant")
            // The full ocean track and the mandatory draw resolve automatically.
          }
          .expect("ProjectCard, 5 Plant")
      // "Will play Equatorial Magnetizer for three steel and two real. Because you have steel.
      // Worth three, yeah." Shuttles and Rego Plastics make that payment exact.
      playProject(EquatorialMagnetizer, 2, steel = 3)
    }

    dad.turn {
      // "I'm going to Sabotage for free. You lose seven money."
      playProject(Sabotage, 0) { doTask("-7 MC<Player2>") }
      // "Then you're going to lose three plants as well." "Asteroid Deflection System. You're not
      // going to lose three plants as well. Hee hee hee." "I'll do it anyway. It costs 19 and I
      // use 18 worth of titanium." The last ocean is already placed.
      playProject(Comet, 1, titanium = 6) { doTask("Ok") }
          .expect("TemperatureStep, TerraformRating, 2 MC")
    }
    ellie.turn {
      // "I play Lava Flows for 18. Doot doot. Temp is at minus two ... I place the volcano tile up
      // here. One, three for a steel. What a steel."
      playProject(LavaFlows, 18) { placeTile(1, 3) }.expect("2 TemperatureStep, 2 TerraformRating")
      // "Oh, I'm going to remember my ten money for ..."
      // "I also do my plant forest ... five, six" for a card.
      convertPlants { placeTile(5, 6) }
    }
    dad.turn {
      // "Let's use the stupid Venus Orbital Survey to reveal Comet for Venus, which is not a Venus
      // card, and Jovian Embassy. I'll buy them both."
      cardAction1(VenusOrbitalSurvey) {
            doTask("Ok")
            doTask("Ok")
            pay(2)
          }
          .expect("2 ProjectCard")
    }
    ellie.turn {
      // "I will remove two GHGs, which will be a temp raise up to zero ... the TR and five monies."
      cardAction2(GhgProducingBacteria).expect("-2 Microbe, TemperatureStep, TerraformRating, 5 MC")
      // "That was my first action, right? ... I'm also going to knock out a heat boop for eight."
      convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating, 5 MC")
    }
    dad.turn {
      // "And then I also do a heat boop, so it's at four now."
      convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating, 0 MC")
      // "Hold on. Might still be my turn. It's kind of weird, but I'm going to play Comet for
      // Venus. Cost me nine ... I remove four money from you."
      playProject(CometForVenus, 9) { doTask("-4 MC<Ellie>") }.expect("-6 MC")
    }
    ellie.turn {
      // "I use Equimag, lose an energy product, gain a TR, gain two monies."
      cardAction1(EquatorialMagnetizer)
    }
    dad.turn {
      // "Business Network to look at this card and buy it for one money."
      cardAction1(BusinessNetwork) { buyCards(1) }.expect("-MC")
    }
    ellie.turn {
      // "Asteroid Deflection System. Reveal a card. No space tag. It is Venus Governor."
      cardAction1(AsteroidDeflectionSystem) { doTask("Ok") }
    }
    dad.turn {
      // "I will use Extreme Cold Fungus to put two microbes on Sulphur-Eating Bacteria."
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      // "Cloud Tourism. Add a floater to self."
      cardAction1(CloudTourism)
    }
    dad.turn {
      // "I'm going to spend 14 to raise the temp."
      stdProject("AsteroidSP")
    }
    // It appears that Ellie gave herself a TR for Dad's temp raise (entry 376)
    ellie.exMachina("TerraformRating")

    // "I guess I just gotta take that last temp raise. And I gain five moolahs. Temp is maxed."
    ellie.turn {
      assertCounts(1 to "GameEndBarrier")
      stdProject("AsteroidSP").expect("-9 MC")
      assertCounts(0 to "GameEndBarrier")
    }
    dad.turn {
      // "I need to place a microbe onto Sulphur-Eating using Symbiotic Fungus ... so I'm just
      // going to use Sulphur-Eating Bacteria to get 18 money."
      cardAction1(SymbioticFungus) { doTask("Microbe<$SulphurEatingBacteria>") }
      cardAction2(SulphurEatingBacteria, x = 6)
    }
    ellie.turn {
      // "For the first time I will use Power Infrastructure: sell one energy, gain one money."
      cardAction1(PowerInfrastructure, x = 1)
    }
    dad.turn {
      // "I'm going to use Focused Organization and get rid of a money and this card, Mohole Area.
      // I'm going to draw this card ... I think I'll take steel."
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Steel")
      }
      // "My second one is to use the steel to get the five money. That's the bada bing and the
      // bada boom."
      cardAction1(SpaceElevator)
    }
    ellie.turn {
      // "All right, I use Venusian Insects. Add a thing to itself."
      cardAction1(VenusianInsects)
    }
    dad.turn {
      // "I'll spend four on Rad Suits, get a money production. And then I'll spend 20 on ...
      // Magnate."
      playProject(RadSuits, 4)
      fundAward(cn("Magnate"), 20)
    }
    ellie.turn {
      // "I will sell a card."
      sellPatents(1)
    }
    dad.turn {
      // "I will do the thing where I use Floating Refinery to remove two floaters and take a
      // titanium and two money. Back to you."
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }
          .expect("Titanium, 2 MC")
    }
    ellie.turn {
      // "Sell a card."
      sellPatents(1)
    }
    dad.turn {
      // "Well, I'll sell a card then. Back to you."
      sellPatents(1)
    }
    ellie.turn {
      // "Sell a card."
      sellPatents(1)
    }
    dad.turn {
      // "I'll use Predators to predate on your last ecomal. And that gives me two money."
      cardAction1(Predators) { doTask("-Animal<Player2, $EcologicalZone<Player2>>") }.expect("2 MC")
    }
    ellie.turn {
      // "What was it? Sell a card."
      sellPatents(1)
    }
    dad.turn {
      // "Well, then I'll sell a card too. I can beat you at the sell-a-card stalling game."
      sellPatents(1)
    }
    ellie.turn {
      // "Use Viron to add a Venusian insect."
      cardAction1(Viron) { cardAction1(VenusianInsects) }
    }
    dad.turn {
      // "Use Stratobirds to add a Stratobird and get two money."
      cardAction1(StratosphericBirds).expect("2 MC")
    }
    ellie.turn {
      // "I have to sell a card."
      sellPatents(1)
    }
    dad.turn {
      // "Sell a card."
      sellPatents(1)
    }
    ellie.turn {
      // "Standard greenery. Way up at one four for two money and a plant."
      stdProject("GreenerySP") { placeTile(1, 4) }
    }
    dad.turn {
      // "Wait a minute! We're fish. You lose a plant production. I pay seven. But I receive one
      // fish from Viral Enhancers ... I get two money for that one fish."
      playProject(Fish, 7) {
            doTask("PROD[-Plant<Player2>]")
            addCardResources(Fish)
          }
          .expect("-5 MC")
    }
    ellie.turn {
      // "I do perfectly have eight money for Luxury Foods. Nice."
      playProject(LuxuryFoods, 8)
    }
    dad.turn {
      // "I will use Fish to take a fish and two money."
      cardAction1(Fish).expect("2 MC")
    }
    // "I believe I pass."
    ellie.pass()
    dad.turn {
      // "I will use Local Heat Trapping for free to spend five heat and get two fish. Blue fish,
      // red fish, blue fish. Yes. And four money."
      playProject(LocalHeatTrapping, 0) { addCardResources(Fish) }.expect("4 MC")
      // "I'll sell at least three cards ... sell one more."
      sellPatents(4)
    }
    // board-18-47-04.jpg shows three cubes on Fish and the fourth knocked above the card beside the
    // deck, corroborating all four narrated fish.
    dad.turn {
      // "All right, let's have some fun. Let's do Bushes of Love. It'll also help me with the
      // bigger lead on Magnate. Bushes of Love cost me eight, and it gives me a planta."
      playProject(Bushes, 8)
      // "Robot Pollinators cost me seven ... it gives me a plant production, and since I have
      // three plant tags now, it also gives me three plants."
      playProject(RobotPollinators, 7)
      // "And I will plant forest, whatever that means, on this spot for two more plants. I made it
      // to the next one." The final photograph identifies the adjacent space.
      convertPlants { placeTile(7, 7) }
      // Dad says "now I'll sell two more cards" while working out whether another greenery is
      // possible, but does not complete that sale: the app goes directly from 8 M€ to the
      // House Printing payment, and the sourced card ledger leaves only the later two-card sale.
      // "I cannot quite make it. So I'm just going to do stupid House Printing for stupid all of
      // my money, and technically I get a steel production from that."
      playProject(HousePrinting, 8)
      // "And then I'll sell two cards. And that is the end."
      sellPatents(2)
      pass()
    }
    // Both app logs after final production and before final greenery.
    with(dad) {
      assertProduction(m = 8, s = 1, t = 2, p = 5, e = 0, h = 0)
      assertResources(m = 42, s = 1, t = 3, p = 8, e = 0, h = 0)
      assertCounts(32 to "TerraformRating")
    }
    with(ellie) {
      assertProduction(m = 11, s = 2, t = 2, p = 4, e = 0, h = 4)
      assertCounts(61 to "TerraformRating")
    }
    assertSidebar(gen = 10, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    // "Do you get to plant a forest?" "I do not because you took me down to seven."
    ellie.declineTask()
    // "Okay, I get to plant a forest. And there's not that many places I can put it. I think
    // literally this is the only ... oh, I guess that also works. But I get a steel for that."
    // The replacement photograph identifies the legal adjacent space at 5,4.
    dad.convertPlants { placeTile(5, 4) }
    dad.declineTask()
    // board-18-47-04.jpg is the replacement final photograph after this greenery. It also confirms
    // Asteroid Deflection System in Ellie's tableau, directly below Shuttles.

    dad.assertResources(m = 42, s = 2, t = 3, p = 0, e = 0, h = 0)
    ellie.assertResources(m = 72, s = 3, t = 2, p = 7, e = 0, h = 6)

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
    // loose fourth Fish cube in the final photograph makes Dad's rule-correct score 84; the spoken
    // tally evidently missed the displaced cube.
    val score = Summarizer(game)
    assertEquals(5, score.net("Milestone", "VictoryPoint<Dad>"))
    assertEquals(10, score.net("Milestone", "VictoryPoint<Ellie>"))
    assertEquals(5, score.net("FirstPlace", "VictoryPoint<Dad>"))
    assertEquals(10, score.net("FirstPlace", "VictoryPoint<Ellie>"))
    assertEquals(10, score.net("Tile", "VictoryPoint<Dad>"))
    assertEquals(
        24,
        score.net("Tile", "VictoryPoint<Ellie>"),
    )
    assertEquals(32, score.net("Card", "VictoryPoint<Dad>"))
    assertEquals(17, score.net("Card", "VictoryPoint<Ellie>"))
    dad.assertCounts(84 to "VictoryPoint", 0 to "Victory")
    ellie.assertCounts(122 to "VictoryPoint", 1 to "Victory")
  }
}
