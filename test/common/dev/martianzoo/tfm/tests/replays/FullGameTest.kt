package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.Agent
import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.GameFileImporter
import dev.martianzoo.agenttestsupport.withTestSoloSetup
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.engine.recording
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.RecordedInput
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.canonicalCatalog
import dev.martianzoo.tfm.tests.withTestTurmoilDeck
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

/** Shared replay setup, assertions, and recording checks using plain Agents. */
internal abstract class FullGameTest(playerPolicy: AutoExecPolicy = NONE) :
    TfmGameplayTest(playerPolicy) {
  protected open val p1: Agent
    get() = player(1)

  protected open val p2: Agent
    get() = player(2)

  protected open val p3: Agent
    get() = player(3)

  protected lateinit var gamePremise: GamePremise
    private set

  protected abstract val config: GameConfig
  internal var exportReplay: Boolean = false
  private var expectedPendingTasksAtEnd: Int = 0

  /** States how many tasks the source position intentionally leaves pending. */
  protected fun expectPendingTasksAtEnd(count: Int) {
    require(count > 0) { "Use the default check when no tasks should remain" }
    expectedPendingTasksAtEnd = count
  }

  internal fun assertPendingTasksAtEnd() {
    val pending = game.tasks.extract { it }
    assertEquals(
        expectedPendingTasksAtEnd,
        pending.size,
        "Unexpected pending tasks at replay end:\n${pending.joinToString("\n")}",
    )
  }

  internal suspend fun verifyGameFileRoundTrip(json: String) {
    val document = GameFileJson.parse(json)
    val orderedEvents = gamePremise.drawOrder.map { it.toString() }.toSet()
    val suppliedEventsOrNeutralDelegates =
        document.inputs(gamePremise).filterIsInstance<RecordedInput.Command>().filter { command ->
          command.actor == ADMIN &&
              (command.instruction in orderedEvents ||
                  command.instruction.startsWith("PartyLeader<") ||
                  command.instruction.startsWith("PartyFollower<"))
        }
    assertEquals(emptyList(), suppliedEventsOrNeutralDelegates)
    val importedAgents =
        GameFileImporter.replay(
            document,
            catalog,
            newAgents = { Agents(TfmEngine.newGame(it)) },
            startGame = { agents ->
              agents.taskLog.gameStarted()
              agents[ADMIN].beginOperation("SetupPhase FROM BootstrapPhase")
            },
        )
    importedAgents[p1.actor].list("Component").entries shouldBe p1.list("Component").entries
  }

  /** Pets declarations for concrete Players with sourced per-seat setup rules. */
  protected open val playerClassPets: String = ""
  /** Neutral solo locations in city, greenery, city, greenery order. */
  protected open val soloTileLocations: List<String> = emptyList()
  /** Ordered events observed by a replay; the fixture adds remaining unused cards. */
  protected open val firstGlobalEvents: List<String> = emptyList()
  protected open val catalog: TfmCatalog by lazy { canonicalCatalog(config) }

  internal suspend fun completedGameFileJson(short: Boolean = false): String? {
    if (game.events.entriesSinceSetup().isEmpty()) return null
    val completed = game.recording()
    return GameFileJson.encode(completed, agents.taskLog.inputs(), short)
  }

  @BeforeTest
  open fun commonSetup() {
    gamePremise =
        withTestTurmoilDeck(
            withTestSoloSetup(
                catalog.gamePremise(config, parseClasses(playerClassPets)),
                soloTileLocations.map(::cn),
            ),
            firstGlobalEvents,
        )
    initializeGame(gamePremise)
    if (exportReplay) agents.taskLog.start()
  }

  protected open fun player(seat: Int): Agent {
    require(seat > 0) { "seat numbers begin at 1" }
    return requireNotNull(players.getOrNull(seat - 1)) { "no Player occupies seat $seat" }
  }

  protected fun startGame() {
    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
  }

  // Script-local counterparts live in
  // test/common/dev/martianzoo/tfm/script/StinaScriptTest.kt.
  protected fun Agent.assertProduction(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) {
    assertProds(
        m to "MC",
        s to "Steel",
        t to "Titanium",
        p to "Plant",
        e to "Energy",
        h to "Heat",
    )
  }

  protected fun Agent.assertResources(m: Int, s: Int, t: Int, p: Int, e: Int, h: Int) {
    assertCounts(
        m to "MC",
        s to "Steel",
        t to "Titanium",
        p to "Plant",
        e to "Energy",
        h to "Heat",
    )
  }

  protected fun Agent.assertCardResources(vararg resources: Pair<Int, ClassName>) {
    assertCounts(*resources.map { (count, card) -> count to "CardResource<$card>" }.toTypedArray())
  }

  protected fun Agent.assertUnusedActionCards(vararg cardNames: ClassName) {
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

  protected fun Agent.assertDashMiddle(
      played: Int,
      actions: Int? = null,
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
  }

  protected fun Agent.assertDashRight(
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
        game.classTable.isInhabited(cn("ColoniesExpansion")) &&
            game.reader.getComponents(cn("ColoniesExpansion").expression).isNotEmpty()
    ) {
      assertCounts(colonies to "Colony")
    }
  }

  protected fun assertSidebar(gen: Int, temp: Int, oxygen: Int, oceans: Int, venus: Int = -1) {
    admin.assertCounts(gen to "Generation")
    (-30 + 2 * admin.count("TemperatureStep")) shouldBe temp
    admin.count("OxygenStep") shouldBe oxygen
    admin.assertCounts(oceans to "OceanTile")
    if (venus != -1) {
      val steps = admin.count("VenusStep")
      (if (steps <= 15) 2 * steps else steps + 15) shouldBe venus
    }
  }
}

internal fun Agent.assertTags(
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
