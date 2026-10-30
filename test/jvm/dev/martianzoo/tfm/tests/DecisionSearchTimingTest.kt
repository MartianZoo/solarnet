package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.AutoExecPolicy
import dev.martianzoo.tfm.engine.DecisionSearch
import dev.martianzoo.tfm.engine.DecisionSearch.Verdict.COMPLETES
import dev.martianzoo.tfm.engine.DecisionSearch.Verdict.UNKNOWN
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

internal class DecisionSearchTimingTest : TfmSandboxTest() {
  @Test
  fun thinkingBetweenChoicesDoesNotSpendSearchTime() {
    newTestGame()
    val task = kim.addTasks("MC").single()
    val search =
        DecisionSearch(
            agents,
            null,
            playerPolicy = AutoExecPolicy.NONE,
            maxDuration = 500.milliseconds,
        )
    Thread.sleep(600)
    assertEquals(COMPLETES, search.verdict { it[kim.actor].doTask(task) })
    Thread.sleep(600)
    assertEquals(COMPLETES, search.verdict { it[kim.actor].doTask(task) })
  }

  @Test
  fun verdictsShareTimeSpentSearching() {
    newTestGame()
    val task = kim.addTasks("MC").single()
    val search =
        DecisionSearch(
            agents,
            null,
            playerPolicy = AutoExecPolicy.NONE,
            maxDuration = 500.milliseconds,
        )
    assertEquals(COMPLETES, search.verdict { it[kim.actor].doTask(task) })
    assertEquals(
        UNKNOWN,
        search.verdict {
          Thread.sleep(600)
          it[kim.actor].doTask(task)
        },
    )
    assertEquals(UNKNOWN, search.verdict { it[kim.actor].doTask(task) })
  }
}
