package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.exMachina
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmTest
import dev.martianzoo.tfm.tests.canonicalCatalog
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest

internal abstract class AbstractFullGameTest : TfmTest() {
  protected lateinit var p1: TfmGameplay
  protected lateinit var p2: TfmGameplay
  protected lateinit var p3: TfmGameplay

  protected abstract val config: GameConfig
  /** Pets declarations for concrete Players with sourced per-seat setup rules. */
  protected open val playerClassPets: String = ""
  protected open val catalog: TfmCatalog by lazy { canonicalCatalog(config) }

  @BeforeTest
  open fun commonSetup() {
    val premise = catalog.gamePremise(config, parseClasses(playerClassPets))
    game = Engine.newGame(premise)
    val players = game.actors.filterIsInstance<Player>()
    p1 = game.testTfm(players[0]).requireExplicitPaymentChoices()
    if (players.size > 1) p2 = game.testTfm(players[1]).requireExplicitPaymentChoices()
    if (players.size > 2) p3 = game.testTfm(players[2]).requireExplicitPaymentChoices()
  }

  /** Returns fresh gameplay for the Player occupying the one-based [seat]. */
  protected fun player(seat: Int): TfmGameplay {
    require(seat > 0) { "seat numbers begin at 1" }
    val player = game.actors.filterIsInstance<Player>().getOrNull(seat - 1)
    requireNotNull(player) { "no Player occupies seat $seat" }
    return game.testTfm(player)
  }

  private fun copyThis() {
    p1.assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
    p1.assertResources(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
    p1.assertDashMiddle(played = 0, actions = 0, vp = 0, tr = 0, hand = 0)
    p1.assertTags(but = 0, spt = 0) // ...
    p1.assertDashRight(events = 0, tagless = 0, cities = 0, colonies = 0)
    assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 0, venus = 0)
  }

  // Script-local counterparts live in
  // test/common/dev/martianzoo/tfm/script/StinaScriptTest.kt.
  protected fun TfmGameplay.assertProduction(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) {
    assertProds(
        m to "MC",
        s to "Steel",
        t to "Titanium",
        p to "Plant",
        e to "Energy",
        h to "Heat",
    )
  }

  protected fun TfmGameplay.assertResources(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) {
    assertCounts(
        m to "MC",
        s to "Steel",
        t to "Titanium",
        p to "Plant",
        e to "Energy",
        h to "Heat",
    )
  }

  protected fun TfmGameplay.assertCardResources(vararg resources: Pair<Int, ClassName>) {
    assertCounts(*resources.map { (count, card) -> count to "CardResource<$card>" }.toTypedArray())
  }

  protected fun TfmGameplay.assertUnusedActionCards(vararg cardNames: ClassName) {
    val expectedUnusedActionCards = cardNames.toSet()
    val unusedActionCards =
        reader
            .getComponents(resolve("ActionCard"))
            .elements
            .filter { count("ActionUsedMarker<${it.className}>") == 0 }
            .map { it.className }
            .toSet()
    unusedActionCards shouldBe expectedUnusedActionCards
  }

  /** Reproduces an evidenced player mistake without leaving a task selected against stale state. */
  protected fun TfmGameplay.exMachina(adjustment: String) {
    agents.exMachina(actor, adjustment)
  }

  protected fun retainStartingProjects(vararg retainedCounts: Int) {
    dev.martianzoo.tfm.tests.retainStartingProjects(game, *retainedCounts)
  }

  protected fun TfmGameplay.assertDashMiddle(
      played: Int,
      actions: Int? = null,
      vp: Int,
      tr: Int,
      hand: Int,
  ) {
    assertCounts(
        hand to "ProjectCard",
        tr to "TerraformRating",
        played to "CardFront OR PlayedEvent",
    )
    if (actions != null) {
      count("ActionCard") - count("ActionUsedMarker") shouldBe actions
    }
    assertVps(vp)
  }

  protected fun TfmGameplay.assertDashRight(
      events: Int,
      tagless: Int,
      cities: Int,
      colonies: Int = 0,
  ) {
    assertCounts(
        events to "PlayedEvent",
        tagless to "CardFront(HAS MAX 0 Tag)",
        cities to "CityTile",
    )
    if (
        game.classTable.isActive(cn("ColoniesExpansion")) &&
            game.reader.getComponents("ColoniesExpansion").isNotEmpty()
    ) {
      assertCounts(colonies to "Colony")
    }
  }

  protected fun assertSidebar(gen: Int, temp: Int, oxygen: Int, oceans: Int, venus: Int = -1) {
    admin.assertCounts(gen to "Generation")
    admin.temperatureC() shouldBe temp
    admin.oxygenPercent() shouldBe oxygen
    admin.assertCounts(oceans to "OceanTile")
    if (venus != -1) {
      admin.venusPercent() shouldBe venus
    }
  }

  private fun TfmGameplay.assertVps(expected: Int) {
    val onTransactionComplete = game.onTransactionComplete
    val checkpoint = game.timeline.checkpoint()
    val autoExecPolicys = game.actors.associateWith { game.testAgent(it).autoExecPolicy }
    game.onTransactionComplete = {}
    try {
      game.actors.forEach { game.testAgent(it).autoExecPolicy = EAGER }
      dropPendingTasksForSnapshot()
      admin.phase("Production") { dropPendingTasksForSnapshot() }
      admin.runOperation("End FROM Phase") { dropPendingTasksForSnapshot() }
      assertCounts(expected to "VictoryPoint")
    } finally {
      game.timeline.rollBack(checkpoint)
      autoExecPolicys.forEach { (actor, mode) -> game.testAgent(actor).autoExecPolicy = mode }
      game.onTransactionComplete = onTransactionComplete
    }
  }

  // Pending choices describe future play, so a snapshot must neither execute nor count them.
  // Unbought research cards need to leave Selecting before task removal; the
  // enclosing checkpoint restores both the components and tasks afterward.
  private fun dropPendingTasksForSnapshot() {
    game.actors
        .filterIsInstance<Player>()
        .map { game.testTfm(it) }
        .filter { it.count("ProjectCard<Selecting>") > 0 }
        .forEach { it.buyCards(0) }
    game.tasks
        .extract { it.id to it.assignee }
        .forEach { (id, assignee) ->
          game.testAgent(assignee).dropTask(id)
        }
  }
}

internal fun TfmGameplay.assertTags(
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
    cit: Int = 0,
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
      cit to "CityTag",
      vet to "VenusTag",
  )
}
