package dev.martianzoo.tfm.script

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Game
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal fun setUpGame(setup: GameSetup): Game =
    Engine.newGame(setup).apply { TfmWorkflow.Manual(this, setup).setupPhase() }

internal fun setUpGame(
    optionCodes: String,
    players: Int,
    colonyTiles: Set<ClassName> = emptySet(),
): Game = setUpGame(Canon.fromOptionCodes(optionCodes, players, colonyTiles))

internal class BasicTest {
  @Test
  fun shortNames() {
    val game = setUpGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER2).godMode()

    session.manual("PROD[5, 4 E]")
    session.manual("ProjectCard")
    session.manual("C138")
    session.manual("PROD[-2 E, 2 S, T]")

    assertEquals(0, session.count("PROD[E]"))
    assertEquals(4, session.count("PROD[S]"))
    assertEquals(2, session.count("PROD[T]"))

    assertTrue(game.tfm(PLAYER1).has("PROD[=0 E, =0 S]"))
  }

  @Test
  fun removeAmap() {
    val game = setUpGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER1).godMode()

    session.manual("3 Heat!")
    session.manual("4 Heat.")
    session.manual("-9 Heat.")
    assertEquals(0, session.count("Heat"))
  }

  @Test
  fun rollback() {
    val game = setUpGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER1).godMode()

    session.manual("3 Heat")
    session.manual("4 Heat")
    assertEquals(7, session.count("Heat"))

    val checkpoint = game.timeline.checkpoint()
    session.manual("-6 Heat")
    assertEquals(1, session.count("Heat"))

    game.timeline.rollBack(checkpoint)
    assertEquals(7, session.count("Heat"))
  }

  @Test
  fun dependencies() {
    val game = setUpGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER1).godMode()

    assertTrue(game.tasks.isEmpty())
    assertEquals(0, session.count("Microbe"))

    session.manual("4 OxygenStep")
    assertEquals(4, session.count("OxygenStep"))
    session.manual("ProjectCard")
    session.manual("Ants")
    assertTrue(game.tasks.isEmpty())
    assertEquals(1, session.count("Ants"))
    session.manual("3 Microbe<Ants>")
    assertEquals(3, session.count("Microbe"))
    session.manual("-Ants")
    assertEquals(0, session.count("Microbe"))
  }

  @Test
  fun counting() {
    val game = setUpGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER1).godMode()
    session.manual("42 Heat")
    assertEquals(42, session.count("Heat"))
    assertEquals(10, session.count("4 Heat"))
    assertEquals(1, session.count("42 Heat"))
    assertEquals(0, session.count("43 Heat"))
    assertEquals(42, session.count("Heat MAX 50"))
    assertEquals(42, session.count("Heat MAX 42"))
    assertEquals(41, session.count("Heat MAX 41"))
    assertEquals(1, session.count("Heat MAX 1"))
    assertEquals(0, session.count("Heat MAX 0"))
    assertEquals(10, session.count("4 Heat MAX 10"))
    assertEquals(9, session.count("4 Heat MAX 9"))
  }

  @Test
  fun tempTrigger() {
    val game = setUpGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER1).godMode()
    assertEquals(20, session.count("TerraformRating"))

    session.manual("2 TemperatureStep")
    assertEquals(2, session.count("TemperatureStep"))
    assertEquals(22, session.count("TerraformRating"))
    assertEquals(0, session.count("Production<Class<Heat>>"))

    session.manual("2 TemperatureStep")
    assertEquals(24, session.count("TerraformRating"))
    assertEquals(1, session.count("Production<Class<Heat>>"))

    session.manual("8 OxygenStep")
    assertEquals(33, session.count("TerraformRating"))
    assertEquals(2, session.count("Production<Class<Heat>>"))
  }
}
