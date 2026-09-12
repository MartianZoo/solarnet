package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete archive replay: Massive Gamma Zone (gf165e91f6977), solo win in generation 12.
// Sources: all database saves for gf165e91f6977 in
// _local/terraforming-mars-solarnet/db/game.db,
// _local/replays/Game20260911/full-log-p662f1cec186f.txt,
// _local/replays/Game20260911/player-p662f1cec186f.json, and
// _local/replays/Game20260911/end-page-p662f1cec186f.html
// http://newazure.local:8080/the-end?id=p662f1cec186f
internal class MassiveGammaZoneTest : AbstractSoloTest() {
  override val config =
      GameConfig(
          """
          TharsisMap
          PreludeExpansion, Prelude2Expansion, PromoCardPack, TurmoilExpansion
          Tr63SoloObjective
          FakeStuffBundle
          """,
          "EK",
      )

  override fun cityAreas(): Pair<String, String> = "Tharsis_2_2" to "Tharsis_5_8"

  override fun greeneryAreas(): Pair<String, String> = "Tharsis_3_3" to "Tharsis_5_9"

  override fun resolveExpansionSetupTasks() {
    admin.doTask("SponsoredProjects")
    admin.doTask("SolarnetShutdown")
  }

  @Test
  internal fun massiveGammaZone() {
    retainStartingProjects(5)
    with(me.requireExplicitUnusedActionCards()) {
      // Solo setup drew these cards solely to choose the four neutral tile areas.
      discardProjectCardsFromDeck(Tardigrades, LunarBeam, CallistoPenalMines, Decomposers)
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
    }
  }

  private fun generation1() {
    with(me) {
      expectProjectCards(
          MeatIndustry,
          MarsNomads,
          RadSuits,
          RobotPollinators,
          MassConverter,
          SpaceMirrors,
          ImportedNutrients,
          PowerGrid,
          Mine,
          Ants,
      )
      playCorp(PolderTechDutch) {
        buyCards(MarsNomads, RobotPollinators, SpaceMirrors, MeatIndustry, Mine)
      }
      discardUnselectedProjectCards(RadSuits, MassConverter, ImportedNutrients, PowerGrid, Ants)

      playPrelude(BoardOfDirectors)
      playPrelude(ProjectEden) {
        doTask("OceanTile<Tharsis_2_6>")
        draw(BribedCommittee, GhgFactories)
        doTask("CityTile<Tharsis_4_6>")
        doTask("GreeneryTile<Tharsis_3_6>")
        me.discard(MarsNomads, SpaceMirrors, MeatIndustry)
      }

      stdAction("DoRequiredActionsAction") {
        doTask("OceanTile<Tharsis_4_8>")
        doTask("GreeneryTile<Tharsis_4_7>")
      }
      cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        playPrelude(FakeAppliedScience)
      }
      playProject(BribedCommittee, 7)
      cardAction1(FakeAppliedScience) { doTask("Plant") }
      playProject(Mine, 4)
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Greens> FROM ReserveDelegate")
      }
      pass()
      admin.doTask("HomeworldSupport")
    }
  }

  private fun generation2() {
    with(me) {
      buyCards(0)
      stdAction("UseTurmoilPolicyAction") {
        draw(SpecialDesign, InventionContest, Meltworks)
      }
      cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        playPrelude(CorporateArchives) { draw(TitaniumMine, MediaGroup) }
      }
      cardAction1(FakeAppliedScience) { doTask("Steel") }
      playProject(TitaniumMine, mc = 3, steel = 2)
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Reds> FROM ReserveDelegate")
      }
      pass()
      draw(Hospitals)
      admin.doTask("SuccessfulOrganisms")
    }
  }

  private fun generation3() {
    with(me) {
      buyCards(ArcticAlgae, UndergroundDetonations)
      cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        playPrelude(RiseToPower) {
          doTask("PlaceReserveDelegate<Reds>")
          doTask("PlaceReserveDelegate<Unity>")
          doTask("PlaceReserveDelegate<Scientists>")
        }
      }
      playProject(ArcticAlgae, 12)
      convertPlants { placeTile(3, 7) }
      cardAction1(FakeAppliedScience) { doTask("Steel") }
      playProject(UndergroundDetonations, steel = 3)
      cardAction1(UndergroundDetonations)
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Unity> FROM ReserveDelegate")
      }
      pass()
      admin.doTask("DryDeserts")
    }
  }

  private fun generation4() {
    with(me) {
      buyCards(PoliticalAlliance, Algae)
      playProject(MediaGroup, 6)
      playProject(InventionContest, 2) { draw(AstraMechanica) }
      cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        playPrelude(AntiDesertificationTechniques)
      }
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Greens> FROM ReserveDelegate")
      }
      cardAction1(FakeAppliedScience) { addCardResources(BoardOfDirectors) }
      pass(unused = UndergroundDetonations)
      admin.doTask("AquiferReleasedByPublicCouncil")
    }
  }

  private fun generation5() {
    with(me) {
      buyCards(MercurianAlloys, IceAsteroid, AsteroidCard)
      cardAction1(FakeAppliedScience) { doTask("Titanium") }
      // The Applied Science wild tag supplies the second science tag required by Mercurian Alloys.
      exMachina(fakeWildTags("ScienceTag"))
      playProject(MercurianAlloys, 3)
      playProject(AsteroidCard, mc = 4, titanium = 2) {
        declineTask() // Decline the optional plant removal.
      }
      playProject(IceAsteroid, mc = 3, titanium = 4) {
        placeTile(5, 6)
        placeTile(5, 5)
      }
      playProject(PoliticalAlliance, 4)
      playProject(SpecialDesign, 4)
      playProject(Algae, 10)
      convertPlants { placeTile(4, 5) }
      cardAction1(BoardOfDirectors) {
        doTask("-PreludeCard")
        me.discardProjectCardsFromDeck(IndustrialComplex)
      }
      // Applied Science's wild tag is the fifth Plant tag counted by Robot Pollinators.
      exMachina(fakeWildTags("PlantTag"))
      playProject(RobotPollinators, 9)
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Unity> FROM ReserveDelegate")
      }
      playProject(AstraMechanica, 7) {
        doWithoutAutoExec(me) {
          doTask("ProjectCard FROM PlayedEvent<Class<$BribedCommittee>>")
          returnToHand(BribedCommittee)
          doTask("ProjectCard FROM PlayedEvent<Class<$PoliticalAlliance>>")
          returnToHand(PoliticalAlliance)
        }
      }
      playProject(PoliticalAlliance, 4)
      playProject(BribedCommittee, 7)
      pass(unused = UndergroundDetonations)
      admin.doTask("SnowCover")
    }
  }

  private fun generation6() {
    with(me) {
      buyCards(RestrictedArea, RegoPlastics)
      cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        playPrelude(Merger) { playCorp(LakefrontResorts) }
      }
      cardAction1(FakeAppliedScience) { addCardResources(BoardOfDirectors) }
      stdProject("AquiferProject") { placeTile(6, 7) }
      playProject(RestrictedArea, 11) { placeTile(6, 6) }
      cardAction1(RestrictedArea) { draw(Hackers) }
      convertHeat()
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Greens> FROM ReserveDelegate")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Greens> FROM ReserveDelegate")
      }
      sellPatents(Hospitals, Hackers)
      cardAction1(UndergroundDetonations)
      pass()
      val previousAdminPolicy = admin.autoExecPolicy
      admin.autoExecPolicy = NONE
      try {
        doTask("-OceanTile<Tharsis_2_6>")
        val resourceChoices = game.tasks.extract { it }
        doTask("Titanium", resourceChoices[0].id)
        doTask("Heat", resourceChoices[1].id)
        admin.doTask("GenerousFunding")
      } finally {
        admin.autoExecPolicy = previousAdminPolicy
      }
    }
  }

  private fun generation7() {
    with(me) {
      buyCards(BiomassCombustors, StJosephOfCupertinoMission, HeatTrappers)
      convertHeat()
      cardAction1(RestrictedArea) { draw(EarthOffice) }
      cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        playPrelude(TerraformingDeal)
      }
      convertPlants { placeTile(3, 5) }
      stdProject("AquiferProject") { placeTile(5, 4) }
      convertPlants { placeTile(4, 4) }
      // Payment reconstruction: database saves 74–75 preserve all 9 steel and spend 10 M€.
      intentionalUnderpay()
      playProject(RegoPlastics, 10)
      playProject(BiomassCombustors, mc = 1, steel = 1) {
        // The archive's solo implementation omitted the mandatory plant-production reduction.
        // Drop only that unsupported effect so the replay preserves the sourced action and state.
        me.dropTask(game.tasks.extract { it }.single().id)
      }
      playProject(GhgFactories, steel = 4)
      playProject(HeatTrappers, steel = 2) {
        // As with Biomass Combustors, the archive omitted the solo production reduction.
        me.dropTask(game.tasks.extract { it }.single().id)
      }
      convertPlants { placeTile(6, 5) }
      playProject(StJosephOfCupertinoMission, 7)
      cardAction1(StJosephOfCupertinoMission) {
        pay(mc = 2, steel = 1)
        doTask("Cathedral<CityTile<Tharsis_4_6>>")
        doTask("UseAction<CathedralOption, Action1>")
        pay(2)
        me.draw(SearchForLife)
      }
      cardAction1(UndergroundDetonations)
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Scientists> FROM ReserveDelegate")
      }
      convertPlants { placeTile(5, 7) }
      convertPlants { placeTile(6, 4) }
      stdProject("AsteroidProject")
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Unity> FROM ReserveDelegate")
      }
      pass(unused = FakeAppliedScience)
      val previousAdminPolicy = admin.autoExecPolicy
      admin.autoExecPolicy = NONE
      try {
        doTask("OceanTile<Tharsis_1_5> BY Admin")
        admin.doTask("MudSlides")
      } finally {
        admin.autoExecPolicy = previousAdminPolicy
      }
    }
  }

  private fun generation8() {
    with(me) {
      buyCards(GiantIceAsteroid, CulturalMetropolis)
      stdAction("UseTurmoilPolicyAction") {
        draw(MineralDeposit, CuttingEdgeTechnology, ImportedNitrogen)
      }
      cardAction1(RestrictedArea) { draw(SpaceElevator) }
      playProject(MineralDeposit, 5)
      playProject(CulturalMetropolis, mc = 2, steel = 6) {
        placeTile(7, 5)
        doTask("PlaceReserveDelegate<Greens>")
      }
      cardAction1(StJosephOfCupertinoMission) {
        pay(mc = 2, steel = 1)
        doTask("Cathedral<CityTile<Tharsis_7_5>>")
        doTask("UseAction<CathedralOption, Action1>")
        pay(2)
        me.draw(SubterraneanReservoir)
      }
      cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        playPrelude(FocusedOrganization) {
          draw(Archaebacteria)
          doTask("Titanium")
        }
      }
      cardAction1(FocusedOrganization) {
        discard(SearchForLife)
        doTask("-MC")
        draw(SolarLogistics)
        doTask("Titanium")
      }
      convertHeat()
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Kelvinists> FROM ReserveDelegate")
      }
      cardAction1(FakeAppliedScience) { doTask("Titanium") }
      // Payment reconstruction: database saves 100–101 spend 18 M€ and 3 of 4 steel.
      intentionalUnderpay()
      playProject(SpaceElevator, mc = 18, steel = 3)
      cardAction1(SpaceElevator)
      playProject(Archaebacteria, 6)
      convertPlants {
        placeTile(8, 6)
        draw(EnergyTapping)
      }
      pass(unused = UndergroundDetonations)
      draw(HomeostasisBureau, NoctisCity)
      val previousAdminPolicy = admin.autoExecPolicy
      admin.autoExecPolicy = NONE
      try {
        admin.doTask("VolcanicEruptions")
      } finally {
        admin.autoExecPolicy = previousAdminPolicy
      }
    }
  }

  private fun generation9() {
    with(me) {
      buyCards(KelpFarming, GhgShipment)
      cardAction1(SpaceElevator)
      cardAction1(RestrictedArea) { draw(RedAppeasement) }
      playProject(HomeostasisBureau, mc = 13, steel = 1)
      convertHeat()
      convertHeat()
      playProject(EarthOffice, 1)
      // Payment reconstruction: database saves 113–114 spend 5 M€ and 3 titanium.
      intentionalUnderpay()
      playProject(SolarLogistics, mc = 5, titanium = 3)
      playProject(GhgShipment, 3) { draw(SmallAsteroid) }
      playProject(KelpFarming, 17)
      stdProject("CityProject") { placeTile(4, 3) }
      cardAction1(StJosephOfCupertinoMission) {
        pay(5)
        doTask("Cathedral<CityTile<Tharsis_4_3>>")
        doTask("UseAction<CathedralOption, Action1>")
        pay(2)
        me.draw(WaterSplittingPlant)
      }
      cardAction1(FocusedOrganization) {
        discard(RedAppeasement)
        doTask("-MC")
        draw(OpenCity)
        doTask("Plant")
      }
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Kelvinists> FROM ReserveDelegate")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<MarsFirst> FROM ReserveDelegate")
      }
      pass(unused = setOf(BoardOfDirectors, FakeAppliedScience, UndergroundDetonations))
      admin.doTask("EcoSabotage")
    }
  }

  private fun generation10() {
    with(me) {
      buyCards(TechnologyDemonstration, InvestmentLoan, Capital)
      playProject(TechnologyDemonstration, titanium = 1) {
        draw(Supercapacitors, IoMiningIndustries)
        draw(SaturnSurfing)
      }
      cardAction1(SpaceElevator)
      cardAction1(RestrictedArea) { draw(EventAnalysts) }
      playProject(GiantIceAsteroid, mc = 1, titanium = 7) {
        draw(RedShips)
        placeTile(2, 6)
        draw(SterlingVents, JovianEnvoys)
        placeTile(1, 4)
        draw(CloudSeeding)
        declineTask() // Decline the optional plant removal.
      }
      playProject(SterlingVents, mc = 2, steel = 1)
      cardAction1(FocusedOrganization) {
        discard(IoMiningIndustries)
        doTask("-MC")
        draw(ProtectedGrowth)
        doTask("Titanium")
      }
      playProject(SmallAsteroid, titanium = 2) {
        draw(NuclearPower)
        declineTask() // Decline the optional plant removal.
      }
      // Applied Science's wild tag is the eighth Earth tag counted by Saturn Surfing.
      exMachina(fakeWildTags("EarthTag"))
      playProject(SaturnSurfing, 8)
      cardAction1(SaturnSurfing)
      convertHeat()
      convertHeat()
      playProject(Capital, 26) { placeTile(2, 5) }
      cardAction1(StJosephOfCupertinoMission) {
        pay(5)
        doTask("Cathedral<CityTile<Tharsis_2_5>>")
        doTask("UseAction<CathedralOption, Action1>")
        pay(2)
        me.draw(ViralEnhancers)
      }
      playProject(RedShips, 2)
      stdProject("CityProject") { placeTile(6, 3) }
      playProject(NoctisCity, 18)
      // Applied Science's wild tag supplies the second Jovian tag for this play.
      exMachina(fakeWildTags("JovianTag"))
      playProject(JovianEnvoys, 2) {
        doTask("PlaceReserveDelegate<Greens>")
      }
      sellPatents(SubterraneanReservoir, WaterSplittingPlant, ProtectedGrowth)
      cardAction1(RedShips)
      playProject(CuttingEdgeTechnology, 12)
      playProject(CloudSeeding, 9) {
        // The archive again omitted a solo-only production target.
        me.dropTask(game.tasks.extract { it }.single().id)
      }
      stdProject("AsteroidProject")
      stdProject("AsteroidProject")
      playProject(InvestmentLoan, 0)
      stdProject("AsteroidProject")
      stdProject("AsteroidProject") { placeTile(9, 9) }
      playProject(ImportedNitrogen, mc = 3, titanium = 3) { draw(AquiferPumping) }
      pass(unused = setOf(BoardOfDirectors, FakeAppliedScience, UndergroundDetonations))
      admin.doTask("SabotageGlobalEvent")
    }
  }

  private fun generation11() {
    with(me) {
      buyCards(Farming, EarthCatapult, Recruitment, LavaTubeSettlement)
      cardAction1(RestrictedArea) { draw(MethaneFromTitan) }
      cardAction1(SpaceElevator)
      cardAction1(SaturnSurfing)
      cardAction1(StJosephOfCupertinoMission) {
        pay(mc = 2, steel = 1)
        doTask("Cathedral<CityTile<Tharsis_5_3>>")
        doTask("UseAction<CathedralOption, Action1>")
        pay(2)
        me.draw(BusinessContacts)
      }
      playProject(EarthCatapult, 18)
      playProject(BusinessContacts, 0) { draw(TundraFarming, StanfordTorus) }
      cardAction1(FocusedOrganization) {
        discard(Supercapacitors)
        doTask("-Heat")
        draw(TectonicStressPower)
        doTask("Steel")
      }
      convertHeat()
      convertHeat()
      playProject(Meltworks, 2)
      cardAction1(Meltworks)
      playProject(TectonicStressPower, mc = 2, steel = 4)
      playProject(LavaTubeSettlement, 13) {
        placeTile(3, 1)
        draw(GiantSpaceMirror)
      }
      convertPlants { placeTile(5, 2) }
      convertPlants { placeTile(4, 2) }
      convertPlants { placeTile(3, 2) }
      convertPlants { placeTile(4, 1) }
      convertPlants { placeTile(6, 2) }
      convertPlants { placeTile(7, 4) }
      convertPlants { placeTile(2, 4) }
      playProject(OpenCity, 19) { placeTile(5, 1) }
      playProject(ViralEnhancers, 7)
      playProject(Farming, 12)
      playProject(TundraFarming, 12)
      convertPlants { placeTile(7, 3) }
      stdProject("AsteroidProject")
      stdProject("AsteroidProject")
      cardAction1(RedShips)
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Greens> FROM ReserveDelegate")
      }
      stdProject("CityProject") { placeTile(3, 4) }
      pass(unused = setOf(BoardOfDirectors, FakeAppliedScience, UndergroundDetonations))
      admin.doTask("GlobalDustStorm")
    }
  }

  private fun generation12() {
    with(me) {
      buyCards(AntiGravityTechnology, MedicalLab, CommercialDistrict)
      cardAction1(RestrictedArea) { draw(BioPrintingFacility) }
      cardAction1(SpaceElevator)
      cardAction1(Meltworks)
      cardAction1(SaturnSurfing)
      cardAction1(StJosephOfCupertinoMission) {
        pay(mc = 2, steel = 1)
        doTask("Cathedral<CityTile<Tharsis_6_3>>")
        doTask("UseAction<CathedralOption, Action1>")
        pay(2)
        me.draw(FrontierTown)
      }
      playProject(Recruitment, 0) {
        doTask("RecruitmentExchange<Reds>")
        doTask("ReserveDelegate<Neutral> FROM PartyDelegate<Reds, Neutral>")
      }
      stdAction("LobbyAction", 1) {
        doTask("PartyDelegate<Reds> FROM ReserveDelegate")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists> FROM ReserveDelegate")
      }
      stdAction("LobbyAction", 2) {
        doTask("PartyDelegate<Scientists> FROM ReserveDelegate")
      }
      playProject(EventAnalysts, 1)
      // Applied Science's wild tag supplies the seventh science tag.
      exMachina(fakeWildTags("ScienceTag"))
      playProject(AntiGravityTechnology, 10)
      cardAction1(FocusedOrganization) {
        discard(AquiferPumping)
        doTask("-Heat")
        draw(CarbonateProcessing)
        doTask("Steel")
      }
      playProject(CommercialDistrict, steel = 4) { placeTile(2, 1) }
      stdProject("CityProject") { placeTile(8, 4) }
      convertPlants { placeTile(9, 5) }
      playProject(MedicalLab, steel = 3)
      playProject(MethaneFromTitan, mc = 2, titanium = 5)
      stdProject("CityProject") {
        placeTile(8, 7)
        draw(SolarWindPower)
      }
      sellPatents(
          EnergyTapping,
          NuclearPower,
          GiantSpaceMirror,
          BioPrintingFacility,
          FrontierTown,
          CarbonateProcessing,
          SolarWindPower,
      )
      cardAction1(RedShips)
      playProject(StanfordTorus, 8)
      stdProject("CityProject") { placeTile(9, 6) }
      convertPlants { placeTile(8, 5) }
      stdProject("GreeneryProject") { placeTile(7, 6) }
      pass(unused = setOf(BoardOfDirectors, FakeAppliedScience, UndergroundDetonations))

      // Player-record evidence: production completed and final greenery placement is pending.
      assertResources(m = 124, s = 4, t = 2, p = 19, e = 0, h = 27)
      assertProduction(m = 50, s = 2, t = 2, p = 15, e = 0, h = 18)
      assertCounts(63 to "TerraformRating")
      assertSidebar(gen = 12, temp = 8, oxygen = 14, oceans = 9)

      convertPlants { placeTile(9, 7) }
      convertPlants { placeTile(2, 3) }
      declineTask() // Decline another final greenery with only 5 plants remaining.

      assertCardTrackingComplete()
      cardsHand shouldBe emptySet()
      checkHandSizes()
      admin.assertCounts(1 to "End", 1 to "Phase")

      // Player-record evidence: complete final dashboard and score table.
      assertResources(m = 124, s = 4, t = 2, p = 5, e = 0, h = 27)
      assertProduction(m = 50, s = 2, t = 2, p = 15, e = 0, h = 18)
      assertCounts(0 to "ProjectCard", 70 to "CardFront OR PlayedEvent")
      assertDashRight(events = 16, tagless = 8, cities = 13)
      assertTags(
          but = 20,
          spt = 5,
          sct = 8,
          pot = 4,
          eat = 8,
          jot = 2,
          plt = 7,
          mit = 3,
          ant = 0,
          cit = 7,
      )
      assertCardResources(5 to SaturnSurfing)

      // Player-record map evidence: all 38 owned tiles and all nine oceans.
      assertCounts(
          38 to "OwnedTile",
          23 to "GreeneryTile",
          13 to "CityTile",
          2 to "SpecialTile",
          1 to "GreeneryTile<Tharsis_2_3>",
          1 to "GreeneryTile<Tharsis_2_4>",
          1 to "GreeneryTile<Tharsis_3_2>",
          1 to "GreeneryTile<Tharsis_3_5>",
          1 to "GreeneryTile<Tharsis_3_6>",
          1 to "GreeneryTile<Tharsis_3_7>",
          1 to "GreeneryTile<Tharsis_4_1>",
          1 to "GreeneryTile<Tharsis_4_2>",
          1 to "GreeneryTile<Tharsis_4_4>",
          1 to "GreeneryTile<Tharsis_4_5>",
          1 to "GreeneryTile<Tharsis_4_7>",
          1 to "GreeneryTile<Tharsis_5_2>",
          1 to "GreeneryTile<Tharsis_5_7>",
          1 to "GreeneryTile<Tharsis_6_2>",
          1 to "GreeneryTile<Tharsis_6_4>",
          1 to "GreeneryTile<Tharsis_6_5>",
          1 to "GreeneryTile<Tharsis_7_3>",
          1 to "GreeneryTile<Tharsis_7_4>",
          1 to "GreeneryTile<Tharsis_7_6>",
          1 to "GreeneryTile<Tharsis_8_5>",
          1 to "GreeneryTile<Tharsis_8_6>",
          1 to "GreeneryTile<Tharsis_9_5>",
          1 to "GreeneryTile<Tharsis_9_7>",
          1 to "CityTile<Tharsis_2_5>",
          1 to "CityTile<Tharsis_3_1>",
          1 to "CityTile<Tharsis_3_4>",
          1 to "CityTile<Tharsis_4_3>",
          1 to "CityTile<Tharsis_4_6>",
          1 to "CityTile<Tharsis_5_1>",
          1 to "CityTile<Tharsis_5_3>",
          1 to "CityTile<Tharsis_6_3>",
          1 to "CityTile<Tharsis_7_5>",
          1 to "CityTile<Tharsis_8_4>",
          1 to "CityTile<Tharsis_8_7>",
          1 to "CityTile<Tharsis_9_6>",
          1 to "CityTile<RemoteArea>",
          1 to "SpecialTile<Tharsis_2_1>",
          1 to "SpecialTile<Tharsis_6_6>",
      )
      admin.assertCounts(
          9 to "OceanTile",
          1 to "OceanTile<Tharsis_1_4>",
          1 to "OceanTile<Tharsis_1_5>",
          1 to "OceanTile<Tharsis_2_6>",
          1 to "OceanTile<Tharsis_4_8>",
          1 to "OceanTile<Tharsis_5_4>",
          1 to "OceanTile<Tharsis_5_5>",
          1 to "OceanTile<Tharsis_5_6>",
          1 to "OceanTile<Tharsis_6_7>",
          1 to "OceanTile<Tharsis_9_9>",
      )

      admin.assertCounts(
          1 to "Current<Class<EcoSabotage>>",
          1 to "Coming<Class<SabotageGlobalEvent>>",
          1 to "Distant<Class<GlobalDustStorm>>",
          1 to "Ruling<Kelvinists>",
          1 to "Dominant<Reds>",
          1 to "Chairman<EK>",
      )
      assertCounts(
          1 to "PartyDelegate<MarsFirst>",
          2 to "PartyDelegate<Scientists>",
          0 to "PartyDelegate<Unity>",
          1 to "PartyDelegate<Greens>",
          2 to "PartyDelegate<Reds>",
          0 to "PartyDelegate<Kelvinists>",
          0 to "LobbyActionAvailable",
          0 to "ReserveDelegate",
      )

      val score = Summarizer(game)
      score.net("TerraformRating", "VictoryPoint<EK>") shouldBe 63
      score.net("GreeneryTile", "VictoryPoint<EK>") shouldBe 23
      score.net("CityTile", "VictoryPoint<EK>") shouldBe 49
      score.net("Card", "VictoryPoint<EK>") shouldBe 31
      score.net("PartyLeader", "VictoryPoint<EK>") shouldBe 4
      score.net("Chairman", "VictoryPoint<EK>") shouldBe 1
      score.net("Milestone", "VictoryPoint<EK>") shouldBe 0
      score.net("FirstPlace", "VictoryPoint<EK>") shouldBe 0
      assertCounts(63 to "TerraformRating", 171 to "VictoryPoint", 1 to "Victory")
    }
  }
}
