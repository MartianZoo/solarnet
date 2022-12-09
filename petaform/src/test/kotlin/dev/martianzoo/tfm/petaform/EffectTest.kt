package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.PetaformParser.Effects
import dev.martianzoo.tfm.petaform.PetaformParser.Instructions
import dev.martianzoo.tfm.petaform.PetaformParser.Predicates
import dev.martianzoo.tfm.petaform.PetaformParser.parse
import dev.martianzoo.tfm.petaform.PrettyPrinter.pp
import org.junit.jupiter.api.Test

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
    for (i in 1..1000) {
      val node = gen.makeRandomNode<Effect>()
      val str = node.toString()
      val trip: Effect = parse(str)
      assertThat(trip.toString()).isEqualTo(str)
      if (trip != node) {
        println(pp(trip))
        println(pp(node))
        assertThat(trip).isEqualTo(node)
      }
    }
  }

  @Test fun debug() {
    val s = "Qux: (Foo, Bar) THEN Xyz"
    testRoundTrip(s)
  }

  @Test fun moreDebug() {
    val s = "NOW Foo IF Bar, Qux: 1 Bar FROM Qux"

    parse(Instructions.instruction, "1 Bar FROM Qux")
    parse(Predicates.predicate, "Foo")
    parse(Predicates.predicate, "Bar, Qux")
    parse(Effects.now, "NOW Foo")
    parse(Effects.condit, "NOW Foo IF Bar, Qux")
    parse(Effects.effect, "NOW Foo IF Bar, Qux: 1")
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
