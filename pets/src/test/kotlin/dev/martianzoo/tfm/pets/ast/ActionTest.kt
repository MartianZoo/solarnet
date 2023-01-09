package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class ActionTest {
  @Test
  fun stupid() {
    testRoundTrip<Action>("-> Ok")
  }

  val inputs = """
    -> 1
    -> 5?
    -> Bar
    1 -> 5?
    5 -> -11
    -> 11 Abc
    1 -> -Abc!
    11 Bar -> 5
    -> Xyz<Xyz>?
    -> PROD[-Foo]
    -> 1! / 11 Abc
    Foo -> PROD[-1]
    Ahh -> 11 Abc, 1
    PROD[Bar] -> Abc!
    1 -> PROD[PROD[1]]
    1 / Qux<Bar> -> Abc
    Abc -> Ooh<Qux>, Qux
    PROD[0 Ahh] -> 11 Qux
    0 Bar -> 11 Wau OR Abc
    PROD[5 OR 0 Qux] -> Bar
    11 Bar -> 1 Ahh FROM Foo
    1, Ooh -> PROD[Ooh, -Bar]
    PROD[PROD[0 Qux]] -> -Ooh!
    1 / 5 Xyz -> PROD[Qux<Bar>]
    0 Foo -> -Abc(HAS MAX 0 Qux)
    PROD[Foo] -> Ooh OR (1., -1!)
    PROD[Abc / Xyz] -> -5 Ooh<Foo>
    1, Bar / Xyz -> ${'$'}name(Abc)
    Qux -> Xyz? THEN ${'$'}name(Bar)
    11 -> 1 Foo FROM Xyz<Bar(HAS 0)>?
    PROD[Bar / Megacredit] -> Xyz<Xyz>
    PROD[PROD[1]] -> (11, -1 OR Foo), 1
    PROD[1] -> Foo<Bar>, ${'$'}name(Foo)
    5 / Eep -> (Foo, Qux): 5 Bar FROM Foo
    Abc, 0 Qux<Qux<Qux, Bar>> OR 0 -> -Qux
    0 Xyz OR 1 / Foo OR 0 / Megacredit -> 1
    PROD[1] -> PROD[Xyz<Qux<Xyz, Xyz, Xyz>>]
    1 -> (1 OR MAX 1 Abc<Qux<Foo>>): 1. / Qux
    5 Bar OR 0 / Megacredit -> 11 Wau FROM Ahh
    (5 Bar OR Bar) OR 5 / Abc -> Wau<Qux, Ooh>?
    PROD[Qux<Bar>], 0 -> (1 Xyz FROM Eep) OR -11
    PROD[0 / 5 Foo] -> PROD[PROD[1 Foo FROM Foo]]
    PROD[0 / 5 Ooh OR 0] -> Qux<Ooh, Xyz, Qux>!, 1
    PROD[1] OR 0 -> 5 Qux<Foo>(HAS MAX 0 Bar<Foo>)!
    5 Foo<Abc<Foo, Foo>, Qux, Wau<Foo>> -> PROD[Qux]
    -> PROD[11 Ooh<Eep<Ooh FROM Bar<Eep<Bar, Foo>>>>]
    1 / Ahh<Qux<Abc>> -> 0 Abc<Abc<Ooh<Foo>>>: (1, 1?)
    PROD[0 Bar / Bar OR (1 OR Qux)] -> 1, 1, Ooh, 0: -1
    PROD[0 Xyz] -> PROD[1, (${'$'}name(Xyz), 0: 1), Bar]
    Xyz -> -5 Wau<Qux, Ahh>!, 5 Qux / Megacredit, PROD[5]
    5 Foo<Qux(HAS (1 OR Abc) OR 0)> -> PROD[Ahh<Qux, Foo>]
    PROD[0 / Foo<Qux<Xyz>>] OR PROD[Foo] -> 5 Eep THEN 1: 1
    5 / Eep<Foo<Foo>> -> 1: 1 Abc<Foo FROM Xyz> / Megacredit
    PROD[0 Bar<Foo<Foo>> OR (1, 0 Abc)] -> -1, 1, Bar OR -Abc
    Ahh OR Foo<Bar> -> PROD[1: Bar, (MAX 1 Megacredit: 1, -1)]
    5 Wau<Xyz<Bar>> -> 1 Qux<Abc<Foo<Abc FROM Foo, Xyz>>> / Ooh
    PROD[0, 1 OR Foo / Megacredit] -> (0 Foo OR 0 Bar): Abc<Foo>
  """.trimIndent()

  @Test
  fun testSampleStrings() {
    val pass = testSampleStrings<Action>(inputs)
    Truth.assertThat(pass).isTrue()
  }

  @Test
  fun simple() {
    testRoundTrip<Action>("PROD[1] -> Foo")
  }
}
