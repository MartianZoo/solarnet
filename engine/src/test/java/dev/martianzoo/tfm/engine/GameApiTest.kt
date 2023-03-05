package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.ChangeRecord.StateChange
import dev.martianzoo.tfm.pets.ast.Expression.Companion.expression
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Metric.Companion.metric
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class GameApiTest {

  // TODO these are convenient...
  fun Game.count(s: String) = count(metric(s))
  fun Game.execute(s: String) = execute(instruction(s))
  fun Game.evaluate(s: String) = evaluate(requirement(s))

  @Test
  fun basicByApi() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 3))

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

    assertThat(game.changeLog().map { it.change })
        .containsExactly(
            StateChange(5, gaining = te("Heat<Player2>")),
            StateChange(10, gaining = te("Heat<Player3>")),
            StateChange(4, removing = te("Heat<Player2>")),
            StateChange(3, removing = te("Heat<Player3>"), gaining = te("Steel<Player3>")),
            StateChange(2, removing = te("Heat<Player3>"), gaining = te("Heat<Player2>")),
        )
        .inOrder()

    assertThat(game.changeLog().toStrings().map { it.replace(Regex("^\\d+"), "") })
        .containsExactly(
            ": 5 Heat<Player2>",
            ": 10 Heat<Player3>",
            ": -4 Heat<Player2>",
            ": 3 Steel<Player3> FROM Heat<Player3>",
            ": 2 Heat<Player2> FROM Heat<Player3>",
        )
        .inOrder()
  }

  private fun te(s: String) = expression(s)
}
