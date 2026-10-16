package dev.martianzoo.tfm.web.playground

import dev.martianzoo.agent.Agents
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ExplorerTreeTest {
  @Test
  fun hidingAspectsPreservesEveryPresentComponentAndItsCount() {
    val premise = Canon.gamePremise(GameConfig("TharsisMap", "Green"))
    val world = TfmEngine.newGame(premise)
    Agents(world)[Player(cn("Green"))].sneak("3 MC, 2 TerraformRating")
    val specimen =
        liveSpecimen(
            "Explorer",
            world.reader,
            world.events,
            world.tasks,
            world.timeline.checkpoint().ordinal,
            world.events.changesSince(Checkpoint(0)),
            eventBalances(premise, world.events.entriesSince(Checkpoint(0))),
        )
    val table = world.classTable
    assertTrue(table.isProbableAspect(table.getClass(cn("Atomized"))))

    for (includeNonReal in listOf(false, true)) {
      val tree = ExplorerTree(table, specimen, includeNonReal)
      val lines = tree.lines { emptyList() }
      val groups = lines.filterIsInstance<ExplorerTree.Line.Group>()
      assertFalse(groups.any { table.isProbableAspect(it.type.rootClass) })
      val expected =
          specimen.components
              .filter { includeNonReal || !it.nonReal }
              .associate { it.type to it.count }
      val actual =
          lines.filterIsInstance<ExplorerTree.Line.Exact>().associate {
            it.entry.type.toString() to it.entry.count
          }
      assertEquals(expected, actual)
      assertEquals(expected.values.sum(), groups.first().count)
    }
  }
}
