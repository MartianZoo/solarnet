package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.ResourceUtils.lookUpProductionLevels
import dev.martianzoo.tfm.api.ResourceUtils.standardResourceNames
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.Parsing.parseInput
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.repl.InteractiveSession
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class ResourceUtilsTest {
  @Test
  fun testLookUpProdLevelsUsingCanon() {
    val session = InteractiveSession(GameSetup(Canon, "BM", 3))
    val prods: Map<ClassName, Int> = lookUpProductionLevels(session.game.reader, cn("Player1").expr)
    assertThat(prods.map { it.key to it.value })
        .containsExactly(
            cn("Megacredit") to 0,
            cn("Steel") to 0,
            cn("Titanium") to 0,
            cn("Plant") to 0,
            cn("Energy") to 0,
            cn("Heat") to 0,
        )

    session.initiateOnly(parseInput("PROD[2 Plant<Player1>!]"))
    val prods2: Map<ClassName, Int> =
        lookUpProductionLevels(session.game.reader, cn("Player1").expr)
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
    val game = Engine.newGame(GameSetup(Canon, "BM", 3))
    assertThat(standardResourceNames(game.reader).toStrings())
        .containsExactly("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat")
  }
}
