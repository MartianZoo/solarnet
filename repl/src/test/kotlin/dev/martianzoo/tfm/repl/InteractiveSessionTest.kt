package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement
import org.junit.jupiter.api.Test

class InteractiveSessionTest {
  @Test
  fun test() {
    val session = InteractiveSession(Canon)
    session.newGame(GameSetup(Canon, "MB", 2))
    session.becomePlayer(2)

    session.execute(Instruction.from("PROD[5, 4 Energy]"))
    session.execute(Instruction.from("StripMine")) // , BuildingTag<Player2, StripMine> ?
    session.execute(Instruction.from("PROD[-2 Energy, 2 Steel, Titanium]"))

    assertThat(session.has(Requirement.from("PROD[=2 Energy, =2 Steel]"))).isTrue()

    session.becomePlayer(1)
    assertThat(session.has(Requirement.from("PROD[=0 Energy, =0 Steel]"))).isTrue()
  }
}
