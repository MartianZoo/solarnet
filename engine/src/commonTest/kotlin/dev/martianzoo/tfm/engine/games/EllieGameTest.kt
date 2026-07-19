package dev.martianzoo.tfm.engine.games

import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class EllieGameTest : AbstractFullGameTest() {
  override fun setup() = Canon.fromOptionCodes("BRHXP", 2)

  @Test
  fun ellieGame() {
    val workflow = TfmWorkflow.Auto(game, setup()).launch()
    workflow.isRunning shouldBe true

    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    p1.playPrelude("UnmiContractor") // 3 TR<P1>
    p1.playPrelude("CorporateArchives")
    p2.playPrelude("BiosphereSupport")
    p2.playPrelude("SocietySupport")

    // Generation 1 (P1 first)

    p1.playProject("MediaGroup", 6)
    p1.playProject("Sabotage", 1) { doTask("-7 M<P2>") }

    p2.playProject("Research", 11) // 1 VP<P2>, 2 TR<P2>
    p2.playProject("MartianSurvey", 9) { doTask("Ok") } // ain't gon flip; 1 VP<P2>

    p1.pass()

    p2.playProject("SearchForLife", 3) {
      doTask("PlayedEvent<Class<PharmacyUnion>> FROM PharmacyUnion THEN 3 TR") // 3 TR<P2>
    }
    p2.cardAction1("SearchForLife") { doTask("Ok") } // no microbe

    p2.pass()

    // Generation 2 (P2 first)
    p1.doFirstTask("1 BuyCard")
    p2.doFirstTask("3 BuyCard")

    p2.sellPatents(1)
    p2.playProject("VestaShipyard", 15) // 1 VP<P2>

    p1.playProject("EarthCatapult", 23) // 2 VP<P1>
    p1.playProject("OlympusConference", steel = 4) // 1 VP<P1>

    p2.pass()

    p1.playProject("DevelopmentCenter", 1, steel = 4) {
      doTask("ProjectCard FROM Science<OlympusConference>")
    }
    p1.playProject("GeothermalPower", 1, steel = 4)

    p1.playProject("MirandaResort", 10) // 1 VP<P1>
    p1.playProject("Hackers", 1) { doTask("PROD[-2 M<P2>]") } // -1 VP<P1>

    p1.playProject("MicroMills", 1)
    p1.declineSecondAction()

    p1.pass()

    // Generation 3 (P1 first)
    p1.doFirstTask("3 BuyCard")
    p2.doFirstTask("1 BuyCard")

    p1.cardAction1("DevelopmentCenter")
    p1.playProject("ImmigrantCity", 1, steel = 5) {
      doTask("CityTile<Hellas_9_7>")
      doTask("OceanTile<Hellas_5_6>") // 1 TR<P1>
    }
    workflow.shutdown()
    workflow.isRunning shouldBe false

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

    engine.phase("End")

    val sum = Summarizer(game)
    sum.net("GreeneryTile", "VictoryPoint") shouldBe 0
    sum.net("CityTile", "VictoryPoint") shouldBe 0

    p1.assertCounts(24 to "TR<P1>")
    p1.assertCounts(27 to "VP<P1>")
    sum.net("Card", "VP<P1>") shouldBe 3

    p2.assertCounts(25 to "TR<P2>")
    sum.net("PharmacyUnion", "TR<P2>") shouldBe 5

    p2.assertCounts(28 to "VictoryPoint")
    sum.net("Card", "VP<P2>") shouldBe 3
  }

  @Test
  fun earlyGameWithNoPrelude() {
    val setup = Canon.fromOptionCodes("BRHX", 2)
    val game = Engine.newGame(setup)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    TfmWorkflow.Auto(game, setup).launch()

    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    p1.playProject("MediaGroup", 6)
    p1.playProject("Sabotage", 1) { doTask("-7 M<Player2>") }

    p2.playProject("Research", 11)
  }
}
