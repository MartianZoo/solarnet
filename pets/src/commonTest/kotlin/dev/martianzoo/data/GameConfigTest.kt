package dev.martianzoo.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameConfigTest {
  @Test
  fun flexiblyParsesCommasSpacesNewlinesAndBlankLines() {
    val config =
        GameConfig(
            """
            Player1, Player2
            TerraformingMars, TharsisMapOption

            VenusNextExpansion, -WorldGovernmentOption
            """
                .trimIndent()
        )

    config.includedClassNames.shouldContainExactly(
        cn("Player1"),
        cn("Player2"),
        cn("TerraformingMars"),
        cn("TharsisMapOption"),
        cn("VenusNextExpansion"),
    )
    config.excludedClassNames.shouldContainExactly(cn("WorldGovernmentOption"))
    config.toString() shouldBe
        "Player1, Player2, TerraformingMars, TharsisMapOption, VenusNextExpansion, " +
            "-WorldGovernmentOption"
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
