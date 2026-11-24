package dev.martianzoo.tfm.tests.archive

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.engine.GameArchiveImporter
import dev.martianzoo.tfm.engine.TfmRoutines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class GameArchiveAssertionTest {
  private val source
    get() = requireNotNull(javaClass.getResource("/games/ActiveProtonFlowTest.game")).readText()

  // A truncated scenario cannot retain the complete game's card-movement ledger.
  private val untrackedSource
    get() = source.replace(Regex("""(?ms)^Cards \{\n.*?^}\n"""), "")

  private val beforeResearchChecks
    get() = untrackedSource.substringBefore("// Database save 28:")

  @Test
  fun importsAScenarioWhoseStateChecksAreWrittenInTheFile() {
    GameArchiveImporter(source).run()
  }

  @Test
  fun assertionsPreserveProductionTasksAndHistoryEvenWithAutomaticExecutionConfigured() {
    val importer = GameArchiveImporter(untrackedSource.substringBefore("KB: Production"))
    importer.run()
    val agents = Agents(importer.agents.world, TfmRoutines.registered) { EAGER }
    val player = agents[agents.world.actors.filterIsInstance<Player>().first()]
    val before = snapshot(agents)
    assertTrue(!agents.world.tasks.isEmpty())

    player.doRoutine("assert", "=0 OxygenStep")
    assertEquals(before, snapshot(agents))

    val failure =
        assertFailsWith<RequirementException> {
          player.doRoutine("assert", "=1 OxygenStep")
        }
    assertTrue("OxygenStep = 0" in failure.message.orEmpty())
    assertEquals(before, snapshot(agents))
  }

  @Test
  fun anotherPlayersAssertionInsideAnActionDoesNotReleaseTheirReward() {
    val withChecks =
        source.replace(
            "    ER: 2 Plant",
            """
            assert(=21 TerraformRating)
            ER: assert(=1 Plant)
            ER: 2 Plant
            ER: assert(=3 Plant)
            """
                .trimIndent()
                .prependIndent("    "),
        )
    GameArchiveImporter(withChecks).run()
  }

  @Test
  fun groupedAlternativesUseTheExistingRequirementMeaning() {
    GameArchiveImporter(
            beforeResearchChecks + "KB: assert((=18 MC, =1 Steel) OR (=19 MC, =2 Steel))"
        )
        .run()

    val source = beforeResearchChecks + "KB: assert((=18 MC, =2 Steel) OR (=19 MC, =1 Steel))"
    val failure =
        assertFailsWith<IllegalStateException> {
          GameArchiveImporter(source).run()
        }
    assertTrue(failure.cause is RequirementException)
    assertTrue("MC<KB> = 18" in failure.message.orEmpty())
    assertTrue("Steel<KB> = 1" in failure.message.orEmpty())
  }

  @Test
  fun failedCheckReportsItsLineAndPlayerAndStopsBeforeLaterEntries() {
    val assertion = "KB: assert(=19 MC)"
    val text =
        beforeResearchChecks +
            """
        $assertion
        KB: exMachina(99 Plant)
        """
                .trimIndent()
    val importer = GameArchiveImporter(text)
    var before = snapshot(importer.agents)

    val failure =
        assertFailsWith<IllegalStateException> {
          importer.run { before = snapshot(importer.agents) }
        }

    val line = text.lineSequence().indexOf(assertion) + 1
    assertTrue("Archive line $line: $assertion" in failure.message.orEmpty())
    assertTrue("Assertion failed for KB: =19 MC" in failure.message.orEmpty())
    assertTrue("MC<KB> = 18" in failure.message.orEmpty())
    assertEquals(before, snapshot(importer.agents))
  }

  @Test
  fun malformedRequirementsFailAtTheirSourceLine() {
    val assertion = "KB: assert(=18 MC,)"
    val text = beforeResearchChecks + assertion
    val failure =
        assertFailsWith<IllegalStateException> {
          GameArchiveImporter(text).run()
        }
    val line = text.lineSequence().indexOf(assertion) + 1
    assertTrue("Archive line $line: $assertion" in failure.message.orEmpty())
  }

  private fun snapshot(agents: Agents) =
      Triple(
          agents.world.reader
              .getComponents(agents.world.classTable.componentClass.baseType)
              .entries
              .associate { it.key to it.value },
          agents.world.tasks.extract { it },
          agents.world.events.entriesSince(0),
      )
}
