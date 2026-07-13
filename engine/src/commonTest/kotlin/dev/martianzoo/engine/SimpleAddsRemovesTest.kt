package dev.martianzoo.engine

import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.types.te
import dev.martianzoo.util.toStrings
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SimpleAddsRemovesTest {
  @Test
  fun basicByApi() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)

    val checkpoint = game.timeline.checkpoint()

    val eng = game.gameplay(ENGINE)
    eng.count("Heat") shouldBe 0

    val p2 = game.tfm(PLAYER2).godMode()

    p2.manual("5 Heat<Player2>!")
    p2.manual("10 Heat<Player1>!")

    eng.count("Heat") shouldBe 15

    p2.manual("-4 Heat")
    eng.has("Heat<Player2>") shouldBe true
    eng.has("=1 Heat<Player2>") shouldBe true
    eng.has("MAX 1 Heat<Player2>") shouldBe true
    eng.has("2 Heat<Player2>") shouldBe false
    eng.count("StandardResource") shouldBe 11
    eng.count("StandardResource<Player1>") shouldBe 10

    p2.manual("3 Steel<Player1> FROM Heat<Player1>!")
    eng.count("StandardResource<Player1>") shouldBe 10
    eng.count("Steel") shouldBe 3

    p2.manual("2 Heat<Player2 FROM Player1>!")
    eng.has("=3 Heat<Player2>") shouldBe true
    eng.has("=5 Heat<Player1>") shouldBe true

    val changes = game.events.changesSince(checkpoint)
    changes
        .map { it.change }
        .shouldContainExactly(
            StateChange(5, gaining = te("Heat<Player2>")),
            StateChange(10, gaining = te("Heat<Player1>")),
            StateChange(4, removing = te("Heat<Player2>")),
            StateChange(3, gaining = te("Steel<Player1>"), removing = te("Heat<Player1>")),
            StateChange(2, gaining = te("Heat<Player2>"), removing = te("Heat<Player1>")),
        )

    strip(changes.toStrings().map { it.replace(Regex("^\\d+"), "") })
        .shouldContainExactly(
            ": +5 Heat<Player2> FOR Player2 (manual)",
            ": +10 Heat<Player1> FOR Player2 (manual)",
            ": -4 Heat<Player2> FOR Player2 (manual)",
            ": +3 Steel<Player1> FROM Heat<Player1> FOR Player2 (manual)",
            ": +2 Heat<Player2> FROM Heat<Player1> FOR Player2 (manual)",
        )
  }

  fun strip(strings: Iterable<String>): List<String> {
    return strings.map { endRegex.replace(startRegex.replace(it, ""), "") }
  }

  private val startRegex = Regex("^[^:]+: ")
  private val endRegex = Regex(" BECAUSE.*")
}
