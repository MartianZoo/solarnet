package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TEST_CLASS_SYNONYMS
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.TfmTest
import dev.martianzoo.tfm.tests.canonicalPremise
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class FirstPartialGameTest : TfmTest() {
  @Test
  internal fun fourWholeGenerations() {
    repeat(1) {
      val setup =
          canonicalPremise(
              Elysium,
              PreludeExpansion,
              TurmoilCardPack,
              players = 2,
          )
      val game = Engine.newGame(setup, inputOnlySynonyms = TEST_CLASS_SYNONYMS)
      val eng = game.tfm(ENGINE)
      val p1 = game.tfm(PLAYER1)
      val p2 = game.tfm(PLAYER2)

      val workflow = TfmWorkflow.Auto(game).launch()

      p1.playCorp(LakefrontResorts, 3)
      p2.playCorp(InterplanetaryCinematics, 8)

      p1.turn {
        playPrelude(MartianIndustries)
        playPrelude(GalileanMining)
      }
      p2.turn {
        playPrelude(MiningOperations)
        playPrelude(UnmiContractor)
      }

      // Generation 1 (Player1 first)
      p1.turn { playProject(AsteroidMining, 30) }
      p2.turn { playProject(NaturalPreserve, 1, steel = 4) { placeTile(3, 7) } }
      p1.pass()
      p2.turn {
        playProject(SpaceElevator, 1, steel = 13)
        cardAction1(SpaceElevator)
        playProject(InventionContest, 2)
        assertCounts(0 to "ProjectCard<Selecting>")
        playProject(GreatEscarpmentConsortium, 6) { doTask("PROD[-S<Player1>]") }
      }
      p2.pass()

      // Generation 2 (Player2 first)
      p1.buyCards(4)
      p2.buyCards(1)

      p2.turn {
        cardAction1(SpaceElevator)
        playProject(EarthCatapult, 23)
      }

      p1.turn {
        playProject(TitaniumMine, 7)
        playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$MartianIndustries>") }
      }

      p2.turn {
        playProject(IndustrialMicrobes, steel = 5)
        playProject(TechnologyDemonstration, titanium = 1)
      }

      p1.turn { playProject(Sponsors, 6) }

      p2.turn {
        playProject(EnergyTapping, 1) { doTask("PROD[-E<Player1>]") }
        playProject(BuildingIndustries, steel = 2)
      }

      p1.pass()
      p2.pass()

      // Generation 3 (Player1 first)
      p1.buyCards(3)
      p2.buyCards(2)

      p1.turn { playProject(Mine, 2, steel = 1) }

      p2.turn {
        cardAction1(SpaceElevator)
        playProject(ElectroCatapult, 5, steel = 5)
      }

      p1.pass()

      p2.turn {
        cardAction2(ElectroCatapult)
        playProject(SpaceHotels, 7, titanium = 1)

        playProject(MarsUniversity, 6) { doTask("-ProjectCard<Hand>! THEN ProjectCard<Hand>") }
        playProject(ArtificialPhotosynthesis, 10) {
          doTask("PROD[2 Energy]")
          // Decline Mars University's discard-and-draw effect for the science tag.
          declineTask()
        }

        playProject(BribedCommittee, 5)

        pass()
      }

      // Generation 4 (Player2 first)
      p1.buyCards(3)
      p2.buyCards(2)

      p2.turn {
        cardAction2(ElectroCatapult)
        cardAction1(SpaceElevator)
      }

      p1.turn {
        playProject(ResearchOutpost, 14, steel = 2) { placeTile(5, 6) }
        playProject(IoMiningIndustries, 1, titanium = 13)
      }

      p2.turn {
        playProject(TransNeptuneProbe, 1, titanium = 1) {
          // Decline Mars University's discard-and-draw effect for the science tag.
          declineTask()
        }
        playProject(Hackers, 1) { doTask("PROD[-2 M<Player1>]") }
      }

      p1.turn { sellPatents(1) }

      p2.turn {
        playProject(SolarPower, 1, steel = 4)
        stdProject("CitySP") { placeTile(6, 5) }
      }

      workflow.shutdown()
      TfmWorkflow.Manual(game).productionPhase()

      eng.assertCounts(4 to "Generation")
      eng.assertCounts(0 to "OceanTile", 0 to "OxygenStep", 0 to "TemperatureStep")

      with(p1) {
        assertCounts(20 to "TerraformRating")

        assertCounts(34 to "M", 2 to "S", 8 to "T", 3 to "P", 1 to "E", 3 to "H")
        assertProds(2 to "M", 2 to "S", 7 to "T", 0 to "P", 1 to "E", 0 to "H")

        assertCounts(15 to "Card", 5 to "ProjectCard", 10 to "CardFront")
        assertCounts(0 to "ProjectCard<Selecting>", 0 to "ProjectCard<Revealed>")
        assertCounts(1 to "ActiveCard", 6 to "AutomatedCard", 0 to "PlayedEvent")

        assertTags(but = 5, spt = 2, sct = 2, eat = 1, jot = 3, cit = 1)

        assertCounts(1 to "CityTile", 0 to "GreeneryTile", 0 to "SpecialTile")
      }

      with(p2) {
        assertCounts(25 to "TerraformRating")

        assertCounts(47 to "M", 6 to "S", 1 to "T", 1 to "P", 2 to "E", 3 to "H")
        assertProds(8 to "M", 6 to "S", 1 to "T", 0 to "P", 2 to "E", 0 to "H")

        assertCounts(23 to "Card", 3 to "ProjectCard", 17 to "CardFront")
        assertCounts(0 to "ProjectCard<Selecting>", 0 to "ProjectCard<Revealed>")
        assertCounts(4 to "ActiveCard", 10 to "AutomatedCard", 3 to "PlayedEvent")

        assertTags(but = 9, spt = 3, sct = 4, pot = 2, eat = 3, mit = 1)

        assertCounts(1 to "CityTile", 0 to "GreeneryTile", 1 to "SpecialTile")
      }
    }
  }
}
