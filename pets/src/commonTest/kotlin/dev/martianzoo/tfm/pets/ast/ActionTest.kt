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
    testRoundTrip<Action>("=0 Award: 8 MC -> Award")
  }

  @Test
  internal fun perCostsParenthesizeNestedGates() {
    testRoundTrip<Action>("(Bar: MC / Foo) / Abc -> X Xyz")
  }

  private val inputs =
      """
      -> 2 MC?
      2 MC -> MC?
      Foo -> MC
      Abc -> Ok
      Bar -> -Abc
      -> Qux<Foo>?
      -> X Bar<Bar>
      PROD[Ooh] -> 2 MC
      2 MC -> -Wau<!Qux>
      X !Xyz -> 2X Bar
      MC -> Eep FROM Foo
      Xyz<Qux<Bar>> -> X MC
      5 Xyz -> -Abc / Ooh
      MC, MC -> 2 Ahh / !Bar
      MC / Ahh -> -Foo<Qux>?
      2 Wau -> -X !Ooh<Xyz>!
      2 Xyz -> -2X MC, Bar / Foo
      2X Qux -> 2 Bar, MC OR Ok
      Foo, MC -> PROD[MC, MC], Wau
      X Abc -> -MC, Foo<Bar<Qux>>
      Qux / Eep -> -Ooh<Foo, Eep>
      -> (Foo: Foo FROM Foo) OR Ok
      PROD[MC / Foo] -> X MC, Foo, -Foo
      2 Xyz, Ooh -> PROD[PROD[-Ooh]]
      PROD[Qux] -> Qux FROM Bar / Abc
      (MAX 0 Qux: Foo) / Ooh -> 5X Foo
      Abc<Qux<Eep>> -> Ok OR (MC, 2 Foo)
      Foo<Qux>, X Ahh -> Bar FROM Qux, X MC
      PROD[MC] -> (-Foo OR MC / Qux) OR Ooh
      X Ooh<Abc>(HAS MC) -> Foo: -MC, -X Ooh
      -> -X Ahh<Foo>(HAS MAX 0 MC)!
      MC / Foo<Qux>, (MAX 0 Foo: MC, MC) -> -MC?
      MC / EVAL Bar.score - Abc MAX 11 -> -Foo
      PROD[Xyz(HAS MC)] -> X !Foo FROM Ooh<Foo>
      5 Qux -> PROD[MC], PROD[Qux THEN Foo<Qux>]
      Ooh / 3 Ooh<Bar> MAX 5 -> 11 Abc FROM Ooh?
      2 MC / Qux -> Bar, (Ooh FROM Bar / Foo) BY Foo
      =1 !Abc: (MAX 0 Qux: Bar) -> X Abc FROM Abc?
      2 Abc, MC -> MC, (Bar FROM Bar) OR (Qux OR Bar)
      5 Ooh<Ooh, Bar> -> -2X Foo(HAS =1 MC).
      -> MAX 0 MC: MC THEN MC., PROD[PROD[Bar]]
      -> 5 MC, (MAX 0 Foo OR (MC OR Foo)): X Bar<Qux, Qux>
      PROD[MC / Abc<Foo> MAX 11], Eep, X Foo -> Bar<Ooh>
      2 Qux<Xyz(HAS MAX 1 MC, MC)> -> X Ahh<Bar>.
      PROD[Bar] -> -2 Bar<!Bar<Eep, Foo<Bar, Foo>, Abc>>!
      PROD[=1 MC: X Bar] -> 2 Foo, X MC / 2 Foo MAX 5
      PROD[PROD[Bar]] -> -Bar<Foo> BY Foo<Xyz<Ooh>> OR Abc.
      PROD[(Qux, MC), PROD[Foo]] -> Bar<Ahh, Ooh, Bar>, 2 Xyz
      PROD[MAX 1 MC OR MC]: Abc / Xyz<Bar> -> 11X Ooh.
      Foo, PROD[Bar<Bar>], X Ahh<Qux> -> 2X Foo<Ahh> FROM Ooh!
      2 Wau -> (MC BY Bar, MC) OR (Ooh / 2 Bar) BY Qux<Xyz>, -Abc
      Ooh -> -Foo / Ooh - Abc - 2 (Foo MAX 5) THEN Ok THEN X Foo
      PROD[(Xyz: MC) / Bar, Bar: MC] -> Qux, Bar<Abc> FROM Foo<Foo>
      (Foo: Foo / 2 (2 Foo)) / EVAL Ooh.score -> -MC / Foo MAX 5, MC
      """
          .trimIndent()

  @Test
  internal fun testSampleStrings() {
    testSampleStrings<Action>(inputs)
  }
}
