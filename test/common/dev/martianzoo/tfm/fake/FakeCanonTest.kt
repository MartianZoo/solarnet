package dev.martianzoo.tfm.fake

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class FakeCanonTest {
  @Test
  internal fun `fake cards are available only when FakeCanon is composed with Canon`() {
    val config = GameConfig("PreludeExpansion, FakeStuffBundle", "Player1", "Player2")
    assertFailsWith<IllegalArgumentException> { Canon.gamePremise(config) }

    val premise = TfmCatalog.compose(Canon, FakeCanon).gamePremise(config)

    assertTrue(ClassTable.forPremise(premise).isActive(cn("FakeResearchNetwork")))
  }
}
