package dev.martianzoo.tfm.engine.games

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.OldTfmHelpers.cardAction1
import dev.martianzoo.tfm.engine.OldTfmHelpers.pass
import dev.martianzoo.tfm.engine.OldTfmHelpers.phase
import dev.martianzoo.tfm.engine.OldTfmHelpers.playCard
import dev.martianzoo.tfm.engine.OldTfmHelpers.playCorp
import dev.martianzoo.tfm.engine.OldTfmHelpers.production
import dev.martianzoo.tfm.engine.OldTfmHelpers.sellPatents
import dev.martianzoo.tfm.engine.OldTfmHelpers.stdProject
import dev.martianzoo.tfm.engine.OldTfmHelpers.turn
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test

class FirstPartialGameTest {
  @Test
  fun fourWholeGenerations() {
    repeat(1) {
      val game = Engine.newGame(GameSetup(Canon, "BREPT", 2))
      val eng = game.session(ENGINE)
      val p1 = game.session(PLAYER1)
      val p2 = game.session(PLAYER2)

      fun newGeneration(cards1: Int, cards2: Int) {
        with(eng) {
          phase("Production")
          operation("ResearchPhase FROM Phase") {
            p1.task(if (cards1 > 0) "$cards1 BuyCard" else "Ok")
            p2.task(if (cards2 > 0) "$cards2 BuyCard" else "Ok")
          }
          phase("Action")
        }
      }

      p1.playCorp("LakefrontResorts", 3)
      p2.playCorp("InterplanetaryCinematics", 8)

      eng.phase("Prelude")

      p1.turn("MartianIndustries")
      p1.turn("GalileanMining")

      p2.turn("MiningOperations")
      p2.turn("UnmiContractor")

      eng.phase("Action")

      p1.playCard("AsteroidMining", 30)
      p1.pass()

      with(p2) {
        playCard("NaturalPreserve", 1, steel = 4, "NpTile<E37>")
        playCard("SpaceElevator", 1, steel = 13)
        cardAction1("SpaceElevator")
        playCard("InventionContest", 2)
        playCard("GreatEscarpmentConsortium", 6, "PROD[-S<P1>]")
      }

      newGeneration(4, 1)

      with(p2) {
        cardAction1("SpaceElevator")
        playCard("EarthCatapult", 23)
      }

      with(p1) {
        playCard("TitaniumMine", 7)
        playCard("RoboticWorkforce", 9, "CopyProductionBox<MartianIndustries>")
        playCard("Sponsors", 6)
      }

      with(p2) {
        playCard("IndustrialMicrobes", steel = 5)
        playCard("TechnologyDemonstration", titanium = 1)
        playCard("EnergyTapping", 1, "PROD[-E<P1>]")
        playCard("BuildingIndustries", steel = 2)
      }

      newGeneration(3, 2)

      with(p1) {
        playCard("Mine", 2, steel = 1)
        pass()
      }
      with(p2) {
        cardAction1("SpaceElevator")
        playCard("ElectroCatapult", 5, steel = 5)
        cardAction1("ElectroCatapult")
        playCard("SpaceHotels", 7, titanium = 1)
        playCard("MarsUniversity", 6)
        playCard("ArtificialPhotosynthesis", 10, "PROD[2 Energy]")
        playCard("BribedCommittee", 5)
      }

      newGeneration(3, 2)

      with(p2) {
        cardAction1("ElectroCatapult") // steel
        cardAction1("SpaceElevator")
      }
      with(p1) {
        playCard("ResearchOutpost", 14, steel = 2, "CityTile<E56>")
        playCard("IoMiningIndustries", 1, titanium = 13)
      }
      with(p2) {
        playCard("TransNeptuneProbe", 1, titanium = 1)
        playCard("Hackers", 1, "PROD[-2 M<P1>]")
      }

      p1.sellPatents(1)

      with(p2) {
        playCard("SolarPower", 1, steel = 4)
        stdProject("CitySP", "CityTile<E65>")
        operation("PROD[-Plant, Energy]") // CORRECTION TODO WHY WHY
      }

      eng.phase("Production")

      // Stuff
      eng.assertCounts(4 to "Generation")
      eng.assertCounts(0 to "OceanTile", 0 to "OxygenStep", 0 to "TemperatureStep")

      with(p1) {
        assertCounts(20 to "TerraformRating")

        assertCounts(34 to "M", 2 to "S", 8 to "T", 3 to "P", 1 to "E", 3 to "H")
        assertThat(production().values).containsExactly(2, 2, 7, 0, 1, 0).inOrder()

        assertCounts(15 to "Card", 5 to "ProjectCard", 10 to "CardFront")
        assertCounts(1 to "ActiveCard", 6 to "AutomatedCard", 0 to "PlayedEvent")

        assertCounts(5 to "BUT", 2 to "SPT", 2 to "SCT", 0 to "POT", 1 to "EAT")
        assertCounts(3 to "JOT", 0 to "PLT", 0 to "MIT", 0 to "ANT", 1 to "CIT")

        assertCounts(1 to "CityTile", 0 to "GreeneryTile", 0 to "SpecialTile")
      }

      with(p2) {
        assertCounts(25 to "TerraformRating")

        assertCounts(47 to "M", 6 to "S", 1 to "T", 1 to "P", 2 to "E", 3 to "H")
        assertThat(production().values).containsExactly(8, 6, 1, 0, 2, 0).inOrder()

        assertCounts(23 to "Card", 3 to "ProjectCard", 17 to "CardFront")
        assertCounts(4 to "ActiveCard", 10 to "AutomatedCard", 3 to "PlayedEvent")

        assertCounts(9 to "BUT", 3 to "SPT", 4 to "SCT", 2 to "POT", 3 to "EAT")
        assertCounts(0 to "JOT", 0 to "PLT", 1 to "MIT", 0 to "ANT", 0 to "CIT")

        assertCounts(1 to "CityTile", 0 to "GreeneryTile", 1 to "SpecialTile")
      }
    }
  }
}
