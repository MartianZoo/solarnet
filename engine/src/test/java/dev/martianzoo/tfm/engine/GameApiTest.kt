package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.pets.Parsing.parseAsIs
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class GameApiTest {
  fun Game.count(s: String) = reader.count(parseAsIs<Metric>(s))
  fun Game.execute(s: String) = asPlayer(Player(cn("Player2"))).initiate(parseAsIs(s))
  fun Game.evaluate(s: String) = reader.evaluate(parseAsIs(s))

  @Test
  fun basicByApi() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 2))

    val checkpoint = game.checkpoint()
    assertThat(game.count("Heat")).isEqualTo(0)

    game.execute("5 Heat<Player2>!")
    game.execute("10 Heat<Player1>!")

    assertThat(game.count("Heat")).isEqualTo(15)

    game.execute("-4 Heat<Player2>!")
    assertThat(game.evaluate("Heat<Player2>")).isTrue()
    assertThat(game.evaluate("=1 Heat<Player2>")).isTrue()
    assertThat(game.evaluate("MAX 1 Heat<Player2>")).isTrue()
    assertThat(game.evaluate("2 Heat<Player2>")).isFalse()

    assertThat(game.count("StandardResource")).isEqualTo(11)
    assertThat(game.count("StandardResource<Player1>")).isEqualTo(10)
    game.execute("3 Steel<Player1> FROM Heat<Player1>!")
    assertThat(game.count("StandardResource<Player1>")).isEqualTo(10)
    assertThat(game.count("Steel")).isEqualTo(3)

    game.execute("2 Heat<Player2 FROM Player1>!")
    assertThat(game.evaluate("=3 Heat<Player2>")).isTrue()
    assertThat(game.evaluate("=5 Heat<Player1>")).isTrue()

    val changes = game.eventLog.changesSince(checkpoint)

    fun te(s: String): Expression = parseAsIs(s)

    assertThat(changes.map { it.change })
        .containsExactly(
            StateChange(5, gaining = te("Heat<Player2>")),
            StateChange(10, gaining = te("Heat<Player1>")),
            StateChange(4, removing = te("Heat<Player2>")),
            StateChange(3, gaining = te("Steel<Player1>"), removing = te("Heat<Player1>")),
            StateChange(2, gaining = te("Heat<Player2>"), removing = te("Heat<Player1>")),
        )
        .inOrder()

    assertThat(strip(changes.toStrings().map { it.replace(Regex("^\\d+"), "") }))
        .containsExactly(
            ": +5 Heat<Player2> FOR Player2 (manual)",
            ": +10 Heat<Player1> FOR Player2 (manual)",
            ": -4 Heat<Player2> FOR Player2 (manual)",
            ": +3 Steel<Player1> FROM Heat<Player1> FOR Player2 (manual)",
            ": +2 Heat<Player2> FROM Heat<Player1> FOR Player2 (manual)",
        )
        .inOrder()
  }

  // TODO duplication

  fun strip(strings: Iterable<String>): List<String> {
    return strings.map { endRegex.replace(startRegex.replace(it, ""), "") }
  }

  private val startRegex = Regex("^[^:]+: ")
  private val endRegex = Regex(" BECAUSE.*")
}
