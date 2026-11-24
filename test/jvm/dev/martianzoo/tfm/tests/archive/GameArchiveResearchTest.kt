package dev.martianzoo.tfm.tests.archive

import dev.martianzoo.state.GameReader
import dev.martianzoo.testsupport.has
import dev.martianzoo.tfm.tests.replays.OtbGame20261111Test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GameArchiveResearchTest {
  @Test
  fun exportsWholeResearchSequencesAndImportsTheSameGame() {
    val replay =
        OtbGame20261111Test().apply {
          commonSetup()
          game20261111()
        }
    val text = requireNotNull(replay.completedGameArchive())
    val blocks = Regex("(?ms)^(\\w+): Research \\{\\n(.*?)^\\}").findAll(text).toList()
    // Eight research phases, with a complete purchase for each of the two players.
    assertEquals(
        mapOf("Green" to 8, "Pink" to 8),
        blocks.groupingBy { it.groupValues[1] }.eachCount(),
    )
    assertEquals(
        """
        ExMachina {
            PROD[-MC]
        }
        4 ProjectCard<Selecting>
        -0 ProjectCard<Selecting>
        Pay(12 MC)
        4 ProjectCard<Hand FROM Selecting>
        """
            .trimIndent(),
        blocks.first { it.groupValues[1] == "Green" }.groupValues[2].trimIndent().trim(),
    )
    val imported = replay.importGameArchive(text)
    assertTrue(imported.world.reader.has("End"))
    assertTrue(imported.world.tasks.isEmpty())
    fun inventory(reader: GameReader) =
        reader.getComponents(reader.classTable.componentClass.baseType).entries.associate {
          it.key.toString() to it.value
        }
    assertEquals(inventory(replay.game.reader), inventory(imported.world.reader))
  }
}
