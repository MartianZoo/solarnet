package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import org.junit.jupiter.api.Test

class ReplSessionTest {
  @Test
  fun test() {
    val repl = ReplSession(Canon)
    repl.command("newgame MB 2")
    repl.command("become Player2")

    assertThat(repl.command("PROD[5, 4 Energy]").first()).startsWith("Ok")
    repl.command("StripMine") // , BuildingTag<Player2, StripMine> ?
    assertThat(repl.command("PROD[-2 Energy, 2 Steel, Titanium]").first()).startsWith("Ok")

    val check1 = "has PROD[=2 Energy, =2 Steel]"
    assertThat(repl.command(check1).first()).startsWith("true")

    repl.command("become Player1")
    val check2 = "has PROD[=0 Energy, =0 Steel]"
    assertThat(repl.command(check2).first()).startsWith("true")

    // assertThat(repl.command("${'$'}copyProductionBox(StripMine)").first()).startsWith("Ok")
    //
    // val check2 = "has PROD[=0 Energy, =4 Steel]"
    // assertThat(repl.command(check1).first()).startsWith("true")
  }
}
