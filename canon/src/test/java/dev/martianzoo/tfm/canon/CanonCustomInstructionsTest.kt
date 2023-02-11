package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.repl.ReplSession
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class CanonCustomInstructionsTest {

  @Test
  fun robinson() {
    val repl = ReplSession(Canon)
    repl.command("newgame BM 3")
    repl.command("become Player1")
    repl.command("PROD[5]") // The standard hack for every player - ignore it!
    repl.command("PROD[Steel, Titanium, Plant, Energy, Heat]")
    repl.command("@gainLowestProduction(Player1)")
    assertThat(repl.command("count Production<Player1, Class<Megacredit>>").first()).startsWith("6")
    assertThat(repl.command("count Production<Class<Megacredit>, Player1>").first()).startsWith("6")
  }

  @Test
  fun robinsonCant() {
    val repl = ReplSession(Canon)
    repl.command("newgame BM 3")
    repl.command("become Player1")
    repl.command("PROD[5]") // The standard hack for every player - ignore it!
    repl.command("PROD[Steel, Titanium, Plant, Heat]")
    assertFails("multiple lowest") { repl.command("@gainLowestProduction(Player1)") }
  }

  @Test
  fun robinson2() {
    val repl = ReplSession(Canon)
    repl.command("newgame BM 3")
    repl.command("become Player1")
    repl.command("PROD[5]") // The standard hack for every player - ignore it!
    repl.command("PROD[-1]")
    repl.command("@gainLowestProduction(Player1)")
    assertThat(repl.command("has PROD[=5 Megacredit]").first()).startsWith("true")
  }

  // Robo work test
  // EXEC PROD[5 Megacredit<Player1>]
  //
  // EXEC PROD[4 Energy<Player1>]
  //
  // EXEC StripMine<Player1>
  // // we don't have effects working yet so...
  // EXEC PROD[-2 Energy<Player1>, 2 Steel<Player1>, Titanium<Player1>]
  //
  // REQUIRE PROD[=2 Energy<Player1>, 2 Steel<Player1>]
  // EXEC @copyProductionBox(StripMine<Player1>)
  //
  // REQUIRE PROD[=0 Energy<Player1>, 4 Steel<Player1>]
}

// TODO share
private fun assertFails(message: String, shouldFail: () -> Unit) =
    assertThrows<RuntimeException>(message, shouldFail)
