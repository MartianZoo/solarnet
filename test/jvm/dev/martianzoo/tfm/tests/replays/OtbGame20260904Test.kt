package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Four-player physical game played across 2026-09-04, 2026-09-05, and 2026-09-09. */
internal class OtbGame20260904Test : AbstractFullGameTest() {
  override val config =
      GameConfig(
          """
          AmazonisMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, PromoCardPack
          FakeStuffBundle

          Builder, Diversifier, Generalist, Landshaper, Tactician
          Administrator, Excentric, Highlander, Promoter, Thermalist
          """
      )
  // "We'll give you two and we'll give her four." Blue and Rainbow used those TR handicaps;
  // the joking suggestion of six for Green never reached any player record.
  override val playerClassPets =
      """
      CLASS Yellow : Player
      CLASS Rainbow : Player { SetupPhase: 4 TerraformRating }
      CLASS Blue : Player { SetupPhase: 2 TerraformRating }
      CLASS Green : Player
      """
          .trimIndent()

  @Test
  internal fun otbGame20260904() {
    TfmWorkflow.Automatic(agents).launch()
    retainStartingProjects(4, 6, 5, 4)
    val yellow = p1.requireExplicitUnusedActionCards()
    val rainbow = p2.requireExplicitUnusedActionCards()
    val blue = p3.requireExplicitUnusedActionCards()
    val green =
        game
            .testTfm(game.actors.filterIsInstance<Player>()[3])
            .requireExplicitPaymentChoices()
            .requireExplicitUnusedActionCards()

    yellow.playCorp(Ecoline, 4)
    rainbow.playCorp(MorningStarInc, 6)
    blue.playCorp(FakeHelion, 5)
    green.playCorp(Factorum, 4)

    yellow.turn {
      playPrelude(DomeFarming)
      playPrelude(HugeAsteroid)
    }
    rainbow.turn {
      playPrelude(FakeAppliedScience)
      playPrelude(SpaceLanes)
    }
    blue.turn {
      // "Here for a plant and a card." "Row four, column nine."
      playPrelude(SelfSufficientSettlement) { placeTile(4, 9) }
      playPrelude(TerraformingDeal)
    }
    // The G2 dashboard and later correction show that neither placement bonus was retained yet.
    blue.exMachina("-Plant, -ProjectCard")
    green.turn {
      playPrelude(SuitableInfrastructure)
      // "I raised temp two times. So temp is already at minus twenty."
      playPrelude(AtmosphericEnhancers) { doTask("2 TemperatureStep") }.expect("PROD[Heat], 2 MC")
    }
    // I already forgot to get the SuitableInfrastructure bonus!
    green.exMachina("-2 MC")

    yellow.turn { playProject(SpaceMirrors, 3) }
    rainbow.turn {
      stdAction("DoRequiredActionsAction")
      // Rainbow first paid seven, then took back the evidenced Space Lanes discount.
      rainbow.exMachina(fakeWildTags("VenusTag"))
      playProject(FloatingRefinery, 5)
    }
    blue.turn { playProject(HomeostasisBureau, 16) }
    green.turn { playProject(TitaniumMine, 1, steel = 3) }
    yellow.turn {
      cardAction1(SpaceMirrors)
      playProject(BuildingIndustries, 6)
    }
    rainbow.turn { playProject(MediaGroup, 4) }
    blue.turn { playProject(SolarWindPower, 11) }
    green.turn {
      playProject(OlympusConference, 6, steel = 2)
      playProject(RoboticWorkforce, 9) {
        doTask("CopyProductionBox<$TitaniumMine>")
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    yellow.pass()
    rainbow.turn {
      // "Lava Flows." "Hecate's Tholus ... five one."
      playProject(LavaFlows, 18) { placeTile(5, 1) }
    }
    blue.pass()
    green.turn { cardAction1(Factorum) }
    rainbow.turn { cardAction2(FloatingRefinery) }
    green.pass()
    rainbow.pass(unused = FakeAppliedScience) // didn't know what resource she wanted

    // "Eight four for two cards and nobody gets the cards, of course."
    yellow.wgt("OceanTile<Amazonis_08_04>")

    with(yellow) {
      assertProduction(m = 2, s = 2, t = 0, p = 3, e = 0, h = 1)
      assertResources(m = 28, s = 2, t = 0, p = 6, e = 0, h = 1)
      assertCounts(23 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 36, s = 2, t = 4, p = 0, e = 0, h = 0)
      assertCounts(26 to "TerraformRating")
      assertCardResources(1 to FloatingRefinery, 6 to FakeAppliedScience)
    }
    with(blue) {
      assertProduction(m = 2, s = 0, t = 0, p = 0, e = 1, h = 5)
      assertResources(m = 24, s = 0, t = 2, p = 0, e = 1, h = 5)
      assertCounts(22 to "TerraformRating")
    }
    with(green) {
      assertProduction(m = 0, s = 1, t = 2, p = 0, e = 1, h = 1)
      assertResources(m = 37, s = 1, t = 2, p = 0, e = 1, h = 1)
      assertCounts(22 to "TerraformRating")
    }
    assertSidebar(gen = 2, temp = -16, oxygen = 0, oceans = 1, venus = 0)

    // Blue noticed the two settlement bonuses while buying cards and restored them together.
    blue.exMachina("Plant, ProjectCard")

    rainbow.buyCards(3)
    blue.buyCards(2)
    green.buyCards(3)
    yellow.buyCards(2)

    rainbow.turn { stdProject("AirScrappingProject") }
    blue.turn { playProject(NuclearZone, 10) { placeTile(3, 3) } }
    green.turn {
      cardAction2(Factorum)
      playProject(Pets, 10)
    }
    yellow.turn { cardAction1(SpaceMirrors) }
    rainbow.turn {
      playProject(SulphurEatingBacteria, 4)
      cardAction1(SulphurEatingBacteria)
    }
    blue.turn { playProject(Sponsors, 6) }
    green.pass()
    yellow.pass()
    rainbow.turn { cardAction1(FloatingRefinery) }
    blue.turn { playProject(NeptunianPowerConsultants, 12, heat = 2) }
    rainbow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
    }
    blue.pass()
    rainbow.turn { playProject(SpaceStation, 1, titanium = 3) }

    rainbow.pass()
    rainbow.wgt("VenusStep")

    with(yellow) {
      assertProduction(m = 2, s = 2, t = 0, p = 3, e = 1, h = 1)
      assertResources(m = 40, s = 4, t = 0, p = 9, e = 1, h = 2)
      assertCounts(23 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 34, s = 2, t = 1, p = 0, e = 0, h = 0)
      assertCounts(27 to "TerraformRating")
      assertCardResources(
          2 to FloatingRefinery,
          2 to SulphurEatingBacteria,
          5 to FakeAppliedScience,
      )
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 1, e = 1, h = 5)
      assertResources(m = 28, s = 0, t = 2, p = 2, e = 1, h = 9)
      assertCounts(24 to "TerraformRating")
    }
    with(green) {
      assertProduction(m = 0, s = 1, t = 2, p = 0, e = 1, h = 1)
      assertResources(m = 37, s = 2, t = 4, p = 0, e = 1, h = 3)
      assertCounts(22 to "TerraformRating")
      assertCardResources(1 to Pets)
    }
    assertSidebar(gen = 3, temp = -12, oxygen = 0, oceans = 1, venus = 4)

    blue.buyCards(1)
    green.buyCards(3)
    yellow.buyCards(3)
    rainbow.buyCards(4)

    blue.turn { playProject(LunarBeam, 13) }
    green.turn { playProject(CloudTourism, 11) }
    yellow.turn { cardAction1(SpaceMirrors) }
    rainbow.turn { playProject(IshtarMining, 3) }
    blue.turn { playProject(AdvancedAlloys, 9) }
    green.turn { cardAction2(Factorum) }
    yellow.turn {
      playProject(Flooding, 7) {
        // Blue does not have to spend a heat (couldn't if she wanted to)
        doTask("OceanTile<Amazonis_05_10>! THEN -3 MC<Blue>")
        blue.declineTask()
      }
      playProject(UndergroundCity, 10, steel = 4) { placeTile(10, 10) }
    }
    rainbow.turn {
      rainbow.exMachina(fakeWildTags("VenusTag"))
      playProject(SulphurExports, 14, titanium = 1)
    }
    blue.turn { convertHeat() }
    green.turn {
      playProject(VenusShuttles, 9) { addCardResources(CloudTourism, 2) }
    }
    yellow.turn {
      convertPlants { placeTile(10, 9) }
      convertPlants { placeTile(10, 11) }
    }
    rainbow.turn { cardAction2(FloatingRefinery) }
    blue.pass()
    green.turn {
      sellPatents(2)
      cardAction1(VenusShuttles)
    }
    yellow.turn {
      playProject(NaturalPreserve, 5, steel = 2) { placeTile(1, 4) }
    }
    rainbow.turn { playProject(RoverConstruction, 4, steel = 2) }
    green.turn { cardAction1(CloudTourism) }
    yellow.pass()
    rainbow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
    }
    green.pass()
    rainbow.turn { cardAction1(SulphurEatingBacteria) }
    rainbow.pass()

    blue.wgt("OceanTile<Amazonis_02_06>")
    blue.doTask("UseAction<NeptunianOption, Action1>")
    blue.pay(5)

    with(yellow) {
      assertProduction(m = 3, s = 4, t = 0, p = 3, e = 0, h = 1)
      assertResources(m = 31, s = 4, t = 0, p = 5, e = 0, h = 4)
      assertCounts(26 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 6, s = 0, t = 1, p = 0, e = 0, h = 0)
      assertResources(m = 37, s = 0, t = 2, p = 0, e = 0, h = 0)
      assertCounts(28 to "TerraformRating")
      assertCardResources(4 to SulphurEatingBacteria, 4 to FakeAppliedScience)
    }
    with(blue) {
      assertProduction(m = 2, s = 0, t = 0, p = 1, e = 4, h = 7)
      assertResources(m = 27, s = 0, t = 2, p = 3, e = 3, h = 9)
      assertCounts(25 to "TerraformRating")
      assertCardResources(1 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 2, s = 1, t = 2, p = 0, e = 1, h = 1)
      assertResources(m = 25, s = 3, t = 6, p = 0, e = 1, h = 5)
      assertCounts(23 to "TerraformRating")
      assertCardResources(2 to Pets, 3 to CloudTourism)
    }
    assertSidebar(gen = 4, temp = -10, oxygen = 2, oceans = 3, venus = 8)

    green.buyCards(4)
    yellow.buyCards(3)
    rainbow.buyCards(3)
    blue.buyCards(3)

    green.turn {
      playProject(EarthOffice, 1)
      playProject(TopsoilContract, 5)
    }
    yellow.turn { playProject(Sabotage, 1) { doTask("-3 Steel<Green>") } }
    rainbow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
      cardAction2(SulphurEatingBacteria, x = 5)
    }
    blue.turn {
      playProject(ProtectedValley, 18, heat = 5) { placeTile(2, 7) }
      doTask("UseAction<ClaimMilestoneAction, Action1>")
      pay(4, heat = 4)
      doTask("Landshaper")
    }
    green.turn {
      sellPatents(1)
      claimMilestone(cn("Diversifier"))
    }
    yellow.turn { cardAction1(SpaceMirrors) }
    rainbow.turn { playProject(WavePower, 8) }
    blue.pass()
    green.turn { cardAction1(CloudTourism) }
    yellow.turn { playProject(AdaptedLichen, 9) }
    rainbow.turn { playProject(IoSulphurResearch, 15) { doTask("3 ProjectCard") } }
    green.pass(unused = setOf(Factorum, VenusShuttles))
    yellow.pass()
    rainbow.turn {
      rainbow.exMachina(fakeWildTags("ScienceTag"))
      playProject(BactoviralResearch, 10) {
        addCardResources(SulphurEatingBacteria)
      }
      rainbow.exMachina(fakeWildTags("ScienceTag"))
      playProject(AtalantaPlanitiaLab, 8)
    }
    rainbow.pass(unused = FloatingRefinery)

    green.wgt("OxygenStep")

    with(yellow) {
      assertProduction(m = 3, s = 4, t = 0, p = 4, e = 1, h = 1)
      assertResources(m = 34, s = 8, t = 0, p = 9, e = 1, h = 5)
      assertCounts(26 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 6, s = 0, t = 1, p = 0, e = 1, h = 0)
      assertResources(m = 36, s = 0, t = 3, p = 0, e = 1, h = 0)
      assertCounts(28 to "TerraformRating")
      assertCardResources(3 to SulphurEatingBacteria, 3 to FakeAppliedScience)
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 1, e = 4, h = 7)
      assertResources(m = 30, s = 0, t = 4, p = 4, e = 4, h = 10)
      assertCounts(26 to "TerraformRating", 1 to "Landshaper")
      assertCardResources(1 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 2, s = 1, t = 2, p = 0, e = 1, h = 1)
      assertResources(m = 25, s = 1, t = 8, p = 3, e = 1, h = 7)
      assertCounts(23 to "TerraformRating", 1 to "Diversifier")
      assertCardResources(2 to Pets, 4 to CloudTourism)
    }
    assertSidebar(gen = 5, temp = -10, oxygen = 4, oceans = 3, venus = 8)

    yellow.buyCards(2)
    rainbow.buyCards(3)
    blue.buyCards(2)
    green.buyCards(2)

    yellow.turn {
      playProject(CuttingEdgeTechnology, 12)
      playProject(MartianSurvey, 7)
    }
    rainbow.turn { claimMilestone(cn("Tactician")) }
    blue.turn {
      playProject(BigAsteroid, 11, titanium = 4) { doTask("-4 Plant<Yellow>") }
    }
    green.turn {
      // "Cost me full price": Green preserved titanium despite the Space tag.
      intentionalUnderpay()
      playProject(TechnologyDemonstration, 5)
      playProject(DiversitySupport, 1)
    }
    // Diversity Support's TR was overlooked at the table; Green's app remains at 23 until Rad-Chem.
    green.exMachina("-TerraformRating")
    yellow.turn { playProject(LagrangeObservatory, 9) }
    rainbow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
      cardAction2(SulphurEatingBacteria, x = 4)
    }
    blue.turn { convertHeat() }
    green.turn { playProject(IcyImpactors, titanium = 5) }
    yellow.pass(unused = SpaceMirrors)
    rainbow.turn {
      // It appears that when she meant to pay 25 she typed just "2"
      exMachina("23 MC")
      // "My Giant Ice Asteroid should be thirty four ... three oceans to place."
      playProject(GiantIceAsteroid, 25, titanium = 3) {
            placeTile(7, 4)
            autoExecNow()
            selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
            blue.doTask("UseAction<NeptunianOption, Action1>")
            blue.pay(5)
            placeTile(9, 4)
            autoExecNow()
            selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
            blue.doTask("UseAction<NeptunianOption, Action1>")
            blue.pay(5)
            // Crossing 0°C supplies Amazonis's temperature-track ocean bonus.
            placeTile(6, 11)
            autoExecNow()
            selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
            blue.doTask("UseAction<NeptunianOption, Action1>")
            blue.pay(5)
            // "Up to six plants from Ellie" cleaned out Yellow's actual five.
            doTask("-5 Plant<Yellow>!")
          }
          .expect("-16 MC, -2 Titanium, Steel, 2 Plant, PROD[3 Energy<Blue>]")
    }
    // She forgot the Media Group rebate
    rainbow.exMachina("-3 MC")
    // Then she realized she must not have paid, and paid the full 25 (not 23)
    rainbow.exMachina("-25 MC")

    blue.turn {
      playProject(GiantSpaceMirror, 1, titanium = 4)
    }
    green.turn { playProject(RadChemFactory, 6, steel = 1) }
    rainbow.turn { cardAction1(FloatingRefinery) }
    // board-16-07-06.jpg visibly has two cubes: Rainbow added two instead of the card's one.
    rainbow.exMachina("Floater<$FloatingRefinery>")
    blue.pass()
    // "Three titanium and one real money."
    green.turn { cardAction1(IcyImpactors) { pay(1, titanium = 3) } }
    rainbow.turn { playProject(Moss, 4) }
    green.turn { playProject(VenusGovernor, 4) }
    rainbow.turn { sellPatents(2) }
    green.turn {
      cardAction2(Factorum)
      cardAction1(CloudTourism)
    }
    rainbow.turn {
      playProject(Thermophiles, 7)
      cardAction1(Thermophiles) { addCardResources(SulphurEatingBacteria) }
    }
    green.pass(unused = VenusShuttles)
    rainbow.pass()

    yellow.wgt("TemperatureStep")

    // The spoken hand census is authoritative where anonymous card flow left different totals.
    yellow.exMachina("-2 ProjectCard")
    rainbow.exMachina("-ProjectCard")
    blue.exMachina("2 ProjectCard")

    // board-16-07-06.jpg and all four app histories: Generation 6 before Research.
    with(yellow) {
      assertProduction(m = 3, s = 4, t = 0, p = 4, e = 1, h = 1)
      assertResources(m = 29, s = 12, t = 0, p = 4, e = 1, h = 7)
      assertCounts(26 to "TerraformRating", 6 to "CardBack<Hand>")
    }
    with(rainbow) {
      assertProduction(m = 6, s = 0, t = 1, p = 1, e = 1, h = 0)
      assertResources(m = 40, s = 1, t = 2, p = 2, e = 1, h = 1)
      assertCounts(33 to "TerraformRating", 1 to "Tactician", 11 to "CardBack<Hand>")
      assertCardResources(
          2 to FloatingRefinery,
          1 to SulphurEatingBacteria,
          2 to FakeAppliedScience,
      )
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 1, e = 10, h = 7)
      assertResources(m = 45, s = 0, t = 0, p = 5, e = 10, h = 13)
      assertCounts(29 to "TerraformRating", 1 to "Landshaper", 8 to "CardBack<Hand>")
      assertCardResources(4 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 4, s = 1, t = 2, p = 0, e = 0, h = 1)
      assertResources(m = 30, s = 1, t = 2, p = 3, e = 0, h = 9)
      assertCounts(25 to "TerraformRating", 1 to "Diversifier", 9 to "CardBack<Hand>")
      assertCardResources(2 to Pets, 5 to CloudTourism, 2 to IcyImpactors, 1 to OlympusConference)
    }
    assertSidebar(gen = 6, temp = 2, oxygen = 4, oceans = 6, venus = 8)

    // Rainbow's app entries 103 and 113 both show 40 M€ even though entry 113 says -5; entry 114
    // then adds 5 M€ and shows 45. The original 2026-09-05 transcript confirms that 45-M€
    // balance before Research but never explains the correction. The only source-compatible
    // window is between entries 113 and 114; the app correction itself is the likeliest cause.
    rainbow.exMachina("5 MC")
    // Green's app entry 104 explicitly raises 30 M€ to 33 M€ between the Generation 6
    // checkpoint and Research, and the original transcript confirms 33. No source says what the
    // correction was for, so both ends of its possible window are entry 104 itself.
    green.exMachina("3 MC")
    // Entry 105 restores the TR Green had omitted from Diversity Support in Generation 5.
    green.exMachina("TerraformRating")

    rainbow.buyCards(1)
    blue.buyCards(2)
    green.buyCards(3)
    yellow.buyCards(3)
    rainbow.assertCounts(42 to "MC")

    rainbow.turn {
      // "I'm gonna play Mining Rights. I'm gonna spend my steel as two money. ... 1-2 for two
      // steels."
      playProject(MiningRights, 7, steel = 1) { placeTile(1, 2) }
      assertCounts(35 to "MC")
    }
    blue.turn {
      // "Convoy from Europa. ... 3-6 for two money."
      playProject(ConvoyFromEuropa, 15) {
        placeTile(3, 6)
        doTask("UseAction<NeptunianOption, Action1>")
        pay(5)
      }
    }
    green.turn {
      playProject(Decomposers, 5)
      fundAward(cn("Excentric"), 8)
    }
    yellow.turn {
      // "Spend seven [on] Astra Mechanica. Choose two project cards from my event pile. Take them
      // into my hand."
      playProject(AstraMechanica, 7) {
        doWithoutAutoExec(yellow) {
          doTask("ProjectCard FROM PlayedEvent<Class<$Flooding>>")
          doTask("ProjectCard FROM PlayedEvent<Class<$Sabotage>>")
        }
      }
    }
    rainbow.turn {
      playProject(Ants, 9)
      cardAction1(Ants) {
        doTask("-Microbe<Green, $Decomposers<Green>>")
      }
      assertCounts(26 to "MC")
    }
    blue.turn { convertHeat() }
    green.turn { cardAction1(Factorum) }
    yellow.turn { cardAction1(SpaceMirrors) }
    rainbow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
    }
    blue.turn {
      playProject(IndenturedWorkers, 0)
      playProject(NitrogenRichAsteroid, 23)
    }
    green.turn { playProject(PowerPlant, 2, steel = 1) }
    yellow.turn {
      // "I pay all twelve of my steel, because I got my Cutting Edge."
      playProject(Capital, steel = 12) { placeTile(7, 3) }
    }
    rainbow.turn {
      playProject(Fish, 9) { doTask("PROD[-Plant<Yellow>]") }
      cardAction1(Fish)
      assertCounts(19 to "MC")
    }
    blue.turn {
      playProject(WaterSplittingPlant, 12)
      // This was physically remembered after Blue passed; keeping it here is the smallest legal
      // chronology distortion and preserves every intervening result.
      cardAction1(WaterSplittingPlant)
    }
    green.turn { playProject(BusinessNetwork, 1) }
    yellow.turn {
      // "I actually can't do that." No opposing tile bordered the selected ocean.
      playProject(Flooding, 7) {
        placeTile(6, 6)
        blue.doTask("UseAction<NeptunianOption, Action1>")
        // "I will spend two money and three heat."
        blue.intentionalUnderpay()
        blue.pay(2, heat = 3)
      }
    }
    rainbow.turn {
      cardAction1(Thermophiles) { addCardResources(SulphurEatingBacteria) }
      rainbow.exMachina(fakeWildTags("VenusTag"))
      stdAction("UseActionOnCardAction") {
        doTask("ActionUsedMarker<$FloatingRefinery>")
        doTask("UseAction<$FloatingRefinery, Action2>")
      }
      assertCounts(21 to "MC")
    }
    blue.pass()
    green.turn { sellPatents(1) }
    yellow.turn { playProject(Sabotage, 1) { doTask("-7 MC<Green>") } }
    rainbow.turn {
      rainbow.exMachina(fakeWildTags("PlantTag"))
      playProject(CloudSeeding, 11) {
        doTask("PROD[-Heat<Blue>]")
      }
      assertCounts(10 to "MC")
    }
    green.turn { sellPatents(1) }
    yellow.pass()
    rainbow.turn {
      rainbow.exMachina(fakeWildTags("MicrobeTag"))
      cardAction2(SulphurEatingBacteria, x = 3)
      assertCounts(19 to "MC")
    }
    green.turn { playProject(NitrophilicMoss, 8) }
    // I accidentally gave myself 2 production instead of 2 money resources
    // But I knew what the ending money balance was supposed to be, so I got confused and ended
    // up giving the 2 money anyway, but never realized what I had done.
    green.exMachina("PROD[2 MC]")

    rainbow.turn {
      playProject(LavaTubeSettlement, 11, steel = 2) { placeTile(7, 11) }
    }
    green.turn { cardAction1(BusinessNetwork) { buyCards(0) } }
    rainbow.turn { sellPatents(2) }
    green.turn { cardAction1(CloudTourism) }
    rainbow.pass()
    green.turn {
      cardAction2(IcyImpactors) {
        rainbow.doTask("OceanTile<Amazonis_02_01> BY Green")
        green.doTask("TerraformRating")
        selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
        blue.narrowTask("Ok")
      }
    }
    green.pass(unused = VenusShuttles)

    rainbow.wgt("VenusStep")

    // board-15-31-15.jpg and all four app histories: Generation 7 before Research.
    with(yellow) {
      assertProduction(m = 8, s = 4, t = 0, p = 3, e = 0, h = 1)
      assertResources(m = 37, s = 4, t = 0, p = 11, e = 0, h = 9)
      assertCounts(27 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 7, s = 1, t = 1, p = 3, e = 0, h = 0)
      assertResources(m = 54, s = 1, t = 4, p = 5, e = 0, h = 2)
      assertCounts(33 to "TerraformRating", 1 to "Tactician")
      assertCardResources(
          1 to Ants,
          1 to Fish,
          1 to FakeAppliedScience,
      )
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 2, e = 12, h = 6)
      assertResources(m = 41, s = 0, t = 0, p = 7, e = 12, h = 15)
      assertCounts(35 to "TerraformRating", 1 to "Landshaper")
      assertCardResources(6 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 5, s = 1, t = 2, p = 2, e = 2, h = 1)
      assertResources(m = 35, s = 1, t = 4, p = 3, e = 2, h = 10)
      assertCounts(27 to "TerraformRating", 1 to "Diversifier")
      assertCardResources(
          4 to Pets,
          6 to CloudTourism,
          1 to IcyImpactors,
          1 to OlympusConference,
      )
    }
    assertSidebar(gen = 7, temp = 6, oxygen = 5, oceans = 9, venus = 10)

    blue.buyCards(2)
    green.buyCards(3)
    yellow.buyCards(1)
    rainbow.buyCards(2)

    blue.turn { convertHeat() }
    green.turn { cardAction2(Factorum) }
    yellow.turn {
      convertHeat()
      playProject(Comet, 21) {
        placeTile(6, 5)
        doTask("-3 Plant<Blue>")
        blue.doTask("UseAction<NeptunianOption, Action1>")
        blue.pay(5)
      }
    }
    rainbow.turn {
      playProject(MoholeLake, 29, steel = 1) {
        placeTile(5, 5)
        autoExecNow()
        blue.doTask("UseAction<NeptunianOption, Action1>")
        blue.pay(5)
      }
      convertPlants { placeTile(6, 10) }
    }
    blue.turn {
      playProject(DomedCrater, 24) { placeTile(3, 7) }
      convertPlants { placeTile(3, 8) }
    }
    // Blue's app has no 2 M€ Terraforming Deal response to this greenery's oxygen step:
    // entry 178 leaves 10 M€, and the 15:57 dashboard plus entry 188 production both leave
    // 54 M€. Preserve the omitted response at the action that caused it.
    blue.exMachina("-2 MC")
    green.turn {
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    yellow.turn {
      convertPlants { placeTile(8, 3) }
      convertPlants { placeTile(11, 11) }
    }
    rainbow.turn {
      cardAction1(FloatingRefinery)
      rainbow.exMachina(fakeWildTags("VenusTag"))
      playProject(StratosphericBirds, 10)
    }
    blue.pass(unused = WaterSplittingPlant)
    green.turn { cardAction1(CloudTourism) }
    yellow.turn {
      playProject(EcologicalZone, 10) { placeTile(4, 8) }
    }
    // Applied Science only accepts a card that already has a resource; the table put its science
    // onto the now-empty Sulphur-Eating Bacteria anyway. Seed and remove a witness resource so the
    // sourced illegal target can pass through the card's real action.
    rainbow.exMachina("Microbe<$SulphurEatingBacteria>")
    rainbow.turn {
      cardAction1(FakeAppliedScience) {
        addCardResources(SulphurEatingBacteria)
      }
      exMachina("-Microbe<$SulphurEatingBacteria>")
      cardAction1(Thermophiles) {
        addCardResources(SulphurEatingBacteria)
      }
    }
    green.turn { playProject(VenusMagnetizer, 7) }
    yellow.turn { playProject(Insects, 7) }
    rainbow.turn {
      cardAction1(Ants) { doTask("-Microbe<Green, $Decomposers<Green>>") }
    }
    green.turn {
      intentionalUnderpay()
      playProject(CupolaCity, 16) { placeTile(6, 4) }
      cardAction1(VenusMagnetizer)
    }
    // Yellow's app had 2 M€ when she announced a 4 M€ cash payment for Small Animals.
    yellow.exMachina("2 MC")
    yellow.turn {
      playProject(SmallAnimals, 4) { doTask("PROD[-Plant<Rainbow>]") }
    }
    rainbow.turn {
      intentionalUnderpay()
      playProject(TollStation, 10)
    }
    green.pass(unused = setOf(VenusShuttles, IcyImpactors))
    yellow.turn {
      playProject(BiomassCombustors, steel = 1) { doTask("PROD[-Plant<Blue>]") }
    }
    rainbow.turn { cardAction1(StratosphericBirds) }
    yellow.pass(unused = setOf(SmallAnimals, SpaceMirrors))
    rainbow.turn {
      cardAction1(Fish)
      cardAction1(MoholeLake) { addCardResources(StratosphericBirds) }
    }
    rainbow.turn {
      intentionalUnderpay()
      rainbow.exMachina(fakeWildTags("EarthTag"))
      playProject(MirandaResort, 8)
    }
    rainbow.pass(unused = SulphurEatingBacteria)

    blue.wgt("VenusStep")

    // board-15-57-46.jpg and all four app histories: Generation 8 before Research.
    with(yellow) {
      assertProduction(m = 8, s = 4, t = 0, p = 7, e = 2, h = 1)
      assertResources(m = 40, s = 7, t = 0, p = 8, e = 2, h = 2)
      assertCounts(32 to "TerraformRating")
      assertCardResources(3 to EcologicalZone)
    }
    with(rainbow) {
      assertProduction(m = 15, s = 1, t = 1, p = 2, e = 0, h = 0)
      assertResources(m = 54, s = 1, t = 5, p = 3, e = 0, h = 2)
      assertCounts(36 to "TerraformRating", 1 to "Tactician")
      assertCardResources(
          2 to Ants,
          2 to Fish,
          2 to StratosphericBirds,
          2 to SulphurEatingBacteria,
      )
    }
    with(blue) {
      assertProduction(m = 7, s = 0, t = 0, p = 1, e = 13, h = 6)
      assertResources(m = 54, s = 0, t = 0, p = 3, e = 13, h = 25)
      assertCounts(37 to "TerraformRating", 1 to "Landshaper")
      assertCardResources(8 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 8, s = 1, t = 2, p = 2, e = 0, h = 1)
      assertResources(m = 42, s = 2, t = 6, p = 7, e = 0, h = 13)
      assertCounts(28 to "TerraformRating", 1 to "Diversifier")
      assertCardResources(6 to Pets, 7 to CloudTourism, 1 to IcyImpactors)
    }
    assertSidebar(gen = 8, temp = 14, oxygen = 9, oceans = 11, venus = 14)

    // The table broke here on 2026-09-05 and resumed on 2026-09-09 at 5:25 PM, opening with two
    // corrections Green had worked out in between.
    // "I have taken away two of my money [production] and four of my money. Hopefully that was the
    // right thing to do."
    green.exMachina("PROD[-2 MC], -4 MC")
    // "I certainly used the Applied Science thing for three money. Should I take away three
    // money?" "Let's just let it stand." Rainbow's app keeps the extra 3 M€.

    green.buyCards(3)
    yellow.buyCards(2)
    rainbow.buyCards(2)
    blue.buyCards(1)

    green.turn {
      // "I do not have discounts, so it cost me four entire money. Decomposers gets a microbe
      // from mom and that gives me a money." "Tardigrades? That didn't do what I thought it was
      // going to do. I keep thinking that I had viral enhancers."
      playProject(Tardigrades, 4)
      // "Now I'm going to use business network to look at this card. God, it's so expensive.
      // I think not."
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    yellow.turn {
      // "I'm gonna pay 14 to fund the Highlander. That's the most tiles not next to oceans, which
      // I got." "Next, dry tiles." "Yes, dry boys."
      fundAward(cn("Highlander"), 14)
      // "Just in case anyone has any ideas, I'm gonna plant forests." "I'll go here for a plant."
      convertPlants { placeTile(7, 2) }
    }
    rainbow.turn {
      // "I'm going to add one microbe to self-reeding bacteria [Sulphur-Eating Bacteria]."
      cardAction1(SulphurEatingBacteria)
      // "Also I'm going to take my thermophiles action to add one microbe to sulfur eating
      // bacteria."
      cardAction1(Thermophiles) { addCardResources(SulphurEatingBacteria) }
    }
    blue.turn {
      // "I'm going to spend fifteen bucks on Plantation." "If I go here, I get a resource,
      // right?" "A standard resource." "I decided that my resource was gonna be a titanium."
      playProject(Plantation, 15) {
        placeTile(5, 9)
        doTask("Titanium")
      }
      // Blue's app debits 35 M€ for the 15 M€ card, preserving a 20 M€ physical overpayment.
      exMachina("-20 MC")
    }

    green.turn {
      // "I will take my factotum action. I have no energy resources so I can get an energy
      // production and that gives me two money from sweetable infrastructure."
      cardAction1(Factorum)
    }
    yellow.turn {
      // "I really hate that this is gonna cost me so much money, but I'm gonna spend twenty bucks
      // to fund Thermalist." "Wait, did you pay the-- Alright, wow." "She did." Her app instead
      // moves from 20 M€ to 4 M€ at entry 209, so supply the missing four before charging the
      // announced award payment through the real action.
      exMachina("4 MC")
      fundAward(cn("Thermalist"), 20)
      // "I will sponsor some Academy. Hope it doesn't give you useful stuff. Pay nine. I pitch a
      // card." "You get three, you get one, you get one, I get one."
      // Yellow's app never records the announced nine. Restore what the engine has to charge
      // while retaining the 4 M€ evidenced after Thermalist.
      exMachina("9 MC")
      playProject(SponsoredAcademies, 9)
    }
    rainbow.turn {
      // "So I'm gonna do gene repair which costs me 12 monies, but it gives me two money
      // production."
      playProject(GeneRepair, 12)
      // Applied Science gains a science resource from the science tag. FakeAppliedScience does not
      // model that trigger, so preserve the sourced resource at its cause.
      rainbow.exMachina("Science<$FakeAppliedScience>")
      // "Oh, but [Yellow] doesn't have microbes, right. Well, I'm gonna take your tasty microbe
      // then, please." "My ants are going to remove a microbe from mirror or whatever."
      cardAction1(Ants) { doTask("-Microbe<Green, $Decomposers<Green>>") }
    }
    blue.turn {
      // "I am playing Soletta, so that's gonna cost me one titanium, twenty mega credits and
      // eleven heat. But I get seven heat production."
      playProject(Soletta, 20, titanium = 1, heat = 11)
    }

    green.turn {
      // "Venus magnetizer. Reduce energy production. Raise Venus to sixteen and get nothing."
      cardAction1(VenusMagnetizer)
    }
    yellow.turn {
      // "Dos patentes... and play for six, um, Lightning Harvest. I gain energy product, gain
      // money product."
      sellPatents(2)
      playProject(LightningHarvest, 6)
    }
    rainbow.turn {
      // Rainbow paid 7 for Venus Waystation and 14 for Maxwell Base, then found she had no energy
      // production to lower, and took both back. "Okay, unpaid for that. Take fourteen money
      // back." "Alright fine, give yourself what, seven money back?"
      // "So first I'm going to take the, it's 11 standard action." "If you want a standard
      // project, tap your money, your 36 money. You don't tap the production."
      stdProject("PowerPlantProject")
      // "I've got to pay sixteen for this." "Reduce your energy production and place a shitty
      // tile." "Do I still get a whatever I get for cities for that?" "Well you get your two
      // money for rover construction." "And you get your pets -- someone gets a pet." "I get a
      // pet!" "I gave you a fucking point."
      playProject(MaxwellBase, 16)
    }
    blue.turn {
      // "You don't want to raise the oxygen?" "But then the game would end." "Not for another
      // seven things, but you get a T.R."
      // "I'll take my water splitting plant action, spend three energy to raise oxygen one step.
      // And I gain a TR and I gain two money."
      cardAction1(WaterSplittingPlant)
    }

    green.turn {
      // "I play [Fusion] Power for four worth of steel and ten real, and one two three energy
      // production which gives me two money, and a science tag removes my only science resource
      // from Olympus conference and gives me this guy. That's actually useful in this
      // circumstance, I don't believe it."
      playProject(FusionPower, 10, steel = 2) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    yellow.pass(unused = setOf(SmallAnimals, SpaceMirrors))
    rainbow.turn {
      // "Venus way station should be nine, but I have a minus two for and a minus two for a
      // thing, so it's five." "Why did we think you paid seven for it last time?" "We did."
      // "You didn't use any titanium, right?" "No. Because I'm saving all my tattoos [titanium]."
      intentionalUnderpay()
      playProject(VenusWaystation, 5)
      // "I'm gonna build vocal shading [Local Shading] for free."
      playProject(LocalShading, 0)
    }
    blue.turn {
      // "I don't pass. I play project inspection. Use a card action that has already been used
      // this generation. I use my water splitting plant action again. And then I spend three more
      // energy, raise the oxygen one more step, and I gain one more T_R_ and two more money."
      playProject(ProjectInspection, 0) {
        doTask("UseAction<$WaterSplittingPlant, Action1>")
        pay(energy = 3)
      }
    }

    green.turn {
      // "So what I'm gonna do is play directed usage, which costs me one."
      playProject(DirectedHeatUsage, 1)
    }
    rainbow.turn {
      // "Floating refinery action, I'm putting a floater on the card." "Local shading action, I am
      // putting a floater on that card."
      cardAction1(FloatingRefinery)
      cardAction1(LocalShading)
    }
    blue.pass()

    green.turn {
      // "Use directed heat usage to spend three useless heat and get two useful plants, and then
      // plant forest which raises oxygen to 14."
      cardAction1(DirectedHeatUsage) { doTask("2 Plant") }
      convertPlants { placeTile(5, 4) }
    }
    rainbow.turn {
      // "I'm going to take my Maxwell base action, add one resource to another Venus card. And I
      // guess I'm going to do that to the floating refinery."
      cardAction1(MaxwellBase) { addCardResources(FloatingRefinery) }
      // "And then add a bird to Stratospheric Bird."
      cardAction1(StratosphericBirds)
    }

    green.turn {
      // "I will add a floater to Cloud Tourism."
      cardAction1(CloudTourism)
    }
    rainbow.turn {
      // "We'll add a fish to fish and I will take the action from the holy lake [Mohole Lake] to
      // add another fish again."
      cardAction1(Fish)
      cardAction1(MoholeLake) { addCardResources(Fish) }
    }

    green.turn {
      // "I will use tardigrades to put a tardigrade on to a tardigrades and I get the money."
      cardAction1(Tardigrades)
    }
    rainbow.turn {
      // "I'm going to pay my last six money for greenhouses. Oh wait, I have no--" "You have a
      // steel if you want to use it." "So giving myself two money and taking away one steel. One
      // plant for each city tile in play. So I believe that is one, two, three, four, five, six,
      // seven." "That is a lot of plants."
      playProject(Greenhouses, 4, steel = 1)
      // "And so I'm going to do a plant forest action." "Oxygen to 15." "Where's my forest going
      // to go? I guess right here between my city and my other forest."
      convertPlants { placeTile(7, 10) }
    }

    green.turn {
      // "How many Venus tags do I have? One, two, three, four, five, six. Venus shuttles says I
      // pay only six to raise Venus." "To 18%."
      cardAction1(VenusShuttles)
    }
    // Gene Repair's science tag reloaded Applied Science; Rainbow saves it for Generation 9.
    rainbow.pass(unused = FakeAppliedScience)

    green.turn {
      // "Let's play water to Venus for three titanium, no discounts. Raise Venus to 20%, get a
      // TR."
      playProject(WaterToVenus, titanium = 3)
      // "I'm going to play air scrapping exhibition for thirteen money. No discounts. I get three
      // cloud tours." "I raise Venus to twenty two which gives me two TR."
      playProject(AirScrappingExpedition, 13) { addCardResources(CloudTourism, 3) }
    }
    green.turn {
      // "I play media archives. That cost me five money. How many event cards has everybody
      // played?" "And five is seven and five is twelve and four is sixteen."
      // "You say wasted my media archives too soon. Didn't really need that money. What a sad
      // waste."
      // The table's spoken tally was one high: it counted 2, 5, 5 and 4, while the played events
      // at this moment are Yellow 4, Rainbow 2, Blue 5 and Green 4. Media Archives paid Green for
      // sixteen events rather than fifteen.
      playProject(MediaArchives, 5)
      exMachina("MC")
    }
    green.pass(unused = IcyImpactors)

    // "I'm sad we hit generation nine. I choose what to world government... I was gonna move the
    // oxygen -- oxygen at 16."
    green.wgt("OxygenStep")

    // board-0909-18-03-31.jpg and all four app histories: Generation 9 before Research.
    with(yellow) {
      assertProduction(m = 9, s = 4, t = 0, p = 7, e = 3, h = 1)
      assertResources(m = 42, s = 11, t = 0, p = 9, e = 3, h = 5)
      assertCounts(33 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 17, s = 1, t = 1, p = 2, e = 0, h = 0)
      assertResources(m = 56, s = 1, t = 6, p = 5, e = 0, h = 2)
      assertCounts(37 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 7, s = 0, t = 0, p = 1, e = 13, h = 13)
      assertResources(m = 51, s = 0, t = 0, p = 4, e = 13, h = 34)
      assertCounts(40 to "TerraformRating")
    }
    with(green) {
      assertProduction(m = 6, s = 1, t = 2, p = 2, e = 3, h = 1)
      assertResources(m = 56, s = 1, t = 5, p = 4, e = 3, h = 11)
      assertCounts(34 to "TerraformRating")
    }
    assertSidebar(gen = 9, temp = 14, oxygen = 16, oceans = 11, venus = 22)

    // Rainbow's app entry 223 removes one TR immediately before Research. The original recording
    // does not explain the correction, but every later app balance retains it; entry 223 is both
    // the earliest and latest source-compatible position, and a deliberate app correction is the
    // likeliest cause.
    rainbow.exMachina("-TerraformRating")

    yellow.buyCards(2)
    rainbow.buyCards(2)
    blue.buyCards(1)
    green.buyCards(1)

    yellow.turn {
      // "What I'm gonna do is plant forest ... nine nine."
      convertPlants { placeTile(9, 9) }
      // "Could actually go ahead and do a city ... on six nine."
      stdProject("CityProject") { placeTile(6, 9) }
    }
    rainbow.turn {
      // "I am going to take my Applied Science action. I have one more science resource. Put that
      // thing onto flitter and then fire."
      cardAction1(FakeAppliedScience) { addCardResources(FloatingRefinery) }
      // "I need to pay for an Interstellar Colony Ship. ... It costs twenty."
      rainbow.exMachina(fakeWildTags("ScienceTag"))
      playProject(InterstellarColonyShip, 2, titanium = 6)
      // The app omits Media Group's 3 M€ response to this event.
      rainbow.exMachina("-3 MC")
    }
    blue.turn {
      // "Take the greenery standard project action ... at four seven."
      stdProject("GreeneryProject") { placeTile(4, 7) }
    }
    green.turn {
      // "Use my Factotum to spend three money and get a building card: Space Elevator."
      cardAction2(Factorum)
    }

    yellow.turn {
      // "Pay seven steels for Carbon Nanosystems."
      playProject(CarbonNanosystems, steel = 7)
    }
    rainbow.turn {
      // "Floating Refinery action ... two off for one titanium and two money."
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>!") }
      // The app records a 3 M€ gain for the card's 2 M€ action.
      rainbow.exMachina("MC")
      // "Take my Thermophiles ... add it to Sulphur-Eating Bacteria."
      cardAction1(Thermophiles) { addCardResources(SulphurEatingBacteria) }
    }
    blue.turn {
      // "I'm gonna plant a city ... four six. And I get one plant and two money."
      stdProject("CityProject") { placeTile(4, 6) }
    }
    // Rainbow's app omits Rover Construction's response to Blue's city.
    rainbow.exMachina("-2 MC")
    green.turn {
      // "Use the Venus Magnetizer, lose an energy production, raise Venus to twenty four."
      cardAction1(VenusMagnetizer)
    }

    yellow.turn {
      // "Mars University. I pay four steels. I pitch and draw. And I add a graphene."
      playProject(MarsUniversity, steel = 4) { doTask("-ProjectCard") }
    }
    rainbow.turn {
      // "Pay sixteen ... for Farming ... two more plants, and two points."
      playProject(Farming, 16)
      // App entries 233-234 move from 53 to 38 despite recording +3 and -16.
      rainbow.exMachina("MC")
      // "Use my Ants action to take one of your microbes."
      cardAction1(Ants) { doTask("-Microbe<Green, $Tardigrades<Green>>") }
    }
    blue.turn {
      // "Take another greenery standard project with all my heat ... three five."
      // The sourced transactions since the 51 M€ dashboard leave 6 M€, but Blue's app debits
      // 9 M€ and 14 heat here. Supply the three-M€ overdraft so the real payment can run.
      blue.exMachina("3 MC")
      stdProject("GreeneryProject", payment = { pay(9, heat = 14) }) { placeTile(3, 5) }
    }
    green.turn {
      // "Use Tardigrades to put a tardigrade on Tardigrades and give a money."
      cardAction1(Tardigrades)
    }
    yellow.turn {
      // "Trans-Neptune Probe. I'm gonna pay a carbon nano and two monies for it."
      playProject(TransNeptuneProbe, 2) {
        doTask("PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>")
        doTask("-ProjectCard")
      }
      // Yellow's app posts -3 M€ for this otherwise fully narrated 2 M€ payment.
      exMachina("-MC")
    }
    rainbow.turn {
      // "I currently have one, two, three, four, five microbes, and I'm going to turn them into
      // fifteen money."
      cardAction2(SulphurEatingBacteria, x = 5)
      // "My city is at six seven."
      stdProject("CityProject") { placeTile(6, 7) }
    }
    blue.turn {
      // Blue first chose 5-7, then moved the greenery to 5-6 before the next action.
      convertPlants { placeTile(5, 6) }
      // Blue's app then removes one of the two plant placement bonuses as part of that correction.
      blue.exMachina("-Plant")
    }
    green.turn {
      // "Let's just play Earth Catapult first. That costs twenty. Then Noxious City is only
      // sixteen: two steel and fourteen real."
      playProject(EarthCatapult, 20)
      playProject(NoctisCity, 14, steel = 1) { placeTile(9, 8) }
    }
    // Rainbow's app also omits Rover Construction's response to Green's city.
    rainbow.exMachina("-2 MC")

    yellow.turn {
      // "I add a Smanimal."
      cardAction1(SmallAnimals)
    }
    // The transcript identifies a three-card patent sale followed later by Asteroid Deflection
    // System, while anonymous card flow leaves Rainbow's reconstructed hand three cards short.
    // TODO: Audit Rainbow's project-card sources and sinks and replace this anonymous injection.
    rainbow.exMachina("3 ProjectCard")
    rainbow.turn {
      // "I'm gonna do a plant forest action ... seven seven."
      convertPlants { placeTile(7, 7) }
      // "Pay one for CEO's Favorite Project ... I put it on EcoZone."
      // Solarnet cannot select another Player's holder, so resolve the mandatory task on Fish and
      // move only that resource to the sourced Ecological Zone target.
      playProject(CeosFavoriteProject, 1) { addCardResources(Fish) }
      rainbow.exMachina("-Animal<$Fish>, Animal<Yellow, $EcologicalZone<Yellow>>")
    }
    blue.turn {
      // "Another greenery standard project ... four five."
      // The app removes 6 M€ and seventeen heat for the invoice. Its adjacent titanium entry
      // starts and ends at zero and cannot pay for a standard project, so it is not a game change.
      stdProject("GreeneryProject", payment = { pay(6, heat = 17) }) {
        placeTile(4, 5)
      }
      // The app omits the 2 M€ ocean-adjacency income from this final placement.
      blue.exMachina("-2 MC")
    }
    green.turn {
      // "Use Cloud Tourism to add a floater to Cloud Tourism."
      cardAction1(CloudTourism)
    }

    yellow.turn {
      // "Two carbon nanos and eight real on Imported Hydrogen ... gain three plants, which lets me
      // place a plant forest." Carbon Nanosystems permits only one graphene per project. Play the
      // legal payment mechanism, then isolate the table's second-graphene rules error while
      // preserving its physical resource and M€ result.
      playProject(
          ImportedHydrogen,
          payment = {
            doTask("PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>")
            exMachina("-Graphene<$CarbonNanosystems>, -4 Owed<>")
            pay(8)
          },
      ) {
        doTask("3 Plant")
      }
      // Conversely, Yellow's app posts -6 M€ for the narrated 8 M€ cash payment.
      exMachina("2 MC")
      // "On six two."
      convertPlants { placeTile(6, 2) }
    }
    rainbow.turn {
      // "I'm gonna go ahead and sell three patents."
      sellPatents(3)
      // "Adding a fish to my Fish card."
      cardAction1(Fish)
    }
    blue.pass(unused = WaterSplittingPlant)
    green.turn {
      // "Use Venus Shuttles ... I pay six ... Venus is now on twenty six."
      cardAction1(VenusShuttles)
    }

    yellow.pass(unused = SpaceMirrors)
    rainbow.turn {
      // "And birds to my Stratospheric Birds."
      cardAction1(StratosphericBirds)
      // "Mohole Lake to put a Stratospheric Bird."
      cardAction1(MoholeLake) { addCardResources(StratosphericBirds) }
    }
    green.turn {
      // "Spend three heat to gain two planta."
      cardAction1(DirectedHeatUsage) { doTask("2 Plant") }
    }

    rainbow.turn {
      // "Maxwell Base. Another bird."
      cardAction1(MaxwellBase) { addCardResources(StratosphericBirds) }
      // "Add one floater to Local Shading."
      cardAction1(LocalShading)
    }
    green.turn {
      // "Stratospheric Expedition ... spend four titanium ... put two float boys on Cloud Tourism
      // and draw two Venus cards."
      playProject(StratosphericExpedition, titanium = 4) {
        addCardResources(CloudTourism, 2)
      }
    }

    // Rainbow app entry 249, immediately before the mistakenly logged second City purchase.
    with(rainbow) {
      assertProduction(m = 20, s = 1, t = 1, p = 4, e = 0, h = 0)
      assertResources(m = 39, s = 2, t = 1, p = 1, e = 2, h = 2)
      assertCounts(36 to "TerraformRating")
    }
    rainbow.turn {
      // Asteroid Deflection System lowers energy production, so Rainbow first bought the needed
      // production and then paid its discounted nine-M€ cost. Her app records the standard project
      // as a City instead, adding one M€ production; its final production credit retains that
      // error.
      stdProject("PowerPlantProject")
      exMachina("-14 MC, PROD[MC]")
      playProject(AsteroidDeflectionSystem, 2, steel = 2, titanium = 1)
      // The app then removes the full 13 M€ cost in addition to the steel and titanium payment.
      exMachina("-11 MC")
    }
    green.turn {
      // "Breathing Filters for nine. And that gives me a cube on Olympus Conference."
      playProject(BreathingFilters, 9)
    }

    rainbow.turn {
      // "Reveal the top card ... no space tag. But I still took the Asteroid Deflection System
      // action."
      cardAction1(AsteroidDeflectionSystem) { doTask("Ok") }
    }
    green.turn {
      // "Spend the four on the Ishtar Expedition ... get three titanium ... and two Venus cards."
      playProject(IshtarExpedition, 4)
    }

    rainbow.pass()
    green.turn {
      // "Six patents to get the money I need for Corroder Suits. That gives me two money
      // production, which gives me two money."
      sellPatents(6)
      playProject(CorroderSuits, 6) { addCardResources(CloudTourism) }
      // Despite the narration, the app omits Suitable Infrastructure's 2 M€ response to the
      // production gain.
      green.exMachina("-2 MC")
      pass(unused = setOf(BusinessNetwork, IcyImpactors))
    }

    // Green's app credits 50 M€ at production despite 36 TR and 11 M€ production totaling 47.
    green.exMachina("3 MC")
    // All four app histories after final production and before final greenery.
    with(yellow) {
      assertProduction(m = 10, s = 4, t = 0, p = 7, e = 3, h = 1)
      assertResources(m = 46, s = 4, t = 0, p = 8, e = 3, h = 9)
      assertCounts(34 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 21, s = 1, t = 1, p = 4, e = 0, h = 0)
      assertResources(m = 58, s = 1, t = 1, p = 5, e = 0, h = 4)
      assertCounts(36 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 8, s = 0, t = 0, p = 1, e = 13, h = 13)
      assertResources(m = 49, s = 0, t = 0, p = 2, e = 13, h = 29)
      assertCounts(41 to "TerraformRating")
    }
    with(green) {
      assertProduction(m = 11, s = 1, t = 2, p = 2, e = 1, h = 1)
      assertResources(m = 50, s = 1, t = 6, p = 8, e = 1, h = 12)
      assertCounts(36 to "TerraformRating")
    }
    assertSidebar(gen = 9, temp = 14, oxygen = 18, oceans = 11, venus = 26)

    // "L_E_ places plants first ... eleven ten for one plant."
    yellow.convertPlants { placeTile(11, 10) }
    yellow.declineTask()
    // "I do not. I only have five."
    rainbow.declineTask()
    blue.declineTask()
    // "Seven five ... four money ... and it gives me two energy."
    green.convertPlants { placeTile(7, 5) }
    green.declineTask()

    yellow.assertResources(m = 46, s = 4, t = 0, p = 2, e = 3, h = 9)
    rainbow.assertResources(m = 58, s = 1, t = 1, p = 5, e = 0, h = 4)
    blue.assertResources(m = 49, s = 0, t = 0, p = 2, e = 13, h = 29)
    green.assertResources(m = 54, s = 1, t = 6, p = 0, e = 3, h = 12)

    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<Yellow>") shouldBe 0
    score.net("Milestone", "VictoryPoint<Rainbow>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Blue>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Green>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Yellow>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Rainbow>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<Blue>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Green>") shouldBe 5
    score.net("SecondPlace", "VictoryPoint<Yellow>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<Rainbow>") shouldBe 4
    score.net("SecondPlace", "VictoryPoint<Blue>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<Green>") shouldBe 2
  }
}
