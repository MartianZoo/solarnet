package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.lookUpProductionLevels
import dev.martianzoo.tfm.api.standardResourceNames
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

class ApiHelpersTest {
  @Test
  fun lookupProds() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 3))
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game, ClassName.cn("Player1").type)
    assertThat(prods).containsExactly(
        ClassName.cn("Megacredit"), -5,
        ClassName.cn("Steel"), 0,
        ClassName.cn("Titanium"), 0,
        ClassName.cn("Plant"), 0,
        ClassName.cn("Energy"), 0,
        ClassName.cn("Heat"), 0,
    )

    game.applyChange(2, gaining = TypeExpression.fromGeneric("Production<Player1, Plant.CLASS>"))
    val prods2: Map<ClassName, Int> = lookUpProductionLevels(game, ClassName.cn("Player1").type)
    assertThat(prods2).containsExactly(
        ClassName.cn("Megacredit"), -5,
        ClassName.cn("Steel"), 0,
        ClassName.cn("Titanium"), 0,
        ClassName.cn("Plant"), 2,
        ClassName.cn("Energy"), 0,
        ClassName.cn("Heat"), 0,
    )
  }

  @Test
  fun stdResNames() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 3))
    assertThat(standardResourceNames(game).toStrings()).containsExactly(
        "Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat"
    )
  }
}
