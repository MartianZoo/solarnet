package dev.martianzoo.tfm.api

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.testlib.assertFails
import dev.martianzoo.util.toSetStrict
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameSetupTest {
  @Test
  fun good() {
    Canon.fromOptionCodes("BM", 2)
    Canon.fromOptionCodes("BE", 3)
    Canon.fromOptionCodes("BRMVPX", 4)
    Canon.fromOptionCodes("BM", 5)
  }

  @Test
  fun badPlayerCount() {
    assertFails("many") { Canon.fromOptionCodes("BM", 6) }
  }

  @Test
  fun badOptions() {
    assertFails("no base") { Canon.fromOptionCodes("M", 4) }
    assertFails("repeated") { Canon.fromOptionCodes("MBM", 4) }
    assertFails("no map") { Canon.fromOptionCodes("B", 4) }
    assertFails("two maps") { Canon.fromOptionCodes("BME", 4) }
    assertFails("wrong bundle") { Canon.fromOptionCodes("BMZ", 4) }
  }

  @Test
  fun optionCodeAdapterSelectsTheNeededRuleset() {
    val setup = Canon.fromOptionCodes("BM", 2)

    setup.ruleset.bundles.map { it.bundleName }.toSet() shouldBe
        setOf(cn("TerraformingMars"), cn("TharsisMap"))
    Canon.optionCodes(setup.options) shouldBe "BM"
    setup.map.className shouldBe cn("Tharsis")
  }

  @Test
  fun onePlayerCompatibilitySetupSelectsSoloMode() {
    Canon.fromOptionCodes("BSM", 1).options.enabled shouldBe
        setOf(cn("TerraformingMars"), cn("TharsisMap"), cn("SoloMode"))
  }

  @Test
  fun coloniesMustBeSpecifiedExactly() {
    assertFails("missing colonies") { Canon.fromOptionCodes("BMC", 2) }
    assertFails("partial colonies") {
      Canon.fromOptionCodes("BMC", 2, setOf(cn("Luna")))
    }

    val exact = listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto").toSetStrict(::cn)
    Canon.fromOptionCodes("BMC", 2, exact).options.colonyTiles shouldBe exact
  }
}
