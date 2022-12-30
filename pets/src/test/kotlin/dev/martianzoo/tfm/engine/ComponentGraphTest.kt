package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.ast.StateChange
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.types.PetClassLoader
import dev.martianzoo.tfm.types.PetClassTable
import org.junit.jupiter.api.Test

class ComponentGraphTest {

  @Test
  fun basic() {
    val table = PetClassLoader(Canon.allDefinitions).loadAll()
    val cg = ComponentGraph()
    val game = Game(cg, table) // it's just more convenient

    assertThat(game.count("Heat")).isEqualTo(0)

    val expression = "Heat<Player2>"
    game.components.gain(5, table.cpt(expression))
    game.components.gain(10, table.cpt("Heat<Player3>"))

    assertThat(game.count("Heat")).isEqualTo(15)

    game.components.remove(4, table.cpt("Heat<Player2>"))
    assertThat(game.isMet("Heat<Player2>")).isTrue()
    assertThat(game.isMet("2 Heat<Player2>")).isFalse()

    assertThat(game.count("StandardResource")).isEqualTo(11)
    assertThat(game.count("StandardResource<Player3>")).isEqualTo(10)
    game.components.transmute(3, table.cpt("Steel<Player3>"), table.cpt("Heat<Player3>"))
    assertThat(game.count("StandardResource<Player3>")).isEqualTo(10)
    assertThat(game.count("Steel")).isEqualTo(3)

    assertThat(game.components.changeLog).containsExactly(
        StateChange(1, 5, gaining = te("Heat", te("Player2"))),
        StateChange(2, 10, gaining = te("Heat", te("Player3"))),
        StateChange(3, 4, removing = te("Heat", te("Player2"))),
        StateChange(4, 3, gaining = te("Steel", te("Player3")), removing = te("Heat", te("Player3"))),
    ).inOrder()
  }

  private fun PetClassTable.cpt(expression: String) = Component(resolve(expression))
}
