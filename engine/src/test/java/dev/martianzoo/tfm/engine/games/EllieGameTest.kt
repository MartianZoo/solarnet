package dev.martianzoo.tfm.engine.games

import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class EllieGameTest : AbstractFullGameTest() {
  override fun setup() = GameSetup(Canon, "BRHXP", 2)

  @Test
  fun game() {
    engine.phase("Corporation")

    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    engine.phase("Prelude")

    p1.playPrelude("UnmiContractor")
    p1.playPrelude("CorporateArchives")
    p2.playPrelude("BiosphereSupport")
    p2.playPrelude("SocietySupport")

    // Action!

    engine.phase("Action")

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

    engine.nextGeneration(1, 3)

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

    engine.nextGeneration(3, 1)

    p1.cardAction1("DevelopmentCenter")
    p1.playProject("ImmigrantCity", 1, steel = 5) {
      doTask("CityTile<Hellas_9_7>")
      doTask("OceanTile<Hellas_5_6>")
    }

    // Check counts, shared stuff first

    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 1)

    with(p1) {
      assertCounts(24 to "TerraformRating")
      assertProduction(m = 5, s = 0, t = 0, p = 0, e = 0, h = 1)
      assertResources(m = 16, s = 3, t = 0, p = 0, e = 0, h = 1)
      assertCounts(7 to "ProjectCard", 12 to "CardFront")
      assertCounts(5 to "ActiveCard", 4 to "AutomatedCard", 1 to "PlayedEvent")
      assertTags(but = 5, spt = 1, sct = 3, pot = 1, eat = 4, jot = 1, cit = 1)
      assertCounts(1 to "CityTile", 0 to "GreeneryTile", 0 to "SpecialTile")
    }

    with(p2) {
      assertCounts(25 to "TerraformRating")
      assertProduction(m = -4, s = 0, t = 1, p = 3, e = 1, h = 1)
      assertResources(m = 18, s = 0, t = 1, p = 6, e = 1, h = 3)
      assertCounts(9 to "ProjectCard", 5 to "CardFront")
      assertCounts(1 to "ActiveCard", 2 to "AutomatedCard", 2 to "PlayedEvent")
      assertTags(spt = 1, sct = 3, jot = 1, plt = 1)
      assertCounts(0 to "CityTile", 0 to "GreeneryTile", 0 to "SpecialTile")
    }

    // TODO why does P1 have 1 more point than I expect?
    // Should be 23 2 1 1 -1 / 25 1 1 1
    p1.assertVps(27)
    p2.assertVps(28)
  }

  @Test
  fun earlyGameWithNoPrelude() {
    val game = Engine.newGame(GameSetup(Canon, "BRHX", 2))
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    engine.phase("Corporation")
    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    engine.phase("Action")

    p1.playProject("MediaGroup", 6)
    p1.playProject("Sabotage", 1) { doTask("-7 M<Player2>") }

    p2.playProject("Research", 11)
  }
}
