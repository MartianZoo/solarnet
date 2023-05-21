package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.te
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class GameApiTest {
  @Test
  fun basicByApi() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    val checkpoint = game.checkpoint()
    assertThat(game.reader.count(parse<Metric>("Heat"))).isEqualTo(0)

    val p2 = game.session(PLAYER2)

    p2.operation("5 Heat<Player2>!")
    p2.operation("10 Heat<Player1>!")

    assertThat(game.reader.count(parse<Metric>("Heat"))).isEqualTo(15)

    p2.operation("-4 Heat<Player2>!")
    assertThat(game.reader.evaluate(parse("Heat<Player2>"))).isTrue()
    assertThat(game.reader.evaluate(parse("=1 Heat<Player2>"))).isTrue()
    assertThat(game.reader.evaluate(parse("MAX 1 Heat<Player2>"))).isTrue()
    assertThat(game.reader.evaluate(parse("2 Heat<Player2>"))).isFalse()
    assertThat(game.reader.count(parse<Metric>("StandardResource"))).isEqualTo(11)
    assertThat(game.reader.count(parse<Metric>("StandardResource<Player1>"))).isEqualTo(10)
    p2.operation("3 Steel<Player1> FROM Heat<Player1>!")
    assertThat(game.reader.count(parse<Metric>("StandardResource<Player1>"))).isEqualTo(10)
    assertThat(game.reader.count(parse<Metric>("Steel"))).isEqualTo(3)

    p2.operation("2 Heat<Player2 FROM Player1>!")
    assertThat(game.reader.evaluate(parse<Requirement>("=3 Heat<Player2>"))).isTrue()
    assertThat(game.reader.evaluate(parse<Requirement>("=5 Heat<Player1>"))).isTrue()

    val changes = game.events.changesSince(checkpoint)
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

  fun strip(strings: Iterable<String>): List<String> {
    return strings.map { endRegex.replace(startRegex.replace(it, ""), "") }
  }

  private val startRegex = Regex("^[^:]+: ")
  private val endRegex = Regex(" BECAUSE.*")
}
