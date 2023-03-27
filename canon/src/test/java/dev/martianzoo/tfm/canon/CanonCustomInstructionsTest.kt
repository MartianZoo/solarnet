package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.repl.InteractiveSession
import dev.martianzoo.tfm.repl.ReplSession
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class CanonCustomInstructionsTest {

  @Test
  fun robinson() {
    val repl = ReplSession(Canon, GameSetup(Canon, "MB", 3)) // TODO
    repl.command("become Player1")
    repl.command("mode green")

    repl.command("exec PROD[Steel, Titanium, Plant, Energy, Heat]")
    repl.command("exec @gainLowestProduction(Player1)")
    // TODO PROD metrics
    assertThat(repl.command("count Production<Class<Megacredit>>").first()).startsWith("6")
  }

  @Test
  fun robinsonCant() {
    val game = Engine.newGame(GameSetup(Canon, "MB", 3))
    val p1 = InteractiveSession(game, PLAYER1)
    p1.execute("PROD[Steel, Titanium, Plant, Heat]")
    p1.execute("@gainLowestProduction(Player1)")

    assertThat(p1.has("PROD[=5 M, =1 S, =1 T, =1 P, =0 E, =1 H]")).isTrue()

    // TODO make better
    assertThat(p1.agent.tasks().values.toStrings())
        .containsExactly(
            "A: [Player1] Production<Player1, Class<Megacredit>>! OR Production<Player1, " +
                "Class<Energy>>! (choice required in: `Production<Player1, Class<Megacredit>>! " +
                "OR Production<Player1, Class<Energy>>!`)")
  }

  @Test
  fun robinson2() {
    val repl = ReplSession(Canon, GameSetup(Canon, "MB", 3)) // TODO
    repl.command("become Player1")
    repl.command("exec PROD[-1]")
    repl.command("exec @gainLowestProduction(Player1)")
    assertThat(repl.command("has PROD[=5 Megacredit]").first()).startsWith("true")
  }

  @Test
  fun roboticWorkforce() {
    val repl = ReplSession(Canon, GameSetup(Canon, "MB", 3)) // TODO
    repl.command("become Player1")
    repl.command("exec 2 ProjectCard")
    repl.command("exec PROD[5 Energy]")
    repl.command("exec StripMine")
    assertThat(repl.command("count Production<Class<Energy>>").single()).startsWith("3 ")
    repl.command("exec RoboticWorkforce")
    // TODO now what
  }
}

// TODO share
private fun assertFails(message: String, shouldFail: () -> Unit) =
    assertThrows<RuntimeException>(message, shouldFail)
