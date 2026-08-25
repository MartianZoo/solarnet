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
  internal fun requirementGatedCost() {
    testRoundTrip<Action>("=0 Award: 8 -> Award")
  }

  @Test
  internal fun perCostsParenthesizeNestedGates() {
    testRoundTrip<Action>("(Bar: 1 / Foo) / Abc -> X Xyz")
  }

  private val inputs =
      """
      -> 2?
      2 -> 1?
      Foo -> 1
      Abc -> Ok
      Bar -> -Abc
      -> Qux<Foo>?
      -> X Bar<Bar>
      PROD[Ooh] -> 2
      2 -> -Wau<!Qux>
      X !Xyz -> 2X Bar
      1 -> Eep FROM Foo
      Xyz<Qux<Bar>> -> X
      5 Xyz -> -Abc / Ooh
      1, 1 -> 2 Ahh / !Bar
      1 / Ahh -> -Foo<Qux>?
      2 Wau -> -X !Ooh<Xyz>!
      2 Xyz -> -2X, Bar / Foo
      2X Qux -> 2 Bar, 1 OR Ok
      Foo, 1 -> PROD[1, 1], Wau
      X Abc -> -1, Foo<Bar<Qux>>
      Qux / Eep -> -Ooh<Foo, Eep>
      -> (Foo: Foo FROM Foo) OR Ok
      PROD[1 / Foo] -> X, Foo, -Foo
      2 Xyz, Ooh -> PROD[PROD[-Ooh]]
      PROD[Qux] -> Qux FROM Bar / Abc
      (MAX 0 Qux: Foo) / Ooh -> 5X Foo
      Abc<Qux<Eep>> -> Ok OR (1, 2 Foo)
      Foo<Qux>, X Ahh -> Bar FROM Qux, X
      PROD[1] -> (-Foo OR 1 / Qux) OR Ooh
      X Ooh<Abc>(HAS 1) -> Foo: -1, -X Ooh
      -> -X Ahh<Foo>(HAS MAX 0 Megacredit)!
      1 / Foo<Qux>, (MAX 0 Foo: 1, 1) -> -1?
      1 / EVAL Bar.score - Abc MAX 11 -> -Foo
      PROD[Xyz(HAS 1)] -> X !Foo FROM Ooh<Foo>
      5 Qux -> PROD[1], PROD[Qux THEN Foo<Qux>]
      Ooh / 3 Ooh<Bar> MAX 5 -> 11 Abc FROM Ooh?
      2 / Qux -> Bar, (Ooh FROM Bar / Foo) BY Foo
      =1 !Abc: (MAX 0 Qux: Bar) -> X Abc FROM Abc?
      2 Abc, 1 -> 1, (Bar FROM Bar) OR (Qux OR Bar)
      5 Ooh<Ooh, Bar> -> -2X Foo(HAS =1 Megacredit).
      -> MAX 0 Megacredit: 1 THEN 1., PROD[PROD[Bar]]
      -> 5, (MAX 0 Foo OR (1 OR Foo)): X Bar<Qux, Qux>
      PROD[1 / Abc<Foo> MAX 11], Eep, X Foo -> Bar<Ooh>
      2 Qux<Xyz(HAS MAX 1 Megacredit, 1)> -> X Ahh<Bar>.
      PROD[Bar] -> -2 Bar<!Bar<Eep, Foo<Bar, Foo>, Abc>>!
      PROD[=1 Megacredit: X Bar] -> 2 Foo, X / 2 Foo MAX 5
      PROD[PROD[Bar]] -> -Bar<Foo> BY Foo<Xyz<Ooh>> OR Abc.
      PROD[(Qux, 1), PROD[Foo]] -> Bar<Ahh, Ooh, Bar>, 2 Xyz
      PROD[MAX 1 Megacredit OR 1]: Abc / Xyz<Bar> -> 11X Ooh.
      Foo, PROD[Bar<Bar>], X Ahh<Qux> -> 2X Foo<Ahh> FROM Ooh!
      2 Wau -> (1 BY Bar, 1) OR (Ooh / 2 Bar) BY Qux<Xyz>, -Abc
      Ooh -> -Foo / Ooh - Abc - 2 (Foo MAX 5) THEN Ok THEN X Foo
      PROD[(Xyz: 1) / Bar, Bar: 1] -> Qux, Bar<Abc> FROM Foo<Foo>
      (Foo: Foo / 2 (2 Foo)) / EVAL Ooh.score -> -1 / Foo MAX 5, 1
      """
          .trimIndent()

  @Test
  internal fun testSampleStrings() {
    testSampleStrings<Action>(inputs)
  }
}
