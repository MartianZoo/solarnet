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

/** Physical game begun Tuesday 2026-08-25 and adjourned before Generation 8 Research. */
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
        doTask("UseAction<UseCardActionSA, First>", 1)
        doTask("ActionUsedMarker<$FocusedOrganization>")
        cardAction1(FocusedOrganization) {
          doTask("-MC", 2)
          doTask("Titanium", 2)
        }

        // "For the other one. Advertising for 4."
        doTask("UseAction<PlayCardSA, First>")
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
      // The phone entry immediately afterward records a titanium resource instead of the
      // production Space Elevator actually grants.
      exMachina("PROD[-Titanium], Titanium")
    }
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
        doTask("UseAction<$AquiferPumping, First>")
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
        doTask("UseAction<$AquiferPumping, First>")
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
        doTask("UseAction<NeptunianOption, First>")
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
        doTask("UseAction<$AquiferPumping, First>")
        pay(8)
        placeTile(8, 9)
        doTask("UseAction<NeptunianOption, First>")
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
        doTask("UseAction<NeptunianOption, First>")
        pay(5)
      }
      // "Viar on. Oceans are now maxed." The final ocean goes at 1,5, followed by the last
      // Neptunian Power Consultants payment.
      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, First>")
        pay(8)
        placeTile(1, 5)
        doTask("UseAction<NeptunianOption, First>")
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
        doTask("UseAction<$GhgProducingBacteria, Second>")
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

    // Repair my gen-1 mistake: I should have had production, and it should have produced 5 times,
    // but I shouldn't have taken the 1 resource.
    dad.exMachina("PROD[Titanium], 4 Titanium")

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
      cardAction1(Viron) { doTask("UseAction<$GhgProducingBacteria, Second>") }
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
      playProject(cn("UnexpectedApplication"), 4)
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
  }
}
