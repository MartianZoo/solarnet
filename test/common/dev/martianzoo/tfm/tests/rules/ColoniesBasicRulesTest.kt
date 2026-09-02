package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

/** Comment lines are quotes directly from the rulebook. */
internal class ColoniesBasicRulesTest : TfmTest() {
  private val normal =
      listOf(
              "Luna",
              "Ceres",
              "Triton",
              "Ganymede",
              "Callisto",
              "Io",
          )
          .toSetStrict(::cn)
  private val premise =
      canonicalPremise(
          ColoniesExpansion,
          players = 4,
          colonyTiles = normal,
      )

  init {
    game = setUpGame(premise)
  }

  private val p1 = game.tfm(PLAYER1)

  @BeforeTest
  fun setUp() {
    p1.sneak("100 MC, 5 ProjectCard")
    engine.phase("Action")
  }

  // Shuffle the Colony Tiles and draw the number of players plus 2, and place them next to the
  // main game board. Exception: use 5 tiles if playing a 2 player game.
  @Test
  internal fun `number of colony tiles`() {
    engine.count("ColonyTile") shouldBe 6
  }

  // Place a white cube on the highlighted second step of each Colony Tile track.
  @Test
  internal fun `starting colony production`() {
    engine.assertCounts(
        1 to "ColonyProduction<Luna>",
        1 to "ColonyProduction<Io>",
        6 to "ColonyProduction",
    )
  }

  // TITAN, ENCELADUS, and MIRANDA start with their white marker on the moon picture itself,
  @Test
  internal fun `card resource colonies start not in play`() {
    val colonies = testColonyTiles(4, "Titan", "Enceladus", "Miranda")
    val premise =
        canonicalPremise(
            ColoniesExpansion,
            players = 4,
            colonyTiles = colonies,
        )
    val engine = setUpGame(premise).tfm(ENGINE)
    val p1 = engine.asPlayer(PLAYER1)

    engine.assertCounts(
        3 to "ColonyTile",
        3 to "ColonyProduction",
        0 to "Miranda",
        0 to "ColonyProduction<Miranda>",
    )

    // and the marker is placed on the highlighted second step of the track immediately when there
    // is any card in play that may collect their respective resources.

    engine.phase("Action")
    p1.sneak("100 MC, 5 ProjectCard")
    p1.playProject(Pets, 10).expect("Miranda, ColonyProduction")
    engine.assertCounts(
        4 to "ColonyTile",
        4 to "ColonyProduction",
        1 to "Miranda", // now it exists / is in play
        1 to "ColonyProduction<Miranda>",
        0 to "Enceladus",
    )
  }

  @Test
  internal fun `solo discards one selected colony tile before setup continues`() {
    val premise =
        canonicalPremise(
            ColoniesExpansion,
            players = 1,
            colonyTiles = setOf("Callisto", "Luna", "Miranda", "Titan").mapTo(linkedSetOf(), ::cn),
        )
    val game = Engine.newGame(premise, inputOnlySynonyms = TEST_CLASS_SYNONYMS)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)

    engine.assertCounts(2 to "ColonyTile", 4 to "ColonyTileSelection")
    TfmWorkflow.Manual(game).setupPhase()
    p1.doTask("-ColonyTileSelection<Class<Luna>>")
    engine.assertCounts(
        1 to "ColonyTile",
        3 to "ColonyTileSelection",
        1 to "Callisto",
        0 to "Luna",
        0 to "ColonyProduction<Luna>",
        1 to "DelayedMiranda",
        1 to "DelayedTitan",
    )
  }

  // You can not place a colony there, or trade there, until that happens.
  @Test
  internal fun `cant do anything with colony not in play`() {
    val colonies = testColonyTiles(4, "Titan", "Enceladus", "Miranda")
    val premise =
        canonicalPremise(
            ColoniesExpansion,
            players = 4,
            colonyTiles = colonies,
        )
    val engine = setUpGame(premise).tfm(ENGINE)
    val p1 = engine.asPlayer(PLAYER1)

    engine.phase("Action")
    p1.sneak("100 MC, 5 ProjectCard")

    shouldThrow<DependencyException> {
      p1.stdProject("BuildColonySP") { doTask("Colony<Miranda>") }
    }

    shouldThrow<DependencyException> { p1.stdAction("TradeSA") { doTask("Trade<Miranda>") } }

    // And just to show that it would have worked otherwise
    p1.playProject(Pets, 10)
    p1.stdAction("TradeSA") {
      doTask("Trade<Miranda>")
      doTask("Animal<$Pets>")
    }
  }

  // Building a colony (standard project): You may use an action to build a colony. This is a
  // standard project that costs 17 M€: place your player marker on the lowest available spot on
  // the Colony Tile track
  @Test
  internal fun `build a colony`() {
    engine.sneak("-ColonyProduction<Luna>")
    engine.assertCounts(0 to "ColonyProduction<Luna>")

    p1.stdProject("BuildColonySP") { doTask("Colony<Luna>") }
        // Take the placement bonus printed inside the track.
        .expect("PROD[2 MC]")

    p1.assertCounts(1 to "Colony")

    // (move the white marker up 1 step if necessary)
    p1.assertCounts(1 to "ColonyProduction<Luna>")
  }

  // Only 3 colonies total per Colony Tile are allowed - no exceptions!
  @Test
  internal fun `three colonies max`() {
    engine.manual("Colony<Player1, Luna>")
    engine.manual("Colony<Player2, Luna>")
    engine.manual("Colony<Player3, Luna>")
    shouldThrow<LimitsException> { engine.manual("Colony<Player4, Luna>") }
  }

  // Each player may only have one colony per Colony Tile (unless stated otherwise on a card).
  @Test
  internal fun `duplicate colony`() {
    p1.stdProject("BuildColonySP") { doTask("Colony<Luna>") }
    p1.assertCounts(1 to "Colony<Luna>")
    shouldThrow<NarrowingException> { p1.stdProject("BuildColonySP") { doTask("Colony<Luna>") } }
    p1.assertCounts(1 to "Colony<Luna>")
  }

  // Trading with a Colony Tile (a new action - not a standard project): You may use one of your
  // actions to trade with a Colony Tile. Pay the cost: 9 M€, or 3 energy, or 3 titanium, and move
  // your Trade Fleet from the Trade Fleets Tile to an available Colony Tile.
  @Test
  internal fun `basic trading`() {
    engine.sneak(
        "5 ColonyProduction<Luna>, Colony<Player1, Luna>, Colony<Player2, Luna>, 3 E<Player1>"
    )
    p1.assertCounts(6 to "ColonyProduction<Luna>")
    p1.stdAction("TradeSA", 2) {
          doTask("Trade<Luna>")
          // Then follow the Colony Tile instructions: Check the Colony Tile track to determine your
          // trade income, and give the local colony owners their colony bonus.
        }
        .expect("19 MC<Player1>, 2 MC<Player2>, -3 E<Player1>")

    // Directly after trading you move the white marker as far left as possible, stopping next to
    // the player colonies, or at the bottom of the track (in the example above the marker is moved
    // to the highlighted second step of the track).
    p1.assertCounts(2 to "ColonyProduction<Luna>")

    // A Colony Tile may only hold 1 trade fleet at a time.
    shouldThrow<LimitsException> { p1.asPlayer(PLAYER2).manual("Trade<Luna>") }

    // When the generation ends, all trade fleets move back from the Colony Tiles to the Trade
    // Fleets Tile, and all white markers moves 1 step up the Colony track.
    engine.phase("Production")
    TfmWorkflow.Manual(game).solarPhase()
    engine.manual("Generation")
    engine.assertCounts(
        0 to "FlownTradeFleet",
        4 to "ReserveTradeFleet",
        2 to "ColonyProduction<Ceres>",
    )
  }

  @Test
  internal fun `trade fleet cannot be reused`() {
    p1.stdAction("TradeSA", 1) { doTask("Trade<Luna>") }

    shouldThrow<LimitsException> { p1.manual("Trade<Triton>") }
  }
}
