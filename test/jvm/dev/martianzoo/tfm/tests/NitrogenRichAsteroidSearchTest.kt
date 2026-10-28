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
import kotlinx.coroutines.runBlocking

internal class NitrogenRichAsteroidSearchTest {
  @Test
  fun allTitaniumAmountsRemainCompletableInRecordedGame() = runBlocking {
    val source =
        checkNotNull(javaClass.getResourceAsStream("/replays/PlaygroundNraFreeze20261027.json.gz"))
    val document =
        GameFileJson.parse(GZIPInputStream(source).bufferedReader().use { it.readText() })
    val agents =
        GameFileImporter.replay(
            document,
            Canon,
            newAgents = { TfmEngine.agents(TfmEngine.newGame(it)) },
            startGame = { game ->
              game[ADMIN].beginOperation("SetupPhase FROM BootstrapPhase")
            },
        )
    val player = agents.world.actors.filterIsInstance<Player>().single()
    val task = agents.world.tasks.selectedTask()!!
    val form = agents[player].fillInTask(task)
    form.choose(form.decisions().single(), form.options(form.decisions().single()).first())
    val options = form.options(form.decisions().single()).toList()
    assertEquals((1..11).map(Int::toString), options)
    val search = DecisionSearch(agents, tracker = null, budget = 2_000)
    for (option in options) {
      val draft = agents[player].fillInTask(task, form.instruction)
      draft.choose(draft.decisions().single(), option)
      assertEquals(COMPLETES, search.verdictForm(task, draft.instruction), option)

      val afterPayment = agents.fork()
      afterPayment[ADMIN].autoExecPolicy = AutoExecPolicy.EAGER
      afterPayment[player].autoExecPolicy = AutoExecPolicy.SAFE
      val payment = afterPayment[player].fillInTask(task)
      payment.choose(
          payment.decisions().single(),
          payment.options(payment.decisions().single()).first(),
      )
      payment.choose(payment.decisions().single(), option)
      payment.commit()
      val remaining =
          afterPayment.world.tasks.extract { it }.filter { it.assignee == player && !it.deferred }
      val continuation = DecisionSearch(afterPayment, tracker = null, budget = 2_000)
      assertTrue(
          remaining.any { next ->
            continuation.verdict { it[player].selectTask(next.id) } == COMPLETES
          },
          option,
      )
    }
  }
}
