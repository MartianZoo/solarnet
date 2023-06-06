package dev.martianzoo.tfm.engine.games

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.analysis.Summarizer
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.Timeline.AbortOperationException
import org.junit.jupiter.api.Test

class Game20230521Test {
  // @Test // for profiling
  fun games() = repeat(10) { game() }

  @Test
  fun game() {
    val game = Engine.newGame(GameSetup(Canon, "BRMVPXCT", 2))
    val engine = game.tfm(Player.ENGINE)

    // https://terraforming-mars.herokuapp.com/player?id=p34f0f06d4ba2
    val blue = game.tfm(Player.PLAYER1)

    // https://terraforming-mars.herokuapp.com/player?id=p938b42ad50a9
    val purp = game.tfm(Player.PLAYER2)

    fun newGeneration(cards1: Int, cards2: Int) {
      with(engine) {
        phase("Production")
        phase("Research") {
          blue.doTask(if (cards1 > 0) "$cards1 BuyCard" else "Ok")
          purp.doTask(if (cards2 > 0) "$cards2 BuyCard" else "Ok")
        }
        phase("Action")
      }
    }

    // TODO list cards drawn in comments, for later

    blue.playCorp("Manutech", 5)
    purp.playCorp("Factorum", 4)

    engine.phase("Prelude")

    with(blue) {
      playPrelude("NewPartner") { playPrelude("UnmiContractor") }
      playPrelude("AlliedBank")
    }

    with(purp) {
      playPrelude("AcquiredSpaceAgency")
      playPrelude("IoResearchOutpost")
    }

    engine.phase("Action")

    blue.playProject("InventorsGuild", 9)
    blue.cardAction1("InventorsGuild") { doTask("BuyCard") }

    purp.playProject("ArcticAlgae", 12)
    purp.cardAction1("Factorum")

    blue.stdProject("PowerPlantSP")
    blue.playProject("BuildingIndustries", 4, steel = 1)
    blue.pass()

    purp.playProject("RotatorImpacts", titanium = 2)
    purp.cardAction1("RotatorImpacts") { doTask("2 Pay<Class<T>> FROM T") }
    purp.godMode().sneak("6") // the titanium were supposed to fill that TODO

    purp.playProject("CarbonateProcessing", 6)
    purp.playProject("Archaebacteria", 6)
    purp.pass()

    newGeneration(2, 2)

    // Check the same values we see in the herokuapp UI

    with(blue) {
      assertProds(5 to "M", 3 to "S", 0 to "T", 0 to "P", 0 to "E", 0 to "H")
      assertCounts(23 to "M", 5 to "S", 0 to "T", 0 to "P", 0 to "E", 1 to "H")
      assertDashMiddle(played = 6, actions = 1, vp = 23, tr = 23, hand = 7)
      assertTags(2 to "BUT", 1 to "SCT", 2 to "EAT")
      assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    with(purp) {
      assertProds(0 to "M", 1 to "S", 1 to "T", 1 to "P", 0 to "E", 3 to "H")
      assertCounts(15 to "M", 1 to "S", 3 to "T", 2 to "P", 0 to "E", 3 to "H")
      assertDashMiddle(played = 7, actions = 2, vp = 20, tr = 20, hand = 5)
      assertTags(2 to "BUT", 1 to "SPT", 1 to "SCT", 1 to "POT", 1 to "JOT", 1 to "PLT", 1 to "MIT")
      assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(2, -30, 0, 0, 0)

    purp.cardAction2("Factorum")
    purp.playProject("MarsUniversity", 6, steel = 1)

    blue.cardAction1("InventorsGuild") { doFirstTask("BuyCard") }
    blue.playProject("EarthOffice", 1)

    purp.cardAction2("RotatorImpacts")
    purp.pass()

    blue.stdProject("PowerPlantSP")
    blue.playProject("DevelopmentCenter", 1, steel = 5)
    blue.cardAction1("DevelopmentCenter")
    blue.playProject("InvestmentLoan", 0)
    blue.playProject("DeuteriumExport", 11)
    blue.cardAction1("DeuteriumExport")
    blue.pass()

    newGeneration(2, 2)

    with(blue) {
      assertProds(4 to "M", 3 to "S", 0 to "T", 0 to "P", 1 to "E", 0 to "H")
      assertCounts(27 to "M", 3 to "S", 0 to "T", 0 to "P", 1 to "E", 1 to "H")
      assertDashMiddle(played = 10, actions = 3, vp = 23, tr = 23, hand = 7)
      assertTags(3 to "BUT", 1 to "SPT", 2 to "SCT", 1 to "POT", 3 to "EAT", 1 to "VET")
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    with(purp) {
      assertProds(0 to "M", 1 to "S", 1 to "T", 1 to "P", 0 to "E", 3 to "H")
      assertCounts(21 to "M", 1 to "S", 4 to "T", 3 to "P", 0 to "E", 6 to "H")
      assertDashMiddle(played = 8, actions = 2, vp = 22, tr = 21, hand = 7)
      assertTags(3 to "BUT", 1 to "SPT", 2 to "SCT", 1 to "POT", 1 to "JOT", 1 to "PLT", 1 to "MIT")
      assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(3, -30, 0, 0, 2)

    blue.cardAction1("DevelopmentCenter")
    blue.cardAction1("InventorsGuild") { doFirstTask("Ok") }

    purp.cardAction1("Factorum")
    purp.playProject("AsteroidCard", 2, steel = 0, titanium = 4) { doFirstTask("Ok") }

    blue.playProject("CorporateStronghold", 5, steel = 3) { doTask("CityTile<Tharsis_4_6>") }
    blue.playProject("OptimalAerobraking", 7)

    purp.playProject("TransNeptuneProbe", 0, titanium = 2)
    purp.cardAction1("RotatorImpacts") { doFirstTask("Ok") } // titanium, sigh

    blue.cardAction2("DeuteriumExport")
    blue.playProject("ImportedGhg", 4)

    newGeneration(0, 0)
    with(blue) {
      assertProds(7 to "M", 3 to "S", 0 to "T", 0 to "P", 1 to "E", 1 to "H")
      assertCounts(47 to "M", 3 to "S", 0 to "T", 1 to "P", 1 to "E", 10 to "H")
      assertDashMiddle(played = 13, actions = 3, vp = 21, tr = 23, hand = 5)
      assertTags(4 to "BUT", 2 to "SPT", 2 to "SCT", 1 to "POT", 3 to "EAT", 1 to "VET", 1 to "CIT")
      assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 1 to "CityTile")
    }

    with(purp) {
      assertProds(0 to "M", 1 to "S", 1 to "T", 1 to "P", 1 to "E", 3 to "H")
      assertCounts(35 to "M", 2 to "S", 1 to "T", 4 to "P", 1 to "E", 9 to "H")
      assertDashMiddle(played = 10, actions = 2, vp = 24, tr = 22, hand = 5)
      assertTags(3 to "BUT", 2 to "SPT", 3 to "SCT", 1 to "POT", 1 to "JOT", 1 to "PLT", 1 to "MIT")
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(4, -28, 0, 0, 2)

    val summer = Summarizer(game)

    // AA's effect never triggered yet (no oceans), so it only generated 1 plant
    assertThat(summer.net("ArcticAlgae", "Plant")).isEqualTo(1)

    // Blue has done 11 card buys: 5 initial, 4 in research, and 2 from inventors guild
    assertThat(summer.net("BuyCard<P1>", "Card<P1>")).isEqualTo(11)

    // DeuteriumExport produced a net of 0 floaters (made one then consumed it)
    assertThat(summer.net("DeuteriumExport", "Floater")).isEqualTo(0)
    assertThat(summer.net("DeuteriumExport", "Production<Class<Energy>>")).isEqualTo(1)

    // EarthOffice saved blue 6 money (InvestmentLoan, ImportedGhg)
    assertThat(summer.net("EarthOffice", "Owed<P1>")).isEqualTo(-6)

    // Manutech has delivered! 1 MC with NewPartner, 4 with AlliedBank, 3 with CorporateStronghold
    // ... plus of course 35 at game start
    assertThat(summer.net("Manutech", "Megacredit<P1>")).isEqualTo(43)

    // Purple got 63 MC from TR (at production phases they had 20, 21, and 22 TR)
    assertThat(summer.net("TerraformRating", "Megacredit<P2>")).isEqualTo(63)
    assertThat(summer.net("TerraformRating<P2>", "Megacredit")).isEqualTo(63)
    assertThat(summer.net("TerraformRating<P2>", "Megacredit<P2>")).isEqualTo(63)
    assertThat(summer.net("TerraformRating", "Megacredit")).isEqualTo(132)

    // Blue never raised a global param, but purple did temp & venus
    assertThat(summer.net("GlobalParameter", "TR<P1>")).isEqualTo(0)
    assertThat(summer.net("GlobalParameter", "TR<P2>")).isEqualTo(2)

    // This is ludicrous
    // assertThat(summer.net("Component", "Component")).isEqualTo(163)
  }

  fun TfmGameplay.assertTags(vararg pair: Pair<Int, String>) {
    assertCounts(*pair)
    assertThat(this.count("Tag")).isEqualTo(pair.toList().sumOf { it.first })
  }

  fun TfmGameplay.assertSidebar(gen: Int, temp: Int, oxygen: Int, oceans: Int, venus: Int) {
    assertCounts(gen to "Generation")
    assertThat(temperatureC()).isEqualTo(temp)
    assertThat(oxygenPercent()).isEqualTo(oxygen)
    assertCounts(oceans to "OceanTile")
    assertThat(venusPercent()).isEqualTo(venus)
  }

  fun TfmGameplay.assertDashMiddle(played: Int, actions: Int, vp: Int, tr: Int, hand: Int) {
    assertCounts(hand to "ProjectCard", tr to "TR", played to "CardFront + PlayedEvent")
    assertActions(actions)
    assertVps(vp)
  }

  fun TfmGameplay.assertVps(expected: Int) {
    godMode().manual("End FROM Phase") {
      autoExecNow()
      assertCounts(expected to "VP")
      throw AbortOperationException()
    }
  }

  fun TfmGameplay.assertActions(expected: Int) {
    assertThat(this.count("ActionCard") - this.count("ActionUsedMarker"))
        .isEqualTo(expected)
  }
}
