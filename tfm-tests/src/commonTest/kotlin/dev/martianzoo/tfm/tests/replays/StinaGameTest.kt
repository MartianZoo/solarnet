package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.data.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class StinaGameTest : AbstractSoloTest() {
  override val config = GameConfig("ElysiumMap, PreludeExpansion", "Me")

  override fun cityAreas() = "Elysium_5_6" to "Elysium_7_7"

  override fun greeneryAreas() = "Elysium_5_5" to "Elysium_7_6"

  @Test
  internal fun stinaSaturnSystemsGame() {
    with(me) {
      // Test inference: unnamed draws are assigned in the order the cards are later played.
      playCorp(SaturnSystems) {
            buyCards(
                EarthOffice,
                MediaGroup,
                InvestmentLoan,
                IndenturedWorkers,
                EarthCatapult,
                HiredRaiders,
                OlympusConference,
                AdvancedAlloys,
                MineralDeposit,
                ResearchOutpost,
            )
          }
          .expect("PROD[1, Titanium], 12, 10 ProjectCard")

      playPrelude(Biolab) {
            draw(InventionContest, BusinessContacts, QuantumExtractor)
          }
          .expect("PROD[Plant], 3 ProjectCard")
      playPrelude(AcquiredSpaceAgency) {
            draw(SpaceStation, OptimalAerobraking)
          }
          .expect("6 Titanium, 2 ProjectCard")

      playProject(EarthOffice, 1)
      playProject(MediaGroup, 3)
      playProject(InvestmentLoan, 0).expect("PROD[-1], 13")
      playProject(IndenturedWorkers, 0).expect("3")
      playProject(EarthCatapult, 12)
      playProject(HiredRaiders, 0) {
            doTask("2 Steel<Me> FROM Steel<SoloOpponent>")
          }
          .expect("3")
      playProject(OlympusConference, 1, steel = 2)
      playProject(AdvancedAlloys, 7) {
            draw(TechnologyDemonstration)
            doTask("ProjectCard FROM Science<OlympusConference>")
          }
          .expect("0 ProjectCard")
      playProject(MineralDeposit, 3).expect("5 Steel")
      playProject(ResearchOutpost, 1, steel = 5) {
            draw(ImportOfAdvancedGhg)
            placeTile(9, 7)
          }
          .expect("0 ProjectCard")
      playProject(InventionContest, 0) {
            draw(ImportedGhg, MassConverter)
            doTask("ProjectCard FROM Science<OlympusConference>")
          }
          .expect("ProjectCard, 3")
      playProject(BusinessContacts, 1) {
            draw(TowingAComet, AdaptationTechnology)
          }
          .expect("ProjectCard, 2")
      playProject(QuantumExtractor, 10).expect("PROD[4 Energy]")
      playProject(SpaceStation, 1, titanium = 1)
      playProject(OptimalAerobraking, 0)
      // Test inference: Lagrange Observatory is free after the accumulated space discounts and
      // draws twice with Olympus Conference. Convoy from Europa costs 3 M€, rebates 6 M€, draws a
      // replacement card, and places an ocean.
      playProject(TechnologyDemonstration, 0) {
            draw(SpecialDesign, Shuttles, LagrangeObservatory)
            doTask("ProjectCard FROM Science<OlympusConference>")
          }
          .expect("2 ProjectCard, 6, 3 Heat")
      playProject(ImportOfAdvancedGhg, 0).expect("PROD[2 Heat], 6, 3 Heat")
      playProject(ImportedGhg, 0).expect("PROD[Heat], 6, 6 Heat")
      playProject(MassConverter, 5).expect("PROD[6 Energy]")
      playProject(TowingAComet, 2, titanium = 3) {
            placeTile(1, 2)
          }
          .expect("4, 2 Plant, OxygenStep, 3 Heat, 2 TR, OceanTile")
      playProject(AdaptationTechnology, 9) {
        addCardResources(OlympusConference)
      }
      playProject(SpecialDesign, 1) {
            draw(ConvoyFromEuropa)
            doTask("ProjectCard FROM Science<OlympusConference>")
          }
          .expect("2, 0 ProjectCard")
      playProject(Shuttles, 1).expect("PROD[-Energy, 2]")

      checkHandSizes()
      assertCardTrackingComplete()

      assertResources(m = 9, s = 0, t = 3, p = 2, e = 0, h = 15)
      assertProduction(m = 2, s = 0, t = 1, p = 1, e = 9, h = 3)
      assertCounts(
          16 to "TR",
          2 to "ProjectCard",
          26 to "CardFront OR PlayedEvent",
          12 to "ActiveCard",
          0 to "AutomatedCard",
          11 to "PlayedEvent",
      )
      assertTags(but = 2, spt = 3, sct = 7, pot = 2, eat = 4, jot = 1, cit = 1)
      assertCounts(
          1 to "CityTile",
          0 to "GreeneryTile",
          0 to "SpecialTile",
          1 to "Generation",
          0 to "TemperatureStep",
          1 to "OxygenStep",
          1 to "OceanTile",
          1 to "Science",
      )
    }
  }
}
