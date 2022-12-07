package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import org.junit.jupiter.api.Test
import javax.swing.text.html.HTML.Tag.P
import kotlin.math.pow

class EffectTest {
  // TODO
  val inputs = """
    Foo: Bar
  """.trimIndent().split('\n')

  @Test fun testSampleStrings() {
    assertThat(inputs.filterNot { checkRoundTrip(it) }).isEmpty()
  }

  @Test fun stressTest() {
    val gen = PetaformGenerator()
    assertThat((1..10000).flatMap {
      val node = gen.makeRandomNode<Effect>()
      val str = node.toString()
      val trip: Effect = parse(str)
      if (trip == node && trip.toString() == str)
        listOf()
      else
        listOf(str)
    }).isEmpty()
  }

  private fun testRoundTrip(start: String, end: String = start) =
      assertThat(parse<Effect>(start).toString()).isEqualTo(end)

  private fun checkRoundTrip(start: String, end: String = start) =
      parse<Effect>(start).toString() == end

  fun generateTestStrings() {
    val set = sortedSetOf<String>(Comparator.comparing { it.length })
    val gen = PetaformGenerator()
    for (i in 1..10000) {
      set += gen.makeRandomNode<Instruction>().toString()
    }
    set.forEach(::println)
  }
}
