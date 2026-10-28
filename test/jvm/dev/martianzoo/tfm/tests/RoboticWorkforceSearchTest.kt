package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.GameFileImporter
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.DecisionSearch
import dev.martianzoo.tfm.engine.DecisionSearch.Verdict.COMPLETES
import dev.martianzoo.tfm.engine.TfmEngine
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking

internal class RoboticWorkforceSearchTest {
  @Test
  fun recordedProjectSearchCompletesWithoutCardTracking() = runBlocking {
    val source =
        checkNotNull(
            javaClass.getResourceAsStream(
                "/replays/PlaygroundRoboticWorkforceFreeze20261027.json.gz"
            )
        )
    val document =
        GameFileJson.parse(GZIPInputStream(source).bufferedReader().use { it.readText() })
    val agents =
        GameFileImporter.replay(
            document,
            Canon,
            newAgents = { TfmEngine.agents(TfmEngine.newGame(it)) },
            startGame = { it[ADMIN].beginOperation("SetupPhase FROM BootstrapPhase") },
        )
    val player = agents.world.actors.filterIsInstance<Player>().single()
    val task = agents.world.tasks.selectedTask()!!
    val search =
        DecisionSearch(agents, tracker = null, budget = 2_000, maxDuration = 500.milliseconds)
    val verdict = search.verdict { trial ->
      val form = trial[player].fillInTask(task)
      fun choose(prefix: String) {
        val decision = form.decisions().single()
        form.choose(decision, form.options(decision).first { prefix in it })
      }
      choose("StandardAction")
      choose("PlayProject")
      choose("RoboticWorkforce")
      choose("Hand")
      form.commit()
    }
    assertEquals(COMPLETES, verdict)
  }
}
