package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth
import dev.martianzoo.tfm.pets.PetsParser.Requirements
import dev.martianzoo.tfm.pets.PetsParser.parse
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
class ActionTest {
  @Test
  fun stupid() {
    testRoundTrip<Action>("-> Ok")
  }

  val inputs = """
    -> 1
    -> 1?
    -> -1!
    -> -Ooh
    -> 5 Wau
    -> 1 Foo?
    Foo -> Ahh
    -> -11 Bar?
    Eep -> -Xyz?
    -> -Xyz<Ahh>?
    Eep -> -5 Xyz!
    Wau -> Foo<Ahh>
    Xyz -> Abc / Qux
    Eep -> -1, -1 Ooh
    5 Eep<Eep> -> Eep!
    Ooh OR Foo -> 1 Foo
    1 -> 11 Ooh FROM Ahh
    11 -> -1 Eep(HAS Foo)
    Wau -> 1 Xyz<Ooh, Qux>
    5 Abc -> -Qux<Ooh<Qux>>
    5 OR 5 Bar / 5 Foo -> 11
    5 / 11 Wau -> -Qux! / Qux
    PROD[1 Bar] -> -Abc OR Ooh
    11 Ooh -> -11, Foo, -11 Ooh
    1 Eep -> -11 Ahh, MAX 5: Bar
    Ooh / Qux -> 1 / Xyz OR 5 Wau
    1 Xyz / Foo<Qux> -> Qux? / Ooh
    5 -> Foo!, 1 Foo<Foo>, Bar<Foo>
    PROD[Abc / 5 Bar] -> 11 Bar, Qux
    1 -> -5 / Foo<Foo, Ahh<Foo>> OR 5
    Ahh<Ooh<Qux>> -> -1 Xyz<Qux<Bar>>?
    11 Wau<Foo<Eep<Ooh, Foo>>> -> 5 Xyz
    PROD[Bar], 5 Ooh -> 11 Qux<Wau<Bar>>
    11 Abc<Qux> -> Foo<Ahh<Foo>> OR 1 Qux
    -> (-Ahh OR 1., Abc) OR -Ahh<Ooh, Foo>
    5 / 1 Eep OR (11 Foo, 1 / Bar) -> 1 Ooh
    5, Bar / 1 Xyz<Bar>, PROD[Ooh] -> 11 Ahh
    Qux<Foo, Foo, Bar>, 1 / Bar, Abc -> -Xyz?
    PROD[1 Bar<Foo>, Bar] -> 1, Abc / Ahh<Bar>
    Wau -> Bar<Ahh> THEN (=1, (1, Foo)): -5 Xyz
    Xyz<Abc, Qux>, Xyz, Bar, Xyz / Ahh -> -1 Wau
    PROD[Ooh<Bar>] -> (Ahh, MAX 0: 1) OR Bar OR 1
    1 Ooh OR 1 Foo<Foo<Bar>> -> Foo<Abc, Ahh<Bar>>
    PROD[5 Qux OR (Bar, 1)] -> 11 OR (Foo FROM Xyz)
    PROD[Bar / Qux<Qux>, 1 Foo] -> -1 Foo<Ahh<Abc>>?
    PROD[1 Foo] / 5 Xyz -> 5 Eep<Foo<Abc>, Qux<Foo>>!
    Xyz -> 11 Bar FROM Qux, -Bar, 5?, -Bar! OR Bar, 11
    1 Ooh, (1 / Foo, 1 Ooh) OR (1, Foo<Foo>) -> -11 Foo
    5 Bar, Qux, Bar OR 1 Abc OR Qux -> -11 Ooh OR 5 Eep.
    PROD[1 Eep<Abc>] -> =0: ((-5 OR Qux) THEN 1 Foo<Foo>)
    PROD[Abc] -> 1 Ooh OR ((1 OR -Abc<Qux<Abc>>) THEN Qux)
    PROD[Foo] OR (Qux / Bar, 11 Bar, Ahh) -> 5, 11 Foo<Abc>
    PROD[Qux OR 11 Foo] -> Qux, 5 Qux THEN (=0: 5 Foo, -Qux)
    -> -1., MAX 5 Qux: (Qux: Foo THEN (Foo, Qux), 1 Bar<Foo>)
    -> Ooh<Ooh> FROM Qux / 5 Bar OR ((1 Foo OR Foo OR 5): Foo)
    Ahh, Qux OR PROD[5] -> Ooh, Ahh, 1 Ooh, Qux OR -Ooh<Abc>, 5
    11, Foo / 1 Bar OR Bar OR Foo / Qux OR 1 / Abc -> 5 Ahh<Bar>
    1 Ooh<Bar>, Foo, Foo / 1 Bar -> =0 Foo<Bar<Foo>>: (-Xyz, Xyz)
    1 Abc<Abc> OR Abc / Qux OR Foo<Bar> -> 1 Abc(HAS Xyz) / 11 Wau
    -> (Bar THEN Qux<Foo<Bar>> THEN 1 Bar FROM Foo THEN Xyz) OR Bar
    PROD[Bar / 1 Foo] OR Bar -> Ooh, (Bar: Abc) OR (1 Qux THEN -Bar)
    PROD[(Ahh, 5 OR Qux / Foo) OR (Ooh OR 5 Foo, Xyz / Ooh)] -> 1 Abc
    1 Foo -> Qux<Foo>, Qux: -Qux, Qux, -Xyz<Xyz<Qux, Bar<Xyz>>>, 1 Qux
    PROD[Foo / Qux OR 5 Ooh OR Qux OR 5 OR 5 Ooh<Ooh>] -> -Xyz(HAS Ooh)
    11 Xyz<Xyz<Bar, Abc>> -> 5 Bar<Xyz> / 5 Bar OR -1 Bar<Ooh> OR 11 Bar
    11 -> -Abc / Abc, Bar. OR -Bar<Abc>, Xyz FROM Bar / Xyz, -Qux / 5 Xyz
    -> -11 Foo<Qux<Bar>> / Ooh<Eep, Foo<Eep<Ooh>, Bar<Bar, Foo>>>(HAS Foo)
    Ooh / 1 Ooh -> (5, Foo): Bar, Foo FROM Xyz / Foo, Qux., 5 Abc THEN -Qux
    PROD[Abc<Eep>] -> -1 OR -5 Bar OR -Abc, Ahh OR Foo OR ((Abc, Foo): -Xyz)
    11 Ooh<Bar> -> Abc, 1, Xyz<Qux FROM Foo>, Qux / Qux, 1 Foo, -5 Bar, 1 Eep
    Ooh OR Xyz OR (5 OR Abc OR Bar, 1, 1 Foo, 5 Qux) OR PROD[Ahh] -> 1 / 1 Foo
    11 Qux<Bar, Foo<Abc>, Qux> -> Eep / Qux OR 11 Foo<Foo> / Eep<Ooh<Foo<Ahh>>>
    PROD[1 / 11 Ooh] -> (MAX 5 Qux, Abc): 1, Wau, Xyz: (Bar OR -Bar OR 5 Foo), 5
    PROD[Foo] -> 5 Foo FROM Bar, 1 OR Foo, 5 Abc<Bar>!, 1 Ooh, 5 Abc / 1 Foo<Ooh>
    PROD[Bar<Bar>] / Ooh OR 1 Xyz -> Xyz<Foo>, 1 Ooh, -Qux / 1 Xyz, -1, Foo THEN 1
    Qux<Bar, Qux> / 5 Xyz OR (11, Qux) -> Bar<Qux> / Xyz THEN 1 Bar, -Xyz<Foo, Qux>
    Foo<Ahh, Qux<Eep, Abc<Bar>>> -> 1 Bar, Bar<Foo, Foo>, Bar, -Foo, 1 Abc, Qux: Qux
  """.trimIndent()

  @Test fun testSampleStrings() {
    val pass = testSampleStrings<Action>(inputs)
    Truth.assertThat(pass).isTrue()
  }

  @Test fun debug() {
    testRoundTrip<Instruction>("\$name()")
    testRoundTrip<Instruction>("\$name(Abc)")
    testRoundTrip<Instruction>("\$name(Abc, Def)")
    testRoundTrip<Instruction>("\$name(Abc<Xyz, Bar>)")
    testRoundTrip<TypeExpression>("Abc")
    testRoundTrip<TypeExpression>("Abc<Bar>")
    testRoundTrip<TypeExpression>("Abc(HAS Bar)")
    testRoundTrip<TypeExpression>("Abc(HAS 11 Bar)")
    testRoundTrip<Requirement>("Bar")
    testRoundTrip<Requirement>("11 Bar")
    parse(Requirements.min, "Bar")
    parse(Requirements.min, "11 Bar")
    parse(Requirements.max, "MAX 11 Bar")

    testRoundTrip<TypeExpression>("Abc(HAS MAX 11 Bar)")

    testRoundTrip<Instruction>("\$name(Abc(HAS MAX 11 Bar))")
    testRoundTrip<Instruction>("\$name(Abc(HAS MAX 11 Bar<Xyz, Bar>))")
  }

  @Test fun debug2() {
    testRoundTrip<Action>("PROD[1] -> Foo")
  }
}
