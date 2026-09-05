package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

// Helion is not yet in Canon. BaseResourceValue makes Heat available for every actual M€ bill;
// only direct resource-removal instructions would require an explicit replay correction.
private val FakeHelion = cn("FakeHelion")
private val fakeHelionDefinition =
    parseClasses(
        """
        CLASS FakeHelion : CardFront<Class<CorporationCard>> {
          cost = 0
          This:: SpaceTag<This>, BaseResourceValue<Class<Heat>>
          This: 42 MC, PROD[3 Heat]
          Billing<HasActions, ActionSlot, Class<MC>> IF Owed<Class<MC>>:: Accepting<Class<Heat>>
        }
        """
    )

private val otbGame20260904Catalog = Canon.withNonstandardClasses(fakeHelionDefinition)

/** Four-player physical game begun Friday, 2026-09-04; the recording ends before G6 Research. */
internal class OtbGame20260904Test : AbstractFullGameTest() {
  override val catalog = otbGame20260904Catalog

  override val config =
      GameConfig(
          """
          AmazonisMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, PromoCardPack
          FakeHelion

          Builder, Diversifier, Generalist, Landshaper, Tactician
          Administrator, Excentric, Highlander, Promoter, Thermalist
          """,
          "Yellow",
          "Rainbow",
          "Blue",
          "Green",
      )

  @Test
  internal fun otbGame20260904() {
    TfmWorkflow.Auto(game).launch()
    val yellow = p1.requireExplicitUnusedActionCards()
    val rainbow = p2.requireExplicitUnusedActionCards()
    val blue = p3.requireExplicitUnusedActionCards()
    val green =
        game
            .tfm(game.actors.filterIsInstance<Player>()[3])
            .requireExplicitPaymentChoices()
            .requireExplicitUnusedActionCards()

    yellow.playCorp(Ecoline, 4)
    rainbow.playCorp(MorningStarInc, 6)
    blue.playCorp(FakeHelion, 5)
    green.playCorp(Factorum, 4)

    // "We'll give you two and we'll give her four." Blue and Rainbow used those TR handicaps;
    // the joking suggestion of six for Green never reached any player record.
    blue.exMachina("2 TerraformRating")
    rainbow.exMachina("4 TerraformRating")

    yellow.turn {
      playPrelude(DomeFarming)
      playPrelude(HugeAsteroid)
    }
    rainbow.turn {
      playPrelude(AppliedScience)
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
      stdAction("DoRequiredActions")
      // Rainbow first paid seven, then took back the evidenced Space Lanes discount.
      playProject(
          FloatingRefinery,
          5,
          butFirst = assignAllWildTags("VenusTag"),
      )
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
    rainbow.pass(unused = AppliedScience) // didn't know what resource she wanted

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
      assertCardResources(1 to FloatingRefinery, 6 to AppliedScience)
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

    rainbow.turn { stdProject("AirScrappingSP") }
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
      cardAction1(AppliedScience) { addCardResources(SulphurEatingBacteria) }
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
      assertCardResources(2 to FloatingRefinery, 2 to SulphurEatingBacteria, 5 to AppliedScience)
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
        doTask("OceanTile<Amazonis_05_10> THEN -3 MC<Blue>")
        blue.declineTask()
      }
      playProject(UndergroundCity, 10, steel = 4) { placeTile(10, 10) }
    }
    rainbow.turn {
      playProject(SulphurExports, 14, titanium = 1, butFirst = assignAllWildTags("VenusTag"))
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
      cardAction1(AppliedScience) { addCardResources(SulphurEatingBacteria) }
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
      assertCardResources(4 to SulphurEatingBacteria, 4 to AppliedScience)
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
      cardAction1(AppliedScience) { addCardResources(SulphurEatingBacteria) }
      cardAction2(SulphurEatingBacteria, x = 5)
    }
    blue.turn {
      playProject(ProtectedValley, 18, heat = 5) { placeTile(2, 7) }
      doTask("UseAction<ClaimMilestone, Action1>")
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
      playProject(
          BactoviralResearch,
          10,
          butFirst = assignAllWildTags("ScienceTag"),
      ) {
        addCardResources(SulphurEatingBacteria)
      }
      playProject(
          AtalantaPlanitiaLab,
          8,
          butFirst = assignAllWildTags("ScienceTag"),
      )
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
      assertCardResources(3 to SulphurEatingBacteria, 3 to AppliedScience)
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
      cardAction1(AppliedScience) { addCardResources(SulphurEatingBacteria) }
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
            selectTask("UseAction<Player3, NeptunianOption<Player3>>?")
            blue.doTask("UseAction<NeptunianOption, Action1>")
            blue.pay(5)
            placeTile(9, 4)
            autoExecNow()
            selectTask("UseAction<Player3, NeptunianOption<Player3>>?")
            blue.doTask("UseAction<NeptunianOption, Action1>")
            blue.pay(5)
            // Crossing 0°C supplies Amazonis's temperature-track ocean bonus.
            placeTile(6, 11)
            autoExecNow()
            selectTask("UseAction<Player3, NeptunianOption<Player3>>?")
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

    // board-16-07-06.jpg and all four app histories: Generation 6 before Research.
    with(yellow) {
      assertProduction(m = 3, s = 4, t = 0, p = 4, e = 1, h = 1)
      assertResources(m = 29, s = 12, t = 0, p = 4, e = 1, h = 7)
      assertCounts(26 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 6, s = 0, t = 1, p = 1, e = 1, h = 0)
      assertResources(m = 40, s = 1, t = 2, p = 2, e = 1, h = 1)
      assertCounts(33 to "TerraformRating", 1 to "Tactician")
      assertCardResources(
          1 to FloatingRefinery,
          1 to SulphurEatingBacteria,
          2 to AppliedScience,
      )
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 1, e = 10, h = 7)
      assertResources(m = 45, s = 0, t = 0, p = 5, e = 10, h = 13)
      assertCounts(29 to "TerraformRating", 1 to "Landshaper")
      assertCardResources(4 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 4, s = 1, t = 2, p = 0, e = 0, h = 1)
      assertResources(m = 30, s = 1, t = 2, p = 3, e = 0, h = 9)
      assertCounts(25 to "TerraformRating", 1 to "Diversifier")
      assertCardResources(2 to Pets, 5 to CloudTourism, 2 to IcyImpactors, 1 to OlympusConference)
    }
    assertSidebar(gen = 6, temp = 2, oxygen = 4, oceans = 6, venus = 8)

    rainbow.exMachina("5 MC") // full reconciliation
    green.exMachina("1 TerraformRating, 3 MC") // full reconciliation
  }
}
