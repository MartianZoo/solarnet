package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.testlib.assertFails
import dev.martianzoo.util.Grid
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class GameSetupTest {
  val authority =
      object : Authority.Empty() {
        override val allBundles = "BRMEVPCX".asIterable().toStrings().toSet()
        override val marsMapDefinitions =
            setOf(
                MarsMapDefinition(cn("Tharsis"), "M", Grid.empty()),
                MarsMapDefinition(cn("Elysium"), "E", Grid.empty()),
            )
      }

  @Test
  fun good() {
    GameSetup(authority, "BM", 2)
    GameSetup(authority, "BE", 3)
    GameSetup(authority, "BRMVPCX", 4)
    GameSetup(authority, listOf("B", "M"), 5)
  }

  @Test
  fun badPlayerCount() {
    assertFails("solo") { GameSetup(authority, "BM", 1) }
    assertFails("many") { GameSetup(authority, "BM", 6) }
  }

  @Test
  fun badBundles() {
    assertFails("no base") { GameSetup(authority, "M", 4) }
    assertFails("repeated") { GameSetup(authority, "MBM", 4) }
    assertFails("no map") { GameSetup(authority, "B", 4) }
    assertFails("two maps") { GameSetup(authority, "BME", 4) }
    assertFails("wrong bundle") { GameSetup(authority, "BMZ", 4) }
  }
}
