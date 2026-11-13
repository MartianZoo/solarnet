package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Rulebook quotations introduce the scenarios below. */
internal class ColoniesBasicRulesTest : TfmSandboxTest() {
  private fun setUp() {
    newTestGame("Luna, Ceres, Triton, Ganymede, Callisto, Io", playerCount = 4)
    kim.setToExMachina(100, "MC")
  }

  // Shuffle the Colony Tiles and draw the number of players plus 2, and place them next to the
  // main game board. Exception: use 5 tiles if playing a 2 player game.
  @Test
  internal fun `number of colony tiles`() {
    setUp()
    admin.count("ColonyTile") shouldBe 6
  }

  // Place a white cube on the highlighted second step of each Colony Tile track.
  @Test
  internal fun `starting colony production`() {
    setUp()
    admin.assertCounts(
        1 to "ColonyProduction<Luna>",
        1 to "ColonyProduction<Io>",
        6 to "ColonyProduction",
    )
  }

  // TITAN, ENCELADUS, and MIRANDA start with their white marker on the moon picture itself,
  @Test
  internal fun `card resource colonies start not in play`() {
    newTestGame("Luna, Ceres, Callisto, Titan, Enceladus, Miranda", playerCount = 4)
    kim.setToExMachina(100, "MC")

    admin.assertCounts(
        3 to "ColonyTile",
        3 to "ColonyProduction",
        0 to "Miranda",
        0 to "ColonyProduction<Miranda>",
    )

    // and the marker is placed on the highlighted second step of the track immediately when there
    // is any card in play that may collect their respective resources.

    kim.inTurn {
          doTasks(
              *playCardTasks("$Pets"),
              "10 Pay<> FROM MC",
              "$Pets FROM ProjectCard",
              "Animal<$Pets>",
          )
        }
        .expect("Miranda, ColonyProduction")
    admin.assertCounts(
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
    // This test observes setup before the prepared sandbox fixture would finish it.
    game = TfmEngine.newGame(premise)
    val solo = agents[PLAYER1]
    solo.autoExecPolicy = NONE

    admin.assertCounts(0 to "ColonyTile", 4 to "SelectedColonyTile")
    agents.taskLog.gameStarted()
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    solo.doTask("-SelectedColonyTile<Class<Luna>>")
    admin.assertCounts(
        0 to "ColonyTile",
        3 to "SelectedColonyTile",
        0 to "Callisto",
        0 to "Luna",
        0 to "ColonyProduction<Luna>",
        0 to "DelayedMiranda",
        0 to "DelayedTitan",
    )
  }

  // You can not place a colony there, or trade there, until that happens.
  @Test
  internal fun `cant do anything with colony not in play`() {
    newTestGame("Luna, Ceres, Callisto, Titan, Enceladus, Miranda", playerCount = 4)
    kim.setToExMachina(100, "MC")

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks(*standardProjectTasks("BuildColonyProject"), "17 Pay<> FROM MC", "Colony<Miranda>")
      }
    }

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC", "Trade<Miranda>")
      }
    }

    // And just to show that it would have worked otherwise
    kim.inTurn {
      doTasks(
          *playCardTasks("$Pets"),
          "10 Pay<> FROM MC",
          "$Pets FROM ProjectCard",
          "Animal<$Pets>",
      )
    }
    kim.inTurn {
      doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC", "Trade<Miranda>")
      doTask("Animal<$Pets>")
    }
  }

  // Building a colony (standard project): You may use an action to build a colony. This is a
  // standard project that costs 17 M€: place your player marker on the lowest available spot on
  // the Colony Tile track
  @Test
  internal fun `build a colony`() {
    setUp()
    admin.exMachina("-ColonyProduction<Luna>")
    admin.assertCounts(0 to "ColonyProduction<Luna>")

    kim.inTurn {
      doTasks(
          *standardProjectTasks("BuildColonyProject"),
          "17 Pay<> FROM MC",
          "Colony<Luna>",
          "PROD[2 MC]", // Take the placement bonus printed inside the track.
      )
    }

    kim.assertCounts(1 to "Colony")

    // (move the white marker up 1 step if necessary)
    kim.assertCounts(1 to "ColonyProduction<Luna>")
  }

  // Only 3 colonies total per Colony Tile are allowed - no exceptions!
  @Test
  internal fun `three colonies max`() {
    setUp()
    admin.exMachina("Colony<Kim, Luna>, Colony<Stan, Luna>, Colony<Rob, Luna>")
    shouldThrow<LimitsException> {
      players[3].inTurn {
        doTasks(*standardProjectTasks("BuildColonyProject"), "17 Pay<> FROM MC", "Colony<Luna>")
      }
    }
    admin.count("Colony<Luna>") shouldBe 3
  }

  // Each player may only have one colony per Colony Tile (unless stated otherwise on a card).
  @Test
  internal fun `duplicate colony`() {
    setUp()
    kim.inTurn {
      doTasks(
          *standardProjectTasks("BuildColonyProject"),
          "17 Pay<> FROM MC",
          "Colony<Luna>",
          "PROD[2 MC]",
      )
    }
    kim.assertCounts(1 to "Colony<Luna>")
    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(*standardProjectTasks("BuildColonyProject"), "17 Pay<> FROM MC", "Colony<Luna>")
      }
    }
    kim.assertCounts(1 to "Colony<Luna>")
  }

  // Trading with a Colony Tile (a new action - not a standard project): You may use one of your
  // actions to trade with a Colony Tile. Pay the cost: 9 M€, or 3 energy, or 3 titanium, and move
  // your Trade Fleet from the Trade Fleets Tile to an available Colony Tile.
  @Test
  internal fun `basic trading`() {
    setUp()
    admin.exMachina(
        "5 ColonyProduction<Luna>, Colony<Kim, Luna>, Colony<Stan, Luna>, 3 Energy<Kim>"
    )
    kim.assertCounts(6 to "ColonyProduction<Luna>")
    kim.inTurn {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
      )
      // Then follow the Colony Tile instructions: Check the Colony Tile track to determine your
      // trade income, and give the local colony owners their colony bonus.
      doTask("19 MC")
      doTask("NOW Stan { 2! MC<Stan> }")
      stan.doTask("2 MC")
    }

    // Directly after trading you move the white marker as far left as possible, stopping next to
    // the player colonies, or at the bottom of the track (in the example above the marker is moved
    // to the highlighted second step of the track).
    kim.assertCounts(2 to "ColonyProduction<Luna>")

    // A Colony Tile may only hold 1 trade fleet at a time.
    shouldThrow<LimitsException> {
      stan.inTurn {
        doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC", "Trade<Luna>")
      }
    }

    // When the generation ends, the recorded trades clear and all white markers move 1 step up the
    // Colony track. The players' trade-fleet capacities remain.
    admin.runOperation("AdvanceColonyTracks")
    admin.runOperation("Generation")
    admin.assertCounts(
        0 to "Trade",
        4 to "TradeFleet",
        2 to "ColonyProduction<Ceres>",
    )
  }

  @Test
  internal fun `colony production leaves a maximum track at maximum`() {
    setUp()
    admin.runOperation("5 ColonyProduction<Luna>")

    admin.runOperation("AdvanceColonyTracks")

    admin.count("ColonyProduction<Luna>") shouldBe 6
  }

  @Test
  internal fun `trade fleet cannot be reused`() {
    setUp()
    kim.inTurn {
      doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC", "Trade<Luna>", "2 MC")
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC", "Trade<Triton>")
      }
    }
    kim.assertCounts(
        1 to "Trade<Luna>",
        0 to "Trade<Triton>",
        1 to "TradeFleet",
    )
  }

  @Test
  internal fun `additional trade fleet permits a later trade`() {
    setUp()
    kim.inTurn {
      doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC", "Trade<Luna>", "2 MC")
    }
    kim.exMachina("TradeFleet")
    kim.inTurn {
      doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC", "Trade<Triton>", "Titanium")
    }

    kim.assertCounts(
        1 to "Trade<Luna>",
        1 to "Trade<Triton>",
        2 to "TradeFleet",
    )
  }
}
