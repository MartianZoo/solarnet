package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.util.toSetStrict
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Comment lines are quotes directly from the rulebook. */
class ColoniesBasicRulesTest {
  val normal = listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto", "Io").toSetStrict(::cn)
  val setup = GameSetup(Canon, "BRMC", 4, normal)
  val engine = Engine.newGame(setup).tfm(ENGINE)
  val p1 = engine.asPlayer(PLAYER1)

  fun TaskResult.expect(string: String) = TestHelpers.assertNetChanges(this, engine, string)

  @BeforeEach
  fun setUp() {
    p1.godMode().sneak("100, 5 ProjectCard")
    engine.phase("Action")
  }

  // Shuffle the Colony Tiles and draw the number of players plus 2, and place them next to the
  // main game board. Exception: use 5 tiles if playing a 2 player game.
  @Test
  fun `number of colony tiles`() {
    assertThat(engine.count("ColonyTile")).isEqualTo(6)

    assertThat(GameSetup(Canon, "BRMC", 2).colonyTiles).hasSize(5)
    assertThat(GameSetup(Canon, "BRMC", 3).colonyTiles).hasSize(5)
    assertThat(GameSetup(Canon, "BRMC", 4).colonyTiles).hasSize(6)
    assertThat(GameSetup(Canon, "BRMC", 5).colonyTiles).hasSize(7)
  }

  // Place a white cube on the highlighted second step of each Colony Tile track.
  @Test
  fun `starting colony production`() {
    engine.assertCounts(
        1 to "ColonyProduction<Luna>",
        1 to "ColonyProduction<Io>",
        6 to "ColonyProduction",
    )
  }

  // TITAN, ENCELADUS, and MIRANDA start with their white marker on the moon picture itself,
  @Test
  fun `card resource colonies start not in play`() {
    val colonies = listOf("Titan", "Enceladus", "Miranda").toSetStrict(::cn)
    val setup = GameSetup(Canon, "BRMC", 4, colonies) // it will pick 3 others
    val engine = Engine.newGame(setup).tfm(ENGINE)
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
    p1.godMode().sneak("100, 5 ProjectCard")
    p1.playProject("Pets", 10).expect("Miranda, ColonyProduction")
    engine.assertCounts(
        4 to "ColonyTile",
        4 to "ColonyProduction",
        1 to "Miranda", // now it exists / is in play
        1 to "ColonyProduction<Miranda>",
        0 to "Enceladus",
    )
  }

  // You can not place a colony there, or trade there, until that happens.
  @Test
  fun `cant do anything with colony not in play`() {
    val colonies = listOf("Titan", "Enceladus", "Miranda").toSetStrict(::cn)
    val setup = GameSetup(Canon, "BRMC", 4, colonies) // it will pick 3 others
    val engine = Engine.newGame(setup).tfm(ENGINE)
    val p1 = engine.asPlayer(PLAYER1)

    engine.phase("Action")
    p1.godMode().sneak("100, 5 ProjectCard")

    assertThrows<DependencyException> {
      p1.stdProject("BuildColonySP") { doTask("Colony<Miranda>") }
    }

    assertThrows<DependencyException> { p1.stdAction("TradeSA") { doTask("Trade<Miranda>") } }

    // And just to show that it would have worked otherwise
    p1.playProject("Pets", 10)
    p1.stdAction("TradeSA") {
      // TODO should it have chosen automatically?
      doTask("Trade<Miranda, TradeFleetA>")
      doTask("Animal<Pets>")
    }
  }

  // Building a colony (standard project): You may use an action to build a colony. This is a
  // standard project that costs 17 M€: place your player marker on the lowest available spot on
  // the Colony Tile track
  @Test
  fun `build a colony`() {
    engine.godMode().sneak("-ColonyProduction<Luna>")
    engine.assertCounts(0 to "ColonyProduction<Luna>")

    p1.stdProject("BuildColonySP") {
      doTask("Colony<Luna>")

      // Take the placement bonus printed inside the track.
    }.expect("-17, PROD[2]")

    p1.assertCounts(1 to "Colony")

    // (move the white marker up 1 step if necessary)
    p1.assertCounts(1 to "ColonyProduction<Luna>")
  }

  // Only 3 colonies total per Colony Tile are allowed - no exceptions!
  @Test
  fun `three colonies max`() {
    engine.godMode().manual("Colony<P1, Luna>")
    engine.godMode().manual("Colony<P2, Luna>")
    engine.godMode().manual("Colony<P3, Luna>")
    assertThrows<LimitsException> { engine.godMode().manual("Colony<P4, Luna>") }
  }

  // Each player may only have one colony per Colony Tile (unless stated otherwise on a card).
  @Test
  fun `duplicate colony`() {
    p1.stdProject("BuildColonySP") { doTask("Colony<Luna>") }
    p1.assertCounts(1 to "Colony<Luna>")
    assertThrows<NarrowingException> { p1.stdProject("BuildColonySP") { doTask("Colony<Luna>") } }
    p1.assertCounts(1 to "Colony<Luna>")
  }

  // Trading with a Colony Tile (a new action - not a standard project): You may use one of your
  // actions to trade with a Colony Tile. Pay the cost: 9 M€, or 3 energy, or 3 titanium, and move
  // your Trade Fleet from the Trade Fleets Tile to an available Colony Tile.
  @Test
  fun `basic trading`() {
    engine.godMode().sneak("5 ColonyProduction<Luna>, Colony<P1, Luna>, Colony<P2, Luna>, 3 E<P1>")
    p1.assertCounts(6 to "ColonyProduction<Luna>")
    p1.stdAction("TradeSA", 2) {
      doTask("Trade<Luna, TradeFleetA>")

      // Then follow the Colony Tile instructions: Check the Colony Tile track to determine your
      // trade income, and give the local colony owners their colony bonus.
    }.expect("19 Megacredit<P1>, 2 Megacredit<P2>, -3 E<P1>")

    // Directly after trading you move the white marker as far left as possible, stopping next to
    // the player colonies, or at the bottom of the track (in the example above the marker is moved
    // to the highlighted second step of the track).
    p1.assertCounts(2 to "ColonyProduction<Luna>")

    // A Colony Tile may only hold 1 trade fleet at a time.
    assertThrows<LimitsException> {
      p1.asPlayer(PLAYER2).godMode().manual("Trade<Luna, TradeFleetB>")
    }

    // When the generation ends, all trade fleets move back from the Colony Tiles to the Trade
    // Fleets Tile, and all white markers moves 1 step up the Colony track.
    engine.nextGeneration(0, 0, 0, 0)
    engine.assertCounts(0 to "Trade", 2 to "ColonyProduction<Ceres>")
  }
}
