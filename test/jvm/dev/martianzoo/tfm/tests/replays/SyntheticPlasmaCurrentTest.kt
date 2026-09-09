package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.canonicalCatalog
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// One card in this archive is not yet canonical Solarnet content. This replay-local Pets form
// states its recorded behavior.
private val syntheticPlasmaCurrentCards =
    parseClasses(
            """
            CLASS RedTourismWave : EventCard<Class<ProjectCard>> {
              cost = 3
              requirement = HAS "Ruling<Reds> OR 2 PartyDelegate<Reds>"
              This:: EarthTag<This>, EventTag<This>
              This: 8 MC
            }
            """
        )
        .toSet()

// Complete archive replay: Synthetic Plasma Current (g5625e0252c7c), solo win in generation 12.
// Source: _local/replays/Game20260908/game-g5625e0252c7c.sqlite and full-log.txt
// http://newazure.local:8080/the-end?id=p145be84b9959
internal class SyntheticPlasmaCurrentTest : AbstractSoloTest() {
  override val config =
      GameConfig(
          """
          TharsisMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, PromoCardPack, TurmoilExpansion
          Tr63SoloObjective
          RedTourismWave
          """,
          "Bloo",
      )

  override val catalog: TfmCatalog by lazy {
    TfmCatalog.compose(
        canonicalCatalog(config),
        object : TfmCatalog() {
          override val explicitClassDeclarations = syntheticPlasmaCurrentCards
        },
    )
  }

  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  override fun cityAreas(): Pair<String, String> = "Tharsis_2_4" to "Tharsis_8_9"

  override fun greeneryAreas(): Pair<String, String> = "Tharsis_3_5" to "Tharsis_7_9"

  override fun resolveExpansionSetupTasks() {
    admin.doTask("Riots")
    admin.doTask("Revolution")
  }

  @Test
  internal fun syntheticPlasmaCurrent() {
    retainStartingProjects(5)
    with(me) {
      // Solo setup drew these cards solely to choose the four neutral tile areas.
      discardProjectCardsFromDeck(StanfordTorus, Virus, CloudSeeding, ImmigrantCity)
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
          LakeMarineris,
          TowingAComet,
          Moss,
          TitaniumMine,
          Algae,
          StratosphericBirds,
          Windmills,
          BuildingIndustries,
          ExtractorBalloons,
          Vermin,
      )
      playCorp(Phobolog) {
        buyCards(TowingAComet, Moss, TitaniumMine, Algae, ExtractorBalloons)
      }
      discardUnselectedProjectCards(
          LakeMarineris,
          StratosphericBirds,
          Windmills,
          BuildingIndustries,
          Vermin,
      )

      playPrelude(AcquiredSpaceAgency) {
        discardProjectCardsFromDeck(
            RegolithEaters,
            Potatoes,
            LuxuryFoods,
            KaguyaTech,
            Sponsors,
            GeothermalPower,
            InvestmentLoan,
            AerialLenses,
            VoteOfNoConfidence,
            CorroderSuits,
        )
        draw(AsteroidMining, Comet)
      }
      playPrelude(CorridorsOfPower)
      playProject(AsteroidMining, mc = 2, titanium = 7)
      draw(LocalShading)

      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Scientists> FROM LobbyDelegate")
        draw(GhgFactories)
      }
      stdAction("SendDelegateSA", 2) {
        doTask("PartyDelegate<Reds> FROM ReserveDelegate")
      }
      playProject(TowingAComet, mc = 3, titanium = 5) {
        placeTile(2, 6)
        draw(DawnCity, RedTourismWave)
      }
      pass()
      // WGT forms government and advances the two visible events; generation 1 has no Current
      // event to resolve yet.
      wgt("OceanTile<Tharsis_6_7>")
          .expect(
              """
              OceanTile<Tharsis_6_7>,
              Ruling<MarsFirst>, Dominant<Reds>,
              Current<Class<Riots>>,
              Coming<Class<Revolution>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin
          .doTask("SponsoredProjects")
          .expect("SponsoredProjects, Distant<Class<SponsoredProjects>>")
    }
  }

  private fun generation2() {
    with(me) {
      buyCards(AdvancedAlloys, SolarLogistics)
      discardUnselectedProjectCards(Stratopolis, Capital)

      // Save 10: immediately after Research.
      assertResources(m = 13, s = 0, t = 6, p = 2, e = 0, h = 0)
      assertProduction(m = 0, s = 0, t = 2, p = 0, e = 0, h = 0)
      assertCounts(16 to "TerraformRating")
      assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 2, venus = 0)
      checkHandSizes()

      playProject(TitaniumMine, 7)
      playProject(LocalShading, 4)
      cardAction1(LocalShading)
      draw(Omnicourt)
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Greens> FROM LobbyDelegate")
      }
      pass()
      // Riots resolves before the Reds government forms, then the visible events advance.
      wgt("OceanTile<Tharsis_5_5>")
          .expect(
              """
              OceanTile<Tharsis_5_5>,
              Ruling<Reds>, 2 TerraformRating<Bloo>,
              Chairman<Bloo>, Dominant<Scientists>,
              -Riots,
              Current<Class<Revolution>>,
              Coming<Class<SponsoredProjects>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin.doTask("StrongSociety").expect("StrongSociety, Distant<Class<StrongSociety>>")
    }
  }

  private fun generation3() {
    with(me) {
      buyCards(0)
      discardUnselectedProjectCards(VenusianAnimals, NoctisCity, CloudTourism, Meltworks)

      // Save 17: immediately after Research.
      assertResources(m = 18, s = 0, t = 9, p = 2, e = 0, h = 0)
      assertProduction(m = 0, s = 0, t = 3, p = 0, e = 0, h = 0)
      assertCounts(17 to "TerraformRating")
      assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 3, venus = 0)
      checkHandSizes()

      cardAction2(LocalShading)
      playProject(AdvancedAlloys, 9)
      draw(GiantSpaceMirror)
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Kelvinists> FROM LobbyDelegate")
      }
      playProject(GiantSpaceMirror, mc = 2, titanium = 3)
      playProject(Moss, 4)
      pass()
      // Revolution is harmless at this influence, Scientists take power, and the visible events
      // advance.
      wgt("OceanTile<Tharsis_6_9>")
          .expect(
              """
              TerraformRating<Bloo>,
              Ruling<Scientists>, MC<Bloo>,
              -PartyDelegate<Scientists>,
              Dominant<Greens>,
              -Revolution,
              Current<Class<SponsoredProjects>>,
              Coming<Class<StrongSociety>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin.doTask("SnowCover").expect("SnowCover, Distant<Class<SnowCover>>")
    }
  }

  private fun generation4() {
    with(me) {
      buyCards(BribedCommittee, MarsUniversity, WavePower)
      discardUnselectedProjectCards(VenusShuttles)

      // Save 25: immediately after Research.
      assertResources(m = 13, s = 0, t = 9, p = 2, e = 3, h = 0)
      assertProduction(m = 1, s = 0, t = 3, p = 1, e = 3, h = 0)
      assertCounts(17 to "TerraformRating")
      assertSidebar(gen = 4, temp = -30, oxygen = 1, oceans = 4, venus = 0)
      admin.assertCounts(0 to "PartyDelegate<Scientists>")
      checkHandSizes()

      cardAction1(LocalShading)
      playProject(SolarLogistics, titanium = 4)
      playProject(Comet, mc = 1, titanium = 4) {
        placeTile(5, 6)
        declineTask()
        draw(CeosFavoriteProject)
      }
      stdAction("UseTurmoilPolicySA") {
        draw(VestaShipyard, ResearchOutpost, MiningArea)
      }
      playProject(VestaShipyard, titanium = 3)
      playProject(BribedCommittee, 5)
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Scientists> FROM LobbyDelegate")
        draw(NewHolland)
      }
      pass()
      // Sponsored Projects draws through its influence effect before Greens form the government;
      // the visible events then advance.
      wgt("TemperatureStep")
          .expect(
              """
              TemperatureStep,
              Ruling<Greens>, MC<Bloo>, TerraformRating<Bloo>,
              Dominant<Kelvinists>,
              -SponsoredProjects,
              Current<Class<StrongSociety>>,
              Coming<Class<SnowCover>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin
          .doTask("ScientificCommunity")
          .expect("ScientificCommunity, Distant<Class<ScientificCommunity>>")
      draw(PublicPlans, MethaneFromTitan)
    }
  }

  private fun generation5() {
    with(me) {
      buyCards(Satellites, MagneticFieldGeneratorsPromo, Steelworks, GanymedeColony)

      // Save 35: immediately after Research.
      assertResources(m = 12, s = 0, t = 4, p = 5, e = 3, h = 3)
      assertProduction(m = 1, s = 0, t = 4, p = 1, e = 3, h = 0)
      assertCounts(21 to "TerraformRating")
      assertSidebar(gen = 5, temp = -26, oxygen = 1, oceans = 5, venus = 0)
      admin.assertCounts(1 to "PartyDelegate<Scientists>")
      checkHandSizes()

      cardAction2(LocalShading)
      playProject(PublicPlans, 7) { doTask("17 ProjectCard<Revealed FROM Hand>") }
      playProject(Satellites, titanium = 2)
      playProject(WavePower, 8)
      playProject(Algae, 10)
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Greens> FROM LobbyDelegate")
        draw(SearchForLife)
      }
      pass()
      // Strong Society pays for influence, Kelvinists form the government, and the visible events
      // advance.
      wgt("VenusStep")
          .expect(
              """
              VenusStep,
              Ruling<Kelvinists>, TerraformRating<Bloo>, Dominant<Reds>,
              -StrongSociety,
              Current<Class<SnowCover>>,
              Coming<Class<ScientificCommunity>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin.doTask("HomeworldSupport").expect("HomeworldSupport, Distant<Class<HomeworldSupport>>")
    }
  }

  private fun generation6() {
    with(me) {
      buyCards(EnergyTapping)
      discardUnselectedProjectCards(DirectedHeatUsage, CupolaCity, JovianEmbassy)

      // Save 44: immediately after Research.
      assertResources(m = 34, s = 0, t = 6, p = 9, e = 4, h = 6)
      assertProduction(m = 8, s = 0, t = 4, p = 3, e = 4, h = 0)
      assertCounts(21 to "TerraformRating")
      assertSidebar(gen = 6, temp = -26, oxygen = 1, oceans = 5, venus = 2)
      admin.assertCounts(1 to "PartyDelegate<Scientists>")
      checkHandSizes()

      stdAction("UseTurmoilPolicySA", 2)
      convertPlants { placeTile(6, 6) }
      playProject(MarsUniversity, 8) {
        doTask("-ProjectCard")
        discard(DawnCity)
        draw(SulphurExports)
      }
      playProject(ResearchOutpost, 18) {
        placeTile(8, 6)
        draw(Worms)
        doTask("-ProjectCard")
        discard(Worms)
        draw(AtalantaPlanitiaLab)
      }
      cardAction2(LocalShading)
      playProject(MethaneFromTitan, mc = 2, titanium = 5)
      playProject(EnergyTapping, 2) { doTask("PROD[-Energy<SoloOpponent>]") }
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Scientists> FROM LobbyDelegate")
      }
      pass()
      // Snow Cover resolves first, Reds form the government, and the visible events advance.
      wgt("VenusStep")
          .expect(
              """
              VenusStep,
              Ruling<Reds>, Chairman<Neutral>, Dominant<Scientists>,
              -SnowCover,
              Current<Class<ScientificCommunity>>,
              Coming<Class<HomeworldSupport>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin.doTask("Pandemic").expect("Pandemic, Distant<Class<Pandemic>>")
      draw(SnowAlgae)
      // FAQ v1.8 p.100 awards the solo Reds bonus only at 20 TR or below. The archived server
      // nevertheless awarded it at 21 after annual revision; retain that source result explicitly.
      exMachina("TerraformRating")
    }
  }

  private fun generation7() {
    with(me) {
      buyCards(SubterraneanReservoir, EnvoysFromVenus)
      discardUnselectedProjectCards(CommercialDistrict, SfMemorial)

      // Save 55: immediately after Research.
      assertResources(m = 25, s = 0, t = 5, p = 7, e = 6, h = 13)
      assertProduction(m = 9, s = 0, t = 4, p = 5, e = 6, h = 3)
      assertCounts(22 to "TerraformRating")
      assertSidebar(gen = 7, temp = -30, oxygen = 2, oceans = 5, venus = 4)
      admin.assertCounts(3 to "PartyDelegate<Scientists>")
      checkHandSizes()

      cardAction1(LocalShading)
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Reds> FROM LobbyDelegate")
        draw(HiTechLab)
      }
      playProject(AtalantaPlanitiaLab, 9) {
        draw(IshtarExpedition, FueledGenerators)
        doTask("-ProjectCard")
        discard(HiTechLab)
        draw(TropicalResort)
      }
      playProject(RedTourismWave, 0)
      playProject(ExtractorBalloons, 20)
      cardAction1(ExtractorBalloons)
      pass()
      // Scientific Community pays before its discard, Scientists form the government, and the
      // visible events advance.
      wgt("VenusStep")
          .expect(
              """
              VenusStep,
              Ruling<Scientists>, 4 MC<Bloo>, Chairman<Bloo>, TerraformRating<Bloo>,
              -ScientificCommunity,
              Current<Class<HomeworldSupport>>,
              Coming<Class<Pandemic>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin.doTask("CelebrityLeaders").expect("CelebrityLeaders, Distant<Class<CelebrityLeaders>>")
    }
  }

  private fun generation8() {
    with(me) {
      buyCards(MartianMediaCenter, StripMine)
      discardUnselectedProjectCards(MaxwellBase, EosChasmaNationalPark)

      // Save 64: immediately after Research.
      assertResources(m = 51, s = 0, t = 9, p = 12, e = 6, h = 22)
      assertProduction(m = 9, s = 0, t = 4, p = 5, e = 6, h = 3)
      assertCounts(22 to "TerraformRating")
      assertSidebar(gen = 8, temp = -30, oxygen = 2, oceans = 5, venus = 6)
      checkHandSizes()

      stdAction("UseTurmoilPolicySA") {
        draw(CometForVenus, UndergroundDetonations, Tardigrades)
      }
      convertHeat()
      convertHeat()
      cardAction2(ExtractorBalloons)
      draw(ProtectedHabitats)
      cardAction2(LocalShading)
      playProject(CometForVenus, titanium = 2) {
        declineTask()
        draw(CallistoPenalMines)
      }
      convertPlants {
        placeTile(8, 7)
        draw(SoilFactory)
      }
      playProject(CallistoPenalMines, mc = 3, titanium = 4)
      playProject(EnvoysFromVenus, 0) {
        doTask("PartyDelegate<Kelvinists> FROM ReserveDelegate")
        draw(Sabotage)
      }
      playProject(Sabotage, 0) { declineTask() }
      playProject(Steelworks, 14)
      cardAction1(Steelworks)
      playProject(FueledGenerators, 0)
      // The archive used only two of the available steel cubes for Strip Mine.
      intentionalUnderpay()
      playProject(StripMine, mc = 18, steel = 2)
      playProject(SulphurExports, mc = 5, titanium = 3)
      draw(RegoPlastics)
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Scientists> FROM LobbyDelegate")
      }
      pass()
      // Homeworld Support pays, Unity forms the government, and the visible events advance.
      wgt("VenusStep")
          .expect(
              """
              VenusStep,
              Ruling<Unity>, 10 MC<Bloo>, Chairman<Neutral>, Dominant<Kelvinists>,
              -HomeworldSupport,
              Current<Class<Pandemic>>,
              Coming<Class<CelebrityLeaders>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin
          .doTask("InterplanetaryTradeGlobalEvent")
          .expect(
              "InterplanetaryTradeGlobalEvent, " + "Distant<Class<InterplanetaryTradeGlobalEvent>>"
          )
    }
  }

  private fun generation9() {
    with(me) {
      buyCards(AstraMechanica, InterplanetaryTrade)
      discardUnselectedProjectCards(MoholeLake, Supermarkets)

      // Save 83: immediately after Research.
      assertResources(m = 58, s = 2, t = 5, p = 9, e = 5, h = 11)
      assertProduction(m = 16, s = 2, t = 5, p = 5, e = 5, h = 3)
      assertCounts(30 to "TerraformRating")
      assertSidebar(gen = 9, temp = -26, oxygen = 6, oceans = 5, venus = 14)
      checkHandSizes()

      convertHeat()
      cardAction2(ExtractorBalloons)
      cardAction1(LocalShading)
      cardAction1(Steelworks)
      playProject(Tardigrades, 3)
      playProject(InterplanetaryTrade, mc = 2, titanium = 4)
      convertPlants { placeTile(7, 6) }
      cardAction1(Tardigrades)
      playProject(IshtarExpedition, 5) {
        discardProjectCardsFromDeck(
            Ironworks,
            ElectroCatapult,
            InventionContest,
            Pets,
            MagneticFieldDome,
            PowerSupplyConsortium,
        )
        draw(AirScrappingExpedition, Extremophiles)
      }
      sellPatents(TropicalResort, UndergroundDetonations, SoilFactory)
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Greens> FROM LobbyDelegate")
      }
      stdAction("SendDelegateSA", 2) {
        doTask("PartyDelegate<Unity> FROM ReserveDelegate")
      }
      playProject(GanymedeColony, mc = 1, titanium = 3)
      playProject(SubterraneanReservoir, 10) { placeTile(6, 8) }
      playProject(AirScrappingExpedition, 12) { addCardResources(ExtractorBalloons, 3) }
      playProject(AstraMechanica, 6) {
        doWithoutAutoExec(me) {
          doTask("ProjectCard FROM PlayedEvent<Class<$BribedCommittee>>")
          returnToHand(BribedCommittee)
          doTask("ProjectCard FROM PlayedEvent<Class<$Comet>>")
          returnToHand(Comet)
        }
        doTask("-ProjectCard")
        discard(ProtectedHabitats)
        draw(SmallAnimals)
      }
      playProject(CeosFavoriteProject, 0) { addCardResources(ExtractorBalloons) }
      playProject(SmallAnimals, 5) { doTask("PROD[-Plant<SoloOpponent>]") }
      cardAction1(SmallAnimals)
      playProject(BribedCommittee, 4)
      playProject(SnowAlgae, 11)
      pass()
      // Pandemic resolves before Kelvinists form the government, then the visible events advance.
      wgt("OxygenStep")
          .expect(
              """
              OxygenStep,
              Ruling<Kelvinists>, 5 MC<Bloo>, Chairman<Bloo>, TerraformRating<Bloo>,
              Dominant<Greens>,
              -Pandemic,
              Current<Class<CelebrityLeaders>>,
              Coming<Class<InterplanetaryTradeGlobalEvent>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin.doTask("SpinOffProducts").expect("SpinOffProducts, Distant<Class<SpinOffProducts>>")
    }
  }

  private fun generation10() {
    with(me) {
      buyCards(OpenCity, AiCentral, FloatingHabs)
      discardUnselectedProjectCards(LavaTubeSettlement)

      // Save 107: immediately after Research.
      assertResources(m = 54, s = 6, t = 6, p = 8, e = 5, h = 9)
      assertProduction(m = 26, s = 2, t = 5, p = 6, e = 5, h = 5)
      assertCounts(40 to "TerraformRating")
      assertSidebar(gen = 10, temp = -22, oxygen = 9, oceans = 6, venus = 18)
      checkHandSizes()

      convertHeat()
      stdAction("UseTurmoilPolicySA", 2)
      cardAction2(ExtractorBalloons)
      cardAction1(Steelworks)
      cardAction1(SmallAnimals)
      // The archive paid cash despite holding steel that could cover Rego Plastics.
      intentionalUnderpay()
      playProject(RegoPlastics, 9)
      playProject(Omnicourt, mc = 2, steel = 2)
      playProject(GhgFactories, mc = 2, steel = 2)
      playProject(AiCentral, mc = 4, steel = 4) {
        doTask("-ProjectCard")
        discard(NewHolland)
        draw(CaretakerContract)
      }
      cardAction1(AiCentral) { draw(BactoviralResearch, OlympusConference) }
      cardAction2(LocalShading)
      playProject(OlympusConference, 7) {
        doTask("-ProjectCard")
        discard(CaretakerContract)
        draw(Heather)
      }
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Kelvinists> FROM LobbyDelegate")
        draw(NitriteReducingBacteria)
      }
      playProject(NitriteReducingBacteria, 10)
      cardAction2(NitriteReducingBacteria)
      cardAction1(Tardigrades)
      playProject(Comet, titanium = 4) {
        draw(FrontierTown)
        placeTile(9, 9)
        declineTask()
      }
      convertPlants { placeTile(8, 8) }
      playProject(Extremophiles, 2)
      cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) }
      playProject(MiningArea, 3) { placeTile(9, 6) }
      playProject(FloatingHabs, 4)
      cardAction1(FloatingHabs) { addCardResources(ExtractorBalloons) }
      pass()
      // Celebrity Leaders pays before Greens take power, then the visible events advance.
      wgt("TemperatureStep")
          .expect(
              """
              TemperatureStep,
              Ruling<Greens>, 7 MC<Bloo>, Chairman<Neutral>, Dominant<MarsFirst>,
              -CelebrityLeaders,
              Current<Class<InterplanetaryTradeGlobalEvent>>,
              Coming<Class<SpinOffProducts>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin
          .doTask("SuccessfulOrganisms")
          .expect("SuccessfulOrganisms, Distant<Class<SuccessfulOrganisms>>")
    }
  }

  private fun generation11() {
    with(me) {
      buyCards(LargeConvoy, AqueductSystems, VenusianPlants)
      discardUnselectedProjectCards(AdaptationTechnology)

      // Save 133: immediately after Research.
      assertResources(m = 89, s = 5, t = 9, p = 6, e = 4, h = 13)
      assertProduction(m = 27, s = 3, t = 5, p = 6, e = 4, h = 11)
      assertCounts(48 to "TerraformRating")
      assertSidebar(gen = 11, temp = -16, oxygen = 11, oceans = 7, venus = 20)
      checkHandSizes()

      convertHeat()
      cardAction1(AiCentral) { draw(SpecialPermit, AntiGravityTechnology) }
      cardAction1(Steelworks)
      cardAction2(ExtractorBalloons)
      playProject(AntiGravityTechnology, 13) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
        draw(Plantation)
        doTask("-ProjectCard")
        discard(MartianMediaCenter)
        draw(KelpFarming)
      }
      cardAction1(Tardigrades)
      playProject(SpecialPermit, 2) {
        doTask("4 Plant<Bloo> FROM Plant<SoloOpponent>")
      }
      convertPlants { placeTile(7, 8) }
      playProject(OpenCity, steel = 5) { placeTile(7, 7) }
      playProject(AqueductSystems, mc = 2, steel = 1) {
        discardProjectCardsFromDeck(
            Zeppelins,
            VenusianInsects,
            Insects,
            ExtremeColdFungus,
            DeuteriumExport,
            ImportedHydrogen,
            Farming,
            Decomposers,
            FloydContinuum,
            AerosportTournament,
            IshtarMining,
        )
        draw(NoctisFarming, OreProcessor, RoverConstruction)
      }
      cardAction1(SmallAnimals)
      playProject(RoverConstruction, mc = 1, steel = 1)
      playProject(BactoviralResearch, 7) {
        draw(AdaptedLichen)
        doTask("-ProjectCard")
        discard(AdaptedLichen)
        draw(Bushes)
        addCardResources(NitriteReducingBacteria, 9)
      }
      cardAction2(NitriteReducingBacteria)
      cardAction1(LocalShading)
      cardAction1(FloatingHabs) { addCardResources(ExtractorBalloons) }
      cardAction1(Extremophiles) { addCardResources(Extremophiles) }
      playProject(Heather, 3)
      playProject(LargeConvoy, mc = 1, titanium = 6) {
        draw(AsteroidRights, LunarBeam)
        draw(PeroxidePower)
        placeTile(5, 4)
        doTask("5 Plant")
      }
      convertPlants { placeTile(7, 5) }
      playProject(KelpFarming, 14)
      playProject(AsteroidRights, titanium = 1)
      cardAction2(AsteroidRights) { doTask("2 Titanium") }
      playProject(PeroxidePower, 4)
      playProject(VenusianPlants, 10) { addCardResources(Extremophiles) }
      playProject(NoctisFarming, 7)
      convertPlants { placeTile(6, 4) }
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<Greens> FROM LobbyDelegate")
        draw(FuelFactory)
      }
      playProject(MagneticFieldGeneratorsPromo, 19) { placeTile(5, 7) }
      playProject(Plantation, 12) { placeTile(8, 5) }
      stdProject("AsteroidSP")
      stdAction("SendDelegateSA", 2) {
        doTask("PartyDelegate<Greens> FROM ReserveDelegate")
      }
      pass()
      // Interplanetary Trade pays before Mars First takes power, then the visible events advance.
      wgt("TemperatureStep")
          .expect(
              """
              TemperatureStep,
              Ruling<MarsFirst>, 18 MC<Bloo>, Dominant<Unity>,
              -InterplanetaryTradeGlobalEvent,
              Current<Class<SpinOffProducts>>,
              Coming<Class<SuccessfulOrganisms>>
              """
                  .trimIndent()
                  .replace('\n', ' ')
          )
      admin
          .doTask("VolcanicEruptions")
          .expect("VolcanicEruptions, Distant<Class<VolcanicEruptions>>")
    }
  }

  private fun generation12() {
    with(me) {
      buyCards(BigAsteroid, GiantIceAsteroid)
      discardUnselectedProjectCards(IndustrialMicrobes, SpaceHotels)

      // Save 168: immediately after Research.
      assertResources(m = 120, s = 3, t = 9, p = 16, e = 1, h = 16)
      assertProduction(m = 33, s = 3, t = 5, p = 12, e = 1, h = 11)
      assertCounts(59 to "TerraformRating")
      assertSidebar(gen = 12, temp = -10, oxygen = 14, oceans = 8, venus = 24)
      checkHandSizes()

      convertHeat()
      convertHeat()
      convertPlants { placeTile(6, 3) }
      playProject(FrontierTown, steel = 2) { placeTile(6, 5) }
      convertPlants { placeTile(8, 4) }
      cardAction1(AiCentral) { draw(BusinessNetwork, StaticHarvesting) }
      cardAction2(ExtractorBalloons)
      cardAction2(AsteroidRights) { doTask("2 Titanium") }
      cardAction1(SmallAnimals)
      cardAction2(NitriteReducingBacteria)
      cardAction1(Extremophiles) { addCardResources(Extremophiles) }
      cardAction1(Tardigrades)
      cardAction2(LocalShading)
      playProject(BigAsteroid, titanium = 5) {
        draw(WgProject)
        declineTask()
      }
      playProject(GiantIceAsteroid, titanium = 7) {
        draw(Fish)
        placeTile(1, 4)
        draw(CulturalMetropolis)
        declineTask()
      }
      stdAction("SendDelegateSA", 1) {
        doTask("PartyDelegate<MarsFirst> FROM LobbyDelegate")
        draw(MartianRails)
      }
      playProject(Bushes, 7)
      // The archive recorded no payment for the one M€ remaining after card discounts.
      intentionalUnderpay()
      playProject(BusinessNetwork, 0)
      cardAction1(BusinessNetwork) { buyCards(0) }
      playProject(Fish, 6) { doTask("PROD[-Plant<SoloOpponent>]") }
      cardAction1(Fish)
      playProject(MartianRails, steel = 3)
      cardAction1(MartianRails)
      sellPatents(
          SearchForLife,
          OreProcessor,
          LunarBeam,
          FuelFactory,
          StaticHarvesting,
          WgProject,
          CulturalMetropolis,
      )
      stdProject("AsteroidSP")
      stdProject("CitySP") { placeTile(7, 4) }
      stdProject("CitySP") { placeTile(9, 8) }
      stdProject("GreenerySP") { placeTile(5, 2) }
      convertPlants { placeTile(9, 7) }
      stdProject("CitySP") { placeTile(6, 2) }
      stdProject("AsteroidSP")
      pass()

      // Save 200: production completed and final greenery placement is pending.
      assertResources(m = 113, s = 12, t = 8, p = 16, e = 0, h = 11)
      assertProduction(m = 36, s = 3, t = 5, p = 14, e = 0, h = 11)
      assertCounts(70 to "TerraformRating")
      assertSidebar(gen = 12, temp = 6, oxygen = 14, oceans = 9, venus = 26)

      convertPlants { placeTile(7, 3) }
      convertPlants { placeTile(5, 1) }
      declineTask()

      assertCardTrackingComplete()
      cardsHand shouldBe emptySet()
      checkHandSizes()
      admin.assertCounts(1 to "End", 1 to "Phase")

      assertResources(m = 113, s = 12, t = 8, p = 2, e = 0, h = 11)
      assertProduction(m = 36, s = 3, t = 5, p = 14, e = 0, h = 11)
      assertCounts(
          0 to "ProjectCard",
          70 to "TerraformRating",
          74 to "CardFront OR PlayedEvent",
          150 to "VictoryPoint",
          1 to "Victory",
      )
      assertDashRight(events = 16, tagless = 1, cities = 7)
      assertTags(
          but = 20,
          spt = 12,
          sct = 9,
          pot = 5,
          eat = 5,
          jot = 5,
          vet = 7,
          plt = 9,
          mit = 4,
          ant = 2,
          cit = 4,
      )
      assertCardResources(
          4 to Tardigrades,
          4 to SmallAnimals,
          1 to OlympusConference,
          4 to NitriteReducingBacteria,
          3 to Extremophiles,
          1 to Fish,
      )

      admin.assertCounts(
          1 to "Current<Class<$SpinOffProducts>>",
          1 to "Coming<Class<$SuccessfulOrganisms>>",
          1 to "Distant<Class<$VolcanicEruptions>>",
          1 to "Ruling<MarsFirst>",
          1 to "Dominant<Unity>",
          1 to "Chairman<Neutral>",
      )
      assertCounts(
          1 to "PartyDelegate<MarsFirst>",
          1 to "PartyDelegate<Scientists>",
          1 to "PartyDelegate<Unity>",
          2 to "PartyDelegate<Greens>",
          1 to "PartyDelegate<Reds>",
          1 to "PartyDelegate<Kelvinists>",
      )

      val score = Summarizer(game)
      score.net("TerraformRating", "VictoryPoint<Bloo>") shouldBe 70
      score.net("GreeneryTile", "VictoryPoint<Bloo>") shouldBe 14
      score.net("CityTile", "VictoryPoint<Bloo>") shouldBe 27
      score.net("Card", "VictoryPoint<Bloo>") shouldBe 35
      score.net("PartyLeader", "VictoryPoint<Bloo>") shouldBe 4
    }
  }

  private companion object {
    val AerialLenses = cn("AerialLenses")
    val CulturalMetropolis = cn("CulturalMetropolis")
    val CorridorsOfPower = cn("CorridorsOfPower")
    val EnvoysFromVenus = cn("EnvoysFromVenus")
    val FrontierTown = cn("FrontierTown")
    val MartianMediaCenter = cn("MartianMediaCenter")
    val RedTourismWave = cn("RedTourismWave")
    val SpecialPermit = cn("SpecialPermit")
    val SpinOffProducts = cn("SpinOffProducts")
    val SuccessfulOrganisms = cn("SuccessfulOrganisms")
    val VolcanicEruptions = cn("VolcanicEruptions")
    val VoteOfNoConfidence = cn("VoteOfNoConfidence")
    val WgProject = cn("WgProject")
  }
}
