package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.cardAction1
import dev.martianzoo.tfm.engine.TerraformingMars.pass
import dev.martianzoo.tfm.engine.TerraformingMars.playCard
import dev.martianzoo.tfm.engine.TerraformingMars.playCorp
import dev.martianzoo.tfm.engine.TerraformingMars.production
import dev.martianzoo.tfm.engine.TerraformingMars.sellPatents
import dev.martianzoo.tfm.engine.TerraformingMars.stdProject
import dev.martianzoo.tfm.repl.TestHelpers.assertCounts
import dev.martianzoo.tfm.types.MClassTable
import org.junit.jupiter.api.Test

class RealGamesTest {
  @Test
  fun fourWholeGenerations() {
    val table = MClassTable.forSetup(GameSetup(Canon, "BREPT", 2))
    repeat(1) { // I change this when profiling
      val game = Engine.newGame(table)
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

  @Test
  fun startOfEllieGameNoPrelude() {
    val game = Engine.newGame(GameSetup(Canon, "BRHX", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)
    val p2 = game.session(PLAYER2)

    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    eng.phase("Action")

    p1.playCard("MediaGroup", 6)
    p1.playCard("Sabotage", 1, "-7 M<Player2>")

    p2.playCard("Research", 11)
  }

  @Test
  fun ellieGame() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)
    val p2 = game.session(PLAYER2)

    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    eng.phase("Prelude")

    p1.turn("UnmiContractor")
    p1.turn("CorporateArchives")

    p2.turn("BiosphereSupport")
    p2.turn("SocietySupport")

    // Action!

    eng.phase("Action")

    p1.playCard("MediaGroup", 6)
    p1.playCard("Sabotage", 1, "-7 M<P2>")

    p2.playCard("Research", 11)
    p2.playCard("MartianSurvey", 9, "Ok") // ain't gon flip

    p1.pass()

    p2.playCard("SearchForLife", 3, "EVT<Class<PharmacyUnion>> FROM PharmacyUnion THEN 3 TR")
    p2.cardAction1("SearchForLife", "Ok") // no microbe

    p2.pass()

    // Generation 2

    with(eng) {
      phase("Production")
      operation("ResearchPhase FROM Phase") {
        p1.task("1 BuyCard")
        p2.task("3 BuyCard")
      }
      phase("Action")
    }

    p2.sellPatents(1)
    p2.playCard("VestaShipyard", 15)
    p2.pass()

    with(p1) {
      playCard("EarthCatapult", 23)
      playCard("OlympusConference", steel = 4)

      playCard("DevelopmentCenter", 1, steel = 4, "ProjectCard FROM Science<OlympusConference>")

      playCard("GeothermalPower", 1, steel = 4)

      playCard("MirandaResort", 10)
      playCard("Hackers", 1, "PROD[-2 M<P2>]")
      playCard("MicroMills", 1)
      pass()
    }

    // Generation 2

    with(eng) {
      phase("Production")
      operation("ResearchPhase FROM Phase") {
        p1.task("3 BuyCard")
        p2.task("BuyCard")
      }
      phase("Action")
    }

    p1.cardAction1("DevelopmentCenter")
    p1.playCard(
        "ImmigrantCity",
        1,
        steel = 5,
        "CityTile<Hellas_9_7>",
        "OceanTile<Hellas_5_6>",
    )

    // Check counts, shared stuff first

    assertThat(eng.counts("Generation")).containsExactly(3)
    assertThat(eng.counts("OceanTile, OxygenStep, TemperatureStep")).containsExactly(1, 0, 0)

    with(p1) {
      assertThat(count("TerraformRating")).isEqualTo(24)

      assertThat(production().values).containsExactly(5, 0, 0, 0, 0, 1).inOrder()

      assertThat(counts("M, S, T, P, E, H")).containsExactly(16, 3, 0, 0, 0, 1).inOrder()

      assertThat(counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
          .containsExactly(7, 12, 5, 4, 1)

      // tag abbreviations
      assertThat(counts("Tag, BUT, SPT, SCT, POT, EAT, JOT, CIT"))
          .containsExactly(16, 5, 1, 3, 1, 4, 1, 1)
          .inOrder()

      assertThat(counts("CityTile, GreeneryTile, SpecialTile")).containsExactly(1, 0, 0).inOrder()
    }

    with(p2) {
      assertThat(count("TerraformRating")).isEqualTo(25)

      assertThat(production().values).containsExactly(-4, 0, 1, 3, 1, 1).inOrder()

      assertThat(counts("M, S, T, P, E, H")).containsExactly(18, 0, 1, 6, 1, 3).inOrder()

      assertThat(counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
          .containsExactly(9, 5, 1, 2, 2)

      assertThat(counts("Tag, SPT, SCT, JOT, PLT")).containsExactly(6, 1, 3, 1, 1).inOrder()

      assertThat(counts("CityTile, GreeneryTile, SpecialTile")).containsExactly(0, 0, 0).inOrder()
    }

    // To check VPs we have to fake the game ending

    eng.operation("End FROM Phase") {
      // TODO why does P1 have 1 more point than I expect?
      // Should be 23 2 1 1 -1 / 25 1 1 1
      eng.assertCounts(27 to "VP<P1>", 28 to "VP<P2>")
      rollItBack()
    }
  }

  fun PlayerSession.counts(s: String) = s.split(",").map(::count)
}
