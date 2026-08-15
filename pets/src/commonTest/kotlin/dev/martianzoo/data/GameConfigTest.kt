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
            TerraformingMars, TharsisMapOption

            VenusNextExpansion, -WorldGovernmentOption
            """
                .trimIndent(),
            "Player1",
            "Player2",
        )

    config.includedClassNames.shouldContainExactly(
        cn("TerraformingMars"),
        cn("TharsisMapOption"),
        cn("VenusNextExpansion"),
    )
    config.excludedClassNames.shouldContainExactly(cn("WorldGovernmentOption"))
    config.playerNames.shouldContainExactly(cn("Player1"), cn("Player2"))
    config.toString() shouldBe
        "TerraformingMars, TharsisMapOption, VenusNextExpansion, -WorldGovernmentOption"
  }

  @Test
  fun entryOrderIsNotSemantic() {
    GameConfig("TerraformingMars, PreludeExpansion") shouldBe
        GameConfig("PreludeExpansion, TerraformingMars")
  }

  @Test
  fun acceptsArbitraryPlayerClassNamesSeparately() {
    val config = GameConfig("TerraformingMars", "Mom", "Ellie")

    config.includedClassNames.shouldContainExactly(cn("TerraformingMars"))
    config.playerNames.shouldContainExactly(cn("Mom"), cn("Ellie"))
    config.toString() shouldBe "TerraformingMars"
  }

  @Test
  fun rejectsDuplicateAndNonClassEntries() {
    shouldThrow<IllegalArgumentException> { GameConfig("TerraformingMars, TerraformingMars") }
    shouldThrow<IllegalArgumentException> { GameConfig("TerraformingMars, -TerraformingMars") }
    shouldThrow<IllegalArgumentException> { GameConfig("TerraformingMars", "Mom", "Mom") }
    shouldThrow<IllegalArgumentException> { GameConfig("2 Player") }
    shouldThrow<IllegalArgumentException> { GameConfig("Select<Class<Card001>>") }
  }
}
