package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete public-state replay: Active Vacuum Core (g62d89e349c97), save 626.
// Private hands, drafts, and deck order are intentionally unnamed.
// Source: _local/replays/Game20260914/Heroku-g62d89e349c97/
// http://newazure.local:8080/the-end?id=p6f50f47be7e8
internal class ActiveVacuumCoreTest : FullGameTest() {
  // Keep the public-state replay setup: private offers and drafts are not reconstructed.
  // The archive offered 3 corporations; this fixture retains its default offer of 2.
  override val config =
      GameConfig(
          """
          TharsisMap
          VenusNextExpansion, PreludeExpansion, prelude2:cards, promos:cards
          fakes:cards

          Diversifier, Mayor, Builder, Coastguard, Terran, Metallurgist
          Visionary, Landlord, Investor, Forecaster, Traveller, Celebrity
          """
      )

  // Initial database save: Blue starts at 24 TR and Purple at 22 TR.
  override val playerClassPets =
      """
      CLASS Blue : Player { SetupPhase: 4 TerraformRating<This> }
      CLASS Pink : Player
      CLASS Green : Player
      CLASS Purple : Player { SetupPhase: 2 TerraformRating<This> }
      """
          .trimIndent()

  private val blue
    get() = p1

  private val pink
    get() = p2

  private val green
    get() = p3

  private val purple
    get() = player(4)

  @Test
  internal fun completeGame() {
    startGame()
    blue.doTask("4 TerraformRating")
    purple.doTask("2 TerraformRating")
    blue.doTask("20 TerraformRating")
    pink.doTask("20 TerraformRating")
    green.doTask("20 TerraformRating")
    purple.doTask("20 TerraformRating")
    blue.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
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
    purple.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    blue.doTask("-5 ProjectCard<Selecting>")
    pink.doTask("-4 ProjectCard<Selecting>")
    green.doTask("-3 ProjectCard<Selecting>")
    purple.doTask("-7 ProjectCard<Selecting>")
    generation1()
    generation2()
    generation3()
    generation4()
    generation5()
    generation6()
    generation7()
    generation8ThroughSave546()
    finishGeneration8()
  }

  private fun generation1() {
    with(blue) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$AstroDrill>>",
          "$AstroDrill FROM CorporationCard",
          "35 MC",
          "3 Asteroid<$AstroDrill>",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(pink) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Vitor>>",
          "$Vitor FROM CorporationCard",
          "45 MC",
          "3 MC",
          "18 Pay<> FROM MC",
          "6 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(green) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$PharmacyUnion>>",
          "$PharmacyUnion FROM CorporationCard",
      )
      doTask("SearchForCard<TagFilter<Class<ScienceTag>>>").expect("ProjectCard")
      doTasks(
          "54 MC",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
          "Disease<$PharmacyUnion>",
          "Disease<$PharmacyUnion>",
          "-4 MC",
          "-4 MC",
      )
    }

    with(purple) {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Inventrix>>",
          "$Inventrix FROM CorporationCard",
          "45 MC",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(blue) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$VenusL1Shade>>",
          "$VenusL1Shade FROM PreludeCard",
          "VenusStep",
          "VenusStep",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AntiDesertificationTechniques>>",
          "$AntiDesertificationTechniques FROM PreludeCard",
          "3 MC",
          "PROD[Plant]",
          "PROD[Steel]",
          "NOW Green { (Disease<$PharmacyUnion<Green>>! OR (MAX 0 $PharmacyUnion<Green>: Ok)) }",
      )
      green.doTask("Disease<$PharmacyUnion>")
      doTask("NOW Green { -4* MC<Green> }")
      green.doTask("-4 MC")
    }

    with(pink) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BusinessEmpire>>",
          "$BusinessEmpire FROM PreludeCard",
          "PROD[6 MC]",
          "-6 MC",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ExcentricSponsor>>",
          "$ExcentricSponsor FROM PreludeCard",
          "PlayCard<Class<ProjectCard>, Class<$ImmigrationShuttles>, CardLocation>",
          "-25 Owed",
          "6 Pay<> FROM MC",
          "$ImmigrationShuttles FROM ProjectCard",
          "PROD[5 MC]",
          "3 MC",
      )
    }

    with(green) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$IoResearchOutpost>>",
          "$IoResearchOutpost FROM PreludeCard",
          "PROD[Titanium]",
          "ProjectCard",
          "TerraformRating FROM Disease<$PharmacyUnion>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AppliedScience>>",
          "$AppliedScience FROM PreludeCard",
          "6 Science<$AppliedScience>",
      )
    }

    with(purple) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
          "$MetalsCompany FROM PreludeCard",
          "PROD[MC]",
          "PROD[Steel]",
          "PROD[Titanium]",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$GiantSolarCollector>>",
          "$GiantSolarCollector FROM PreludeCard",
          "PROD[2 Energy]",
          "VenusStep",
          "TerraformRating",
          "ProjectCard",
      )
    }

    with(blue) {
      doTasks(*cardActionTasks("$AstroDrill", "Action2"), "-Asteroid<$AstroDrill>", "3 Titanium")
      doTasks(
          *playCardTasks("$CometAiming"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "14 Pay<> FROM MC",
          "$CometAiming FROM ProjectCard",
      )
    }

    with(pink) {
      doTasks("DoRequiredActions", "Investor")
      doTasks(
          *playCardTasks("$IndustrialMicrobes"),
          "12 Pay<> FROM MC",
          "$IndustrialMicrobes FROM ProjectCard",
          "PROD[Energy]",
          "PROD[Steel]",
          "NOW Green { (Disease<$PharmacyUnion<Green>>! OR (MAX 0 $PharmacyUnion<Green>: Ok)) }",
      )
      green.doTask("Disease<$PharmacyUnion>")
      doTask("NOW Green { -4* MC<Green> }")
      green.doTask("-4 MC")
    }

    with(green) {
      doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Titanium")
      doTask("PassBaton")
    }

    with(purple) {
      doTasks("DoRequiredActions", "3 ProjectCard")
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "7 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
      )
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$CometAiming"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$AstroDrill>",
      )
      doTask("PassBaton")
    }

    pink.doTask("Pass")

    with(green) {
      doTasks(
          *playCardTasks("$CuttingEdgeTechnology"),
          "12 Pay<> FROM MC",
          "$CuttingEdgeTechnology FROM ProjectCard",
          "TerraformRating FROM Disease<$PharmacyUnion>",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$WaterToVenus"),
          "9 Pay<> FROM MC",
          "$WaterToVenus FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$NeutralizerFactory"),
          "7 Pay<> FROM MC",
          "$NeutralizerFactory FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
      )
    }

    blue.doTask("Pass")

    with(green) {
      doTasks(
          *playCardTasks("$SulphurEatingBacteria"),
          "4 Pay<> FROM MC",
          "$SulphurEatingBacteria FROM ProjectCard",
          "Disease<$PharmacyUnion>",
          "-MC",
      )
      doTask("PassBaton")
    }

    purple.doTask("Pass")

    with(green) {
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTask("Pass")
    }

    // Production.
    blue.doTasks("27 MC", "Plant", "Steel")
    pink.doTasks("31 MC", "Energy", "Steel")
    green.doTasks("22 MC", "Titanium")
    purple.doTasks("28 MC", "Steel", "Titanium", "2 Energy")

    // World Government.
    blue.doTask("VenusStep! BY Admin")
  }

  private fun generation2() {

    // Research.
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    purple.doTask("4 ProjectCard<Selecting>")

    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    // Database save 59: public player boards after generation 2 Research purchases.
    blue.assertResources(m = 30, s = 1, t = 1, p = 1, e = 0, h = 0)
    blue.assertProduction(m = 0, s = 1, t = 0, p = 1, e = 0, h = 0)
    blue.assertDashMiddle(played = 4, actions = 2, tr = 27, hand = 6)
    blue.assertTags(spt = 3, plt = 1, mit = 1)
    blue.assertDashRight(events = 0, tagless = 0, cities = 0)
    pink.assertResources(m = 34, s = 1, t = 0, p = 0, e = 1, h = 0)
    pink.assertProduction(m = 11, s = 1, t = 0, p = 0, e = 1, h = 0)
    pink.assertDashMiddle(played = 5, actions = 0, tr = 20, hand = 6)
    pink.assertTags(but = 1, spt = 1, eat = 3, mit = 1)
    pink.assertDashRight(events = 0, tagless = 1, cities = 0)
    green.assertResources(m = 13, s = 0, t = 2, p = 0, e = 0, h = 0)
    green.assertProduction(m = 0, s = 0, t = 1, p = 0, e = 0, h = 0)
    green.assertDashMiddle(played = 5, actions = 2, tr = 22, hand = 10)
    green.assertTags(sct = 2, jot = 1, vet = 1, mit = 3)
    green.assertDashRight(events = 0, tagless = 1, cities = 0)
    purple.assertResources(m = 35, s = 1, t = 1, p = 0, e = 2, h = 0)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 0, e = 2, h = 0)
    purple.assertDashMiddle(played = 6, actions = 0, tr = 27, hand = 6)
    purple.assertTags(spt = 1, sct = 1, pot = 1, vet = 1)
    purple.assertDashRight(events = 2, tagless = 1, cities = 0)
    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 14)

    with(pink) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$Soletta"),
          "35 Pay<> FROM MC",
          "$Soletta FROM ProjectCard",
          "PROD[7 Heat]",
      )
    }

    with(green) {
      doTasks(
          *playCardTasks("$Omnicourt"),
          "9 Pay<> FROM MC",
          "$Omnicourt FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$SolarPower"),
          "1 Pay<Class<Steel>> FROM Steel",
          "9 Pay<> FROM MC",
          "$SolarPower FROM ProjectCard",
          "PROD[Energy]",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$AstroDrill", "Action2"), "-Asteroid<$AstroDrill>", "3 Titanium")
      doTasks(
          *cardActionTasks("$CometAiming"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$CometAiming>",
      )
    }

    pink.doTask("Pass")

    with(green) {
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$MagneticFieldDome"),
          "5 Pay<> FROM MC",
          "$MagneticFieldDome FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[Plant]",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$IceAsteroid"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "17 Pay<> FROM MC",
          "$IceAsteroid FROM ProjectCard",
          "OceanTile<Tharsis_5_4>",
          "TerraformRating",
          "2 Plant",
          "OceanTile<Tharsis_5_5>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$ArcticAlgae"),
          "12 Pay<> FROM MC",
          "$ArcticAlgae FROM ProjectCard",
          "Plant",
      )
      doTasks(
          *playCardTasks("$Psychrophiles"),
          "2 Pay<> FROM MC",
          "$Psychrophiles FROM ProjectCard",
          "NOW Green { (Disease<$PharmacyUnion<Green>>! OR (MAX 0 $PharmacyUnion<Green>: Ok)) }",
      )
      green.doTask("Disease<$PharmacyUnion>")
      doTask("NOW Green { -4* MC<Green> }")
      green.doTask("-4 MC")
    }

    green.doTask("Pass")

    purple.doTask("Pass")

    with(blue) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTask("Pass")
    }

    // Production.
    blue.doTasks("29 MC", "Plant", "Steel")
    pink.doTasks("31 MC", "Energy", "Steel", "Heat FROM Energy", "7 Heat")
    green.doTasks("24 MC", "Titanium")
    purple.doTasks("29 MC", "Steel", "Titanium", "2 Energy", "2 Heat FROM Energy", "Plant")

    // World Government.
    pink.doTask("VenusStep! BY Admin")
  }

  private fun generation3() {

    // Research.
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    purple.doTask("4 ProjectCard<Selecting>")

    purple.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    // Database save 111: public player boards after generation 3 Research purchases.
    blue.assertResources(m = 21, s = 2, t = 1, p = 7, e = 0, h = 0)
    blue.assertProduction(m = 0, s = 1, t = 0, p = 1, e = 0, h = 0)
    blue.assertDashMiddle(played = 7, actions = 3, tr = 29, hand = 6)
    blue.assertTags(spt = 3, plt = 2, mit = 2)
    blue.assertDashRight(events = 1, tagless = 0, cities = 0)
    pink.assertResources(m = 25, s = 2, t = 0, p = 0, e = 1, h = 8)
    pink.assertProduction(m = 11, s = 1, t = 0, p = 0, e = 1, h = 7)
    pink.assertDashMiddle(played = 6, actions = 0, tr = 20, hand = 6)
    pink.assertTags(but = 1, spt = 2, eat = 3, mit = 1)
    pink.assertDashRight(events = 0, tagless = 1, cities = 0)
    green.assertResources(m = 18, s = 0, t = 3, p = 0, e = 0, h = 0)
    green.assertProduction(m = 0, s = 0, t = 1, p = 0, e = 0, h = 0)
    green.assertDashMiddle(played = 6, actions = 2, tr = 24, hand = 11)
    green.assertTags(but = 1, sct = 2, jot = 1, vet = 1, mit = 3)
    green.assertDashRight(events = 0, tagless = 1, cities = 0)
    purple.assertResources(m = 30, s = 1, t = 2, p = 1, e = 2, h = 2)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 1, e = 2, h = 0)
    purple.assertDashMiddle(played = 8, actions = 0, tr = 28, hand = 7)
    purple.assertTags(but = 2, spt = 1, sct = 1, pot = 2, vet = 1)
    purple.assertDashRight(events = 2, tagless = 1, cities = 0)
    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 2, venus = 16)

    with(green) {
      doTasks(
          *playCardTasks("$LightningHarvest"),
          "6 Pay<> FROM MC",
          "$LightningHarvest FROM ProjectCard",
          "PROD[Energy]",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$DeimosDownPromo"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "25 Pay<> FROM MC",
          "$DeimosDownPromo FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TemperatureStep",
          "4 Steel",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
          "PROD[Heat]",
          "DeimosDownPromo_SpecialTile<Tharsis_4_4>",
          "Plant",
          "2 MC",
          "2 MC",
          "-6 Plant<Blue>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$CometAiming", "Action2"),
          "-Asteroid<$CometAiming>",
          "OceanTile<Tharsis_5_6>",
          "TerraformRating",
          "2 Plant",
          "2 Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$DirectedImpactors"),
          "8 Pay<> FROM MC",
          "$DirectedImpactors FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Plant")
      doTasks(
          *playCardTasks("$Moss"),
          "2 Pay<> FROM MC",
          "$Moss FROM ProjectCard",
          "PROD[Plant]",
          "-Plant",
      )
    }

    with(purple) {
      doTasks(
          *playCardTasks("$PowerSupplyConsortium"),
          "5 Pay<> FROM MC",
          "$PowerSupplyConsortium FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Pink>]",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$AstroDrill", "Action2"), "-Asteroid<$AstroDrill>", "3 Titanium")
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
    }

    with(pink) {
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "6 Pay<> FROM MC",
          "Asteroid<$DirectedImpactors>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Diversifier")
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$Teslaract"),
          "5 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$Teslaract FROM ProjectCard",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$MiningExpedition"),
          "12 Pay<> FROM MC",
          "$MiningExpedition FROM ProjectCard",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
          "-2 Plant<Purple>",
      )
      doTask("PassBaton")
    }

    pink.doTask("Pass")

    with(green) {
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(*cardActionTasks("$Teslaract"), "PROD[-Energy]", "PROD[Plant]")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$WeatherBalloons"),
          "11 Pay<> FROM MC",
          "$WeatherBalloons FROM ProjectCard",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    green.doTask("Pass")

    purple.doTask("Pass")

    with(blue) {
      doTasks(*cardActionTasks("$WeatherBalloons"), "Floater<$WeatherBalloons>")
      doTask("Pass")
    }

    // Production.
    blue.doTasks("31 MC", "Plant", "Steel")
    pink.doTasks("31 MC", "Steel", "Heat FROM Energy", "7 Heat")
    green.doTasks("25 MC", "Titanium", "Energy", "Plant")
    purple.doTasks(
        "33 MC",
        "Steel",
        "Titanium",
        "2 Energy",
        "2 Heat FROM Energy",
        "2 Plant",
        "Heat",
    )

    // World Government.
    green.doTask("OxygenStep! BY Admin")
  }

  private fun generation4() {
    // Database save 162: the public state at the start of generation 4 drafting. This checkpoint
    // precedes Research, so it neither names nor models cards that were still private then.
    blue.assertResources(m = 31, s = 5, t = 4, p = 6, e = 0, h = 0)
    blue.assertProduction(m = 0, s = 1, t = 0, p = 1, e = 0, h = 0)
    blue.assertDashMiddle(played = 9, actions = 4, tr = 31, hand = 5)
    blue.assertTags(spt = 3, sct = 1, plt = 2, mit = 2)
    blue.assertDashRight(events = 2, tagless = 0, cities = 0)
    pink.assertResources(m = 42, s = 3, t = 0, p = 0, e = 0, h = 16)
    pink.assertProduction(m = 11, s = 1, t = 0, p = 0, e = 0, h = 7)
    pink.assertDashMiddle(played = 7, actions = 1, tr = 20, hand = 5)
    pink.assertTags(but = 1, spt = 3, eat = 3, mit = 1)
    pink.assertDashRight(events = 0, tagless = 1, cities = 0)
    green.assertResources(m = 27, s = 0, t = 4, p = 1, e = 1, h = 0)
    green.assertProduction(m = 1, s = 0, t = 1, p = 1, e = 1, h = 0)
    green.assertDashMiddle(played = 8, actions = 2, tr = 24, hand = 9)
    green.assertTags(but = 1, sct = 2, pot = 1, jot = 1, vet = 1, plt = 1, mit = 3)
    green.assertDashRight(events = 0, tagless = 1, cities = 0)
    purple.assertResources(m = 33, s = 1, t = 1, p = 2, e = 2, h = 5)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 2, e = 2, h = 1)
    purple.assertDashMiddle(played = 11, actions = 1, tr = 32, hand = 4)
    purple.assertTags(but = 3, spt = 1, sct = 1, pot = 4, vet = 1)
    purple.assertDashRight(events = 3, tagless = 1, cities = 0)
    assertSidebar(gen = 4, temp = -24, oxygen = 2, oceans = 3, venus = 16)

    // Research.
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    purple.doTask("4 ProjectCard<Selecting>")

    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(purple) {
      doTasks(
          *playCardTasks("$DeepWellHeating"),
          "1 Pay<Class<Steel>> FROM Steel",
          "11 Pay<> FROM MC",
          "$DeepWellHeating FROM ProjectCard",
          "PROD[Energy]",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
    }

    with(blue) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTasks(*cardActionTasks("$WeatherBalloons"), "Floater<$WeatherBalloons>")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
          "3 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }

    purple.doTask("Pass")

    with(blue) {
      doTasks(*cardActionTasks("$AstroDrill"), "Asteroid<$CometAiming>")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$HomeostasisBureau"),
          "3 Pay<Class<Steel>> FROM Steel",
          "8 Pay<> FROM MC",
          "$HomeostasisBureau FROM ProjectCard",
          "PROD[2 Heat]",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$VenusianPlants"),
          "11 Pay<> FROM MC",
          "$VenusianPlants FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$NoctisFarming"),
          "5 Pay<Class<Steel>> FROM Steel",
          "Ok", // No Psychrophiles microbes paid.
          "Ok", // No M€ needed after paying with steel.
          "$NoctisFarming FROM ProjectCard",
          "PROD[MC]",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_4>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "TerraformRating",
      )
    }

    with(pink) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$TransNeptuneProbe"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "$TransNeptuneProbe FROM ProjectCard",
          "TerraformRating FROM Disease<$PharmacyUnion>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "16 Pay<> FROM MC",
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-6 Microbe<$SulphurEatingBacteria>",
          "18 MC",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$TerraformingContract"),
          "8 Pay<> FROM MC",
          "$TerraformingContract FROM ProjectCard",
          "PROD[4 MC]",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *cardActionTasks("$DirectedImpactors", "Action2"),
          "-Asteroid<$DirectedImpactors>",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$Solarnet"),
          "5 Pay<> FROM MC",
          "$Solarnet FROM ProjectCard",
          "2 ProjectCard",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$CometAiming", "Action2"),
          "-Asteroid<$CometAiming>",
          "OceanTile<Tharsis_6_7>",
          "TerraformRating",
          "2 Plant",
          "Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }

    pink.doTask("Pass")

    with(green) {
      doTasks(
          *playCardTasks("$SnowAlgae"),
          "10 Pay<> FROM MC",
          "$SnowAlgae FROM ProjectCard",
          "PROD[Plant]",
          "PROD[Heat]",
      )
      doTask("PassBaton")
    }

    blue.doTask("Pass")

    green.doTask("Pass")

    // Production.
    blue.doTasks("38 MC", "3 Plant", "Steel", "2 Heat")
    pink.doTasks("34 MC", "Steel", "9 Heat")
    green.doTasks("27 MC", "Titanium", "Energy", "2 Plant", "Heat FROM Energy", "Heat")
    purple.doTasks(
        "35 MC",
        "Steel",
        "Titanium",
        "3 Energy",
        "2 Heat FROM Energy",
        "2 Plant",
        "2 Heat",
    )

    // World Government.
    purple.doTask("VenusStep! BY Admin")
  }

  private fun generation5() {
    // Database save 237: the public state at the start of generation 5 drafting. This checkpoint
    // precedes Research, so it neither names nor models cards that are still private.
    blue.assertResources(m = 40, s = 1, t = 0, p = 7, e = 0, h = 2)
    blue.assertProduction(m = 5, s = 1, t = 0, p = 3, e = 0, h = 2)
    blue.assertDashMiddle(played = 12, actions = 4, tr = 33, hand = 5)
    blue.assertTags(but = 1, spt = 4, sct = 1, eat = 1, jot = 1, plt = 3, mit = 2)
    blue.assertDashRight(events = 2, tagless = 0, cities = 0)
    pink.assertResources(m = 51, s = 1, t = 0, p = 0, e = 0, h = 9)
    pink.assertProduction(m = 11, s = 1, t = 0, p = 0, e = 0, h = 9)
    pink.assertDashMiddle(played = 9, actions = 1, tr = 23, hand = 5)
    pink.assertTags(but = 2, spt = 3, eat = 4, mit = 1)
    pink.assertDashRight(events = 0, tagless = 1, cities = 0)
    green.assertResources(m = 37, s = 0, t = 3, p = 3, e = 1, h = 2)
    green.assertProduction(m = 1, s = 0, t = 1, p = 2, e = 1, h = 1)
    green.assertDashMiddle(played = 12, actions = 2, tr = 26, hand = 10)
    green.assertTags(but = 1, spt = 1, sct = 3, pot = 1, jot = 1, vet = 2, plt = 3, mit = 3)
    green.assertDashRight(events = 0, tagless = 2, cities = 0)
    purple.assertResources(m = 40, s = 1, t = 2, p = 4, e = 3, h = 9)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 2, e = 3, h = 2)
    purple.assertDashMiddle(played = 12, actions = 1, tr = 34, hand = 4)
    purple.assertTags(but = 4, spt = 1, sct = 1, pot = 5, vet = 1)
    purple.assertDashRight(events = 3, tagless = 1, cities = 0)
    assertSidebar(gen = 5, temp = -14, oxygen = 3, oceans = 4, venus = 20)

    // Research.
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    purple.doTask("4 ProjectCard<Selecting>")

    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(blue) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTasks(*cardActionTasks("$WeatherBalloons"), "Floater<$WeatherBalloons>")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$SterlingVents"),
          "1 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$SterlingVents FROM ProjectCard",
          "PROD[-2 Heat]",
          "PROD[2 Energy]",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$ImmigrantCity"),
          "11 Pay<> FROM MC",
          "$ImmigrantCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[-2 MC]",
          "CityTile<Tharsis_6_5>",
          "PROD[MC]",
          "Plant",
          "2 MC",
          "2 MC",
      )
    }

    with(green) {
      doTasks(
          *playCardTasks("$MiningRights"),
          "9 Pay<> FROM MC",
          "$MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Tharsis_8_4>",
          "Production<Class<Metal>(HAS PlacementBonus<LandArea(HAS MiningRights_SpecialTile)>)>",
          "2 Steel",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$AstroDrill", "Action2"), "-Asteroid<$AstroDrill>", "3 Titanium")
      doTasks(
          *cardActionTasks("$CometAiming"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$CometAiming>",
      )
    }

    with(pink) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$Comet"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "15 Pay<> FROM MC",
          "$Comet FROM ProjectCard",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Tharsis_2_6>",
          "TerraformRating",
          "NOW Blue { 2! Plant<Blue> }",
      )
      blue.doTask("2 Plant")
      doTasks("2 ProjectCard", "-3 Plant<Blue>")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$ResearchCoordination"),
          "4 Pay<> FROM MC",
          "$ResearchCoordination FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$Satellites"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<> FROM MC",
          "$Satellites FROM ProjectCard",
          "PROD[6 MC]",
      )
    }

    with(pink) {
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "6 Pay<> FROM MC",
          "Asteroid<$DirectedImpactors>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Titanium")
      doTask("PassBaton")
    }

    // Database save 282: complete public state after Green's Applied Science action. Private hands
    // are represented only by their public aggregate counts.
    blue.assertResources(m = 23, s = 1, t = 0, p = 6, e = 0, h = 2)
    blue.assertProduction(m = 11, s = 1, t = 0, p = 3, e = 0, h = 2)
    blue.assertDashMiddle(played = 14, actions = 0, tr = 33, hand = 6)
    blue.assertTags(but = 1, spt = 5, sct = 1, eat = 1, jot = 1, plt = 3, mit = 2)
    blue.assertDashRight(events = 2, tagless = 1, cities = 0)
    pink.assertResources(m = 31, s = 0, t = 0, p = 1, e = 0, h = 1)
    pink.assertProduction(m = 10, s = 1, t = 0, p = 0, e = 1, h = 7)
    pink.assertDashMiddle(played = 11, actions = 0, tr = 24, hand = 7)
    pink.assertTags(but = 4, spt = 3, pot = 1, eat = 4, mit = 1, cit = 1)
    pink.assertDashRight(events = 0, tagless = 1, cities = 1)
    green.assertResources(m = 19, s = 2, t = 4, p = 3, e = 1, h = 2)
    green.assertProduction(m = 1, s = 1, t = 1, p = 2, e = 1, h = 1)
    green.assertDashMiddle(played = 13, actions = 0, tr = 26, hand = 12)
    green.assertTags(but = 2, spt = 1, sct = 3, pot = 1, jot = 1, vet = 2, plt = 3, mit = 3)
    green.assertDashRight(events = 0, tagless = 2, cities = 0)
    purple.assertResources(m = 19, s = 1, t = 0, p = 4, e = 3, h = 1)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 2, e = 3, h = 2)
    purple.assertDashMiddle(played = 13, actions = 1, tr = 37, hand = 7)
    purple.assertTags(but = 4, spt = 1, sct = 1, pot = 5, vet = 1)
    purple.assertDashRight(events = 4, tagless = 1, cities = 0)
    assertSidebar(gen = 5, temp = -8, oxygen = 3, oceans = 5, venus = 20)

    with(purple) {
      doTasks(
          *playCardTasks("$PowerGrid"),
          "18 Pay<> FROM MC",
          "$PowerGrid FROM ProjectCard",
          "PROD[6 Energy]",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$WavePower"),
          "8 Pay<> FROM MC",
          "$WavePower FROM ProjectCard",
          "PROD[Energy]",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$FieldCappedCity"),
          "27 Pay<> FROM MC",
          "$FieldCappedCity FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[Energy]",
          "3 Plant",
          "CityTile<Tharsis_7_4>",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$NitrogenRichAsteroid"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "19 Pay<> FROM MC",
          "$NitrogenRichAsteroid FROM ProjectCard",
          "2 TerraformRating",
          "TemperatureStep",
          "TerraformRating",
          "PROD[4 Plant]",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(*cardActionTasks("$Teslaract"), "PROD[-Energy]", "PROD[Plant]")
      doTask("PassBaton")
    }

    blue.doTask("Pass")

    pink.doTask("Pass")

    with(green) {
      doTasks(
          *playCardTasks("$MiningArea"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$MiningArea FROM ProjectCard",
          "MiningArea_SpecialTile<Tharsis_9_5>",
          "Production<Class<Metal>(HAS PlacementBonus<LandArea(HAS MiningArea_SpecialTile)>)>",
          "Steel",
      )
      doTask("PassBaton")
    }

    purple.doTask("Pass")

    green.doTask("Pass")

    // Production.
    blue.doTasks("44 MC", "3 Plant", "Steel", "2 Heat", "Energy")
    pink.doTasks("37 MC", "Steel", "7 Heat", "2 Energy")
    green.doTasks("30 MC", "Titanium", "Energy", "6 Plant", "Heat FROM Energy", "Heat", "2 Steel")
    purple.doTasks(
        "38 MC",
        "Steel",
        "Titanium",
        "8 Energy",
        "3 Heat FROM Energy",
        "3 Plant",
        "2 Heat",
    )

    // World Government.
    blue.doTask("VenusStep! BY Admin")
  }

  private fun generation6() {
    // Database save 310: complete public state at the start of generation 6 drafting. Research has
    // not begun, so private offers and hands remain represented only by aggregate hand counts.
    blue.assertResources(m = 59, s = 2, t = 0, p = 9, e = 1, h = 4)
    blue.assertProduction(m = 11, s = 1, t = 0, p = 3, e = 1, h = 2)
    blue.assertDashMiddle(played = 15, actions = 4, tr = 33, hand = 5)
    blue.assertTags(but = 1, spt = 5, sct = 1, pot = 1, eat = 1, jot = 1, plt = 3, mit = 2)
    blue.assertDashRight(events = 2, tagless = 1, cities = 0)
    pink.assertResources(m = 41, s = 1, t = 0, p = 4, e = 2, h = 8)
    pink.assertProduction(m = 13, s = 1, t = 0, p = 0, e = 2, h = 7)
    pink.assertDashMiddle(played = 12, actions = 1, tr = 24, hand = 6)
    pink.assertTags(but = 5, spt = 3, pot = 2, eat = 4, mit = 1, cit = 2)
    pink.assertDashRight(events = 0, tagless = 1, cities = 2)
    green.assertResources(m = 30, s = 3, t = 1, p = 9, e = 1, h = 4)
    green.assertProduction(m = 1, s = 2, t = 1, p = 6, e = 1, h = 1)
    green.assertDashMiddle(played = 15, actions = 2, tr = 29, hand = 10)
    green.assertTags(but = 3, spt = 1, sct = 3, pot = 1, jot = 1, vet = 2, plt = 3, mit = 3)
    green.assertDashRight(events = 1, tagless = 2, cities = 0)
    purple.assertResources(m = 39, s = 2, t = 1, p = 7, e = 8, h = 6)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 3, e = 8, h = 2)
    purple.assertDashMiddle(played = 14, actions = 1, tr = 37, hand = 6)
    purple.assertTags(but = 4, spt = 1, sct = 1, pot = 6, vet = 1)
    purple.assertDashRight(events = 4, tagless = 1, cities = 0)
    assertSidebar(gen = 6, temp = -6, oxygen = 3, oceans = 5, venus = 22)

    // Research.
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    purple.doTask("4 ProjectCard<Selecting>")

    blue.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(pink) {
      doTasks(
          *playCardTasks("$VenusianAnimals"),
          "13 Pay<> FROM MC",
          "$VenusianAnimals FROM ProjectCard",
          "3 MC",
          "Animal<$VenusianAnimals>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_9_6>",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$BusinessContacts"),
          "7 Pay<> FROM MC",
          "$BusinessContacts FROM ProjectCard",
          "4 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$SpaceElevator"),
          "1 Pay<Class<Steel>> FROM Steel",
          "25 Pay<> FROM MC",
          "$SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$CeosFavoriteProject"),
          "$CeosFavoriteProject FROM ProjectCard",
          "Asteroid<$DirectedImpactors>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$RegoPlastics"),
          "1 Pay<Class<Steel>> FROM Steel",
          "8 Pay<> FROM MC",
          "$RegoPlastics FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$FuelFactory"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$FuelFactory FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$CarbonNanosystems"),
          "4 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$CarbonNanosystems FROM ProjectCard",
          "TerraformRating FROM Disease<$PharmacyUnion>",
          "Graphene<$CarbonNanosystems>",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$GiantSpaceMirror"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "14 Pay<> FROM MC",
          "$GiantSpaceMirror FROM ProjectCard",
          "PROD[3 Energy]",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$AstroDrill"), "Titanium")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *cardActionTasks("$DirectedImpactors", "Action2"),
          "-Asteroid<$DirectedImpactors>",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
          "NOW Blue { 2! Plant<Blue> }",
      )
      blue.doTask("2 Plant")
      doTask("2 Steel")
    }

    with(green) {
      doTasks(
          *playCardTasks("$ArtificialPhotosynthesis"),
          "12 Pay<> FROM MC",
          "$ArtificialPhotosynthesis FROM ProjectCard",
          "TerraformRating FROM Disease<$PharmacyUnion>",
          "Graphene<$CarbonNanosystems>",
          "PROD[2 Energy]",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$SmallAsteroid"),
          "10 Pay<> FROM MC",
          "$SmallAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TerraformRating",
          "-2 Plant<Blue>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$CometAiming", "Action2"),
          "-Asteroid<$CometAiming>",
          "OceanTile<Tharsis_6_8>",
          "TerraformRating",
          "2 Plant",
          "Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$DevelopmentCenter"),
          "3 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$DevelopmentCenter FROM ProjectCard",
          "Animal<$VenusianAnimals>",
      )
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }

    with(green) {
      doTasks(*cardActionTasks("$AppliedScience"), "-Science<$AppliedScience>", "Titanium")
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(*cardActionTasks("$Teslaract"), "PROD[-Energy]", "PROD[Plant]")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$WeatherBalloons", "Action2"), "-Floater<$WeatherBalloons>", "2 MC")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$JovianEmbassy"),
          "12 Pay<> FROM MC",
          "$JovianEmbassy FROM ProjectCard",
          "TerraformRating",
          "3 MC",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Builder")
    }

    with(green) {
      doTasks(
          *playCardTasks("$OrbitalCleanup"),
          "2 PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>",
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with graphene and titanium.
          "$OrbitalCleanup FROM ProjectCard",
          "PROD[-2 MC]",
      )
      doTask("PassBaton")
    }

    purple.doTask("Pass")

    with(blue) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_5_7>",
          "NOW Pink { Production<Pink, Class<MC>>! }",
      )
      pink.doTask("PROD[MC]")
      doTasks("2 Plant", "2 MC", "2 MC", "2 MC")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_4_6>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "TerraformRating",
      )
    }

    pink.doTask("Pass")

    with(green) {
      doTasks(*cardActionTasks("$OrbitalCleanup"), "6 MC")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Coastguard")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$SecurityFleet"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "9 Pay<> FROM MC",
          "$SecurityFleet FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    blue.doTask("Pass")

    with(green) {
      doTasks(
          *playCardTasks("$AdaptedLichen"),
          "9 Pay<> FROM MC",
          "$AdaptedLichen FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("Pass")
    }

    // Production.
    blue.doTasks("47 MC", "3 Plant", "Steel", "2 Heat", "Energy", "Heat FROM Energy", "Titanium")
    pink.doTasks("43 MC", "Steel", "7 Heat", "2 Energy", "Heat FROM Energy")
    green.doTasks("31 MC", "Titanium", "3 Energy", "7 Plant", "Heat FROM Energy", "Heat", "2 Steel")
    purple.doTasks(
        "40 MC",
        "Steel",
        "2 Titanium",
        "9 Energy",
        "8 Heat FROM Energy",
        "4 Plant",
        "2 Heat",
    )

    // World Government.
    pink.doTask("OceanTile<Tharsis_1_4>! BY Admin")
    // Arctic Algae gives Blue 2 plants when World Government places the ocean.
    blue.doTask("2 Plant")
  }

  private fun generation7() {
    // Database save 414: complete public state at the start of generation 7 drafting.
    blue.assertResources(m = 47, s = 1, t = 1, p = 12, e = 1, h = 7)
    blue.assertProduction(m = 12, s = 1, t = 1, p = 3, e = 1, h = 2)
    blue.assertDashMiddle(played = 17, actions = 6, tr = 35, hand = 6)
    blue.assertTags(but = 2, spt = 7, sct = 1, pot = 1, eat = 1, jot = 1, plt = 3, mit = 2)
    blue.assertDashRight(events = 2, tagless = 1, cities = 1)
    pink.assertResources(m = 43, s = 1, t = 0, p = 4, e = 2, h = 8)
    pink.assertProduction(m = 14, s = 1, t = 0, p = 0, e = 2, h = 7)
    pink.assertDashMiddle(played = 16, actions = 2, tr = 29, hand = 5)
    pink.assertTags(
        but = 7,
        spt = 3,
        sct = 2,
        pot = 2,
        eat = 4,
        jot = 1,
        vet = 1,
        mit = 1,
        ant = 1,
        cit = 2,
    )
    pink.assertDashRight(events = 1, tagless = 1, cities = 2)
    green.assertResources(m = 31, s = 2, t = 1, p = 8, e = 3, h = 6)
    green.assertProduction(m = -1, s = 2, t = 1, p = 7, e = 3, h = 1)
    green.assertDashMiddle(played = 20, actions = 3, tr = 32, hand = 6)
    green.assertTags(
        but = 5,
        spt = 2,
        sct = 5,
        pot = 1,
        eat = 1,
        jot = 1,
        vet = 2,
        plt = 4,
        mit = 3,
    )
    green.assertDashRight(events = 1, tagless = 2, cities = 0)
    purple.assertResources(m = 40, s = 1, t = 2, p = 11, e = 9, h = 16)
    purple.assertProduction(m = 2, s = 1, t = 2, p = 4, e = 9, h = 2)
    purple.assertDashMiddle(played = 18, actions = 1, tr = 38, hand = 6)
    purple.assertTags(but = 5, spt = 2, sct = 1, pot = 7, vet = 1)
    purple.assertDashRight(events = 6, tagless = 1, cities = 0)
    assertSidebar(gen = 7, temp = 2, oxygen = 5, oceans = 8, venus = 22)

    // Research.
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    purple.doTask("4 ProjectCard<Selecting>")

    pink.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      doTasks(
          *playCardTasks("$SubterraneanReservoir"),
          "11 Pay<> FROM MC",
          "$SubterraneanReservoir FROM ProjectCard",
          "OceanTile<Tharsis_9_9>",
          "TerraformRating",
          "NOW Blue { 2! Plant<Blue> }",
      )
      blue.doTask("2 Plant")
      doTask("2 Titanium")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_8_6>",
          "OxygenStep",
          "ProjectCard",
          "TerraformRating",
      )
    }

    with(purple) {
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

    with(blue) {
      doTasks(
          *playCardTasks("$KaguyaTech"),
          "5 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>",
          "Ok", // No M€ needed after paying with Psychrophiles microbes.
          "$KaguyaTech FROM ProjectCard",
          "PROD[2 MC]",
          "ProjectCard",
          "CityTile<Tharsis_6_4> FROM GreeneryTile<Tharsis_6_4>",
          "NOW Pink { Production<Pink, Class<MC>>! }",
      )
      pink.doTask("PROD[MC]")
      doTasks("Plant", "2 MC")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *cardActionTasks("$DirectedImpactors", "Action2"),
          "-Asteroid<$DirectedImpactors>",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Traveller")
    }

    with(green) {
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "2 Titanium",
          "TerraformRating FROM Disease<$PharmacyUnion>",
          "Graphene<$CarbonNanosystems>",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *playCardTasks("$PhysicsComplex"),
          "1 Pay<Class<Steel>> FROM Steel",
          "10 Pay<> FROM MC",
          "$PhysicsComplex FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$PhysicsComplex"),
          "6 Pay<Class<Energy>> FROM Energy",
          "Science<$PhysicsComplex>",
      )
    }

    with(blue) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$Trees"),
          "11 Pay<> FROM MC",
          "$Trees FROM ProjectCard",
          "PROD[3 Plant]",
          "Plant",
          "3 MC",
      )
      doTasks(
          *playCardTasks("$AdvancedEcosystems"),
          "9 Pay<> FROM MC",
          "$AdvancedEcosystems FROM ProjectCard",
          "3 MC",
          "NOW Green { (Disease<$PharmacyUnion<Green>>! OR (MAX 0 $PharmacyUnion<Green>: Ok)) }",
      )
      green.doTask("Disease<$PharmacyUnion>")
      doTask("NOW Green { -4* MC<Green> }")
      green.doTask("-4 MC")
    }

    with(green) {
      doTasks(*cardActionTasks("$OrbitalCleanup"), "7 MC")
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_4_5>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_3_5>",
          "NOW Pink { Production<Pink, Class<MC>>! }",
      )
      pink.doTask("PROD[MC]")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$MercurianAlloys"),
          "Ok", // No titanium paid.
          "3 Pay<> FROM MC",
          "$MercurianAlloys FROM ProjectCard",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_3_7>",
          "NOW Pink { Production<Pink, Class<MC>>! }",
      )
      pink.doTask("PROD[MC]")
      doTasks("Steel", "2 MC")
    }

    with(pink) {
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$HiredRaiders"),
          "$HiredRaiders FROM ProjectCard",
          "3 MC FROM MC<Blue>",
      )
    }

    with(green) {
      doTasks(
          *playCardTasks("$SisterPlanetSupport"),
          "5 Pay<> FROM MC",
          "$SisterPlanetSupport FROM ProjectCard",
          "PROD[3 MC]",
      )
      doTasks(
          *playCardTasks("$Insects"),
          "7 Pay<> FROM MC",
          "$Insects FROM ProjectCard",
          "PROD[5 Plant]",
          "Disease<$PharmacyUnion>",
          "-0 MC",
      )
    }

    with(purple) {
      doTasks(*cardActionTasks("$Teslaract"), "PROD[-Energy]", "PROD[Plant]")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$CometAiming"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$AstroDrill>",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_4_7>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
          "0 TemperatureStep",
      )
    }

    with(pink) {
      doTasks(
          *playCardTasks("$SponsoredAcademies"),
          "7 Pay<> FROM MC",
          "$SponsoredAcademies FROM ProjectCard",
          "-ProjectCard",
          "EACH Other@Player(NOT Pink) { ProjectCard<Other@Player, Hand>! }",
          "3 MC",
          "Animal<$VenusianAnimals>",
          "3 ProjectCard",
          "ProjectCard<Blue, Hand>",
          "ProjectCard<Green, Hand>",
          "ProjectCard<Purple, Hand>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-2 Microbe<$SulphurEatingBacteria>",
          "6 MC",
      )
      doTasks(
          *playCardTasks("$Worms"),
          "6 Pay<> FROM MC",
          "$Worms FROM ProjectCard",
          "PROD[3 Plant]",
          "Disease<$PharmacyUnion>",
          "-0 MC",
      )
    }

    with(purple) {
      doTasks(
          *playCardTasks("$ProtectedGrowth"),
          "2 Pay<> FROM MC",
          "$ProtectedGrowth FROM ProjectCard",
          "7 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_2_5>",
          "OxygenStep",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
    }

    with(blue) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$SoilFactory"),
          "1 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$SoilFactory FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Plant]",
          "3 MC",
      )
      doTask("PassBaton")
    }

    green.doTask("Pass")

    with(purple) {
      doTasks(
          *playCardTasks("$Lichen"),
          "7 Pay<> FROM MC",
          "$Lichen FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$WeatherBalloons", "Action2"), "-Floater<$WeatherBalloons>", "6 MC")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$CorroderSuits"),
          "6 Pay<> FROM MC",
          "$CorroderSuits FROM ProjectCard",
          "PROD[2 MC]",
          "Animal<$VenusianAnimals>",
      )
      doTask("PassBaton")
    }

    purple.doTask("Pass")

    with(blue) {
      doTasks(*cardActionTasks("$AstroDrill", "Action2"), "-Asteroid<$AstroDrill>", "3 Titanium")
      doTasks(
          *cardActionTasks("$SecurityFleet"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Fighter<$SecurityFleet>",
      )
    }

    pink.doTask("Pass")

    with(blue) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*standardProjectTasks("SellPatentsProject"), "4 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$NoctisCity"),
          "1 Pay<Class<Steel>> FROM Steel",
          "16 Pay<> FROM MC",
          "$NoctisCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "NormalCityTile<Tharsis_5_3>",
          "NOW Pink { Production<Pink, Class<MC>>! }",
      )
      pink.doTask("PROD[MC]")
      doTasks("2 Plant", "2 MC")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_4_3>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Mangrove"),
          "PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>",
          "10 Pay<> FROM MC",
          "$Mangrove FROM ProjectCard",
          "GreeneryTile<Tharsis_4_8>",
          "OxygenStep",
          "2 Plant",
          "TerraformRating",
      )
      doTask("Pass")
    }

    // Production.
    blue.doTasks("56 MC", "3 Plant", "Steel", "2 Heat", "Heat FROM Energy", "Titanium")
    pink.doTasks("50 MC", "Steel", "7 Heat", "Energy", "Heat FROM Energy", "4 Plant")
    green.doTasks(
        "37 MC",
        "Titanium",
        "4 Energy",
        "15 Plant",
        "3 Heat FROM Energy",
        "Heat",
        "2 Steel",
    )
    purple.doTasks(
        "45 MC",
        "Steel",
        "2 Titanium",
        "8 Energy",
        "3 Heat FROM Energy",
        "6 Plant",
        "2 Heat",
    )

    // World Government.
    green.doTask("OxygenStep! BY Admin")
  }

  private fun generation8ThroughSave546() {
    // Database save 513: public state before generation 8 Research.
    blue.assertResources(m = 61, s = 1, t = 3, p = 8, e = 0, h = 10)
    blue.assertProduction(m = 18, s = 1, t = 1, p = 3, e = 0, h = 2)
    blue.assertDashMiddle(played = 21, actions = 6, tr = 38, hand = 2)
    blue.assertTags(
        but = 3,
        spt = 8,
        sct = 1,
        pot = 1,
        eat = 1,
        jot = 1,
        plt = 5,
        mit = 2,
        cit = 2,
    )
    blue.assertDashRight(events = 2, tagless = 1, cities = 4)
    pink.assertResources(m = 50, s = 1, t = 0, p = 9, e = 1, h = 16)
    pink.assertProduction(m = 20, s = 1, t = 0, p = 4, e = 1, h = 7)
    pink.assertDashMiddle(played = 22, actions = 2, tr = 30, hand = 5)
    pink.assertTags(
        but = 8,
        spt = 3,
        sct = 3,
        pot = 2,
        eat = 5,
        jot = 1,
        vet = 2,
        plt = 2,
        mit = 2,
        ant = 2,
        cit = 2,
    )
    pink.assertDashRight(events = 2, tagless = 1, cities = 2)
    green.assertResources(m = 37, s = 4, t = 3, p = 15, e = 4, h = 10)
    green.assertProduction(m = 2, s = 2, t = 1, p = 15, e = 4, h = 1)
    green.assertDashMiddle(played = 25, actions = 3, tr = 35, hand = 6)
    green.assertTags(
        but = 5,
        spt = 3,
        sct = 6,
        pot = 2,
        eat = 2,
        jot = 1,
        vet = 3,
        plt = 4,
        mit = 5,
    )
    green.assertDashRight(events = 2, tagless = 2, cities = 0)
    purple.assertResources(m = 46, s = 1, t = 4, p = 10, e = 8, h = 5)
    purple.assertProduction(m = 3, s = 1, t = 2, p = 6, e = 8, h = 2)
    purple.assertDashMiddle(played = 21, actions = 2, tr = 42, hand = 5)
    purple.assertTags(but = 6, spt = 2, sct = 2, pot = 7, vet = 1, plt = 1)
    purple.assertDashRight(events = 7, tagless = 1, cities = 1)
    assertSidebar(gen = 8, temp = 8, oxygen = 12, oceans = 9, venus = 22)

    // Research.
    blue.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")
    purple.doTask("4 ProjectCard<Selecting>")

    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    purple.doTask("-4 ProjectCard<Selecting>")
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(purple) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_2_4>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$PhysicsComplex"),
          "6 Pay<Class<Energy>> FROM Energy",
          "Science<$PhysicsComplex>",
      )
    }

    with(blue) {
      doTasks(*cardActionTasks("$SpaceElevator"), "Pay<Class<Steel>> FROM Steel", "5 MC")
      doTasks(
          *cardActionTasks("$SecurityFleet"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Fighter<$SecurityFleet>",
      )
    }

    with(pink) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_6_6>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Celebrity")
    }

    with(green) {
      doTasks(
          *playCardTasks("$UndergroundCity"),
          "PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>",
          "4 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$UndergroundCity FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[2 Steel]",
          "CityTile<Tharsis_4_1>",
          "NOW Pink { Production<Pink, Class<MC>>! }",
      )
      pink.doTask("PROD[MC]")
      doTasks("Plant", "Titanium")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_5_1>",
          "OxygenStep",
          "2 Plant",
      )
    }

    with(purple) {
      doTasks(*cardActionTasks("$Teslaract"), "PROD[-Energy]", "PROD[Plant]")
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Tharsis_2_3>",
          "NOW Pink { Production<Pink, Class<MC>>! }",
      )
      pink.doTask("PROD[MC]")
      doTask("2 MC")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$CometAiming"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$AstroDrill>",
      )
      doTasks(*cardActionTasks("$AstroDrill", "Action2"), "-Asteroid<$AstroDrill>", "3 Titanium")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$StratosphericExpedition"),
          "10 Pay<> FROM MC",
          "$StratosphericExpedition FROM ProjectCard",
          // Pink has no card that can hold floaters, so this placement adds none.
          "2 Floater",
      )
      doTask("2 SearchForCard<TagFilter<Class<VenusTag>>>").expect("2 ProjectCard")
      doTask("3 MC")
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }

    // Database save 546: after Pink plays Stratospheric Expedition and uses Development Center.
    blue.assertResources(m = 60, s = 0, t = 4, p = 8, e = 0, h = 10)
    blue.assertProduction(m = 18, s = 1, t = 1, p = 3, e = 0, h = 2)
    blue.assertDashMiddle(played = 21, actions = 2, tr = 38, hand = 4)
    blue.assertTags(
        but = 3,
        spt = 8,
        sct = 1,
        pot = 1,
        eat = 1,
        jot = 1,
        plt = 5,
        mit = 2,
        cit = 2,
    )
    blue.assertDashRight(events = 2, tagless = 1, cities = 4)
    pink.assertResources(m = 23, s = 1, t = 0, p = 2, e = 0, h = 16)
    pink.assertProduction(m = 22, s = 1, t = 0, p = 4, e = 1, h = 7)
    pink.assertDashMiddle(played = 23, actions = 1, tr = 31, hand = 9)
    pink.assertTags(
        but = 8,
        spt = 3,
        sct = 3,
        pot = 2,
        eat = 5,
        jot = 1,
        vet = 2,
        plt = 2,
        mit = 2,
        ant = 2,
        cit = 2,
    )
    pink.assertDashRight(events = 3, tagless = 1, cities = 2)
    green.assertResources(m = 29, s = 0, t = 4, p = 10, e = 4, h = 10)
    green.assertProduction(m = 2, s = 4, t = 1, p = 15, e = 2, h = 1)
    green.assertDashMiddle(played = 26, actions = 3, tr = 35, hand = 7)
    green.assertTags(
        but = 6,
        spt = 3,
        sct = 6,
        pot = 2,
        eat = 2,
        jot = 1,
        vet = 3,
        plt = 4,
        mit = 5,
        cit = 1,
    )
    green.assertDashRight(events = 2, tagless = 2, cities = 1)
    purple.assertResources(m = 25, s = 1, t = 4, p = 2, e = 2, h = 5)
    purple.assertProduction(m = 4, s = 1, t = 2, p = 7, e = 7, h = 2)
    purple.assertDashMiddle(played = 21, actions = 0, tr = 43, hand = 5)
    purple.assertTags(but = 6, spt = 2, sct = 2, pot = 7, vet = 1, plt = 1)
    purple.assertDashRight(events = 7, tagless = 1, cities = 2)
    assertSidebar(gen = 8, temp = 8, oxygen = 14, oceans = 9, venus = 22)
  }

  private fun finishGeneration8() {
    with(green) {
      doTasks(
          *playCardTasks("$CallistoPenalMines"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "12 Pay<> FROM MC",
          "$CallistoPenalMines FROM ProjectCard",
          "PROD[3 MC]",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_3_4>",
          "0 OxygenStep",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTasks(*cardActionTasks("$WeatherBalloons", "Action2"), "-Floater<$WeatherBalloons>", "9 MC")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$Research"),
          "9 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
          "3 MC",
          "Animal<$VenusianAnimals>",
          "Animal<$VenusianAnimals>",
      )
      doTasks(*playCardTasks("$Sabotage"), "$Sabotage FROM ProjectCard", "-3 Titanium<Blue>")
    }

    with(green) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_3_1>",
          "OxygenStep",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(purple) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$Grass"),
          "PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>",
          "9 Pay<> FROM MC",
          "$Grass FROM ProjectCard",
          "PROD[Plant]",
          "3 Plant",
      )
      doTasks(
          *playCardTasks("$CityParks"),
          "7 Pay<> FROM MC",
          "$CityParks FROM ProjectCard",
          "2 Plant",
      )
    }

    with(pink) {
      doTasks(
          *playCardTasks("$FreyjaBiodomes"),
          "12 Pay<> FROM MC",
          "$FreyjaBiodomes FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "3 MC",
          "2 Animal<$VenusianAnimals>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$SaturnSurfing"),
          "13 Pay<> FROM MC",
          "$SaturnSurfing FROM ProjectCard",
          "4 Floater<$SaturnSurfing>",
      )
      doTask("PassBaton")
    }

    purple.doTask("Pass")

    with(blue) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_5_8>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(
          *playCardTasks("$AstraMechanica"),
          "5 Pay<> FROM MC",
          "$AstraMechanica FROM ProjectCard",
          "Animal<$VenusianAnimals>",
          "ProjectCard FROM PlayedEvent<Class<$CeosFavoriteProject>>",
          "ProjectCard FROM PlayedEvent<Class<$Sabotage>>",
      )
      doTasks(*playCardTasks("$Sabotage"), "$Sabotage FROM ProjectCard", "-7 MC<Blue>")
    }

    with(green) {
      doTasks(*cardActionTasks("$SaturnSurfing"), "-Floater<$SaturnSurfing>", "3 MC", "MC")
      doTasks(
          *playCardTasks("$TopsoilContract"),
          "8 Pay<> FROM MC",
          "$TopsoilContract FROM ProjectCard",
          "3 Plant",
          "Disease<$PharmacyUnion>",
          "-0 MC",
      )
    }

    with(blue) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$AsteroidMining"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "26 Pay<> FROM MC",
          "$AsteroidMining FROM ProjectCard",
          "PROD[2 Titanium]",
      )
    }

    with(pink) {
      doTasks(
          *playCardTasks("$CeosFavoriteProject"),
          "$CeosFavoriteProject FROM ProjectCard",
          "Animal<$VenusianAnimals>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>", "MC")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_5_2>",
          "OxygenStep",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_3_6>",
          "OxygenStep",
          "2 MC",
      )
    }

    with(pink) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$OrbitalCleanup"), "7 MC")
      doTasks(
          *playCardTasks("$SoilEnrichment"),
          "6 Pay<> FROM MC",
          "$SoilEnrichment FROM ProjectCard",
          "-Microbe<$SulphurEatingBacteria>",
          "5 Plant",
          "Disease<$PharmacyUnion>",
          "-2 MC",
      )
    }

    blue.doTask("Pass")

    with(pink) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_8_7>",
          "OxygenStep",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(pink) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$CometForVenus"),
          "9 Pay<> FROM MC",
          "$CometForVenus FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "-4 MC<Purple>",
      )
    }

    green.doTask("Pass")

    pink.doTask("Pass")

    // Production.
    blue.doTasks("56 MC", "4 Plant", "Steel", "2 Heat", "3 Titanium")
    pink.doTasks("56 MC", "Steel", "7 Heat", "4 Plant")
    green.doTasks(
        "40 MC",
        "Titanium",
        "2 Energy",
        "15 Plant",
        "4 Heat FROM Energy",
        "Heat",
        "4 Steel",
    )
    purple.doTasks(
        "47 MC",
        "Steel",
        "2 Titanium",
        "7 Energy",
        "2 Heat FROM Energy",
        "7 Plant",
        "2 Heat",
    )

    // Database save 622: final production, before the remaining greenery placements.
    blue.assertResources(m = 58, s = 1, t = 3, p = 5, e = 0, h = 12)
    blue.assertProduction(m = 18, s = 1, t = 3, p = 4, e = 0, h = 2)
    blue.assertDashMiddle(played = 24, actions = 0, tr = 38, hand = 0)
    blue.assertTags(
        but = 3,
        spt = 9,
        sct = 1,
        pot = 1,
        eat = 1,
        jot = 2,
        plt = 7,
        mit = 2,
        cit = 2,
    )
    blue.assertDashRight(events = 2, tagless = 1, cities = 4)
    pink.assertResources(m = 56, s = 2, t = 0, p = 6, e = 0, h = 23)
    pink.assertProduction(m = 24, s = 1, t = 0, p = 4, e = 0, h = 7)
    pink.assertDashMiddle(played = 28, actions = 1, tr = 32, hand = 0)
    pink.assertTags(
        but = 8,
        spt = 3,
        sct = 6,
        pot = 2,
        eat = 5,
        jot = 1,
        vet = 3,
        plt = 3,
        mit = 2,
        ant = 2,
        cit = 2,
    )
    pink.assertDashRight(events = 5, tagless = 1, cities = 2)
    green.assertResources(m = 45, s = 4, t = 1, p = 17, e = 2, h = 15)
    green.assertProduction(m = 5, s = 4, t = 1, p = 15, e = 2, h = 1)
    green.assertDashMiddle(played = 30, actions = 1, tr = 35, hand = 0)
    green.assertTags(
        but = 6,
        spt = 4,
        sct = 6,
        pot = 2,
        eat = 4,
        jot = 3,
        vet = 3,
        plt = 4,
        mit = 6,
        cit = 1,
    )
    green.assertDashRight(events = 3, tagless = 2, cities = 1)
    purple.assertResources(m = 50, s = 2, t = 6, p = 9, e = 7, h = 9)
    purple.assertProduction(m = 4, s = 1, t = 2, p = 7, e = 7, h = 2)
    purple.assertDashMiddle(played = 21, actions = 0, tr = 43, hand = 0)
    purple.assertTags(but = 6, spt = 2, sct = 2, pot = 7, vet = 1, plt = 1)
    purple.assertDashRight(events = 7, tagless = 1, cities = 2)
    assertSidebar(gen = 8, temp = 8, oxygen = 14, oceans = 9, venus = 24)

    with(purple) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_1_3>",
          "2 MC",
          "2 MC",
      )
    }

    purple.doTask("PassBaton")

    blue.doTask("PassBaton")

    pink.doTask("PassBaton")

    with(green) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_4_2>",
          "Plant",
      )
    }

    with(green) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_8_8>",
          "2 MC",
      )
    }

    green.doTask("PassBaton")

    // Database save 626 and the final player API record: completed public boards and scores.
    blue.assertResources(m = 58, s = 1, t = 3, p = 5, e = 0, h = 12)
    blue.assertProduction(m = 18, s = 1, t = 3, p = 4, e = 0, h = 2)
    blue.assertCounts(
        0 to "ProjectCard",
        38 to "TerraformRating",
        24 to "CardFront OR PlayedEvent",
        74 to "VictoryPoint",
        0 to "Victory",
    )
    blue.assertTags(
        but = 3,
        spt = 9,
        sct = 1,
        pot = 1,
        eat = 1,
        jot = 2,
        plt = 7,
        mit = 2,
        cit = 2,
    )
    blue.assertDashRight(events = 2, tagless = 1, cities = 4)
    pink.assertResources(m = 56, s = 2, t = 0, p = 6, e = 0, h = 23)
    pink.assertProduction(m = 24, s = 1, t = 0, p = 4, e = 0, h = 7)
    pink.assertCounts(
        0 to "ProjectCard",
        32 to "TerraformRating",
        28 to "CardFront OR PlayedEvent",
        78 to "VictoryPoint",
        1 to "Victory",
    )
    pink.assertTags(
        but = 8,
        spt = 3,
        sct = 6,
        pot = 2,
        eat = 5,
        jot = 1,
        vet = 3,
        plt = 3,
        mit = 2,
        ant = 2,
        cit = 2,
    )
    pink.assertDashRight(events = 5, tagless = 1, cities = 2)
    green.assertResources(m = 47, s = 4, t = 1, p = 2, e = 2, h = 15)
    green.assertProduction(m = 5, s = 4, t = 1, p = 15, e = 2, h = 1)
    green.assertCounts(
        0 to "ProjectCard",
        35 to "TerraformRating",
        30 to "CardFront OR PlayedEvent",
        70 to "VictoryPoint",
        0 to "Victory",
    )
    green.assertTags(
        but = 6,
        spt = 4,
        sct = 6,
        pot = 2,
        eat = 4,
        jot = 3,
        vet = 3,
        plt = 4,
        mit = 6,
        cit = 1,
    )
    green.assertDashRight(events = 3, tagless = 2, cities = 1)
    purple.assertResources(m = 54, s = 2, t = 6, p = 1, e = 7, h = 9)
    purple.assertProduction(m = 4, s = 1, t = 2, p = 7, e = 7, h = 2)
    purple.assertCounts(
        0 to "ProjectCard",
        43 to "TerraformRating",
        21 to "CardFront OR PlayedEvent",
        60 to "VictoryPoint",
        0 to "Victory",
    )
    purple.assertTags(but = 6, spt = 2, sct = 2, pot = 7, vet = 1, plt = 1)
    purple.assertDashRight(events = 7, tagless = 1, cities = 2)
    assertSidebar(gen = 8, temp = 8, oxygen = 14, oceans = 9, venus = 24)
    admin.assertCounts(1 to "End", 1 to "Phase", 0 to "GameScope")

    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<Purple>") shouldBe 0
    score.net("Milestone", "VictoryPoint<Blue>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Pink>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Green>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Purple>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<Blue>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<Pink>") shouldBe 10
    score.net("FirstPlace", "VictoryPoint<Green>") shouldBe 5
    score.net("SecondPlace", "VictoryPoint<Purple>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<Blue>") shouldBe 2
    score.net("SecondPlace", "VictoryPoint<Pink>") shouldBe 2
    score.net("SecondPlace", "VictoryPoint<Green>") shouldBe 2
    score.net("GreeneryTile", "VictoryPoint<Purple>") shouldBe 5
    score.net("GreeneryTile", "VictoryPoint<Blue>") shouldBe 7
    score.net("GreeneryTile", "VictoryPoint<Pink>") shouldBe 1
    score.net("GreeneryTile", "VictoryPoint<Green>") shouldBe 7
    score.net("CityTile", "VictoryPoint<Purple>") shouldBe 9
    score.net("CityTile", "VictoryPoint<Blue>") shouldBe 9
    score.net("CityTile", "VictoryPoint<Pink>") shouldBe 1
    score.net("CityTile", "VictoryPoint<Green>") shouldBe 4
    score.net("Card", "VictoryPoint<Purple>") shouldBe 3
    score.net("Card", "VictoryPoint<Blue>") shouldBe 13
    score.net("Card", "VictoryPoint<Pink>") shouldBe 27
    score.net("Card", "VictoryPoint<Green>") shouldBe 12
  }
}
