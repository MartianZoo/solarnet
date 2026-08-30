package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
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
      5 MC
      11 MC
      Qux
      5 MC, MC
      5 Foo
      =1 Foo
      =11 Xyz
      EVAL Foo.requirement
      PROD[11 MC]
      MAX 1 Abc
      MAX 11 Bar
      =1 Qux<Wau>
      PROD[=1 Bar]
      =0 Xyz(HAS MC)
      MC OR MAX 1 Xyz
      PROD[MAX 1 Qux]
      MAX 5 MC
      PROD[=0 Abc<Foo>]
      (5 MC, Bar) OR 11 Abc
      Bar, Bar, MAX 1 Ooh
      5 Qux<Qux<Xyz, Foo>>
      PROD[MAX 1 Bar] OR 11 MC
      PROD[=0 MC, MC]
      PROD[Bar OR (Abc, Abc)]
      MAX 5 Xyz<Foo<Ooh<Foo>>>
      PROD[Foo<Xyz> OR (5 MC, 11 MC)]
      MAX 5 MC OR 11 Qux
      PROD[=1 Qux<Qux, Bar, Abc>]
      PROD[MAX 11 MC, Bar]
      Foo<Qux>, PROD[=0 MC]
      ((MC, Foo), MC OR Foo), Wau<Foo>
      PROD[Qux<Foo>, MAX 11 Bar, Qux]
      MAX 0 Ahh<Foo>, MAX 1 MC
      MAX 1 MC, Xyz OR Xyz<Foo>
      PROD[11 Bar(HAS MAX 1 MC)]
      PROD[MAX 1 Bar], (Qux, Bar<Foo>, MC)
      =5 Wau<Qux, Bar<Foo>, Ooh<Eep, Bar>>
      PROD[=0 MC, Abc OR MAX 1 Abc]
      MAX 1 Abc<Qux, Ahh(HAS =0 MC)>
      PROD[MAX 1 MC, 5 Bar<Qux, Foo>]
      PROD[MC, (MAX 1 MC, Abc<Bar>, MC)]
      PROD[MC OR ((MAX 0 Bar, Foo OR MC) OR Foo)]
      Bar, 5 Foo, 11 Foo OR (MC, Abc<Abc>) OR Ahh
      5 Abc OR (Qux OR Bar, Abc<Foo>) OR MC OR Bar
      PROD[(MAX 0 MC, Qux<Foo>) OR =0 Bar]
      MAX 1 Bar OR (MAX 0 MC, PROD[=0 Ahh])
      ((MC OR MAX 0 Foo) OR =1 MC, =0 Foo), MC
      PROD[=0 Abc OR ((MAX 1 MC, MC), =1 Bar)]
      MAX 5 MC, 5 Foo OR Qux OR MAX 1 Foo<Bar>
      11 Foo, (MAX 1 Foo OR (Abc, MC)) OR PROD[Abc<Bar>]
      Xyz<Qux, Xyz> OR (MC, MAX 1 Bar), PROD[11 Qux], Xyz
      MAX 1 Bar OR 5 MC OR =5 MC OR MC, 5 Ooh<Qux>, MC
      MAX 5 Foo<Abc, Foo> OR 11 MC OR PROD[Bar, 5 Qux] OR Xyz
      MC OR Bar<Bar>, MC OR ((Foo OR MC) OR =1 MC), 11 MC
      (Qux, MAX 1 Foo OR Foo, (MC, MC)), =0 Xyz, =1 MC
      ((Bar, MAX 0 Foo<Xyz>) OR =5 Qux OR Foo) OR PROD[5 Qux]
      (Foo, =11 Abc, MAX 11 MC), (Bar, (Xyz, MC) OR Foo)
      ((MAX 1 Qux OR MAX 1 MC OR MC) OR Bar) OR MAX 1 Eep
      MAX 1 Foo<Qux, Ooh>, (MC, MAX 1 Foo), PROD[MAX 1 MC]
      PROD[MAX 0 Xyz OR MAX 1 Foo OR 5 Ahh OR Ooh, MAX 0 Ahh OR MC]
      """
          .trimIndent()

  @Test
  internal fun testSampleStrings() {
    testSampleStrings<Requirement>(inputs)
  }

  private val fooEx = cn("Foo").expression

  @Test
  internal fun simpleSourceToApi() {
    parse<Requirement>("Foo") shouldBe Min(scaledEx(fooEx, 1))
    parse<Requirement>("3 Foo") shouldBe Min(scaledEx(fooEx, 3))
    parse<Requirement>("MAX 3 Foo") shouldBe Max(scaledEx(fooEx, 3))
  }

  @Test
  internal fun requirementTargetsAreNotPartOfTheirMetrics() {
    val metric = Metric.Count(fooEx)
    parse<Requirement>("8 Foo") shouldBe Min(8, metric)
    parse<Requirement>("MAX 8 Foo") shouldBe Max(8, metric)
  }

  @Test
  internal fun simpleApiToSource() {
    Min(scaledEx(fooEx, 1)).toString() shouldBe "Foo"
    Min(scaledEx(fooEx, 3)).toString() shouldBe "3 Foo"
    Min(scaledEx(cn("MC").expression, count = 3)).toString() shouldBe "3 MC"
    Max(scaledEx(fooEx, 0)).toString() shouldBe "MAX 0 Foo"
    Max(scaledEx(fooEx, 1)).toString() shouldBe "MAX 1 Foo"
    Max(scaledEx(fooEx, 3)).toString() shouldBe "MAX 3 Foo"
  }

  private fun testRoundTrip(start: String, end: String = start) =
      testRoundTrip<Requirement>(start, end)

  @Test
  internal fun roundTrips() {
    testRoundTrip("MC")
    testRoundTrip("MC", "MC")
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
  internal fun testProd() {
    testRoundTrip("PROD[2 MC]")
    testRoundTrip("Steel, PROD[MC]")
    testRoundTrip("PROD[Steel, MC]")
    testRoundTrip("PROD[Steel OR MC]")
  }

  @Test
  internal fun unexpandedEvalIsAProgrammerError() {
    kotlin.test.assertFailsWith<IllegalStateException> {
      parse<Requirement>("EVAL Foo.requirement").isMetBy { 0 }
    }
  }
}
