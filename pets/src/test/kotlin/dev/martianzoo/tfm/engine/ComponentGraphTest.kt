package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.types.PetClassLoader
import org.junit.jupiter.api.Test

class ComponentGraphTest {

  @Test
  fun simple() {
    val table = PetClassLoader(Canon.allDefinitions).loadAll()
    val cg = ComponentGraph()
    val game = Game(cg, table)

    assertThat(game.count("Heat")).isEqualTo(0)

    game.components.applyChange(5, gaining = Component(table.resolve("Heat<Player2>")))
    game.components.applyChange(10, gaining = Component(table.resolve("Heat<Player3>")))

    assertThat(game.count("Heat")).isEqualTo(15)

    game.components.applyChange(4, removing = Component(table.resolve("Heat<Player2>")))
    assertThat(game.isMet("Heat<Player2>")).isTrue()
    assertThat(game.isMet("2 Heat<Player2>")).isFalse()
  }
}
