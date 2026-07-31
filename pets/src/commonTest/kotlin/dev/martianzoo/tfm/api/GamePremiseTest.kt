package dev.martianzoo.tfm.api

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameOptions
import dev.martianzoo.tfm.testlib.assertFails
import dev.martianzoo.util.toSetStrict
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GamePremiseTest {
  @Test
  fun good() {
    premise("TerraformingMars,TharsisMapOption", 2)
    premise("TerraformingMars,ElysiumMapOption", 3)
    premise(
        "TerraformingMars,CorporateEraExpansion,TharsisMapOption,VenusNextExpansion,PreludeExpansion,PromoCardPack",
        4,
    )
    premise("TerraformingMars,TharsisMapOption", 5)
  }

  @Test
  fun badPlayerCount() {
    assertFails("many") { premise("TerraformingMars,TharsisMapOption", 6) }
  }

  @Test
  fun badOptions() {
    assertFails("no base") { premise("TharsisMapOption", 4) }
    assertFails("repeated") {
      premise("TharsisMapOption,TerraformingMars,TharsisMapOption", 4)
    }
    assertFails("no map") { premise("TerraformingMars", 4) }
    assertFails("two maps") {
      premise("TerraformingMars,TharsisMapOption,ElysiumMapOption", 4)
    }
    assertFails("wrong bundle") { premise("TerraformingMars,UnknownOption", 4) }
  }

  @Test
  fun exactOptionsSelectTheNeededRuleset() {
    val premise = premise("TerraformingMars,TharsisMapOption", 2)
    val ruleset = premise.ruleset as TfmRuleset

    ruleset.bundles.map { it.bundleName }.toSet() shouldBe
        setOf(cn("TerraformingMars"), cn("TharsisMap"))
    ruleset.marsMapDefinitions.single().className shouldBe cn("Tharsis")
    ruleset.marsMapDefinitions.single().areas[5, 5]!!.shortName shouldBe cn("Tharsis_5_5")
  }

  @Test
  fun onePlayerCompatibilitySetupSelectsSoloMode() {
    premise("TerraformingMars,SoloMode,TharsisMapOption", 1).initialComponents.toSet() shouldBe
        setOf("TerraformingMars", "TharsisMapOption", "SoloMode")
  }

  @Test
  fun coloniesMustBeSpecifiedExactly() {
    assertFails("missing colonies") {
      premise("TerraformingMars,TharsisMapOption,ColoniesExpansion", 2)
    }
    assertFails("partial colonies") {
      premise("TerraformingMars,TharsisMapOption,ColoniesExpansion", 2, setOf(cn("Luna")))
    }

    val exact = listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto").toSetStrict(::cn)
    val premise = premise("TerraformingMars,TharsisMapOption,ColoniesExpansion", 2, exact)
    premise.initialComponents.toSet().containsAll(exact.map { "${it}Selected" }) shouldBe true
  }

  @Test
  fun soloColoniesUseExactlyThreeTiles() {
    val exact = setOf(cn("Luna"), cn("Ceres"), cn("Triton"))
    premise("TerraformingMars,SoloMode,TharsisMapOption,ColoniesExpansion", 1, exact)

    assertFails("four solo colonies") {
      premise(
          "TerraformingMars,SoloMode,TharsisMapOption,ColoniesExpansion",
          1,
          exact + cn("Ganymede"),
      )
    }
  }

  private fun premise(
      optionNames: String,
      players: Int,
      colonyTiles: Set<dev.martianzoo.pets.ast.ClassName> = emptySet(),
  ) = Canon.gamePremise(GameOptions(players, optionNames.split(',').toSetStrict(::cn), colonyTiles))
}
