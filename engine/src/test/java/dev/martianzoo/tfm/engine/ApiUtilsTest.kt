package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Gameplay.GodMode
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.ApiUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.ApiUtils.standardResourceNames
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.util.toStrings
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.test.Test

internal class ApiUtilsTest {
  @Test
  fun testLookUpProdLevelsUsingCanon() {
    val game = Engine.newGame(Canon.SIMPLE_GAME)
    val prods: Map<ClassName, Int> = lookUpProductionLevels(game.reader, PLAYER1.expression)
    prods
        .map { it.key to it.value }
        .shouldContainExactlyInAnyOrder(
            cn("Megacredit") to 0,
            cn("Steel") to 0,
            cn("Titanium") to 0,
            cn("Plant") to 0,
            cn("Energy") to 0,
            cn("Heat") to 0,
        )

    (game.gameplay(PLAYER1) as GodMode).sneak("PROD[2 Plant]")
    val prods2: Map<ClassName, Int> = lookUpProductionLevels(game.reader, PLAYER1.expression)
    prods2
        .map { it.key to it.value }
        .shouldContainExactlyInAnyOrder(
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
    standardResourceNames(game.reader)
        .toStrings()
        .shouldContainExactlyInAnyOrder(
            "Megacredit",
            "Steel",
            "Titanium",
            "Plant",
            "Energy",
            "Heat",
        )
  }
}
