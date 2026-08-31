package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GameEvent.TaskEditedEvent
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.TEST_CLASS_SYNONYMS
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmTest
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest

internal abstract class AbstractFullGameTest : TfmTest() {
  protected lateinit var p1: TfmGameplay
  protected lateinit var p2: TfmGameplay
  protected lateinit var p3: TfmGameplay

  protected abstract val config: GameConfig
  protected open val catalog: TfmCatalog = Canon
  protected open val inputOnlySynonyms: List<Pair<String, String>> = TEST_CLASS_SYNONYMS

  @BeforeTest
  open fun commonSetup() {
    game = Engine.newGame(catalog.gamePremise(config), inputOnlySynonyms = inputOnlySynonyms)
    val players = game.actors.filterIsInstance<Player>()
    p1 = game.tfm(players[0]).requireExplicitPaymentChoices()
    if (players.size > 1) p2 = game.tfm(players[1]).requireExplicitPaymentChoices()
    if (players.size > 2) p3 = game.tfm(players[2]).requireExplicitPaymentChoices()
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
    val selectedId = game.tasks.selectedTask()
    if (selectedId != null) {
      val selectedTask = game.tasks.getTaskData(selectedId)
      var expectedTask = selectedTask
      val unselectedTask =
          game.events
              .entriesSince(Checkpoint(0))
              .asReversed()
              .asSequence()
              .map { event ->
                check(event is TaskEditedEvent && event.task == expectedTask) {
                  "unexpected event after selection of task $selectedId: $event"
                }
                if (!event.oldTask.selected && event.task.selected) return@map event.oldTask

                check(event.task == event.oldTask.copy(whyPending = event.task.whyPending)) {
                  "unexpected edit after selection of task $selectedId: $event"
                }
                expectedTask = event.oldTask
                null
              }
              .firstNotNullOf { it }

      game.tasks.editTask(unselectedTask)
    }

    godMode().sneak(adjustment)

    if (selectedId != null) {
      val task = game.tasks.getTaskData(selectedId)
      game.agent(task.assignee).selectTask(selectedId)
      game.agent(task.assignee).autoExecNow()
    }
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
    engine.assertCounts(gen to "Generation")
    engine.temperatureC() shouldBe temp
    engine.oxygenPercent() shouldBe oxygen
    engine.assertCounts(oceans to "OceanTile")
    if (venus != -1) {
      engine.venusPercent() shouldBe venus
    }
  }

  private fun TfmGameplay.assertVps(expected: Int) {
    val onAtomicComplete = game.onAtomicComplete
    val checkpoint = game.timeline.checkpoint()
    val autoExecModes = game.actors.associateWith { game.agent(it).autoExecMode }
    game.onAtomicComplete = {}
    try {
      game.actors.forEach { game.agent(it).autoExecMode = FIRST }
      dropPendingTasksForSnapshot()
      engine.phase("Production") { dropPendingTasksForSnapshot() }
      engine.phase("End") {
        dropPendingTasksForSnapshot()
        assertCounts(expected to "VictoryPoint")
      }
    } finally {
      game.timeline.rollBack(checkpoint)
      autoExecModes.forEach { (actor, mode) -> game.agent(actor).autoExecMode = mode }
      game.onAtomicComplete = onAtomicComplete
    }
  }

  // Pending choices describe future play, so a snapshot must neither execute nor count them.
  // Unbought research cards need to leave their temporary locations before task removal; the
  // enclosing checkpoint restores both the components and tasks afterward.
  private fun dropPendingTasksForSnapshot() {
    game.actors
        .filterIsInstance<Player>()
        .map { game.tfm(it) }
        .filter { it.count("ProjectCard<Selecting>") > 0 }
        .forEach { it.buyCards(0) }
    game.tasks
        .extract { it.assignee }
        .toSet()
        .forEach {
          game.agent(it).godMode().dropTasks()
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
