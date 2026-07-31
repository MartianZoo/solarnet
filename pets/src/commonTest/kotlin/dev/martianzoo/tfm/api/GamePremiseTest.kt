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
    premise("TerraformingMars,TharsisMap", 2)
    premise("TerraformingMars,ElysiumMap", 3)
    premise(
        "TerraformingMars,CorporateEraExpansion,TharsisMap,VenusNextExpansion,PreludeExpansion,PromoCardPack",
        4,
    )
    premise("TerraformingMars,TharsisMap", 5)
  }

  @Test
  fun badPlayerCount() {
    assertFails("many") { premise("TerraformingMars,TharsisMap", 6) }
  }

  @Test
  fun badOptions() {
    assertFails("no base") { premise("TharsisMap", 4) }
    assertFails("repeated") { premise("TharsisMap,TerraformingMars,TharsisMap", 4) }
    assertFails("no map") { premise("TerraformingMars", 4) }
    assertFails("two maps") { premise("TerraformingMars,TharsisMap,ElysiumMap", 4) }
    assertFails("wrong bundle") { premise("TerraformingMars,UnknownOption", 4) }
  }

  @Test
  fun exactOptionsSelectTheNeededRuleset() {
    val premise = premise("TerraformingMars,TharsisMap", 2)
    val ruleset = premise.ruleset as TfmRuleset

    ruleset.bundles.map { it.bundleName }.toSet() shouldBe
        setOf(cn("TerraformingMars"), cn("TharsisMap"))
    ruleset.marsMapDefinitions.single().className shouldBe cn("Tharsis")
    ruleset.marsMapDefinitions.single().areas[5, 5]!!.shortName shouldBe cn("Tharsis_5_5")
  }

  @Test
  fun onePlayerCompatibilitySetupSelectsSoloMode() {
    premise("TerraformingMars,SoloMode,TharsisMap", 1).initialComponents.toSet() shouldBe
        setOf("TerraformingMars", "TharsisMap", "SoloMode")
  }

  @Test
  fun coloniesMustBeSpecifiedExactly() {
    assertFails("missing colonies") { premise("TerraformingMars,TharsisMap,ColoniesExpansion", 2) }
    assertFails("partial colonies") {
      premise("TerraformingMars,TharsisMap,ColoniesExpansion", 2, setOf(cn("Luna")))
    }

    val exact = listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto").toSetStrict(::cn)
    val premise = premise("TerraformingMars,TharsisMap,ColoniesExpansion", 2, exact)
    premise.initialComponents.toSet().containsAll(exact.map { "${it}Selected" }) shouldBe true
  }

  @Test
  fun soloColoniesUseExactlyThreeTiles() {
    val exact = setOf(cn("Luna"), cn("Ceres"), cn("Triton"))
    premise("TerraformingMars,SoloMode,TharsisMap,ColoniesExpansion", 1, exact)

    assertFails("four solo colonies") {
      premise("TerraformingMars,SoloMode,TharsisMap,ColoniesExpansion", 1, exact + cn("Ganymede"))
    }
  }

  private fun premise(
      optionNames: String,
      players: Int,
      colonyTiles: Set<dev.martianzoo.pets.ast.ClassName> = emptySet(),
  ) = Canon.gamePremise(GameOptions(players, optionNames.split(',').toSetStrict(::cn), colonyTiles))
}
