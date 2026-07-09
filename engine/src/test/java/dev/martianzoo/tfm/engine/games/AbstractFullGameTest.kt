package dev.martianzoo.tfm.engine.games

import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Game
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.BeforeTest
import io.kotest.matchers.shouldBe

abstract class AbstractFullGameTest {
  protected lateinit var game: Game
  protected lateinit var engine: TfmGameplay
  protected lateinit var p1: TfmGameplay
  protected lateinit var p2: TfmGameplay

  protected abstract fun setup(): GameSetup

  @BeforeTest
  open fun commonSetup() {
    game = Engine.newGame(setup())
    engine = game.tfm(ENGINE)
    p1 = game.tfm(PLAYER1)
    p2 = game.tfm(PLAYER2)
  }

  fun copyThis() {
    p1.assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
    p1.assertResources(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
    p1.assertDashMiddle(played = 0, actions = 0, vp = 0, tr = 0, hand = 0)
    p1.assertTags(but = 0, spt = 0) // ...
    p1.assertDashRight(events = 0, tagless = 0, cities = 0, colonies = 0)
    assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 0, venus = 0)
  }

  protected fun TaskResult.expect(string: String) =
      TestHelpers.assertNetChanges(this, game, engine, string)

  protected fun TfmGameplay.assertProduction(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) {
    assertProds(m to "M", s to "S", t to "T", p to "P", e to "E", h to "H")
  }

  protected fun TfmGameplay.assertResources(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) {
    assertCounts(m to "M", s to "S", t to "T", p to "P", e to "E", h to "H")
  }

  protected fun TfmGameplay.assertDashMiddle(
      played: Int,
      actions: Int,
      vp: Int,
      tr: Int,
      hand: Int
  ) {
    assertCounts(hand to "ProjectCard", tr to "TR", played to "CardFront + PlayedEvent")
    assertActions(actions)
    // assertVps requires an empty task queue (needs to transition to EndPhase); skip when busy
    if (game.tasks.isEmpty()) assertVps(vp)
  }

  protected fun TfmGameplay.assertTags(
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
    assertCounts(
        but to "BuildingTag",
        spt to "SpaceTag",
        sct to "ScienceTag",
        pot to "PowerTag",
        eat to "EarthTag",
        jot to "JovianTag",
        plt to "PlantTag",
        mit to "MicrobeTag",
        ant to "AnimalTag",
        cit to "CityTag")
    if (cn("VenusTag") in game.classes.allClassNamesAndIds) {
      assertCounts(vet to "VenusTag")
    }
  }

  protected fun TfmGameplay.assertDashRight(
      events: Int,
      tagless: Int,
      cities: Int,
      colonies: Int = 0
  ) {
    assertCounts(
        events to "PlayedEvent", tagless to "CardFront(HAS MAX 0 Tag)", cities to "CityTile")
    if ("C" in setup().bundles) assertCounts(colonies to "Colony")
  }

  protected fun assertSidebar(gen: Int, temp: Int, oxygen: Int, oceans: Int, venus: Int = -1) {
    engine.assertCounts(gen to "Generation")
    engine.temperatureC() shouldBe temp
    engine.oxygenPercent() shouldBe oxygen
    engine.assertCounts(oceans to "OceanTile")
    if (venus != -1) {
      engine.venusPercent() shouldBe venus
    }
  }

  protected fun TfmGameplay.assertVps(expected: Int) {
    // TODO: rework so assertVps always runs (currently skipped when tasks are pending):
    //  1. Save/replace game.onAtomicComplete with {} to suspend the workflow coroutine.
    //  2. Take a timeline checkpoint, then drain pending tasks via doTask("Ok") (or
    //     reviseTask(id, "Ok") if tasks are mandatory and can't be revised to Ok — verify).
    //  3. Call engine.phase("End") to score VPs, then rollBack(checkpoint) to undo everything.
    //  4. Restore game.onAtomicComplete in finally. Also: should really do production phase too.
    engine.phase("End") {
      assertCounts(expected to "VP")
      abort()
    }
  }

  protected fun TfmGameplay.assertActions(expected: Int) {
    count("ActionCard") - count("ActionUsedMarker") shouldBe expected
  }
}
