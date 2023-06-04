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
import dev.martianzoo.tfm.engine.OldTfmHelpers.turn
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test

class EllieGameTest {
  @Test
  fun game() {
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
      abortAndRollBack()
    }
  }

  @Test
  fun earlyGameWithNoPrelude() {
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

  fun PlayerSession.counts(s: String) = s.split(",").map(::count)
}
