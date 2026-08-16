package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.Requirement.Max
import dev.martianzoo.pets.ast.Requirement.Min
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Most testing is done by AutomatedTest
internal class RequirementTest {

  private val inputs =
      """
      5
      Foo
      !Eep
      2 Ahh
      5 !Eep
      =1 !Abc
      Xyz<Qux>
      PROD[Qux]
      2 Qux OR 1
      PROD[2 Foo]
      PROD[=1 Abc]
      =1 Megacredit
      MAX 1 Ahh<Qux>
      2 Foo<Qux> OR 1
      PROD[MAX 1 !Xyz]
      =2 Qux<!Qux<Qux>>
      PROD[2 Eep OR Qux]
      MAX 0 Ahh<Abc<Foo>>
      PROD[MAX 2 Bar<Foo>]
      5 Wau<!Eep<Qux>, Qux>
      =11 Ooh<Ahh<Foo>, Ooh>
      PROD[PROD[=0 Bar OR 1]]
      PROD[Wau<Qux, Bar, Abc>]
      PROD[MAX 1 Megacredit, 1]
      5 Xyz, Ahh, MAX 1 Foo<Foo>
      (Foo OR Abc, Ooh<Qux>), Abc
      PROD[PROD[MAX 1 Megacredit]]
      =1 Megacredit, Foo, MAX 1 Foo
      PROD[PROD[Bar, 1 OR Bar<Foo>]]
      MAX 5 Xyz<Ooh<Xyz>(HAS 1, Qux)>
      PROD[=0 Abc<Bar<Abc<Bar, Foo>>>]
      Foo<Foo<Foo, Ooh, Foo>> OR =2 Bar
      MAX 0 Megacredit OR Xyz, PROD[Foo]
      Ooh OR MAX 1 Bar OR PROD[MAX 0 Abc]
      MAX 1 Wau, ((Foo, MAX 0 Ahh), 2 Qux)
      (1 OR 2) OR (1 OR =1 Abc), 5, PROD[1]
      PROD[=1 Foo, =1 Bar, MAX 1 Foo(HAS 1)]
      MAX 2 Bar OR (=1 !Abc OR Foo) OR =0 Bar
      (2 OR Qux) OR (MAX 1 Abc, Xyz OR =0 Qux)
      =1 Ahh, 1, Ahh<Xyz(HAS 1), Xyz<Foo<Xyz>>>
      1 OR PROD[Qux] OR (MAX 1 Ahh OR MAX 0 Bar)
      PROD[MAX 1 Megacredit OR 2 OR 1 OR PROD[1]]
      PROD[Xyz OR (MAX 1 Megacredit OR MAX 1 Bar)]
      PROD[=0 Megacredit, MAX 1 Abc, =1 Megacredit]
      2 Qux<Xyz<Eep, Qux>, Abc<Eep>, Ooh<Ooh, !Abc>>
      (1, (Qux, 1, 1)), =0 Eep<Foo>, MAX 1 Megacredit
      2 Ahh<Ahh> OR MAX 2 Megacredit OR MAX 1 Qux OR 1
      PROD[Xyz OR MAX 1 Qux], (1, Abc), PROD[MAX 1 Ooh]
      ((MAX 0 Foo, 5) OR Foo) OR Xyz OR Qux OR MAX 5 Abc
      PROD[MAX 2 Foo, Qux OR (1 OR Ooh<Foo>) OR (Abc, 1)]
      (1 OR (1 OR Foo), PROD[Foo]), (MAX 1 Xyz, MAX 1 Bar)
      MAX 5 Megacredit OR MAX 1 Megacredit OR =2 Megacredit
      PROD[(Abc, MAX 1 Bar), Qux, 1 OR (1 OR =0 Megacredit)]
      PROD[MAX 1 Xyz OR MAX 0 Megacredit OR Bar OR MAX 1 Bar]
      MAX 0 Megacredit OR 1 OR MAX 1 Xyz OR (1 OR =1 Foo<Bar>)
      (MAX 1 Megacredit, =2 Ooh), MAX 0 Bar OR Ooh OR MAX 1 Bar
      PROD[=1 Abc] OR (2 OR 1) OR ((MAX 1 Megacredit OR 1) OR 1)
      MAX 1 Foo<Qux, Foo<Bar>>, Bar OR (1, 1 OR MAX 1 Megacredit)
      PROD[MAX 1 Qux<Qux>] OR PROD[Ahh] OR MAX 0 Ahh OR PROD[!Bar]
      """
          .trimIndent()

  @Test
  fun testSampleStrings() {
    testSampleStrings<Requirement>(inputs)
  }

  private val fooEx = cn("Foo").expression

  @Test
  fun simpleSourceToApi() {
    parse<Requirement>("Foo") shouldBe Min(scaledEx(fooEx, 1))
    parse<Requirement>("3 Foo") shouldBe Min(scaledEx(fooEx, 3))
    parse<Requirement>("MAX 3 Foo") shouldBe Max(scaledEx(fooEx, 3))
  }

  @Test
  fun simpleApiToSource() {
    Min(scaledEx(fooEx, 1)).toString() shouldBe "Foo"
    Min(scaledEx(fooEx, 3)).toString() shouldBe "3 Foo"
    Min(scaledEx(count = 3)).toString() shouldBe "3"
    Min(scaledEx(cn("Megacredit").expression, count = 3)).toString() shouldBe "3"
    Max(scaledEx(fooEx, 0)).toString() shouldBe "MAX 0 Foo"
    Max(scaledEx(fooEx, 1)).toString() shouldBe "MAX 1 Foo"
    Max(scaledEx(fooEx, 3)).toString() shouldBe "MAX 3 Foo"
    Max(scaledEx(count = 3)).toString() shouldBe "MAX 3 Megacredit"
  }

  private fun testRoundTrip(start: String, end: String = start) =
      testRoundTrip<Requirement>(start, end)

  @Test
  fun roundTrips() {
    testRoundTrip("1", "1")
    testRoundTrip("Megacredit", "1")
    testRoundTrip("1 Megacredit", "1")
    testRoundTrip("Plant")
    testRoundTrip("1 Plant", "Plant")
    testRoundTrip("3 Plant")
    testRoundTrip("MAX 0 Plant")
    testRoundTrip("MAX 1 Plant")
    testRoundTrip("MAX 3 Plant")
    testRoundTrip("15 (ActiveCard OR AutomatedCard)")
    testRoundTrip("MAX 4 (OwnedTile OR CityTile)")
    testRoundTrip("6 PROD[Steel OR Titanium]")
    testRoundTrip("=6 (PROD[Steel OR Titanium])", "=6 PROD[Steel OR Titanium]")
    testRoundTrip("CityTile<LandArea>, GreeneryTile<WaterArea>")
    testRoundTrip("PlantTag, MicrobeTag OR AnimalTag")
    testRoundTrip("(PlantTag, MicrobeTag) OR AnimalTag")
  }

  @Test
  fun testProd() {
    testRoundTrip("PROD[2]")
    testRoundTrip("Steel, PROD[1]")
    testRoundTrip("PROD[Steel, 1]")
    testRoundTrip("PROD[Steel OR 1]")
  }
}
