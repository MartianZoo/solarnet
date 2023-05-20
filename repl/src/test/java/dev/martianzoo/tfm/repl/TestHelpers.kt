package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.engine.PlayerSession

object TestHelpers {
  fun PlayerSession.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()
}
