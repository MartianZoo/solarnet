package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

class ActionTest {
  fun printEm() {
    val set = sortedSetOf<String>(Comparator.comparing { it.length })
    val gen = PetaformGenerator()

    for (i in 1..10000) {
      set += gen.makeRandomNode<Action>().toString()
    }
    set.forEach(::println)
  }

  @Test fun barrage() {
    val gen = PetaformGenerator()
    for (i in 1..1000) {
      Truth.assertThat(gen.testRandom<Action>()).isTrue()
    }
  }

  @Test
  fun stupid() {
    testRoundTrip("-> Ok")
  }

  private fun testRoundTrip(start: String, end: String = start) {
    val parse: Action = PetaformParser.parse(start)
    Truth.assertThat(parse.toString()).isEqualTo(end)
  }

}
