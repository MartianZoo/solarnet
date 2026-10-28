package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.AutoExecPolicy
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
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking

internal class SolarWindPowerSearchTest {
  @Test
  fun recordedTitaniumPaymentRemainsCompletable() = runBlocking {
    val source =
        checkNotNull(
            javaClass.getResourceAsStream("/replays/PlaygroundSolarWindPowerFreeze20261027.json.gz")
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
    val form = agents[player].fillInTask(task)
    val payTitanium = form.options(form.decisions().single()).first()
    form.choose(form.decisions().single(), payTitanium)
    val amounts = form.options(form.decisions().single()).toList()
    assertTrue(amounts.isNotEmpty())

    for (amount in amounts) {
      val draft = agents[player].fillInTask(task, form.instruction)
      draft.choose(draft.decisions().single(), amount)
      val search = DecisionSearch(agents, null, budget = 2_000, maxDuration = 500.milliseconds)
      assertEquals(COMPLETES, search.verdictForm(task, draft.instruction), amount)

      val afterPayment = agents.fork()
      afterPayment[ADMIN].autoExecPolicy = AutoExecPolicy.EAGER
      afterPayment[player].autoExecPolicy = AutoExecPolicy.SAFE
      val payment = afterPayment[player].fillInTask(task, draft.instruction)
      payment.commit()
      assertTrue(
          afterPayment.world.timeline.checkpoint().ordinal >
              agents.world.timeline.checkpoint().ordinal
      )
      val next =
          afterPayment.world.tasks.extract { it }.filter { it.assignee == player && !it.deferred }
      val continuation =
          DecisionSearch(afterPayment, null, budget = 2_000, maxDuration = 500.milliseconds)
      assertTrue(
          next.any { task ->
            continuation.verdict { it[player].selectTask(task.id) } == COMPLETES
          },
          amount,
      )
    }
  }
}
