package dev.martianzoo.tfm.api

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.testlib.assertFails
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameSetupTest {
  @Test
  fun good() {
    GameSetup(Canon, "BM", 2)
    GameSetup(Canon, "BE", 3)
    GameSetup(Canon, "BRMVPX", 4)
    GameSetup(Canon, "BM", 5)
  }

  @Test
  fun badPlayerCount() {
    assertFails("many") { GameSetup(Canon, "BM", 6) }
  }

  @Test
  fun badBundles() {
    assertFails("no base") { GameSetup(Canon, "M", 4) }
    assertFails("repeated") { GameSetup(Canon, "MBM", 4) }
    assertFails("no map") { GameSetup(Canon, "B", 4) }
    assertFails("two maps") { GameSetup(Canon, "BME", 4) }
    assertFails("wrong bundle") { GameSetup(Canon, "BMZ", 4) }
  }

  @Test
  fun compatibilityConstructorOnlyTranslatesLettersToBundleNames() {
    val setup = GameSetup(Canon, "BM", 2)

    setup.ruleset shouldBe Canon
    setup.bundles shouldBe setOf(cn("TerraformingMars"), cn("TharsisMap"))
    setup.bundleString shouldBe "BM"
    setup.map.className shouldBe cn("Tharsis")
  }

  @Test
  fun onePlayerCompatibilitySetupSelectsSoloMode() {
    GameSetup(Canon, "BM", 1).bundles shouldBe
        setOf(cn("TerraformingMars"), cn("TharsisMap"), cn("SoloMode"))
  }
}
