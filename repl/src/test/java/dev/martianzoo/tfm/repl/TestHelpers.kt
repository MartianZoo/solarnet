package dev.martianzoo.tfm.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.engine.OldTfmHelpers.production
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn

object TestHelpers {
  fun PlayerSession.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  fun PlayerSession.assertProductions(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { production(cn(it.second)) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()
}
