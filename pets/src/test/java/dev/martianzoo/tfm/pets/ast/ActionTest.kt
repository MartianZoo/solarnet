package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import dev.martianzoo.tfm.testlib.PetGenerator
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class ActionTest {
  @Test
  fun stupid() {
    testRoundTrip<Action>("-> Ok")
  }

  val inputs =
      """
    -> 11
    -> Bar
    1 -> -5!
    1 -> -11!
    11 Abc -> 1
    -> PROD[Qux]
    5 -> -11 Xyz?
    PROD[1] -> Qux
    Bar<Bar> -> 11!
    Abc -> Bar / Wau
    PROD[1] -> 11 Xyz
    -> @name(Qux, Xyz)
    PROD[1] -> Foo<Foo>
    PROD[1] -> Bar / Foo
    11 Abc<Qux> -> 11 Bar?
    Qux -> Wau<Ahh>: 11 Abc
    5 Xyz<Foo> -> 5 Qux<Bar>
    11 Abc -> 1. / Megacredit
    Foo -> PROD[-1 / Bar<Wau>]
    Bar -> 5 Ooh<Qux> FROM Abc.
    PROD[Bar] / Foo<Qux> -> -Xyz
    PROD[Foo] -> @name(Qux), Foo.
    5 Abc<Abc<Xyz>, Qux, Qux> -> 1
    1 -> -11 Ooh<Foo>, (Foo, -1, 1)
    PROD[1], 11 Xyz, 5 Foo -> 5 Ahh.
    Xyz<Foo> -> 11 Qux? THEN Ooh / Foo
    Foo<Foo> / Qux -> (1, Qux), -11 Foo
    Xyz<Abc<Qux>> -> PROD[@name(Qux), 1]
    -> (5 Qux FROM Bar, Bar) OR -1 OR Foo
    PROD[Bar<Foo>] / Foo -> -Foo<Foo, Bar>
    Bar, Bar -> Qux, Foo THEN ((1: 1) OR 1)
    5 Ooh<Abc> -> Ooh, 1 OR (Abc FROM Foo)
    1 / Eep -> 5 Foo THEN Bar: Abc<Qux> / Xyz
    5 OR 1 -> Foo<Foo<Foo>>. OR -Foo<Foo, Qux>
    Foo<Ahh<Foo<Ahh>>> -> 5, ((-1, 1: -1), Qux)
    1 / 11 Bar -> PROD[Abc], (5 Bar, (-Foo, -1))
    Xyz, Bar, Abc -> PROD[Xyz OR Abc / Megacredit]
    1 -> (1 OR PROD[1] OR (1 OR Foo OR 5 Bar)): -Qux
    Ooh<Qux> / Foo, 1 / Qux -> -Bar(HAS =1 Megacredit)
    Foo / Ooh OR 1, Xyz / 5 Abc, PROD[Foo, Foo] -> Foo!
    11 Foo / 11 Eep -> -Bar, 1, Ahh FROM Ahh?, 1 / Bar
    1 / Megacredit OR PROD[11 / Megacredit] -> 1. OR -Xyz
    PROD[Eep] -> Ooh<Foo<Abc>>., Abc., 11 Bar<Qux, Bar>, 1
    Qux<Qux> -> 1 OR -1. OR (Foo<Qux>: ((1 OR -Foo) OR Foo))
  """
          .trimIndent()

  @Test
  fun testSampleStrings() {
    testSampleStrings<Action>(inputs)
  }

  // @Test
  fun genGrossApiCalls() {
    PetGenerator(0.9).generateTestApiConstructions<Action>(20)
  }
}
