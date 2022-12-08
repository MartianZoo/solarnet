package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
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
      val node = gen.makeRandomNode<Action>()
      val str = node.toString()
      val trip = PetaformParser.parse<Action>(str)
      assertThat(trip.toString()).isEqualTo(str)
      assertThat(trip).isEqualTo(node)
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
