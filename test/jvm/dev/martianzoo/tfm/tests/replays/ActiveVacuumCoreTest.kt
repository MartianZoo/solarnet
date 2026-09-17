package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete public-state replay: Active Vacuum Core (g62d89e349c97), save 626.
// Private hands, drafts, and deck order are intentionally unnamed.
// Source: _local/replays/Game20260914/Heroku-g62d89e349c97/
// http://newazure.local:8080/the-end?id=p6f50f47be7e8
internal class ActiveVacuumCoreTest : AbstractFullGameTest() {
  override val config =
      GameConfig(
          """
          TharsisMap
          VenusNextExpansion, PreludeExpansion, Prelude2CardPack, PromoCardPack
          FakeStuffBundle

          Diversifier, Mayor, Builder, Coastguard, Terran, Metallurgist
          Visionary, Landlord, Investor, Forecaster, Traveller, Celebrity
          """
      )

  override val playerClassPets =
      """
      CLASS Blue : Player { SetupPhase: 4 TerraformRating }
      CLASS Pink : Player
      CLASS Green : Player
      CLASS Purple : Player { SetupPhase: 2 TerraformRating }
      """
          .trimIndent()

  private val blue
    get() = p1

  private val pink
    get() = p2

  private val green
    get() = p3

  private lateinit var purple: TfmGameplay

  override fun commonSetup() {
    super.commonSetup()
    purple = player(4).requireExplicitPaymentChoices()
  }

  @Test
  internal fun completeGame() {
    TfmWorkflow.Automatic(agents).launch()
    retainStartingProjects(5, 6, 7, 3)
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
    blue.playCorp(AstroDrill) { buyCards(5) }
    pink.playCorp(Vitor) { buyCards(6) }
    green.playCorp(PharmacyUnion) { buyCards(7) }
    purple.playCorp(Inventrix) { buyCards(3) }

    blue.turn {
      playPrelude(VenusL1Shade)
      playPrelude(AntiDesertificationTechniques)
    }
    pink.turn {
      playPrelude(BusinessEmpire)
      playPrelude(ExcentricSponsor) { playProject(ImmigrationShuttles, 6) }
    }
    green.turn {
      playPrelude(IoResearchOutpost)
      playPrelude(FakeAppliedScience)
    }
    purple.turn {
      playPrelude(MetalsCompany)
      playPrelude(GiantSolarCollector)
    }

    blue.turn {
      cardAction2(AstroDrill)
      intentionalUnderpay()
      playProject(CometAiming, 14, titanium = 1)
    }
    pink.turn {
      stdAction("DoRequiredActionsAction") { doTask("Investor") }
      playProject(IndustrialMicrobes, 12)
    }
    green.turn { cardAction1(FakeAppliedScience) { doTask("Titanium") } }
    purple.turn {
      stdAction("DoRequiredActionsAction")
      playProject(BribedCommittee, 7)
    }
    blue.turn { cardAction1(CometAiming) { addCardResources(AstroDrill) } }
    pink.pass()
    green.turn { playProject(CuttingEdgeTechnology, 12) }
    purple.turn {
      playProject(WaterToVenus, 9)
      playProject(NeutralizerFactory, 7)
    }
    blue.pass()
    green.turn { playProject(SulphurEatingBacteria, 4) }
    purple.pass()
    green.turn {
      cardAction1(SulphurEatingBacteria)
      pass()
    }
    blue.wgt("VenusStep")
  }

  private fun generation2() {
    pink.buyCards(2)
    blue.buyCards(2)
    purple.buyCards(2)
    green.buyCards(3)

    // Database save 59: public player boards after generation 2 Research purchases.
    blue.assertResources(m = 30, s = 1, t = 1, p = 1, e = 0, h = 0)
    blue.assertProduction(m = 0, s = 1, t = 0, p = 1, e = 0, h = 0)
    blue.assertDashMiddle(played = 4, actions = 2, vp = 29, tr = 27, hand = 6)
    blue.assertTags(spt = 3, plt = 1, mit = 1)
    blue.assertDashRight(events = 0, tagless = 0, cities = 0)
    pink.assertResources(m = 34, s = 1, t = 0, p = 0, e = 1, h = 0)
    pink.assertProduction(m = 11, s = 1, t = 0, p = 0, e = 1, h = 0)
    pink.assertDashMiddle(played = 5, actions = 0, vp = 25, tr = 20, hand = 6)
    pink.assertTags(but = 1, spt = 1, eat = 3, mit = 1)
    pink.assertDashRight(events = 0, tagless = 1, cities = 0)
    green.assertResources(m = 13, s = 0, t = 2, p = 0, e = 0, h = 0)
    green.assertProduction(m = 0, s = 0, t = 1, p = 0, e = 0, h = 0)
    green.assertDashMiddle(played = 5, actions = 2, vp = 25, tr = 22, hand = 10)
    green.assertTags(sct = 2, jot = 1, vet = 1, mit = 3)
    green.assertDashRight(events = 0, tagless = 1, cities = 0)
    purple.assertResources(m = 35, s = 1, t = 1, p = 0, e = 2, h = 0)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 0, e = 2, h = 0)
    purple.assertDashMiddle(played = 6, actions = 0, vp = 27, tr = 27, hand = 6)
    purple.assertTags(spt = 1, sct = 1, pot = 1, vet = 1)
    purple.assertDashRight(events = 2, tagless = 1, cities = 0)
    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 14)

    pink.turn {
      sellPatents(1)
      playProject(Soletta, 35)
    }
    green.turn {
      green.exMachina(fakeWildTags("EarthTag"))
      playProject(Omnicourt, 9)
      green.assertCounts(0 to "FakeWildTagUse")
    }
    purple.turn { playProject(SolarPower, 9, steel = 1) }
    blue.turn {
      cardAction2(AstroDrill)
      cardAction1(CometAiming) { addCardResources(CometAiming) }
    }
    pink.pass()
    green.turn { cardAction1(SulphurEatingBacteria) }
    purple.turn { playProject(MagneticFieldDome, 5) }
    blue.turn {
      intentionalUnderpay()
      playProject(IceAsteroid, 17, titanium = 2) {
        placeTile(5, 4)
        placeTile(5, 5)
      }
    }
    green.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
    }
    purple.turn { stdProject("PowerPlantProject") }
    blue.turn {
      playProject(ArcticAlgae, 12)
      playProject(Psychrophiles, 2)
    }
    green.pass()
    purple.pass()
    blue.turn {
      cardAction1(Psychrophiles)
      pass()
    }
    pink.wgt("VenusStep")
  }

  private fun generation3() {
    purple.buyCards(3)
    blue.buyCards(3)
    green.buyCards(2)
    pink.buyCards(2)

    // Database save 111: public player boards after generation 3 Research purchases.
    blue.assertResources(m = 21, s = 2, t = 1, p = 7, e = 0, h = 0)
    blue.assertProduction(m = 0, s = 1, t = 0, p = 1, e = 0, h = 0)
    blue.assertDashMiddle(played = 7, actions = 3, vp = 31, tr = 29, hand = 6)
    blue.assertTags(spt = 3, plt = 2, mit = 2)
    blue.assertDashRight(events = 1, tagless = 0, cities = 0)
    pink.assertResources(m = 25, s = 2, t = 0, p = 0, e = 1, h = 8)
    pink.assertProduction(m = 11, s = 1, t = 0, p = 0, e = 1, h = 7)
    pink.assertDashMiddle(played = 6, actions = 0, vp = 25, tr = 20, hand = 6)
    pink.assertTags(but = 1, spt = 2, eat = 3, mit = 1)
    pink.assertDashRight(events = 0, tagless = 1, cities = 0)
    green.assertResources(m = 18, s = 0, t = 3, p = 0, e = 0, h = 0)
    green.assertProduction(m = 0, s = 0, t = 1, p = 0, e = 0, h = 0)
    green.assertDashMiddle(played = 6, actions = 2, vp = 27, tr = 24, hand = 11)
    green.assertTags(but = 1, sct = 2, jot = 1, vet = 1, mit = 3)
    green.assertDashRight(events = 0, tagless = 1, cities = 0)
    purple.assertResources(m = 30, s = 1, t = 2, p = 1, e = 2, h = 2)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 1, e = 2, h = 0)
    purple.assertDashMiddle(played = 8, actions = 0, vp = 29, tr = 28, hand = 7)
    purple.assertTags(but = 2, spt = 1, sct = 1, pot = 2, vet = 1)
    purple.assertDashRight(events = 2, tagless = 1, cities = 0)
    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 2, venus = 16)

    green.turn {
      green.exMachina(fakeWildTags("ScienceTag"))
      playProject(LightningHarvest, 6)
      green.assertCounts(0 to "FakeWildTagUse")
    }
    purple.turn {
      playProject(DeimosDownPromo, 25, titanium = 2) {
        placeTile(4, 4)
        doTask("-6 Plant<Blue>")
      }
    }
    blue.turn {
      cardAction2(CometAiming) { placeTile(5, 6) }
    }
    pink.turn { playProject(DirectedImpactors, 8) }
    green.turn {
      cardAction1(FakeAppliedScience) { doTask("Plant") }
      playProject(Moss, 2)
    }
    purple.turn {
      playProject(PowerSupplyConsortium, 5) { doTask("PROD[-Energy<Pink>]") }
    }
    blue.turn {
      cardAction2(AstroDrill)
      cardAction1(Psychrophiles)
    }
    pink.turn {
      cardAction1(DirectedImpactors) {
        pay(6)
        addCardResources(DirectedImpactors)
      }
    }
    green.turn {
      green.exMachina(fakeWildTags("EarthTag"))
      claimMilestone(cn("Diversifier"))
      green.assertCounts(0 to "FakeWildTagUse")
    }
    purple.turn { playProject(Teslaract, 4, steel = 5) }
    blue.turn {
      playProject(MiningExpedition, 12) { doTask("-2 Plant<Purple>") }
    }
    pink.pass()
    green.turn { cardAction1(SulphurEatingBacteria) }
    purple.turn { cardAction1(Teslaract) }
    blue.turn { playProject(WeatherBalloons, 11) }
    green.pass()
    purple.pass()
    blue.turn {
      cardAction1(WeatherBalloons)
      pass()
    }
    green.wgt("OxygenStep")
  }

  private fun generation4() {
    // Database save 162: the public state at the start of generation 4 drafting. This checkpoint
    // precedes Research, so it neither names nor models cards that were still private then.
    blue.assertResources(m = 31, s = 5, t = 4, p = 6, e = 0, h = 0)
    blue.assertProduction(m = 0, s = 1, t = 0, p = 1, e = 0, h = 0)
    blue.assertDashMiddle(played = 9, actions = 4, vp = 33, tr = 31, hand = 5)
    blue.assertTags(spt = 3, sct = 1, plt = 2, mit = 2)
    blue.assertDashRight(events = 2, tagless = 0, cities = 0)
    pink.assertResources(m = 42, s = 3, t = 0, p = 0, e = 0, h = 16)
    pink.assertProduction(m = 11, s = 1, t = 0, p = 0, e = 0, h = 7)
    pink.assertDashMiddle(played = 7, actions = 1, vp = 25, tr = 20, hand = 5)
    pink.assertTags(but = 1, spt = 3, eat = 3, mit = 1)
    pink.assertDashRight(events = 0, tagless = 1, cities = 0)
    green.assertResources(m = 27, s = 0, t = 4, p = 1, e = 1, h = 0)
    green.assertProduction(m = 1, s = 0, t = 1, p = 1, e = 1, h = 0)
    green.assertDashMiddle(played = 8, actions = 2, vp = 33, tr = 24, hand = 9)
    green.assertTags(but = 1, sct = 2, pot = 1, jot = 1, vet = 1, plt = 1, mit = 3)
    green.assertDashRight(events = 0, tagless = 1, cities = 0)
    purple.assertResources(m = 33, s = 1, t = 1, p = 2, e = 2, h = 5)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 2, e = 2, h = 1)
    purple.assertDashMiddle(played = 11, actions = 1, vp = 33, tr = 32, hand = 4)
    purple.assertTags(but = 3, spt = 1, sct = 1, pot = 4, vet = 1)
    purple.assertDashRight(events = 3, tagless = 1, cities = 0)
    assertSidebar(gen = 4, temp = -24, oxygen = 2, oceans = 3, venus = 16)

    pink.buyCards(2)
    blue.buyCards(3)
    purple.buyCards(1)
    green.buyCards(3)

    purple.turn {
      playProject(DeepWellHeating, 11, steel = 1)
      stdProject("AsteroidProject")
    }
    blue.turn {
      cardAction1(Psychrophiles)
      cardAction1(WeatherBalloons)
    }
    pink.turn { playProject(EarthCatapult, 23) }
    green.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
    }
    purple.pass()
    blue.turn { cardAction1(AstroDrill) { addCardResources(CometAiming) } }
    pink.turn { playProject(HomeostasisBureau, 8, steel = 3) }
    green.turn {
      playProject(VenusianPlants, 11) { addCardResources(SulphurEatingBacteria) }
    }
    blue.turn {
      playProject(NoctisFarming, steel = 5)
      convertPlants { placeTile(6, 4) }
    }
    pink.turn { convertHeat() }
    green.turn { playProject(TransNeptuneProbe, titanium = 2) }
    blue.turn { playProject(MethaneFromTitan, 16, titanium = 4) }
    pink.turn { convertHeat() }
    green.turn { cardAction2(SulphurEatingBacteria, x = 6) }
    blue.turn { playProject(TerraformingContract, 8) }
    pink.turn { cardAction2(DirectedImpactors) }
    green.turn {
      green.exMachina(fakeWildTags("EarthTag"))
      playProject(Solarnet, 5)
      green.assertCounts(0 to "FakeWildTagUse")
    }
    blue.turn { cardAction2(CometAiming) { placeTile(6, 7) } }
    pink.pass()
    green.turn { playProject(SnowAlgae, 10) }
    blue.pass()
    green.pass()
    purple.wgt("VenusStep")
  }

  private fun generation5() {
    // Database save 237: the public state at the start of generation 5 drafting. This checkpoint
    // precedes Research, so it neither names nor models cards that are still private.
    blue.assertResources(m = 40, s = 1, t = 0, p = 7, e = 0, h = 2)
    blue.assertProduction(m = 5, s = 1, t = 0, p = 3, e = 0, h = 2)
    blue.assertDashMiddle(played = 12, actions = 4, vp = 39, tr = 33, hand = 5)
    blue.assertTags(but = 1, spt = 4, sct = 1, eat = 1, jot = 1, plt = 3, mit = 2)
    blue.assertDashRight(events = 2, tagless = 0, cities = 0)
    pink.assertResources(m = 51, s = 1, t = 0, p = 0, e = 0, h = 9)
    pink.assertProduction(m = 11, s = 1, t = 0, p = 0, e = 0, h = 9)
    pink.assertDashMiddle(played = 9, actions = 1, vp = 30, tr = 23, hand = 5)
    pink.assertTags(but = 2, spt = 3, eat = 4, mit = 1)
    pink.assertDashRight(events = 0, tagless = 1, cities = 0)
    green.assertResources(m = 37, s = 0, t = 3, p = 3, e = 1, h = 2)
    green.assertProduction(m = 1, s = 0, t = 1, p = 2, e = 1, h = 1)
    green.assertDashMiddle(played = 12, actions = 2, vp = 36, tr = 26, hand = 10)
    green.assertTags(but = 1, spt = 1, sct = 3, pot = 1, jot = 1, vet = 2, plt = 3, mit = 3)
    green.assertDashRight(events = 0, tagless = 2, cities = 0)
    purple.assertResources(m = 40, s = 1, t = 2, p = 4, e = 3, h = 9)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 2, e = 3, h = 2)
    purple.assertDashMiddle(played = 12, actions = 1, vp = 33, tr = 34, hand = 4)
    purple.assertTags(but = 4, spt = 1, sct = 1, pot = 5, vet = 1)
    purple.assertDashRight(events = 3, tagless = 1, cities = 0)
    assertSidebar(gen = 5, temp = -14, oxygen = 3, oceans = 4, venus = 20)

    green.buyCards(3)
    pink.buyCards(4)
    purple.buyCards(2)
    blue.buyCards(3)

    blue.turn {
      cardAction1(Psychrophiles)
      cardAction1(WeatherBalloons)
    }
    pink.turn {
      playProject(SterlingVents, 1, steel = 1)
      playProject(ImmigrantCity, 11) { placeTile(6, 5) }
    }
    green.turn { playProject(MiningRights, 9) { placeTile(8, 4) } }
    purple.turn { convertHeat() }
    blue.turn {
      cardAction2(AstroDrill)
      cardAction1(CometAiming) { addCardResources(CometAiming) }
    }
    pink.turn { convertHeat() }
    green.turn { cardAction1(SulphurEatingBacteria) }
    purple.turn {
      playProject(Comet, 15, titanium = 2) {
        placeTile(2, 6)
        doTask("-3 Plant<Blue>")
      }
    }
    blue.turn {
      playProject(FakeResearchCoordination, 4)
      blue.exMachina(fakeWildTags("SpaceTag"))
      playProject(Satellites, 4, titanium = 2)
      blue.assertCounts(0 to "FakeWildTagUse")
    }
    pink.turn {
      cardAction1(DirectedImpactors) {
        pay(6)
        addCardResources(DirectedImpactors)
      }
    }
    green.turn { cardAction1(FakeAppliedScience) { doTask("Titanium") } }

    // Database save 282: complete public state after Green's Applied Science action. Private hands
    // are represented only by their public aggregate counts.
    blue.assertResources(m = 23, s = 1, t = 0, p = 6, e = 0, h = 2)
    blue.assertProduction(m = 11, s = 1, t = 0, p = 3, e = 0, h = 2)
    blue.assertDashMiddle(played = 14, actions = 0, vp = 39, tr = 33, hand = 6)
    blue.assertTags(but = 1, spt = 5, sct = 1, eat = 1, jot = 1, plt = 3, mit = 2)
    blue.assertDashRight(events = 2, tagless = 1, cities = 0)
    pink.assertResources(m = 31, s = 0, t = 0, p = 1, e = 0, h = 1)
    pink.assertProduction(m = 10, s = 1, t = 0, p = 0, e = 1, h = 7)
    pink.assertDashMiddle(played = 11, actions = 0, vp = 33, tr = 24, hand = 7)
    pink.assertTags(but = 4, spt = 3, pot = 1, eat = 4, mit = 1, cit = 1)
    pink.assertDashRight(events = 0, tagless = 1, cities = 1)
    green.assertResources(m = 19, s = 2, t = 4, p = 3, e = 1, h = 2)
    green.assertProduction(m = 1, s = 1, t = 1, p = 2, e = 1, h = 1)
    green.assertDashMiddle(played = 13, actions = 0, vp = 36, tr = 26, hand = 12)
    green.assertTags(but = 2, spt = 1, sct = 3, pot = 1, jot = 1, vet = 2, plt = 3, mit = 3)
    green.assertDashRight(events = 0, tagless = 2, cities = 0)
    purple.assertResources(m = 19, s = 1, t = 0, p = 4, e = 3, h = 1)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 2, e = 3, h = 2)
    purple.assertDashMiddle(played = 13, actions = 1, vp = 36, tr = 37, hand = 7)
    purple.assertTags(but = 4, spt = 1, sct = 1, pot = 5, vet = 1)
    purple.assertDashRight(events = 4, tagless = 1, cities = 0)
    assertSidebar(gen = 5, temp = -8, oxygen = 3, oceans = 5, venus = 20)

    purple.turn { playProject(PowerGrid, 18) }
    blue.turn { playProject(WavePower, 8) }
    pink.turn { playProject(FieldCappedCity, 27) { placeTile(7, 4) } }
    green.turn {
      playProject(NitrogenRichAsteroid, 19, titanium = 4) { doTask("PROD[4 Plant]") }
    }
    purple.turn { cardAction1(Teslaract) }
    blue.pass()
    pink.pass()
    green.turn { playProject(MiningArea, steel = 2) { placeTile(9, 5) } }
    purple.pass()
    green.pass()
    blue.wgt("VenusStep")
  }

  private fun generation6() {
    // Database save 310: complete public state at the start of generation 6 drafting. Research has
    // not begun, so private offers and hands remain represented only by aggregate hand counts.
    blue.assertResources(m = 59, s = 2, t = 0, p = 9, e = 1, h = 4)
    blue.assertProduction(m = 11, s = 1, t = 0, p = 3, e = 1, h = 2)
    blue.assertDashMiddle(played = 15, actions = 4, vp = 40, tr = 33, hand = 5)
    blue.assertTags(but = 1, spt = 5, sct = 1, pot = 1, eat = 1, jot = 1, plt = 3, mit = 2)
    blue.assertDashRight(events = 2, tagless = 1, cities = 0)
    pink.assertResources(m = 41, s = 1, t = 0, p = 4, e = 2, h = 8)
    pink.assertProduction(m = 13, s = 1, t = 0, p = 0, e = 2, h = 7)
    pink.assertDashMiddle(played = 12, actions = 1, vp = 34, tr = 24, hand = 6)
    pink.assertTags(but = 5, spt = 3, pot = 2, eat = 4, mit = 1, cit = 2)
    pink.assertDashRight(events = 0, tagless = 1, cities = 2)
    green.assertResources(m = 30, s = 3, t = 1, p = 9, e = 1, h = 4)
    green.assertProduction(m = 1, s = 2, t = 1, p = 6, e = 1, h = 1)
    green.assertDashMiddle(played = 15, actions = 2, vp = 39, tr = 29, hand = 10)
    green.assertTags(but = 3, spt = 1, sct = 3, pot = 1, jot = 1, vet = 2, plt = 3, mit = 3)
    green.assertDashRight(events = 1, tagless = 2, cities = 0)
    purple.assertResources(m = 39, s = 2, t = 1, p = 7, e = 8, h = 6)
    purple.assertProduction(m = 1, s = 1, t = 1, p = 3, e = 8, h = 2)
    purple.assertDashMiddle(played = 14, actions = 1, vp = 36, tr = 37, hand = 6)
    purple.assertTags(but = 4, spt = 1, sct = 1, pot = 6, vet = 1)
    purple.assertDashRight(events = 4, tagless = 1, cities = 0)
    assertSidebar(gen = 6, temp = -6, oxygen = 3, oceans = 5, venus = 22)

    blue.buyCards(3)
    pink.buyCards(2)
    green.buyCards(2)
    purple.buyCards(2)

    pink.turn { playProject(VenusianAnimals, 13) }
    green.turn { convertPlants { placeTile(9, 6) } }
    purple.turn { playProject(BusinessContacts, 7) }
    blue.turn {
      intentionalUnderpay()
      playProject(SpaceElevator, 25, steel = 1)
      cardAction1(SpaceElevator)
    }
    pink.turn {
      playProject(CeosFavoriteProject, 0) { addCardResources(DirectedImpactors) }
    }
    green.turn {
      intentionalUnderpay()
      playProject(RegoPlastics, 8, steel = 1)
    }
    purple.turn { playProject(FuelFactory, 2, steel = 2) }
    blue.turn { cardAction1(Psychrophiles) }
    pink.turn { convertHeat() }
    green.turn { playProject(CarbonNanosystems, 2, steel = 4) }
    purple.turn { playProject(GiantSpaceMirror, 14, titanium = 1) }
    blue.turn { cardAction1(AstroDrill) { doTask("Titanium") } }
    pink.turn {
      cardAction2(DirectedImpactors)
      stdProject("AsteroidProject") { placeTile(1, 2) }
    }
    green.turn {
      playProject(ArtificialPhotosynthesis, 12) { doTask("PROD[2 Energy]") }
    }
    purple.turn { playProject(SmallAsteroid, 10) { doTask("-2 Plant<Blue>") } }
    blue.turn { cardAction2(CometAiming) { placeTile(6, 8) } }
    pink.turn {
      playProject(DevelopmentCenter, 3, steel = 3)
      cardAction1(DevelopmentCenter)
    }
    green.turn { cardAction1(FakeAppliedScience) { doTask("Titanium") } }
    purple.turn { cardAction1(Teslaract) }
    blue.turn { cardAction2(WeatherBalloons) }
    pink.turn {
      playProject(JovianEmbassy, 12)
      claimMilestone(cn("Builder"))
    }
    green.turn {
      playProject(
          OrbitalCleanup,
          payment = {
            pay(titanium = 2)
            doTask("2 PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>")
          },
      )
    }
    purple.pass()
    blue.turn {
      stdProject("CityProject") { placeTile(5, 7) }
      convertPlants { placeTile(4, 6) }
    }
    pink.pass()
    green.turn {
      green.exMachina(fakeWildTags("ScienceTag"))
      cardAction1(OrbitalCleanup)
      green.assertCounts(0 to "FakeWildTagUse")
    }
    blue.turn { claimMilestone(cn("Coastguard")) }
    green.turn { cardAction1(SulphurEatingBacteria) }
    blue.turn { playProject(SecurityFleet, 9, titanium = 1) }
    green.turn { sellPatents(1) }
    blue.pass()
    green.turn {
      playProject(AdaptedLichen, 9)
      pass()
    }
    pink.wgt("OceanTile<Tharsis_1_4>")
  }

  private fun generation7() {
    // Database save 414: complete public state at the start of generation 7 drafting.
    blue.assertResources(m = 47, s = 1, t = 1, p = 12, e = 1, h = 7)
    blue.assertProduction(m = 12, s = 1, t = 1, p = 3, e = 1, h = 2)
    blue.assertDashMiddle(played = 17, actions = 6, vp = 51, tr = 35, hand = 6)
    blue.assertTags(but = 2, spt = 7, sct = 1, pot = 1, eat = 1, jot = 1, plt = 3, mit = 2)
    blue.assertDashRight(events = 2, tagless = 1, cities = 1)
    pink.assertResources(m = 43, s = 1, t = 0, p = 4, e = 2, h = 8)
    pink.assertProduction(m = 14, s = 1, t = 0, p = 0, e = 2, h = 7)
    pink.assertDashMiddle(played = 16, actions = 2, vp = 48, tr = 29, hand = 5)
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
    green.assertDashMiddle(played = 20, actions = 3, vp = 49, tr = 32, hand = 6)
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
    purple.assertDashMiddle(played = 18, actions = 1, vp = 37, tr = 38, hand = 6)
    purple.assertTags(but = 5, spt = 2, sct = 1, pot = 7, vet = 1)
    purple.assertDashRight(events = 6, tagless = 1, cities = 0)
    assertSidebar(gen = 7, temp = 2, oxygen = 5, oceans = 8, venus = 22)

    pink.buyCards(3)
    blue.buyCards(3)
    purple.buyCards(1)
    green.buyCards(3)

    green.turn {
      playProject(SubterraneanReservoir, 11) { placeTile(9, 9) }
      convertPlants { placeTile(8, 6) }
    }
    purple.turn {
      convertHeat()
      convertHeat()
    }
    blue.turn {
      playProject(
          KaguyaTech,
          payment = {
            doTask("5 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
          },
      ) {
        doTask("CityTile<Tharsis_6_4> FROM GreeneryTile<Tharsis_6_4>")
      }
    }
    pink.turn {
      cardAction2(DirectedImpactors)
      fundAward(cn("Traveller"), 14)
    }
    green.turn { playProject(SolarWindPower, 2, titanium = 3) }
    purple.turn {
      playProject(PhysicsComplex, 10, steel = 1)
      cardAction1(PhysicsComplex)
    }
    blue.turn { cardAction1(SpaceElevator) }
    pink.turn {
      playProject(Trees, 11)
      playProject(AdvancedEcosystems, 9)
    }
    green.turn {
      green.exMachina(fakeWildTags("ScienceTag"))
      cardAction1(OrbitalCleanup)
      green.assertCounts(0 to "FakeWildTagUse")
    }
    purple.turn {
      convertPlants { placeTile(4, 5) }
      stdProject("CityProject") { placeTile(3, 5) }
    }
    blue.turn {
      blue.exMachina(fakeWildTags("ScienceTag"))
      intentionalUnderpay()
      playProject(MercurianAlloys, 3)
      blue.assertCounts(0 to "FakeWildTagUse")
      stdProject("CityProject") { placeTile(3, 7) }
    }
    pink.turn {
      cardAction1(DevelopmentCenter)
      playProject(HiredRaiders, 0) { doTask("3 MC<Pink> FROM MC<Blue>") }
    }
    green.turn {
      playProject(SisterPlanetSupport, 5)
      green.exMachina(fakeWildTags("PlantTag"))
      playProject(Insects, 7)
      green.assertCounts(0 to "FakeWildTagUse")
    }
    purple.turn { cardAction1(Teslaract) }
    blue.turn {
      cardAction1(CometAiming) { addCardResources(AstroDrill) }
      convertPlants { placeTile(4, 7) }
    }
    pink.turn { playProject(SponsoredAcademies, 7) }
    green.turn {
      cardAction2(SulphurEatingBacteria, x = 2)
      green.exMachina(fakeWildTags("MicrobeTag"))
      playProject(Worms, 6)
      green.assertCounts(0 to "FakeWildTagUse")
    }
    purple.turn {
      playProject(ProtectedGrowth, 2)
      convertPlants { placeTile(2, 5) }
    }
    blue.turn { cardAction1(Psychrophiles) }
    pink.turn { playProject(SoilFactory, 5, steel = 1) }
    green.pass()
    purple.turn { playProject(Lichen, 7) }
    blue.turn { cardAction2(WeatherBalloons) }
    pink.turn {
      playProject(CorroderSuits, 6) { addCardResources(VenusianAnimals) }
    }
    purple.pass()
    blue.turn {
      cardAction2(AstroDrill)
      cardAction1(SecurityFleet)
    }
    pink.pass()
    blue.turn {
      sellPatents(1)
      sellPatents(4)
      playProject(NoctisCity, 16, steel = 1)
      convertPlants { placeTile(4, 3) }
      playProject(
          Mangrove,
          payment = {
            pay(10)
            doTask("PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
          },
      ) {
        placeTile(4, 8)
      }
      pass()
    }
    green.wgt("OxygenStep")
  }

  private fun generation8ThroughSave546() {
    blue.assertResources(m = 61, s = 1, t = 3, p = 8, e = 0, h = 10)
    blue.assertProduction(m = 18, s = 1, t = 1, p = 3, e = 0, h = 2)
    blue.assertDashMiddle(played = 21, actions = 6, vp = 60, tr = 38, hand = 2)
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
    pink.assertDashMiddle(played = 22, actions = 2, vp = 61, tr = 30, hand = 5)
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
    green.assertDashMiddle(played = 25, actions = 3, vp = 55, tr = 35, hand = 6)
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
    purple.assertDashMiddle(played = 21, actions = 2, vp = 48, tr = 42, hand = 5)
    purple.assertTags(but = 6, spt = 2, sct = 2, pot = 7, vet = 1, plt = 1)
    purple.assertDashRight(events = 7, tagless = 1, cities = 1)
    assertSidebar(gen = 8, temp = 8, oxygen = 12, oceans = 9, venus = 22)

    pink.buyCards(2)
    blue.buyCards(2)
    purple.buyCards(0)
    green.buyCards(2)

    purple.turn {
      convertPlants { placeTile(2, 4) }
      cardAction1(PhysicsComplex)
    }
    blue.turn {
      cardAction1(SpaceElevator)
      cardAction1(SecurityFleet)
    }
    pink.turn {
      convertPlants { placeTile(6, 6) }
      fundAward(cn("Celebrity"), 20)
    }
    green.turn {
      playProject(
          UndergroundCity,
          payment = {
            pay(2, steel = 4)
            doTask("PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>")
          },
      ) {
        placeTile(4, 1)
      }
      convertPlants { placeTile(5, 1) }
    }
    purple.turn {
      cardAction1(Teslaract)
      stdProject("CityProject") { placeTile(2, 3) }
    }
    blue.turn {
      cardAction1(CometAiming) { addCardResources(AstroDrill) }
      cardAction2(AstroDrill)
    }
    pink.turn {
      playProject(StratosphericExpedition, 10)
      cardAction1(DevelopmentCenter)
    }

    blue.assertResources(m = 60, s = 0, t = 4, p = 8, e = 0, h = 10)
    blue.assertProduction(m = 18, s = 1, t = 1, p = 3, e = 0, h = 2)
    blue.assertDashMiddle(played = 21, actions = 2, vp = 63, tr = 38, hand = 4)
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
    pink.assertDashMiddle(played = 23, actions = 1, vp = 71, tr = 31, hand = 9)
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
    green.assertDashMiddle(played = 26, actions = 3, vp = 57, tr = 35, hand = 7)
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
    purple.assertDashMiddle(played = 21, actions = 0, vp = 54, tr = 43, hand = 5)
    purple.assertTags(but = 6, spt = 2, sct = 2, pot = 7, vet = 1, plt = 1)
    purple.assertDashRight(events = 7, tagless = 1, cities = 2)
    assertSidebar(gen = 8, temp = 8, oxygen = 14, oceans = 9, venus = 22)
  }

  private fun finishGeneration8() {
    green.turn { playProject(CallistoPenalMines, 12, titanium = 4) }
    purple.turn { stdProject("GreeneryProject") { placeTile(3, 4) } }
    blue.turn {
      cardAction1(Psychrophiles)
      cardAction2(WeatherBalloons)
    }
    pink.turn {
      playProject(Research, 9)
      playProject(Sabotage, 0) { doTask("-3 Titanium<Blue>") }
    }
    green.turn { convertPlants { placeTile(3, 1) } }
    purple.turn { sellPatents(5) }
    blue.turn {
      playProject(
          Grass,
          payment = {
            pay(9)
            doTask("PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
          },
      )
      playProject(CityParks, 7)
    }
    pink.turn {
      playProject(FreyjaBiodomes, 12) { addCardResources(VenusianAnimals, 2) }
    }
    green.turn {
      green.exMachina(fakeWildTags("EarthTag"))
      playProject(SaturnSurfing, 13)
      green.assertCounts(0 to "FakeWildTagUse")
    }
    purple.pass()
    blue.turn { convertPlants { placeTile(5, 8) } }
    pink.turn {
      playProject(AstraMechanica, 5) {
        doWithoutAutoExec(pink) {
          doTask("ProjectCard FROM PlayedEvent<Class<$CeosFavoriteProject>>")
          doTask("ProjectCard FROM PlayedEvent<Class<$Sabotage>>")
        }
      }
      playProject(Sabotage, 0) { doTask("-7 MC<Blue>") }
    }
    green.turn {
      cardAction1(SaturnSurfing)
      playProject(TopsoilContract, 8)
    }
    blue.turn {
      sellPatents(1)
      playProject(AsteroidMining, 26, titanium = 1)
    }
    pink.turn {
      playProject(CeosFavoriteProject, 0) { addCardResources(VenusianAnimals) }
    }
    green.turn { cardAction1(SulphurEatingBacteria) }
    blue.turn {
      stdProject("GreeneryProject") { placeTile(5, 2) }
      convertPlants { placeTile(3, 6) }
    }
    pink.turn { sellPatents(1) }
    green.turn {
      green.exMachina(fakeWildTags("ScienceTag"))
      cardAction1(OrbitalCleanup)
      green.assertCounts(0 to "FakeWildTagUse")
      playProject(SoilEnrichment, 6)
    }
    blue.pass()
    pink.turn { sellPatents(1) }
    green.turn { convertPlants { placeTile(8, 7) } }
    pink.turn { sellPatents(1) }
    green.turn { sellPatents(5) }
    pink.turn {
      sellPatents(3)
      playProject(CometForVenus, 9) { doTask("-4 MC<Purple>") }
    }
    green.pass()
    pink.pass()

    blue.assertResources(m = 58, s = 1, t = 3, p = 5, e = 0, h = 12)
    blue.assertProduction(m = 18, s = 1, t = 3, p = 4, e = 0, h = 2)
    blue.assertDashMiddle(played = 24, actions = 0, vp = 73, tr = 38, hand = 0)
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
    pink.assertDashMiddle(played = 28, actions = 1, vp = 78, tr = 32, hand = 0)
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
    green.assertDashMiddle(played = 30, actions = 1, vp = 67, tr = 35, hand = 0)
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
    purple.assertDashMiddle(played = 21, actions = 0, vp = 58, tr = 43, hand = 0)
    purple.assertTags(but = 6, spt = 2, sct = 2, pot = 7, vet = 1, plt = 1)
    purple.assertDashRight(events = 7, tagless = 1, cities = 2)
    assertSidebar(gen = 8, temp = 8, oxygen = 14, oceans = 9, venus = 24)

    purple.convertPlants { placeTile(1, 3) }
    purple.declineTask()
    blue.declineTask()
    pink.declineTask()
    green.convertPlants { placeTile(4, 2) }
    green.convertPlants { placeTile(8, 8) }
    green.declineTask()

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
    admin.assertCounts(1 to "End", 1 to "Phase")

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
