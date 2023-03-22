package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.pets.Parsing.parseInput
import dev.martianzoo.tfm.pets.Raw
import dev.martianzoo.tfm.pets.ast.Instruction
import org.junit.jupiter.api.Test

private class InteractiveSessionTest {
  @Test
  fun test() {
    val session = InteractiveSession(GameSetup(Canon, "MB", 2))
    session.become(Actor.PLAYER2)

    session.initiateOnly(instruction("PROD[5, 4 Energy], ProjectCard"))
    session.initiateOnly(instruction("StripMine"))
    session.initiateOnly(instruction("PROD[-2 Energy, 2 Steel, Titanium]"))

    assertThat(session.has(parseInput("PROD[=2 Energy, =2 Steel]"))).isTrue()

    session.become(Actor.PLAYER1)
    assertThat(session.has(parseInput("PROD[=0 Energy, =0 Steel]"))).isTrue()
  }

  @Test
  fun shortNames() {
    val session = InteractiveSession(GameSetup(Canon, "MB", 2))
    session.become(Actor.PLAYER2)

    session.initiateOnly(instruction("PROD[5, 4 E]"))
    session.initiateOnly(instruction("ProjectCard"))
    session.initiateOnly(instruction("C138"))
    session.initiateOnly(instruction("PROD[-2 E, 2 S, T]"))

    assertThat(session.has(parseInput("PROD[=2 E, =2 S]"))).isTrue()

    session.become(Actor.PLAYER1)
    assertThat(session.has(parseInput("PROD[=0 E, =0 S]"))).isTrue()
  }

  @Test
  fun removeAmap() {
    val session = InteractiveSession(GameSetup(Canon, "MB", 2))
    session.become(Actor.PLAYER1)

    session.initiateOnly(instruction("3 Heat!"))
    session.initiateOnly(instruction("4 Heat."))
    session.initiateOnly(instruction("-9 Heat."))
    assertThat(session.count(parseInput("Heat"))).isEqualTo(0)
  }

  @Test
  fun rollback() {
    val session = InteractiveSession(GameSetup(Canon, "MB", 2))
    session.become(Actor.PLAYER1)

    session.initiateOnly(instruction("3 Heat"))
    session.initiateOnly(instruction("4 Heat"))
    assertThat(session.count(parseInput("Heat"))).isEqualTo(7)

    val checkpoint = session.game.checkpoint()
    session.initiateOnly(instruction("-6 Heat"))
    assertThat(session.count(parseInput("Heat"))).isEqualTo(1)

    session.rollBack(checkpoint.ordinal)
    assertThat(session.count(parseInput("Heat"))).isEqualTo(7)
  }

  private fun instruction(s: String) = Raw(Instruction.instruction(s), setOf())

  @Test
  fun dependencies() {
    val session = InteractiveSession(GameSetup(Canon, "MB", 2))
    session.become(Actor.PLAYER2)

    assertThat(session.game.taskQueue.isEmpty()).isTrue()
    assertThat(session.count(parseInput("Microbe"))).isEqualTo(0)

    session.initiateAndAutoExec(instruction("4 OxygenStep"))
    assertThat(session.count(parseInput("OxygenStep"))).isEqualTo(4)
    session.initiateAndAutoExec(instruction("ProjectCard"))
    session.initiateAndAutoExec(instruction("Ants"))
    assertThat(session.game.taskQueue.isEmpty())
    assertThat(session.count(parseInput("Ants"))).isEqualTo(1)
    session.initiateAndAutoExec(instruction("3 Microbe<Ants>"))
    assertThat(session.count(parseInput("Microbe"))).isEqualTo(3)
    session.initiateAndAutoExec(instruction("-Ants"))
    assertThat(session.count(parseInput("Microbe"))).isEqualTo(0)
  }

  @Test
  fun counting() {
    val session = InteractiveSession(GameSetup(Canon, "MB", 2))
    session.become(Actor.PLAYER2)
    session.initiateOnly(instruction("42 Heat"))
    assertThat(session.count(parseInput("Heat"))).isEqualTo(42)
    assertThat(session.count(parseInput("4 Heat"))).isEqualTo(10)
    assertThat(session.count(parseInput("42 Heat"))).isEqualTo(1)
    assertThat(session.count(parseInput("43 Heat"))).isEqualTo(0)
    assertThat(session.count(parseInput("Heat MAX 50"))).isEqualTo(42)
    assertThat(session.count(parseInput("Heat MAX 42"))).isEqualTo(42)
    assertThat(session.count(parseInput("Heat MAX 41"))).isEqualTo(41)
    assertThat(session.count(parseInput("Heat MAX 1"))).isEqualTo(1)
    assertThat(session.count(parseInput("Heat MAX 0"))).isEqualTo(0)
    assertThat(session.count(parseInput("4 Heat MAX 10"))).isEqualTo(10)
    assertThat(session.count(parseInput("4 Heat MAX 9"))).isEqualTo(9)
  }

  @Test
  fun tempTrigger() {
    val session = InteractiveSession(GameSetup(Canon, "MB", 2))

    session.become(Actor.PLAYER1)
    assertThat(session.count(parseInput("TerraformRating"))).isEqualTo(20)

    session.initiateAndAutoExec(instruction("2 TemperatureStep"))
    assertThat(session.count(parseInput("TemperatureStep"))).isEqualTo(2)
    assertThat(session.count(parseInput("TerraformRating"))).isEqualTo(22)
    assertThat(session.count(parseInput("Production<Class<Heat>>"))).isEqualTo(0)

    session.initiateAndAutoExec(instruction("2 TemperatureStep"))
    assertThat(session.count(parseInput("TerraformRating"))).isEqualTo(24)
    assertThat(session.count(parseInput("Production<Class<Heat>>"))).isEqualTo(1)

    session.initiateAndAutoExec(instruction("8 OxygenStep"))
    assertThat(session.count(parseInput("TerraformRating"))).isEqualTo(33)
    assertThat(session.count(parseInput("Production<Class<Heat>>"))).isEqualTo(2)
  }
}
