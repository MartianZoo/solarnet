package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Gameplay.GodMode
import dev.martianzoo.tfm.api.ApiUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class ApiUtilsTest {
  @Test
  fun testLookUpProdLevelsUsingCanon() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game.reader, PLAYER1.expression)
    assertThat(prods.map { it.key to it.value })
        .containsExactly(
            cn("Megacredit") to 0,
            cn("Steel") to 0,
            cn("Titanium") to 0,
            cn("Plant") to 0,
            cn("Energy") to 0,
            cn("Heat") to 0,
        )

    (game.gameplay(PLAYER1) as GodMode).sneak("PROD[2 Plant]")
    val prods2: Map<ClassName, Int> = lookUpProductionLevels(game.reader, PLAYER1.expression)
    assertThat(prods2.map { it.key to it.value })
        .containsExactly(
            cn("Megacredit") to 0,
            cn("Steel") to 0,
            cn("Titanium") to 0,
            cn("Plant") to 2,
            cn("Energy") to 0,
            cn("Heat") to 0,
        )
  }

  @Test
  fun stdResNamesInCanon() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    assertThat(standardResourceNames(game.reader).toStrings())
        .containsExactly("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat")
  }
}
