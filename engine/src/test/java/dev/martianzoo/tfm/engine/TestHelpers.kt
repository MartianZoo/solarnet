package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn

object TestHelpers {
  fun TerraformingMarsApi.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { gameplay.count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  fun Gameplay.assertCounts(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { count(it.second) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()

  fun TerraformingMarsApi.assertProds(vararg pairs: Pair<Int, String>) =
      assertThat(pairs.map { production(cn(it.second)) })
          .containsExactlyElementsIn(pairs.map { it.first })
          .inOrder()
}
