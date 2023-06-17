package dev.martianzoo.tfm.engine.games

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Game
import dev.martianzoo.engine.Timeline.AbortOperationException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.BeforeEach

abstract class AbstractSoloTest {
  protected lateinit var game: Game
  protected lateinit var engine: TfmGameplay
  protected lateinit var me: TfmGameplay
  protected lateinit var opponent: TfmGameplay

  @BeforeEach
  fun commonSetup() {
    game = Engine.newGame(GameSetup(Canon, "BRHVPX", 2))
    engine = game.tfm(ENGINE)
    me = game.tfm(PLAYER1)
    opponent = game.tfm(PLAYER2)

    me.godMode().sneak("-6 TR")

    opponent.godMode().sneak("99, 99 S, 99 T, 99 P, 99 E, 99 H")
    opponent.godMode().sneak("PROD[99, 99 S, 99 T, 99 P, 99 E, 99 H]")
  }

  fun copyThis() {
    assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
    assertResources(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
    assertDashMiddle(played = 0, actions = 0, vp = 0, tr = 0, hand = 0)
    assertTags(but = 0, spt = 0) // ...
    assertDashRight(events = 0, tagless = 0, cities = 0)
    assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 0, venus = 0)
  }

  protected fun TaskResult.expect(string: String) =
      TestHelpers.assertNetChanges(this, game, me, string)

  protected fun assertCounts(vararg pairs: Pair<Int, String>) = me.assertCounts(*pairs)

  protected fun assertProduction(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) {
    me.assertProds(m to "M", s to "S", t to "T", p to "P", e to "E", h to "H")
  }

  protected fun assertResources(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) {
    me.assertCounts(m to "M", s to "S", t to "T", p to "P", e to "E", h to "H")
  }

  protected fun assertDashMiddle(played: Int, actions: Int, vp: Int, tr: Int, hand: Int) {
    me.assertCounts(hand to "ProjectCard", tr to "TR", played to "CardFront + PlayedEvent")
    assertActions(actions)
    assertVps(vp)
  }

  protected fun assertTags(
      but: Int = 0,
      spt: Int = 0,
      sct: Int = 0,
      pot: Int = 0,
      eat: Int = 0,
      jot: Int = 0,
      vet: Int = 0,
      plt: Int = 0,
      mit: Int = 0,
      ant: Int = 0,
      cit: Int = 0
  ) {
    me.assertCounts(
        but to "BuildingTag",
        spt to "SpaceTag",
        sct to "ScienceTag",
        pot to "PowerTag",
        eat to "EarthTag",
        jot to "JovianTag",
        vet to "VenusTag",
        plt to "PlantTag",
        mit to "MicrobeTag",
        ant to "AnimalTag",
        cit to "CityTag")
  }

  protected fun assertDashRight(events: Int, tagless: Int, cities: Int) {
    assertCounts(
        events to "PlayedEvent", tagless to "CardFront(HAS MAX 0 Tag)", cities to "CityTile")
  }

  protected fun assertSidebar(gen: Int, temp: Int, oxygen: Int, oceans: Int, venus: Int) {
    assertCounts(gen to "Generation")
    assertThat(me.temperatureC()).isEqualTo(temp)
    assertThat(me.oxygenPercent()).isEqualTo(oxygen)
    assertCounts(oceans to "OceanTile")
    assertThat(me.venusPercent()).isEqualTo(venus)
  }

  protected fun assertVps(expected: Int) {
    engine.phase("End") { // TODO should really do production too!
      me.assertCounts(expected to "VP")
      throw AbortOperationException()
    }
  }

  protected fun assertActions(expected: Int) {
    assertThat(me.count("ActionCard") - me.count("ActionUsedMarker")).isEqualTo(expected)
  }
}
