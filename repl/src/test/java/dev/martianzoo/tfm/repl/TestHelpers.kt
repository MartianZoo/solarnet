package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.engine.PlayerSession.OperationBody

object TestHelpers {
  fun OperationBody.assertCounts(vararg pairs: Pair<Int, String>) {
    assertThat(pairs.map { session.count(it.second) })
        .containsExactlyElementsIn(pairs.map { it.first })
        .inOrder()
  }

  fun PlayerSession.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  fun OperationBody.taskReasons() = tasks.map { it.whyPending }
}
