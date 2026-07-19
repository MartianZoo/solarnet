package dev.martianzoo.tfm.api

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.testlib.assertFails
import dev.martianzoo.util.Grid
import dev.martianzoo.util.toStrings
import kotlin.test.Test

internal class GameSetupTest {
  val ruleset =
      object : TfmRuleset.Empty() {
        override val allBundles = "BRMEVPCX".asIterable().toStrings().toSet()
        override val marsMapDefinitions =
            setOf(
                MarsMapDefinition(cn("Tharsis"), "M", Grid.empty()),
                MarsMapDefinition(cn("Elysium"), "E", Grid.empty()),
            )
      }

  @Test
  fun good() {
    GameSetup(ruleset, "BM", 2)
    GameSetup(ruleset, "BE", 3)
    GameSetup(ruleset, "BRMVPX", 4)
    GameSetup(ruleset, "BM", 5)
  }

  @Test
  fun badPlayerCount() {
    assertFails("many") { GameSetup(ruleset, "BM", 6) }
  }

  @Test
  fun badBundles() {
    assertFails("no base") { GameSetup(ruleset, "M", 4) }
    assertFails("repeated") { GameSetup(ruleset, "MBM", 4) }
    assertFails("no map") { GameSetup(ruleset, "B", 4) }
    assertFails("two maps") { GameSetup(ruleset, "BME", 4) }
    assertFails("wrong bundle") { GameSetup(ruleset, "BMZ", 4) }
  }
}
