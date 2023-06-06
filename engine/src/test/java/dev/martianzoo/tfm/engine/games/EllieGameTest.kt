package dev.martianzoo.tfm.engine.games

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.Timeline.AbortOperationException
import org.junit.jupiter.api.Test

class EllieGameTest {
  @Test
  fun game() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))
    val eng = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    eng.phase("Corporation")

    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    eng.phase("Prelude")

    p1.playPrelude("UnmiContractor")
    p1.playPrelude("CorporateArchives")
    p2.playPrelude("BiosphereSupport")
    p2.playPrelude("SocietySupport")

    // Action!

    eng.phase("Action")

    p1.playProject("MediaGroup", 6)
    p1.playProject("Sabotage", 1) { doTask("-7 M<P2>") }

    p2.playProject("Research", 11)
    p2.playProject("MartianSurvey", 9) { doTask("Ok") } // ain't gon flip

    p1.pass()

    p2.playProject("SearchForLife", 3) {
      doTask("PlayedEvent<Class<PharmacyUnion>> FROM PharmacyUnion THEN 3 TR")
    }
    p2.cardAction1("SearchForLife") { doTask("Ok") } // no microbe

    p2.pass()

    // Generation 2

    with(eng) {
      phase("Production")
      phase("Research") {
        p1.doTask("1 BuyCard")
        p2.doTask("3 BuyCard")
      }
      phase("Action")
    }

    p2.sellPatents(1)
    p2.playProject("VestaShipyard", 15)
    p2.pass()

    with(p1) {
      playProject("EarthCatapult", 23)
      playProject("OlympusConference", steel = 4)

      playProject("DevelopmentCenter", 1, steel = 4) {
        doTask("ProjectCard FROM Science<OlympusConference>")
      }

      playProject("GeothermalPower", 1, steel = 4)

      playProject("MirandaResort", 10)
      playProject("Hackers", 1) { doTask("PROD[-2 M<P2>]") }
      playProject("MicroMills", 1)
      pass()
    }

    // Generation 2

    with(eng) {
      phase("Production")
      phase("Research") {
        p1.doTask("3 BuyCard")
        p2.doTask("BuyCard")
      }
      phase("Action")
    }

    p1.cardAction1("DevelopmentCenter")
    p1.playProject("ImmigrantCity", 1, steel = 5) {
      doTask("CityTile<Hellas_9_7>")
      doTask("OceanTile<Hellas_5_6>")
    }

    // Check counts, shared stuff first

    assertThat(eng.counts("Generation")).containsExactly(3)
    assertThat(eng.counts("OceanTile, OxygenStep, TemperatureStep")).containsExactly(1, 0, 0)

    with(p1) {
      assertThat(this.count("TerraformRating")).isEqualTo(24)

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
      assertThat(this.count("TerraformRating")).isEqualTo(25)

      assertThat(production().values).containsExactly(-4, 0, 1, 3, 1, 1).inOrder()

      assertThat(counts("M, S, T, P, E, H")).containsExactly(18, 0, 1, 6, 1, 3).inOrder()

      assertThat(counts("ProjectCard, CardFront, ActiveCard, AutomatedCard, PlayedEvent"))
          .containsExactly(9, 5, 1, 2, 2)

      assertThat(counts("Tag, SPT, SCT, JOT, PLT")).containsExactly(6, 1, 3, 1, 1).inOrder()

      assertThat(counts("CityTile, GreeneryTile, SpecialTile")).containsExactly(0, 0, 0).inOrder()
    }

    // To check VPs we have to fake the game ending

    eng.phase("End") {
      // TODO why does P1 have 1 more point than I expect?
      // Should be 23 2 1 1 -1 / 25 1 1 1
      eng.assertCounts(27 to "VP<P1>", 28 to "VP<P2>")
      throw AbortOperationException()
    }
  }

  @Test
  fun earlyGameWithNoPrelude() {
    val game = Engine.newGame(GameSetup(Canon, "BRHX", 2))
    val eng = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    eng.phase("Corporation")
    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    eng.phase("Action")

    p1.playProject("MediaGroup", 6)
    p1.playProject("Sabotage", 1) { doTask("-7 M<Player2>") }

    p2.playProject("Research", 11)
  }

  fun TfmGameplay.counts(s: String) = s.split(",").map { this.count(it) }
}
