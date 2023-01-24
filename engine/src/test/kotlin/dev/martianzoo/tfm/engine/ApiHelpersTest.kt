package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.lookUpProductionLevels
import dev.martianzoo.tfm.api.standardResourceNames
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

class ApiHelpersTest {
  @Test
  fun testLookUpProdLevelsUsingCanon() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 3))
    val prods: Map<ClassName, Int> =
        lookUpProductionLevels(game.asGameState, ClassName.cn("Player1").type)
    assertThat(prods.map { it.key to it.value })
        .containsExactly(
            ClassName.cn("Megacredit") to -5,
            ClassName.cn("Steel") to 0,
            ClassName.cn("Titanium") to 0,
            ClassName.cn("Plant") to 0,
            ClassName.cn("Energy") to 0,
            ClassName.cn("Heat") to 0,
        )

    game.execute(instruction("2 Production<Player1, Plant.CLASS>"))
    val prods2: Map<ClassName, Int> =
        lookUpProductionLevels(game.asGameState, ClassName.cn("Player1").type)
    assertThat(prods2.map { it.key to it.value })
        .containsExactly(
            ClassName.cn("Megacredit") to -5,
            ClassName.cn("Steel") to 0,
            ClassName.cn("Titanium") to 0,
            ClassName.cn("Plant") to 2,
            ClassName.cn("Energy") to 0,
            ClassName.cn("Heat") to 0,
        )
  }

  @Test
  fun stdResNamesInCanon() {
    val game = Engine.newGame(GameSetup(Canon, "BM", 3))
    assertThat(standardResourceNames(game.asGameState).toStrings())
        .containsExactly("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat")
  }
}
