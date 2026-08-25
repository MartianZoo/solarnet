package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Gameplay.GodMode
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.ApiUtils.getOwner
import dev.martianzoo.tfm.canon.ApiUtils.getPlayerOwner
import dev.martianzoo.tfm.canon.ApiUtils.lookUpProductionLevels
import dev.martianzoo.tfm.canon.ApiUtils.standardResourceNames
import dev.martianzoo.util.toStrings
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ApiUtilsTest {
  @Test
  internal fun componentOwnershipUsesTheOwnerRole() {
    val game = Engine.newGame(canonicalPremise())
    val plant = game.gameplay(PLAYER1).resolve("Plant")

    getOwner(game.reader, plant).className shouldBe PLAYER1.className
    getPlayerOwner(game.reader, plant) shouldBe PLAYER1
  }

  @Test
  internal fun testLookUpProdLevelsUsingCanon() {
    val game = Engine.newGame(canonicalPremise())
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
  internal fun stdResNamesInCanon() {
    val game = Engine.newGame(canonicalPremise())
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
