package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.parser.PetaformParser.Instructions
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse
import org.junit.jupiter.api.Test
import kotlin.math.pow

class InstructionTest {
  val inputs = """
    Qux
    Ooh!
    -Bar!
    5 Ooh!
    -11 Foo
    Foo<Qux>
    Xyz / Xyz
    -Xyz<Foo>.
    -3 Foo<Abc>
    11 Qux<Foo>.
    3 Xyz OR -Qux
    1 Foo FROM Ahh
    5 Foo: Qux<Eep>
    11 Ooh<Qux>: Xyz
    -3 Qux / Qux<Eep>
    Bar<Qux, Foo>: Xyz
    1 Qux FROM Bar<Xyz>
    -3 Foo<Foo> / 11 Ahh
    11 Foo<Abc, Qux, Ooh>
    -Qux OR -3 Qux / 5 Foo
    Qux<Qux, Foo>, Ahh<Bar>
    Abc: 1 Abc<Abc> FROM Bar
    Qux(HAS 3 Foo<Foo>) / Qux
    5 Xyz / Xyz<Qux, Foo<Ahh>>
    -Xyz OR -Bar<Bar>(HAS Qux)!
    3 Xyz: (3 Foo THEN Ahh<Qux>)
    -Foo<Abc<Xyz>> OR -5 Foo<Bar>
    -3 Qux OR -3 Ooh OR -Abc / Xyz
    3 Qux<Bar> / 3 Xyz, 3 Xyz, -Abc
    (1 Qux FROM Abc, Foo.) OR -5 Bar
    -Foo<Xyz>, 3 Foo<Qux>, -Ooh / Qux
    Xyz: -Abc THEN 5 Qux: (3 Bar, Foo)
    11 Ahh FROM Foo<Bar<Abc<Foo>, Xyz>>
    1 Qux(HAS Foo OR Bar<Bar>). FROM Foo
    3 Foo, 3 Foo, 3 Bar / Bar, -Xyz / Bar
    Foo<Ahh>: (1 Foo FROM Bar<Qux> OR Bar)
    Foo OR -Bar OR Foo OR Foo OR Qux, 5 Abc
    1 Ooh FROM Ooh<Xyz>, -Qux?, Foo THEN Foo
    Bar OR Foo<Bar> OR Qux OR 11 Qux FROM Bar
    Foo, -Bar / Qux, Bar OR Qux<Foo>, Foo, Bar
    1 Ooh FROM Abc<Xyz<Foo<Abc, Qux>>> THEN Qux
    Ahh<Qux> OR (1 Foo FROM Foo, 1 Foo FROM Bar)
    (Abc THEN 3 Qux) OR 1 Abc FROM Qux / Abc<Foo>
    Bar: Foo, (-3 Qux, 1 Xyz FROM Foo) THEN -3 Foo
    Bar: 5 Foo<Foo<Qux>>, Ooh!, MAX 1 Abc: Bar<Foo>
    5 Qux<Bar> OR (-Foo, Bar, -Foo, Qux, -Foo / Bar)
    Foo. / Eep OR Bar / Foo OR 5 Qux<Bar, Foo> OR Xyz
    Bar, (Qux<Qux> OR MAX 1 Bar): Foo / Foo, Foo, -Bar
    Ooh, 5 Foo, -Qux, Ooh, Foo<Foo<Foo<Foo>, Bar>>, Bar
    Qux OR ((Bar OR ((Foo, Foo), Foo)): -Foo<Foo> / Bar)
    -Foo, Foo / Qux<Bar<Foo>>, -3 Foo<Bar<Foo>, Bar<Foo>>
    Foo / Qux, (MAX 1 Bar, Foo): 1 Xyz<Foo> FROM Foo / Qux
    3 Foo FROM Abc, Xyz<Qux>, Abc, Bar<Foo>: Bar / Abc<Abc>
    Foo, 3 Qux FROM Qux, Bar THEN -Foo, -Qux / Foo<Bar<Abc>>
    5 Bar<Bar<Foo>>, 1 Foo FROM Abc, 1 Foo FROM Qux<Qux, Abc>
    Bar: -Abc, (Foo, MAX 1 Bar): (Foo, -Foo, Foo), -3 Foo<Foo>
    1 Ahh FROM Abc<Abc> OR 3 Bar<Xyz<Foo<Abc>, Bar<Abc<Foo>>>>!
    MAX 1 Foo: ((Abc / Foo OR Qux) THEN (-Foo, Bar) THEN -Foo)
    Foo<Foo<Qux>>, -Qux!, Bar<Bar<Foo>>, 1 Ahh<Eep> FROM Ahh<Abc>
    (-Foo THEN (Foo, Foo)) OR (1 Qux FROM Bar THEN Qux / Qux<Foo>)
    3 Bar., (Foo<Abc>, Qux OR 3 Foo) THEN -Foo<Foo, Foo<Bar>> / Foo
    (Foo OR Foo / Bar OR Foo / 3 Foo) THEN (Qux THEN -Foo, Xyz, Abc)
    1 Abc<Bar, Abc>(HAS =1 Bar) FROM Qux<Foo<Abc<Qux<Foo>>>>(HAS Bar)
    Foo<Qux, Xyz<Qux>>, Ooh<Bar>, (Foo<Foo<Bar>>, Foo / Foo) THEN -Foo
    Foo OR -3 Xyz! OR Foo OR Bar OR -3 Foo OR (Foo / 3 Foo, -Bar / Bar)
    (-Bar, -Foo / Bar, -Bar THEN 3 Foo<Qux> FROM Bar<Foo<Qux>>) THEN Qux
    5 Foo, -Xyz, Qux<Foo>, (Foo<Foo> / Qux, Foo) THEN -Bar<Foo>, Foo, Qux
    -3 Qux THEN (5 Bar OR (((Foo OR Foo) OR (Foo OR Bar<Foo>)): Foo, Bar))
    Bar OR (=1 Qux: Abc) OR ((Bar OR (Foo OR MAX 1 Foo)): Foo, -3 Foo<Foo>)
    (Bar OR Foo OR (5 Bar, Bar)) THEN 3 Foo: ((Foo OR -Foo OR Foo) THEN Bar)
    5 Foo(HAS Foo<Bar>). FROM Foo THEN (Foo OR -3 Foo! OR (MAX 1 Bar: 3 Foo))
    (Foo, Xyz, (MAX 1 Qux<Foo>, Foo<Bar>): Qux) OR (3 Bar: (-Foo / Bar, -Foo))
    Foo OR Bar / Foo, (Qux, Foo, Foo<Foo>) OR 3 Abc, Bar / Xyz THEN -3 Foo, Qux
    (Foo<Bar<Bar>> THEN Foo) OR (Qux<Foo>, 3 Bar, 3 Foo, Foo / Qux) OR (Bar, Bar)
    MAX 1 Bar: (Bar / Qux THEN Qux THEN 3 Bar<Bar> THEN (-Foo OR Foo / 3 Foo))
    (Foo, Abc / Foo) OR (Foo<Foo, Abc>: Qux) OR (MAX 1 Foo: (Foo OR Abc<Qux<Foo>>))
    3 Bar! OR 5 Qux, 1 Qux<Foo> FROM Eep OR (Foo, (Foo, Foo): Foo, -Foo) OR Bar<Bar>
    (((Qux OR MAX 1 Foo<Bar>) OR Bar<Foo>) OR (Bar, MAX 1 Foo)): (-Bar, Bar, Foo, Foo)
    Bar, -5 Foo<Foo<Foo, Bar>> THEN (Bar OR (MAX 1 Qux OR Bar)): Bar, Abc / 3 Foo, -Bar
    Ahh: (1 Xyz<Foo<Foo<Bar>>> FROM Bar OR (Foo<Foo> OR 1 Bar FROM Foo, 1 Bar FROM Foo))
    Foo, Foo / Qux<Bar>, -Foo / Foo<Foo, Abc>, MAX 1 Foo: (Foo OR (Foo OR Foo, Foo)), Foo
    ((Foo, Foo<Foo>): Bar / Qux) OR Bar / Foo OR (Foo / Bar THEN (Qux OR (Foo, Bar, Foo)))
    Foo OR Bar<Foo<Foo>> OR 1 Qux<Bar> FROM Foo, Foo THEN (Foo, MAX 1 Foo<Qux>: -Foo, -Foo)
    ((Foo OR Bar, 3 Foo): Bar) OR (Abc OR Foo<Foo> OR Foo OR (Foo THEN Bar), 1 Foo FROM Abc)
    (Foo<Bar> OR Foo<Foo>) THEN Foo, -Bar<Bar<Qux>> THEN ((MAX 1 Foo: Foo) OR Foo, Foo: -Foo)
    (5 Qux<Bar> THEN ((1 Foo FROM Foo THEN Foo) OR Qux)) OR 3 Foo OR 11 Bar FROM Abc<Xyz, Ahh>
    Foo / Foo, Foo, 3 Foo OR Bar, Bar: -Foo / Foo, -Bar, -Qux<Qux<Qux>>, Ahh, Qux OR -Foo / Foo
    Foo<Foo>, -Bar OR 3 Bar<Bar>, (MAX 1 Foo OR ((MAX 1 Foo OR (Foo, Foo)) OR Qux)): -5 Foo<Bar>
    Foo: Qux THEN (MAX 1 Foo: (-Foo OR -Foo), -Bar<Bar<Foo>>, 1 Bar FROM Xyz<Foo<Bar<Foo<Foo>>>>)
    5 Ooh FROM Abc THEN (Foo OR Bar, Foo<Ooh<Foo<Qux>, Foo<Bar>>>): (Bar OR (Foo / Foo, Foo<Foo>))
    (1 Qux<Bar> FROM Qux, -Bar, MAX 1 Foo: -Qux, 3 Foo FROM Foo<Foo>, 1 Abc FROM Foo) THEN Bar<Bar>
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
    parse(PetaformParser.expression, "Foo<Foo<Foo, Bar>, Qux<Foo>>")
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
    println("$avInsLen -> $factor")
    return PetaformGenerator { factor / 1.2.pow(it) - 1.0 }
  }

  fun generateTestStrings() {
    val set = sortedSetOf<String>(Comparator.comparing { it.length })
    val gen = generator(20)
    for (i in 1..10000) {
      set += gen.makeRandomNode<Instruction>().toString()
    }
    set.forEach(::println)
  }
}
