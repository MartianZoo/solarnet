package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.repl.ReplSession
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class CustomInstructionsTest {

  @Test
  fun robinson() {
    val repl = ReplSession(Canon)
    repl.command("newgame BM 3")
    repl.command("become Player1")
    repl.command("PROD[5]") // The standard hack for every player - ignore it!
    repl.command("PROD[Steel, Titanium, Plant, Energy, Heat]")
    repl.command('$' + "gainLowestProduction(Player1)")

    // TODO fix ordering problem
    assertThat(repl.command("count Production<Player1, Megacredit.CLASS>").first()).startsWith("6")
  }

  @Test
  fun robinsonCant() {
    val repl = ReplSession(Canon)
    repl.command("newgame BM 3")
    repl.command("become Player1")
    repl.command("PROD[5]") // The standard hack for every player - ignore it!
    repl.command("PROD[Steel, Titanium, Plant, Heat]")
    assertThrows<RuntimeException> { repl.command('$' + "gainLowestProduction(Player1)") }
  }

  // TODO figure out how to make gradle compile the java code
  // It seemed like adding plugins { `java-library` } should have been enough
  fun java() {
    val auth = object : Authority.Forwarding(Canon) {
      override val customInstructions = listOf(CustomJavaExample.GainLowestProduction())
    }

    val repl = ReplSession(auth)
    repl.command("newgame BM 3")
    repl.command("become Player1")
    repl.command("PROD[5]") // The standard hack for every player - ignore it!
    repl.command("PROD[Steel, Titanium, Plant, Energy, Heat]")
    repl.command('$' + "gainLowestProduction(Player1)")

    // TODO fix ordering problem
    assertThat(repl.command("count Production<Player1, Megacredit.CLASS>").first()).startsWith("6")
  }

  @Test
  fun robinson2() {
    val repl = ReplSession(Canon)
    repl.command("newgame BM 3")
    repl.command("become Player1")
    repl.command("PROD[5]") // The standard hack for every player - ignore it!
    repl.command("PROD[-1]")
    repl.command('$' + "gainLowestProduction(Player1)")
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
  // EXEC $${""}copyProductionBox(StripMine<Player1>)
  //
  // REQUIRE PROD[=0 Energy<Player1>, 4 Steel<Player1>]
}
