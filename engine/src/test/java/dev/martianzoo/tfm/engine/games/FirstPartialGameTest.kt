package dev.martianzoo.tfm.engine.games

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class FirstPartialGameTest {
  @Test
  fun fourWholeGenerations() {
    repeat(1) {
      val game = Engine.newGame(GameSetup(Canon, "BREPT", 2))
      val eng = game.tfm(ENGINE)
      val p1 = game.tfm(PLAYER1)
      val p2 = game.tfm(PLAYER2)

      eng.phase("Corporation")

      p1.playCorp("LakefrontResorts", 3)
      p2.playCorp("InterplanetaryCinematics", 8)

      eng.phase("Prelude")

      p1.playPrelude("MartianIndustries")
      p1.playPrelude("GalileanMining")

      p2.playPrelude("MiningOperations")
      p2.playPrelude("UnmiContractor")

      eng.phase("Action")

      p1.playProject("AsteroidMining", 30)
      p1.pass()

      with(p2) {
        playProject("NaturalPreserve", 1, steel = 4) { doTask("NpTile<E37>") }
        playProject("SpaceElevator", 1, steel = 13)
        cardAction1("SpaceElevator")
        playProject("InventionContest", 2)
        playProject("GreatEscarpmentConsortium", 6) { doTask("PROD[-S<P1>]") }
      }

      eng.nextGeneration(4, 1)

      with(p2) {
        cardAction1("SpaceElevator")
        playProject("EarthCatapult", 23)
      }

      with(p1) {
        playProject("TitaniumMine", 7)
        playProject("RoboticWorkforce", 9) { doTask("CopyProductionBox<MartianIndustries>") }
        playProject("Sponsors", 6)
      }

      with(p2) {
        playProject("IndustrialMicrobes", steel = 5)
        playProject("TechnologyDemonstration", titanium = 1)
        playProject("EnergyTapping", 1) { doTask("PROD[-E<P1>]") }
        playProject("BuildingIndustries", steel = 2)
      }

      eng.nextGeneration(3, 2)

      with(p1) {
        playProject("Mine", 2, steel = 1)
        pass()
      }
      with(p2) {
        cardAction1("SpaceElevator")
        playProject("ElectroCatapult", 5, steel = 5)
        cardAction1("ElectroCatapult")
        playProject("SpaceHotels", 7, titanium = 1)
        playProject("MarsUniversity", 6)
        playProject("ArtificialPhotosynthesis", 10) { doTask("PROD[2 Energy]") }
        playProject("BribedCommittee", 5)
      }

      eng.nextGeneration(3, 2)

      with(p2) {
        cardAction1("ElectroCatapult") // steel
        cardAction1("SpaceElevator")
      }
      with(p1) {
        playProject("ResearchOutpost", 14, steel = 2) { doTask("CityTile<E56>") }
        playProject("IoMiningIndustries", 1, titanium = 13)
      }
      with(p2) {
        playProject("TransNeptuneProbe", 1, titanium = 1)
        playProject("Hackers", 1) { doTask("PROD[-2 M<P1>]") }
      }

      p1.sellPatents(1)

      with(p2) {
        playProject("SolarPower", 1, steel = 4)
        stdProject("CitySP") { doTask("CityTile<E65>") }
        godMode().manual("PROD[-Plant, Energy]") // CORRECTION TODO WHY WHY
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
