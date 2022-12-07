package dev.martianzoo.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CollectionHelpersTest {
  @Test
  fun weird() {
    assertThat(toListWeirdly(multiset(0 to "x"))).containsExactly().inOrder()
    assertThat(toListWeirdly(multiset(1 to "x"))).containsExactly("x").inOrder()
    assertThat(toListWeirdly(multiset(3 to "x"))).containsExactly("x", "x", "x").inOrder()
    assertThat(toListWeirdly(multiset(1 to "x", 1 to "y"))).containsExactly("x", "y").inOrder()
    assertThat(toListWeirdly(multiset(2 to "x", 1 to "y"))).containsExactly("x", "x", "y").inOrder()
    assertThat(toListWeirdly(multiset(1 to "x", 2 to "y"))).containsExactly("y", "x", "y").inOrder()
    assertThat(toListWeirdly(multiset(2 to "x", 2 to "y"))).containsExactly("x", "y", "x", "y").inOrder()
    assertThat(toListWeirdly(multiset(6 to "x", 10 to "y"))).containsExactly(
        "y", "x", "y", "y", "x", "y", "x", "y", "y", "x", "y", "y", "x", "y", "x", "y").inOrder()

    assertThat(toListWeirdly(multiset(6 to "x", 10 to "y", 15 to "z"))).containsExactly(
        "z", // 07
        "y", // 10
        "z", // 13
        "x", // 17
        "y", // 20
        "z", // 20
        "z", // 27
        "y", // 30
        "x", // 33
        "z", // 33
        "y", // 40
        "z", // 40
        "z", // 47
        "x", // 50
        "y", // 50
        "z", // 53
        "y", // 60
        "z", // 60
        "x", // 67
        "z", // 67
        "y", // 70
        "z", // 73
        "y", // 80
        "z", // 80
        "x", // 83
        "z", // 87
        "y", // 90
        "z", // 93
        "x", // 100
        "y", // 100
        "z", // 100
    )
  }
}
