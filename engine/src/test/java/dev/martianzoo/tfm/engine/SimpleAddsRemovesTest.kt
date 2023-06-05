package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TerraformingMarsApi.Companion.tfm
import dev.martianzoo.tfm.types.te
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

internal class SimpleAddsRemovesTest {
  @Test
  fun basicByApi() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    val checkpoint = game.timeline.checkpoint()

    val eng = game.gameplay(ENGINE)
    assertThat(eng.count("Heat")).isEqualTo(0)

    val p2 = game.tfm(PLAYER2).gameplay.operationLayer()

    p2.manual("5 Heat<Player2>!")
    p2.manual("10 Heat<Player1>!")

    assertThat(eng.count("Heat")).isEqualTo(15)

    p2.manual("-4 Heat")
    assertThat(eng.has("Heat<Player2>")).isTrue()
    assertThat(eng.has("=1 Heat<Player2>")).isTrue()
    assertThat(eng.has("MAX 1 Heat<Player2>")).isTrue()
    assertThat(eng.has("2 Heat<Player2>")).isFalse()
    assertThat(eng.count("StandardResource")).isEqualTo(11)
    assertThat(eng.count("StandardResource<Player1>")).isEqualTo(10)

    p2.manual("3 Steel<Player1> FROM Heat<Player1>!")
    assertThat(eng.count("StandardResource<Player1>")).isEqualTo(10)
    assertThat(eng.count("Steel")).isEqualTo(3)

    p2.manual("2 Heat<Player2 FROM Player1>!")
    assertThat(eng.has("=3 Heat<Player2>")).isTrue()
    assertThat(eng.has("=5 Heat<Player1>")).isTrue()

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
