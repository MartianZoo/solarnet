package dev.martianzoo.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CartesianProductTest {
  @Test
  fun zeroLists() {
    val sequence = listOf<List<Unit>>().cartesianProduct()
    assertThat(sequence.toList()).containsExactly(listOf<Unit>())
  }

  @Test
  fun severalDimensions() {
    val letters = listOf("a", "b", "c")
    val numbers = listOf(1, 2, 3, 4, 5)
    val colors = listOf("red", "blue")

    val product: List<List<Comparable<*>>> = listOf(
      letters,
      numbers,
      colors,
    )

    val sequence = product.cartesianProduct()

    assertThat(sequence.toList()).containsExactly(
      listOf("a", 1, "red"),
      listOf("a", 1, "blue"),
      listOf("a", 2, "red"),
      listOf("a", 2, "blue"),
      listOf("a", 3, "red"),
      listOf("a", 3, "blue"),
      listOf("a", 4, "red"),
      listOf("a", 4, "blue"),
      listOf("a", 5, "red"),
      listOf("a", 5, "blue"),
      listOf("b", 1, "red"),
      listOf("b", 1, "blue"),
      listOf("b", 2, "red"),
      listOf("b", 2, "blue"),
      listOf("b", 3, "red"),
      listOf("b", 3, "blue"),
      listOf("b", 4, "red"),
      listOf("b", 4, "blue"),
      listOf("b", 5, "red"),
      listOf("b", 5, "blue"),
      listOf("c", 1, "red"),
      listOf("c", 1, "blue"),
      listOf("c", 2, "red"),
      listOf("c", 2, "blue"),
      listOf("c", 3, "red"),
      listOf("c", 3, "blue"),
      listOf("c", 4, "red"),
      listOf("c", 4, "blue"),
      listOf("c", 5, "red"),
      listOf("c", 5, "blue"),
    ).inOrder()
  }
}
