package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.PetaformParser.Instructions
import dev.martianzoo.tfm.petaform.PetaformParser.parse
import org.junit.jupiter.api.Test
import kotlin.math.pow

class InstructionTest {
  val inputs = """
    5
    1?
    Ooh
    -Bar
    5 Foo
    Bar: 5
    1 / Ooh
    Qux, Bar
    Bar / Qux
    -Foo / Qux
    -1 Eep<Bar>
    Ahh FROM Ahh
    Eep<Foo, Foo>
    5 Foo FROM Bar
    Bar<Foo> OR Ooh
    5 Foo<Qux, Ooh>!
    Bar / 42 Ahh<Bar>
    Abc<Bar>! FROM Foo
    Abc / Foo<Qux, Foo>
    Foo OR (Qux: -1 Xyz)
    5 Bar, Foo OR 1 / Foo
    Xyz, Xyz<Qux> FROM Qux
    -Foo, -5 Abc<Foo>, -Abc
    1 Abc<Qux> THEN Foo<Foo>
    5 Foo<Ahh<Foo>, Qux<Foo>>
    -Abc<Qux>, Qux, 1 Qux, Bar
    Bar THEN (1, Bar) THEN -Foo
    -1 / Xyz<Foo, Qux<Foo<Ooh>>>
    5 Bar<Ahh, Foo> FROM Foo<Bar>
    1 OR ((Qux<Foo> OR Foo): -Bar)
    Foo FROM Foo, Foo FROM Abc, Foo
    Abc FROM Foo OR -Foo<Abc> OR Foo
    (MAX 0 Qux, MAX 0): Bar<Foo, Qux>
    5 Bar<Bar<Foo>> THEN Foo<Foo, Qux>
    -Qux<Xyz<Qux<Foo>, Bar>>(HAS 1 Foo)
    Qux: (-Bar / Bar OR -Qux<Qux> / Qux)
    (Foo, Foo OR Foo) THEN 1 / Qux THEN 1
    Bar<Bar, Foo> OR (Bar FROM Qux, 1 Foo)
    (Foo, Foo<Foo>): (-1 Qux OR -Bar, -Foo)
    1 Foo FROM Bar / Bar, Foo FROM Foo / Abc
    Qux OR 1 OR (-Foo, Foo OR Foo, -Bar<Foo>)
    Ahh FROM Bar<Abc<Xyz<Qux<Abc<Qux>>>, Foo>>
    1 Foo, (Foo THEN Bar FROM Bar) OR -Xyz, Xyz
    Abc<Foo>, Foo FROM Qux<Qux<Qux>> OR Foo<Foo>
    Xyz<Abc, Foo<Foo>> FROM Abc, -1 Xyz<Bar, Bar>
    MAX 0: (Bar<Qux>, Foo FROM Bar<Foo<Foo>>, Foo)
    Foo<Qux> / 1 Xyz, Bar, 1 Abc, Foo<Bar> FROM Abc
    1 Foo THEN Foo / Ooh THEN Bar<Foo> / Foo THEN -5
    (-Foo OR (Foo: Foo), Foo) THEN (Bar, Xyz) THEN -1
    -Bar<Bar, Ooh>(HAS (MAX 0, Foo) OR MAX 0 Bar<Abc>)
    Foo / Abc THEN -Foo THEN Abc FROM Qux THEN Foo: Abc
    Foo FROM Ooh, Xyz THEN 1 Bar<Qux> THEN Abc<Foo, Foo>
    1 Bar<Qux, Foo> THEN Abc / 1 Abc THEN -Abc / Foo<Abc>
    Bar FROM Xyz / Qux<Foo>, 5 Foo, Foo FROM Bar<Abc>, Foo
    1 Bar FROM Foo OR -Foo<Foo<Abc>, Bar<Foo>> / 5 Bar<Foo>
    Foo / Foo<Qux<Foo, Ooh>> OR 1 OR Qux! FROM Foo<Bar<Ahh>>
    (MAX 0: -Foo) OR -1 OR (Foo FROM Foo<Xyz>, Foo<Qux<Foo>>)
    ((Foo<Foo> OR Foo): Foo<Bar<Foo>>, Bar OR Bar<Foo>) OR Ooh
    MAX 1 Xyz: (Foo<Foo> OR Bar OR Foo OR (Foo: Foo, Qux: Foo))
    (Bar<Foo<Foo>> OR Foo) THEN Foo<Qux<Xyz>, Foo> THEN Foo<Foo>
    Xyz OR (Qux<Foo>: Foo / Qux) OR Foo / Foo OR (-Foo<Bar>, Abc)
    Xyz<Ahh> THEN 1 Foo, 1 Qux FROM Abc<Bar> THEN Qux<Foo>, -1 Foo
    (Bar<Foo, Bar>, Qux<Foo<Foo>> / Bar) OR (Foo, Qux), Bar OR Foo.
    -1 Bar, Qux<Bar>, (Foo OR (Foo OR Foo, -Foo<Foo<Foo>>)) THEN Bar
    -42 THEN ((Bar: Foo / Foo) OR Foo) THEN Foo / 1 Foo THEN Qux<Foo>
    Foo<Bar> FROM Foo THEN Bar<Foo>, -Bar<Xyz> OR (Foo THEN Foo), -Qux
    (-Foo, -Foo, Bar FROM Foo, -Foo OR Bar FROM Foo, -Bar) OR Foo / Bar
    -Foo OR -Foo<Abc, Foo<Foo>> OR Abc OR Wau FROM Qux OR Qux<Qux> / Bar
    Bar<Qux> FROM Ooh<Foo>, MAX 0 Qux: (Bar, Bar<Qux> FROM Qux<Bar, Foo>)
    Foo, Foo, Foo, Foo OR Foo, Foo OR Foo, Foo THEN Foo, -Foo OR Bar, -Bar
    (1 Foo<Bar> OR MAX 0 Foo OR (Foo OR (Foo OR (Foo OR Bar)))): 1 Foo<Xyz>
    Ooh FROM Xyz OR -Abc / 1 Xyz<Bar<Foo>, Foo> OR -Foo<Foo> / Foo<Abc<Bar>>
    Xyz: ((-Foo<Foo<Foo<Qux>>>, -Foo) THEN (Foo<Foo<Bar>> / Foo OR Xyz<Qux>))
    Bar FROM Bar, Qux, 1 Foo<Foo> OR -Foo<Foo> OR (-Bar, MAX 0 Bar: Foo / Bar)
    -Foo / Foo, Foo / Foo, -Foo THEN (Bar, Bar: (Foo<Foo>, Bar), Bar<Foo>, Foo)
    5 THEN (Qux!, Foo, Foo<Foo<Bar>>, Foo, Foo<Bar> / Bar) THEN -Bar<Abc>! / Foo
    (Abc<Bar>, Foo<Bar> / 1 Qux<Foo>, -Foo OR (Foo, Foo, Foo) OR -Foo<Foo>) OR 5!
    -Foo, Bar<Foo>, Foo<Qux<Qux>> OR -1 Foo<Foo<Foo>> OR (Foo, Foo<Foo>, Bar<Qux>)
    (Foo, MAX 0): (1 Foo: (Foo, Foo OR Foo / Foo OR Foo<Foo> / Foo) THEN -Qux<Bar>)
    Bar, -Foo<Foo<Bar<Foo>>> OR (-Bar OR Foo, Foo OR ((Foo OR (Foo, Foo)) THEN Foo))
    Bar, (-Foo, Foo) THEN (-Foo OR Foo<Bar>) THEN (-Bar<Foo, Qux> OR (Foo, Foo)), Bar
    -Bar / Foo, Abc<Foo>, Xyz, (Bar, Foo, Foo OR Bar FROM Foo OR Foo<Foo, Foo>) OR Qux
    Qux: (((Foo, Foo) OR -Foo<Foo> OR -Foo, Qux<Abc> FROM Foo, Bar) OR (Foo: Bar<Bar>))
    1, Foo<Xyz<Bar<Bar<Qux>>>> FROM Qux THEN (Foo, (Foo, -Foo) OR Foo) THEN Foo THEN Bar
    (-Foo<Foo<Bar>, Bar> OR 1 Foo<Qux<Foo<Qux<Foo>>>>) THEN (5 Foo FROM Foo OR (Bar, -1))
    -Ahh<Qux<Foo<Foo>>> OR (((Foo OR (Foo<Foo>, MAX 0)) OR Qux<Bar>): (Foo FROM Bar, Bar))
    Qux OR (-1 Foo<Foo>, Foo) OR (Bar<Foo>, Foo THEN (Foo, Foo / Foo<Foo>)) OR Foo FROM Foo
    Foo OR Qux OR (Bar, Bar) OR (5 THEN ((Bar, Foo, Foo) OR ((Foo OR Foo) THEN (Foo, Foo))))
    1 THEN (-Qux OR (Foo, Foo) OR (Bar, Foo)) THEN Ooh THEN (Xyz<Abc<Foo<Foo>>> OR (Foo, -1))
    Foo, 1 Foo<Bar>, Foo<Bar> OR Foo / Qux, Foo / Qux THEN (Foo / Bar, Foo OR Foo / Foo, -Bar)
    (Bar THEN (-Foo THEN Foo, Foo) THEN -Qux) OR -1 Bar<Bar> OR -5 Foo<Bar<Bar, Bar<Foo<Foo>>>>
    (((-Qux, Foo) THEN -Foo) OR Qux<Abc>) THEN Bar<Bar> THEN (Foo / Foo<Foo>, (Foo, MAX 0): Foo)
    Foo, Bar, Bar<Bar> FROM Xyz, (Bar, Foo / Foo<Foo>) THEN (MAX 0: (Foo THEN Foo FROM Foo), Qux)
    (Foo<Foo> / Foo OR (Bar, Foo<Foo> OR (Foo, Foo)) OR Foo / Bar OR Foo) THEN -Bar<Bar<Foo>>, Qux
    Bar, Foo, Foo, Foo / Foo, -Foo, Foo, (Bar OR (1, Foo, Bar)) THEN (-Foo, Foo), -5 Bar, -Foo<Qux>
    Qux, Bar<Bar>: Bar, Foo THEN Foo, (Foo OR Foo<Foo>) THEN Foo, Bar<Foo>! OR Foo OR Bar<Bar> / Foo
  """.trimIndent().split('\n')

  @Test fun testSampleStrings() {
    inputs.forEach { testRoundTrip(it) }
  }

  @Test fun stressTest() {
    val gen = generator(20)
    assertThat((1..1000).flatMap {
      val node = gen.makeRandomNode<Instruction>()
      val str = node.toString()
      val trip: Instruction = parse(str)
      if (trip == node && trip.toString() == str)
        listOf()
      else
        listOf(str)
    }).isEmpty()
  }

  @Test fun thens() {
    val s = "MAX 1 Foo: ((Abc / Foo OR Qux) THEN (-Foo, Bar) THEN -Foo)"
    parse(Instructions.perable, "Abc")
    parse(Instructions.maybePer, "Abc / Foo")
    parse(Instructions.orInstr, "Abc / Foo OR Qux")
    parse(Instructions.anyGroup, "(Abc / Foo OR Qux)")
    parse(Instructions.then, "(Abc / Foo OR Qux) THEN (-Foo, Bar) THEN -Foo")
  }

  @Test fun debug2() {
    val s = "Qux: (Foo: -Foo) OR -Bar / 5 Foo<Foo<Foo, Bar>, Qux<Foo>>"
    parse(PetaformParser.typeExpression, "Foo<Foo<Foo, Bar>, Qux<Foo>>")
    parse(PetaformParser.qe, "5 Foo<Foo<Foo, Bar>, Qux<Foo>>")
    parse(Instructions.maybePer, "-Bar / 5 Foo<Foo<Foo, Bar>, Qux<Foo>>")
    parse(Instructions.maybePer, "-Bar / 5 Foo<Foo<Foo, Bar>, Qux<Foo>>")
  }
  private fun testRoundTrip(start: String, end: String = start) =
      assertThat(parse<Instruction>(start).toString()).isEqualTo(end)

  private fun checkRoundTrip(start: String, end: String = start) =
      parse<Instruction>(start).toString() == end

  fun generator(avInsLen: Int): PetaformGenerator {
    val factor: Double = 0.737 + 0.0325 * avInsLen - .000169 * avInsLen * avInsLen
    return PetaformGenerator { factor / 1.2.pow(it) - 1.0 }
  }

  fun generateTestStrings() {
    val set = sortedSetOf<String>(Comparator.comparing { it.length })
    val gen = generator(15)
    for (i in 1..10000) {
      set += gen.makeRandomNode<Instruction>().toString()
    }
    set.forEach(::println)
  }
}
