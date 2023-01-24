package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.typeExpression
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class GameApiTest {

  fun Game.count(s: String) = count(typeExpression(s))
  fun Game.execute(s: String) = execute(instruction(s))
  fun Game.isMet(s: String) = isMet(requirement(s))

  @Test
  fun basicByApi() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 3))

    assertThat(game.count("Heat")).isEqualTo(0)

    game.execute("5 Heat<Player2>")
    game.execute("10 Heat<Player3>")

    assertThat(game.count("Heat")).isEqualTo(15)

    game.execute("-4 Heat<Player2>")
    assertThat(game.isMet("Heat<Player2>")).isTrue()
    assertThat(game.isMet("=1 Heat<Player2>")).isTrue()
    assertThat(game.isMet("MAX 1 Heat<Player2>")).isTrue()
    assertThat(game.isMet("2 Heat<Player2>")).isFalse()

    assertThat(game.count("StandardResource")).isEqualTo(11)
    assertThat(game.count("StandardResource<Player3>")).isEqualTo(10)
    game.execute("3 Steel<Player3> FROM Heat<Player3>")
    assertThat(game.count("StandardResource<Player3>")).isEqualTo(10)
    assertThat(game.count("Steel")).isEqualTo(3)

    game.execute("2 Heat<Player2 FROM Player3>")
    assertThat(game.isMet("=3 Heat<Player2>")).isTrue()
    assertThat(game.isMet("=5 Heat<Player3>")).isTrue()

    assertThat(game.changeLog())
        .containsExactly(
            StateChange(5, gaining = cn("Heat").addArgs(cn("Player2").type)),
            StateChange(10, gaining = cn("Heat").addArgs(cn("Player3").type)),
            StateChange(4, removing = cn("Heat").addArgs(cn("Player2").type)),
            StateChange(
                3,
                gaining = cn("Steel").addArgs(cn("Player3").type),
                removing = cn("Heat").addArgs(cn("Player3").type)),
            StateChange(
                2,
                gaining = cn("Heat").addArgs(cn("Player2").type),
                removing = cn("Heat").addArgs(cn("Player3").type)),
        )
        .inOrder()

    assertThat(game.changeLog().toStrings())
        .containsExactly(
            "5 Heat<Player2>",
            "10 Heat<Player3>",
            "-4 Heat<Player2>",
            "3 Steel<Player3> FROM Heat<Player3>",
            "2 Heat<Player2> FROM Heat<Player3>",
        )
        .inOrder()
  }
}
