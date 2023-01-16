package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.util.Grid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameSetupTest {
  val authority = object : Authority.Empty() { // TODO share...
    override val allBundles = "BRMEVPCX".asIterable().map { "$it" }.toSet()
    override val marsMapDefinitions = listOf(
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
    assertThrows<RuntimeException>("solo") { GameSetup(authority, "BM", 1) }
    assertThrows<RuntimeException>("many") { GameSetup(authority, "BM", 6) }
  }

  @Test
  fun badBundles() {
    assertThrows<RuntimeException>("no base") { GameSetup(authority, "M", 4) }
    assertThrows<RuntimeException>("repeated") { GameSetup(authority, "MBM", 4) }
    assertThrows<RuntimeException>("no map") { GameSetup(authority, "B", 4) }
    assertThrows<RuntimeException>("two maps") { GameSetup(authority, "BME", 4) }
    assertThrows<RuntimeException>("wrong bundle") { GameSetup(authority, "BMZ", 4) }
  }
}
