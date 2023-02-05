package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class InteractiveSessionTest {
  @Test
  fun test() {
    val session = InteractiveSession()
    session.newGame(GameSetup(Canon, "MB", 2))
    session.becomePlayer(2)

    session.execute(instruction("PROD[5, 4 Energy]"))
    session.execute(instruction("StripMine")) // , BuildingTag<Player2, StripMine> ?
    session.execute(instruction("PROD[-2 Energy, 2 Steel, Titanium]"))

    assertThat(session.has(requirement("PROD[=2 Energy, =2 Steel]"))).isTrue()

    session.becomePlayer(1)
    assertThat(session.has(requirement("PROD[=0 Energy, =0 Steel]"))).isTrue()
  }

  @Test
  fun removeAmap() {
    val session = InteractiveSession()
    session.newGame(GameSetup(Canon, "MB", 2))
    session.becomePlayer(1)

    session.execute(instruction("3 Heat!"))
    session.execute(instruction("4 Heat."))
    session.execute(instruction("-9 Heat."))
    assertThat(session.count(typeExpr("Heat"))).isEqualTo(0)
  }

  @Test
  fun rollback() {
    val session = InteractiveSession()
    session.newGame(GameSetup(Canon, "MB", 2))
    session.becomePlayer(1)

    session.execute(instruction("3 Heat"))
    session.execute(instruction("4 Heat"))
    session.execute(instruction("-6 Heat"))
    assertThat(session.count(typeExpr("Heat"))).isEqualTo(1)

    val count = session.game!!.changeLogFull().size
    session.rollBackToBefore(count)
    assertThat(session.count(typeExpr("Heat"))).isEqualTo(1)

    session.rollBackToBefore(count - 2)
    assertThat(session.count(typeExpr("Heat"))).isEqualTo(3)
  }

  @Test
  fun dependencies() {
    val session = InteractiveSession()
    session.newGame(GameSetup(Canon, "MB", 2))
    session.becomePlayer(2)

    assertThrows<Exception> { session.execute(instruction("3 Microbe<Ants>")) }
    session.execute(instruction("Ants"))
    session.execute(instruction("3 Microbe<Ants>"))
    assertThrows<Exception> { session.execute(instruction("-Ants")) }
  }
}
