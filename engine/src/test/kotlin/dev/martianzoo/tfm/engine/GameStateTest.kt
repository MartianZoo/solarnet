package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.PetsParser
import dev.martianzoo.tfm.pets.StateChange
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.types.PetClassTable
import dev.martianzoo.tfm.types.PetType.PetGenericType
import org.junit.jupiter.api.Test

private class GameStateTest {

  @Test
  fun basic() {
    val game = Engine.newGame(Canon, 3, setOf("B"))

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

    assertThat(game.changeLog).containsExactly(
        StateChange(1, 5, gaining = gte("Heat", gte("Player2"))),
        StateChange(2, 10, gaining = gte("Heat", gte("Player3"))),
        StateChange(3, 4, removing = gte("Heat", gte("Player2"))),
        StateChange(4, 3,
            gaining = gte("Steel", gte("Player3")),
            removing = gte("Heat", gte("Player3"))),
        StateChange(5, 2,
            gaining = gte("Heat", gte("Player2")),
            removing = gte("Heat", gte("Player3"))),
    ).inOrder()
  }

  @Test
  fun script() {
    val game = Engine.newGame(Canon, 3, setOf("B"))

    val s = """
      REQUIRE =0 Heat
      REQUIRE =0 Component
      EXEC 5 Heat<Player2>
      EXEC 10 Heat<Player3>
      REQUIRE =15 Heat
      EXEC -4 Heat<Player2>
      REQUIRE Heat<Player2>
      REQUIRE =1 Heat<Player2>
      REQUIRE MAX 1 Heat<Player2>

      COUNT StandardResource -> eleven
      REQUIRE =10 Resource<Player3>

      EXEC 3 Steel<Player3> FROM Heat<Player3>
      REQUIRE =10 Resource<Player3>
      REQUIRE =3 Steel<Anyone>

      EXEC 2 Heat<Player2 FROM Player3>
      REQUIRE =3 Heat<Player2>
      REQUIRE =5 Heat<Player3>
    """

    val script = PetsParser.parseScript(s)
    val results = game.execute(script)

    assertThat(results).containsExactly("eleven", 11)

    assertThat(game.changeLog.map { "$it" }).containsExactly(
        "1: 5 Heat<Player2> BY Unknown BECAUSE Unknown",
        "2: 10 Heat<Player3> BY Unknown BECAUSE Unknown",
        "3: -4 Heat<Player2> BY Unknown BECAUSE Unknown",
        "4: 3 Steel<Player3> FROM Heat<Player3> BY Unknown BECAUSE Unknown",
        "5: 2 Heat<Player2> FROM Heat<Player3> BY Unknown BECAUSE Unknown",
    ).inOrder()

    assertThat(game.changeLog).containsExactly(
        StateChange(1, 5, gaining = gte("Heat", gte("Player2"))),
        StateChange(2, 10, gaining = gte("Heat", gte("Player3"))),
        StateChange(3, 4, removing = gte("Heat", gte("Player2"))),
        StateChange(4, 3,
            gaining = gte("Steel", gte("Player3")),
            removing = gte("Heat", gte("Player3"))),
        StateChange(5, 2,
            gaining = gte("Heat", gte("Player2")),
            removing = gte("Heat", gte("Player3"))),
    ).inOrder()


  }

  private fun PetClassTable.cpt(expression: String) =
      Component(resolve(expression) as PetGenericType)
}
