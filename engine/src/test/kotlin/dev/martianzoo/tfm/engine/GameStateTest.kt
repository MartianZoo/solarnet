package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.lookUpProductionLevels
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.pets.Parsing.parsePets
import dev.martianzoo.tfm.pets.Parsing.parseScript
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test

private class GameStateTest {

  @Test
  fun basicByApi() {
    val game = Engine.newGame(Canon, 3, setOf("B", "M"))

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
        StateChange(1, 5, gaining = cn("Heat").addArgs(cn("Player2").type)),
        StateChange(2, 10, gaining = cn("Heat").addArgs(cn("Player3").type)),
        StateChange(3, 4, removing = cn("Heat").addArgs(cn("Player2").type)),
        StateChange(4, 3,
            gaining = cn("Steel").addArgs(cn("Player3").type),
            removing = cn("Heat").addArgs(cn("Player3").type)),
        StateChange(5, 2,
            gaining = cn("Heat").addArgs(cn("Player2").type),
            removing = cn("Heat").addArgs(cn("Player3").type)),
    ).inOrder()
  }

  @Test
  fun basicByScript() {
    val game = Engine.newGame(Canon, 3, setOf("B", "M"))

    val s = """
      REQUIRE =0 Heat
      REQUIRE =0 Resource
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

    val script = parseScript(s)
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
        StateChange(1, 5, gaining = cn("Heat").addArgs(cn("Player2").type)),
        StateChange(2, 10, gaining = cn("Heat").addArgs(cn("Player3").type)),
        StateChange(3, 4, removing = cn("Heat").addArgs(cn("Player2").type)),
        StateChange(4, 3,
            gaining = cn("Steel").addArgs(cn("Player3").type),
            removing = cn("Heat").addArgs(cn("Player3").type)),
        StateChange(5, 2,
            gaining = cn("Heat").addArgs(cn("Player2").type),
            removing = cn("Heat").addArgs(cn("Player3").type)),
    ).inOrder()


  }

  @Test
  fun lookup() { // TODO move where belongs
    val game = Engine.newGame(Canon, 3, setOf("B", "M"))
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game, cn("Player1").type)
    assertThat(prods).containsExactly(
        cn("Megacredit"), -5,
        cn("Steel"), 0,
        cn("Titanium"), 0,
        cn("Plant"), 0,
        cn("Energy"), 0,
        cn("Heat"), 0,
    )

    game.applyChange(2, gaining = parsePets("Production<Player1, Plant.CLASS>"))
    val prods2: Map<ClassName, Int> = lookUpProductionLevels(game, cn("Player1").type)
    assertThat(prods2).containsExactly(
        cn("Megacredit"), -5,
        cn("Steel"), 0,
        cn("Titanium"), 0,
        cn("Plant"), 2,
        cn("Energy"), 0,
        cn("Heat"), 0,
    )
  }
}
