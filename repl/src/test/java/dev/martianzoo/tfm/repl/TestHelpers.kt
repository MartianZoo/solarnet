package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.engine.PlayerSession.Tasker

object TestHelpers {
  fun Tasker.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { session.count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()
}
