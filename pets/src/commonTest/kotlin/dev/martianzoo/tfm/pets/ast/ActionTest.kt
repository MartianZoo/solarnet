package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.pets.ast.Action
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import kotlin.test.Test

// Most testing is done by AutomatedTest
internal class ActionTest {
  @Test
  internal fun stupid() {
    testRoundTrip<Action>("-> Ok")
  }

  @Test
  internal fun requirementGatedCosts() {
    testRoundTrip<Action>("(=0 Award: 8) OR (=1 Award: 14) OR (=2 Award: 20) -> Award")
  }

  @Test
  internal fun perCostsParenthesizeNestedGates() {
    testRoundTrip<Action>("(Bar: 1 / Foo) / Abc -> X Xyz")
  }

  private val inputs =
      """
      -> Ok
      -> -Wau
      2X -> Ok
      -> 11 Xyz
      -> -X Qux!
      Ahh, X -> 2
      1 -> 1 OR 1?
      -> 1: 1, -Ahh
      5 Foo -> X Foo
      PROD[Foo] -> Ok
      PROD[1] -> X Foo
      -> =1 Bar: -2 Xyz
      PROD[1] -> -Wau, 5
      -> PROD[1 / Foo], 1
      5X Abc<Foo> -> 1, 5?
      -> -1. OR Ok OR 2 Abc
      2 Eep / Bar MAX 5 -> 1
      Foo / Bar MAX 5 - Qux -> Ok
      PROD[PROD[Ooh]] -> -Eep
      PROD[Foo] -> Bar / 2 Foo
      Qux -> 1 OR Qux<Bar, Xyz>
      PROD[X] -> 2 Ooh<Ahh, Ahh>
      -> 5 Ahh? / PROD[Bar MAX 5]
      PROD[X Foo] -> -2X Xyz / Bar
      PROD[((1, 1): 1) / Xyz] -> 5X
      PROD[Bar OR 2] -> -X Foo<Qux>!
      PROD[Qux(HAS Foo)] -> -11X Eep?
      Qux -> PROD[MAX 1 Megacredit: 2]
      Qux<Abc> -> Qux BY Bar, Foo OR 1!
      PROD[1 / PROD[Bar]] -> -1? / 2 Abc
      PROD[2: Foo] -> Foo / 2 (Qux MAX 5)
      PROD[MAX 0 Megacredit: (1, 1)] -> 5?
      Eep, Qux -> PROD[Xyz FROM Qux], -!Qux
      PROD[1] -> (-Abc, Foo) OR -2 OR (1, 1)
      PROD[1] -> -2 Qux / (Qux OR Foo) OR Abc
      PROD[X Ooh<Foo> / Bar] -> 11 Bar(HAS 1)!
      PROD[Ahh / 2 Foo MAX 11] -> -1 / Xyz<Qux>
      5 Ahh<Bar, Ooh, Foo> -> 1, Foo<Foo> BY Eep
      PROD[(MAX 0 Foo: 1) / Xyz, Qux] -> Bar<Xyz>
      1, Qux OR (1 OR Bar, 1) -> PROD[5X / Foo], 1
      Foo<Foo<Bar, Bar>>: 1, PROD[Bar] -> Ahh<Qux>!
      Eep -> 11 Ooh<Ahh>, (Bar FROM Qux!) OR 1 / Ahh
      PROD[=1 Bar OR Bar]: PROD[Qux / Qux] -> 1 / Foo
      (MAX 1 Abc, (Qux, 1)): 2 Bar -> X / Foo, PROD[1]
      Qux, 1 / 2 (2 Xyz), (1, (1, Abc<Foo>, Bar)) -> Ok
      PROD[1: Foo, (Bar: 1) / 2 Foo] -> Qux! / 3 (3 Qux)
      PROD[1: Abc] / (Bar OR Foo<Foo, Bar>) OR Qux -> 11?
      2, (Ahh, (MAX 0 Megacredit: 1) / 2 Qux) -> Foo<Foo>.
      (Xyz, Bar): 1, PROD[Abc / Abc<Xyz>] -> X Abc FROM Ahh
      5 Bar / Foo<Foo, Bar> -> Foo: -Foo, 1 / 2 Foo, 1 OR Ok
      ((Xyz, 1) OR Xyz): PROD[1 / 2 Qux MAX 5] -> Foo(HAS 5)?
      1 -> -Ooh / 3 Qux, X Abc / Xyz<Bar>, MAX 1 Megacredit: 1
      (Ooh OR 1): (Foo<Bar, Qux> OR Qux OR 1) -> 1 / Abc OR Xyz
      Bar: (((1 OR Ooh) OR (Foo: 1 / Qux) / Foo) OR 1) -> 5 Eep?
      PROD[Bar<Foo>], 1, Eep<Bar> -> 11X Eep FROM !Xyz<Eep, Foo>?
      Foo<Abc<Foo<Bar<Qux<Qux>>>>>, Bar / Qux OR 2 Foo -> PROD[-1]
      """
          .trimIndent()

  @Test
  internal fun testSampleStrings() {
    testSampleStrings<Action>(inputs)
  }
}
