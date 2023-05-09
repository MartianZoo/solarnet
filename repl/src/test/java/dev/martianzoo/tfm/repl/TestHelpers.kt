package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.engine.PlayerSession.Tasker

object TestHelpers {
  fun Tasker.assertCounts(vararg pairs: Pair<Int, String>) = session.assertCounts(*pairs)

  fun PlayerSession.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  fun Tasker.taskReasons() = tasks().map { it.whyPending }
}
