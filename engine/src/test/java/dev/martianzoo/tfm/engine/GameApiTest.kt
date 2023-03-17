package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression.Companion.expression
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Metric.Companion.metric
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class GameApiTest {
  fun Game.count(s: String) = count(metric(s))
  fun Game.execute(s: String) = initiate(instruction(s), Actor(cn("Player2")))
  fun Game.evaluate(s: String) = evaluate(requirement(s))

  @Test
  fun basicByApi() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 3))

    val checkpoint = game.eventLog.checkpoint()
    assertThat(game.count("Heat")).isEqualTo(0)

    game.execute("5 Heat<Player2>!")
    game.execute("10 Heat<Player3>!")

    assertThat(game.count("Heat")).isEqualTo(15)

    game.execute("-4 Heat<Player2>!")
    assertThat(game.evaluate("Heat<Player2>")).isTrue()
    assertThat(game.evaluate("=1 Heat<Player2>")).isTrue()
    assertThat(game.evaluate("MAX 1 Heat<Player2>")).isTrue()
    assertThat(game.evaluate("2 Heat<Player2>")).isFalse()

    assertThat(game.count("StandardResource")).isEqualTo(11)
    assertThat(game.count("StandardResource<Player3>")).isEqualTo(10)
    game.execute("3 Steel<Player3> FROM Heat<Player3>!")
    assertThat(game.count("StandardResource<Player3>")).isEqualTo(10)
    assertThat(game.count("Steel")).isEqualTo(3)

    game.execute("2 Heat<Player2 FROM Player3>!")
    assertThat(game.evaluate("=3 Heat<Player2>")).isTrue()
    assertThat(game.evaluate("=5 Heat<Player3>")).isTrue()

    val changes = game.eventLog.changesSince(checkpoint)
    assertThat(changes.map { it.change })
        .containsExactly(
            StateChange(5, gaining = te("Heat<Player2>")),
            StateChange(10, gaining = te("Heat<Player3>")),
            StateChange(4, removing = te("Heat<Player2>")),
            StateChange(3, gaining = te("Steel<Player3>"), removing = te("Heat<Player3>")),
            StateChange(2, gaining = te("Heat<Player2>"), removing = te("Heat<Player3>")),
        )
        .inOrder()

    assertThat(strip(changes.toStrings().map { it.replace(Regex("^\\d+"), "") }))
        .containsExactly(
            ": +5 Heat<Player2> FOR Player2 (manual)",
            ": +10 Heat<Player3> FOR Player2 (manual)",
            ": -4 Heat<Player2> FOR Player2 (manual)",
            ": +3 Steel<Player3> FROM Heat<Player3> FOR Player2 (manual)",
            ": +2 Heat<Player2> FROM Heat<Player3> FOR Player2 (manual)",
        )
        .inOrder()
  }

  private fun te(s: String) = expression(s)

  // TODO duplication

  fun strip(strings: Iterable<String>): List<String> {
    return strings.map { endRegex.replace(startRegex.replace(it, ""), "") }
  }

  private val startRegex = Regex("^[^:]+: ")
  private val endRegex = Regex(" BECAUSE.*")
}
