package dev.martianzoo.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameConfigTest {
  @Test
  fun parsesCommaAndNewlineSeparatedSignedClassNames() {
    val config = GameConfig("Player1, TerraformingMars\n-WorldGovernmentOption")

    config.includedClassNames.shouldContainExactly(cn("Player1"), cn("TerraformingMars"))
    config.excludedClassNames.shouldContainExactly(cn("WorldGovernmentOption"))
    config.toString() shouldBe "Player1, TerraformingMars, -WorldGovernmentOption"
  }

  @Test
  fun entryOrderIsNotSemantic() {
    GameConfig("Player1, TerraformingMars") shouldBe GameConfig("TerraformingMars, Player1")
  }

  @Test
  fun rejectsDuplicateAndNonClassEntries() {
    shouldThrow<IllegalArgumentException> { GameConfig("TerraformingMars, TerraformingMars") }
    shouldThrow<IllegalArgumentException> { GameConfig("TerraformingMars, -TerraformingMars") }
    shouldThrow<IllegalArgumentException> { GameConfig("2 Player") }
    shouldThrow<IllegalArgumentException> { GameConfig("Select<Class<Card001>>") }
  }
}
