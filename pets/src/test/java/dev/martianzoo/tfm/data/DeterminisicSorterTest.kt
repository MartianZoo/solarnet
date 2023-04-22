package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import okio.ByteString.Companion.encodeUtf8
import org.junit.jupiter.api.Test

private class DeterminisicSorterTest {
  @Test
  fun happyPath() {
    val a = Canon.card(cn("Birds"))
    val b = Canon.card(cn("Fish"))
    val c = Canon.card(cn("Pets"))

    val cards = listOf(a, b, c)
    assertThat(DeterministicSorter("seed-1").sort(cards)).isEqualTo(listOf(a, b, c))
    assertThat(DeterministicSorter("seed-2").sort(cards)).isEqualTo(listOf(c, a, b))
    assertThat(DeterministicSorter("seed-3").sort(cards)).isEqualTo(listOf(b, c, a))
  }

  class DeterministicSorter(
    private val seed: String,
  ) {
    fun sort(cards: List<CardDefinition>): List<CardDefinition> {
      return cards.sortedBy {
        "${seed}\n${it.id}".encodeUtf8().sha256()
      }
    }
  }
}
