package dev.martianzoo.tfm.tests.archive

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.recording
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.state.TaskQueue
import dev.martianzoo.tfm.engine.GameArchiveExporter
import dev.martianzoo.tfm.engine.GameArchiveImporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class GameArchiveInferenceTest {
  private fun roundTrip(rule: String, body: String): String {
    val source =
        """
      Config {
          Green, Pink
          TharsisMap
      }
      GlobalEventDeck {
      }
      SetupRules {
          CLASS Green : Player {
              SetupPhase: $rule
          }
      }
      Green: Setup {
          $body
      }
    """
            .trimIndent()
    val original = GameArchiveImporter(source).also { it.run() }
    val text =
        GameArchiveExporter(
                original.agents.world.recording(),
                GameConfig("TharsisMap", "Green", "Pink"),
            )
            .export()
    val imported = GameArchiveImporter(text).also { it.run() }
    fun inventory(reader: GameReader) =
        reader.getComponents(reader.classTable.componentClass.baseType).entries.associate {
          it.key.toString() to it.value
        }
    fun pending(tasks: TaskQueue) =
        tasks
            .extract {
              "${it.assignee}|${it.selected}|${it.instruction}|${it.then}|${it.deferral}"
            }
            .sorted()
    assertEquals(inventory(original.agents.world.reader), inventory(imported.agents.world.reader))
    assertEquals(pending(original.agents.world.tasks), pending(imported.agents.world.tasks))
    return text.substring(text.indexOf("Green: Setup"))
  }

  @Test
  fun omitsAnEachThatTheFirstGiftUniquelyIdentifies() {
    val text =
        roundTrip(
            "EACH @Player { Plant<@Player> }",
            """
            EACH @Player { Plant<@Player> }
            Plant
            Plant<Pink>
            """
                .trimIndent(),
        )
    assertFalse("EACH" in text)
    assertTrue("Plant<Pink>" in text)
  }

  @Test
  fun omitsASelectionWhoseFirstStepBindsItsContinuation() {
    val text =
        roundTrip(
            "X Plant<Green> THEN 3X Heat<Green>",
            """
            selectTask(2 Plant THEN 6 Heat)
            2 Plant
            6 Heat
            """
                .trimIndent(),
        )
    assertFalse("selectTask" in text, text)
    assertTrue("2 Plant" in text)
    assertTrue("6 Heat" in text)
  }

  @Test
  fun retainsTheSelectionWhenTheSameFirstStepHasDifferentContinuations() {
    val text =
        roundTrip(
            "(Plant<Green> THEN Heat<Green>) OR (Plant<Green> THEN Energy<Green>)",
            """
            selectTask(Plant THEN Heat)
            Plant
            Heat
            """
                .trimIndent(),
        )
    assertTrue("selectTask" in text)
  }

  @Test
  fun aGroupedChoiceExportsOnlyTheWorkActuallyPerformed() {
    val text =
        roundTrip(
            "(Plant<Green>, Heat<Green>) OR Steel<Green>",
            """
            selectTask((Plant, Heat))
            Plant
            """
                .trimIndent(),
        )
    assertFalse("selectTask" in text, text)
    assertFalse("Heat" in text, text)
    assertTrue("Plant" in text)
  }

  @Test
  fun retainsASelectionThatDistinguishesAnExposedTaskFromAnEach() {
    val text =
        roundTrip(
            "Plant<Green>, EACH Player { Plant<Green> }",
            """
            selectTask(Plant)
            Plant
            """
                .trimIndent(),
        )
    assertTrue("selectTask(Plant)" in text, text)
  }

  @Test
  fun anExplicitSelectionCanChooseTheCompleteShorterAlternative() {
    val text =
        roundTrip(
            "Plant<Green> OR (Plant<Green> THEN Heat<Green>)",
            """
            selectTask(Plant)
            Plant
            """
                .trimIndent(),
        )
    assertTrue("selectTask(Plant)" in text, text)
  }

  @Test
  fun refusesToLoseAnUnfinishedSelection() {
    val failure =
        assertFailsWith<IllegalStateException> {
          roundTrip("Plant<Green> OR Heat<Green>", "selectTask(Plant)")
        }
    assertTrue("unfinished selection" in failure.message.orEmpty())
  }

  @Test
  fun exportsBetweenActionsButRejectsAnActionStillInProgress() {
    val source = requireNotNull(javaClass.getResource("/games/Game20230521Test.game")).readText()
    val prefix = source.substringBefore("Player2: playCard(ArcticAlgae)")
    val original = GameArchiveImporter(prefix).also { it.run() }.agents
    val player = original[original.world.actors.filterIsInstance<Player>().first()]
    val config =
        GameConfig(
            "VenusNextExpansion, PreludeExpansion, promos:cards, -WorldGovernmentRule",
            "Player1",
            "Player2",
        )
    fun export() = GameArchiveExporter(original.world.recording(), config).export()
    fun pending(tasks: TaskQueue) = tasks.extract { "${it.assignee}|${it.instruction}" }.sorted()

    assertFalse(original.world.tasks.isEmpty())
    val imported = GameArchiveImporter(export()).also { it.run() }.agents
    assertEquals(pending(original.world.tasks), pending(imported.world.tasks))

    player.doTask("UseAction<UseStandardProjectAction, Action1>")
    assertEquals(null, original.world.tasks.selectedTask())
    val failure = assertFailsWith<IllegalStateException> { export() }
    assertTrue("unfinished action" in failure.message.orEmpty())
  }

  @Test
  fun rejectsAnUnpaidResearchPurchaseOutsideAnAction() {
    val source = requireNotNull(javaClass.getResource("/games/Game20230521Test.game")).readText()
    val original =
        GameArchiveImporter(source.substringBefore("Player1: Research")).also { it.run() }.agents
    val player = original[original.world.actors.filterIsInstance<Player>().first()]
    val config =
        GameConfig(
            "VenusNextExpansion, PreludeExpansion, promos:cards, -WorldGovernmentRule",
            "Player1",
            "Player2",
        )
    player.doTasks("4 ProjectCard<Selecting>", "-0 ProjectCard<Selecting>")

    assertTrue(player.has("MAX 0 ActionScope"))
    assertEquals(null, original.world.tasks.selectedTask())
    val failure =
        assertFailsWith<IllegalStateException> {
          GameArchiveExporter(original.world.recording(), config).export()
        }
    assertTrue("unfinished payment" in failure.message.orEmpty())
  }

  @Test
  fun retainsAnEachWhenTheNextAmountCouldAlsoBeADirectTotal() {
    val text =
        roundTrip(
            "EACH @Player { 2 Plant<@Player> }, Plant<Green>, Plant<Green>",
            """
            EACH @Player { 2 Plant<@Player> }
            selectTask(2 Plant)
            2 Plant
            """
                .trimIndent(),
        )
    assertTrue("EACH" in text, text)
  }

  @Test
  fun retainsAnEachWhenDelayingItWouldChangeItsAmounts() {
    val text =
        roundTrip(
            "EACH @Player { Plant<@Player> / Heat<@Player> }",
            """
            exMachina {
                Heat
            }
            EACH @Player { Plant<@Player> / Heat<@Player> }
            exMachina {
                Heat
            }
            Plant
            """
                .trimIndent(),
        )
    assertTrue("EACH" in text)
  }
}
