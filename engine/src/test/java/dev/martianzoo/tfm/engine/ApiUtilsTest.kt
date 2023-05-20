package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.ApiUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class ApiUtilsTest {
  @Test
  fun testLookUpProdLevelsUsingCanon() {
    val session = PlayerSession(Game.create(Canon.SIMPLE_GAME), ENGINE)
    val prods: Map<ClassName, Int> = lookUpProductionLevels(session.reader, PLAYER1.expression)
    assertThat(prods.map { it.key to it.value })
        .containsExactly(
            cn("Megacredit") to 0,
            cn("Steel") to 0,
            cn("Titanium") to 0,
            cn("Plant") to 0,
            cn("Energy") to 0,
            cn("Heat") to 0,
        )

    session.writer.unsafe().sneak("PROD[2 Plant<Player1>!]")
    val prods2: Map<ClassName, Int> = lookUpProductionLevels(session.reader, PLAYER1.expression)
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
    val game = Game.create(Canon.SIMPLE_GAME)
    assertThat(standardResourceNames(game.reader).toStrings())
        .containsExactly("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat")
  }
}
