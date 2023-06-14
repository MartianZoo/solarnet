package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

internal class BasicTest {
  @Test
  fun shortNames() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER2).godMode()

    session.manual("PROD[5, 4 E]")
    session.manual("ProjectCard")
    session.manual("C138")
    session.manual("PROD[-2 E, 2 S, T]")

    assertThat(session.count("PROD[E]")).isEqualTo(0)
    assertThat(session.count("PROD[S]")).isEqualTo(4)
    assertThat(session.count("PROD[T]")).isEqualTo(2)

    assertThat(game.tfm(PLAYER1).has("PROD[=0 E, =0 S]")).isTrue()
  }

  @Test
  fun removeAmap() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER1).godMode()

    session.manual("3 Heat!")
    session.manual("4 Heat.")
    session.manual("-9 Heat.")
    assertThat(session.count("Heat")).isEqualTo(0)
  }

  @Test
  fun rollback() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER1).godMode()

    session.manual("3 Heat")
    session.manual("4 Heat")
    assertThat(session.count("Heat")).isEqualTo(7)

    val checkpoint = game.timeline.checkpoint()
    session.manual("-6 Heat")
    assertThat(session.count("Heat")).isEqualTo(1)

    game.timeline.rollBack(checkpoint)
    assertThat(session.count("Heat")).isEqualTo(7)
  }

  @Test
  fun dependencies() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER1).godMode()

    assertThat(game.tasks.isEmpty()).isTrue()
    assertThat(session.count("Microbe")).isEqualTo(0)

    session.manual("4 OxygenStep")
    assertThat(session.count("OxygenStep")).isEqualTo(4)
    session.manual("ProjectCard")
    session.manual("Ants")
    assertThat(game.tasks.isEmpty())
    assertThat(session.count("Ants")).isEqualTo(1)
    session.manual("3 Microbe<Ants>")
    assertThat(session.count("Microbe")).isEqualTo(3)
    session.manual("-Ants")
    assertThat(session.count("Microbe")).isEqualTo(0)
  }

  @Test
  fun counting() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER1).godMode()
    session.manual("42 Heat")
    assertThat(session.count("Heat")).isEqualTo(42)
    assertThat(session.count("4 Heat")).isEqualTo(10)
    assertThat(session.count("42 Heat")).isEqualTo(1)
    assertThat(session.count("43 Heat")).isEqualTo(0)
    assertThat(session.count("Heat MAX 50")).isEqualTo(42)
    assertThat(session.count("Heat MAX 42")).isEqualTo(42)
    assertThat(session.count("Heat MAX 41")).isEqualTo(41)
    assertThat(session.count("Heat MAX 1")).isEqualTo(1)
    assertThat(session.count("Heat MAX 0")).isEqualTo(0)
    assertThat(session.count("4 Heat MAX 10")).isEqualTo(10)
    assertThat(session.count("4 Heat MAX 9")).isEqualTo(9)
  }

  @Test
  fun tempTrigger() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val session = game.tfm(PLAYER1).godMode()
    assertThat(session.count("TerraformRating")).isEqualTo(20)

    session.manual("2 TemperatureStep")
    assertThat(session.count("TemperatureStep")).isEqualTo(2)
    assertThat(session.count("TerraformRating")).isEqualTo(22)
    assertThat(session.count("Production<Class<Heat>>")).isEqualTo(0)

    session.manual("2 TemperatureStep")
    assertThat(session.count("TerraformRating")).isEqualTo(24)
    assertThat(session.count("Production<Class<Heat>>")).isEqualTo(1)

    session.manual("8 OxygenStep")
    assertThat(session.count("TerraformRating")).isEqualTo(33)
    assertThat(session.count("Production<Class<Heat>>")).isEqualTo(2)
  }
}
