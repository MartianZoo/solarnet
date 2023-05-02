package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class RequirementTest {

  val inputs =
      """
    5
    11
    Qux
    5, 1
    5 Foo
    =1 Foo
    =11 Xyz
    PROD[11]
    MAX 1 Abc
    MAX 11 Bar
    =1 Qux<Wau>
    PROD[=1 Bar]
    =0 Xyz(HAS 1)
    1 OR MAX 1 Xyz
    PROD[MAX 1 Qux]
    MAX 5 Megacredit
    PROD[=0 Abc<Foo>]
    (5, Bar) OR 11 Abc
    Bar, Bar, MAX 1 Ooh
    5 Qux<Qux<Xyz, Foo>>
    PROD[MAX 1 Bar] OR 11
    PROD[=0 Megacredit, 1]
    PROD[Bar OR (Abc, Abc)]
    MAX 5 Xyz<Foo<Ooh<Foo>>>
    PROD[Foo<Xyz> OR (5, 11)]
    MAX 5 Megacredit OR 11 Qux
    PROD[=1 Qux<Qux, Bar, Abc>]
    PROD[MAX 11 Megacredit, Bar]
    Foo<Qux>, PROD[=0 Megacredit]
    ((1, Foo), 1 OR Foo), Wau<Foo>
    PROD[Qux<Foo>, MAX 11 Bar, Qux]
    MAX 0 Ahh<Foo>, MAX 1 Megacredit
    MAX 1 Megacredit, Xyz OR Xyz<Foo>
    PROD[11 Bar(HAS MAX 1 Megacredit)]
    PROD[MAX 1 Bar], (Qux, Bar<Foo>, 1)
    =5 Wau<Qux, Bar<Foo>, Ooh<Eep, Bar>>
    PROD[=0 Megacredit, Abc OR MAX 1 Abc]
    MAX 1 Abc<Qux, Ahh(HAS =0 Megacredit)>
    PROD[MAX 1 Megacredit, 5 Bar<Qux, Foo>]
    PROD[1, (MAX 1 Megacredit, Abc<Bar>, 1)]
    PROD[1 OR ((MAX 0 Bar, Foo OR 1) OR Foo)]
    Bar, 5 Foo, 11 Foo OR (1, Abc<Abc>) OR Ahh
    5 Abc OR (Qux OR Bar, Abc<Foo>) OR 1 OR Bar
    PROD[(MAX 0 Megacredit, Qux<Foo>) OR =0 Bar]
    MAX 1 Bar OR (MAX 0 Megacredit, PROD[=0 Ahh])
    ((1 OR MAX 0 Foo) OR =1 Megacredit, =0 Foo), 1
    PROD[=0 Abc OR ((MAX 1 Megacredit, 1), =1 Bar)]
    MAX 5 Megacredit, 5 Foo OR Qux OR MAX 1 Foo<Bar>
    11 Foo, (MAX 1 Foo OR (Abc, 1)) OR PROD[Abc<Bar>]
    Xyz<Qux, Xyz> OR (1, MAX 1 Bar), PROD[11 Qux], Xyz
    MAX 1 Bar OR 5 OR =5 Megacredit OR 1, 5 Ooh<Qux>, 1
    MAX 5 Foo<Abc, Foo> OR 11 OR PROD[Bar, 5 Qux] OR Xyz
    1 OR Bar<Bar>, 1 OR ((Foo OR 1) OR =1 Megacredit), 11
    (Qux, MAX 1 Foo OR Foo, (1, 1)), =0 Xyz, =1 Megacredit
    ((Bar, MAX 0 Foo<Xyz>) OR =5 Qux OR Foo) OR PROD[5 Qux]
    (Foo, =11 Abc, MAX 11 Megacredit), (Bar, (Xyz, 1) OR Foo)
    ((MAX 1 Qux OR MAX 1 Megacredit OR 1) OR Bar) OR MAX 1 Eep
    MAX 1 Foo<Qux, Ooh>, (1, MAX 1 Foo), PROD[MAX 1 Megacredit]
    PROD[MAX 0 Xyz OR MAX 1 Foo OR 5 Ahh OR Ooh, MAX 0 Ahh OR 1]
  """
          .trimIndent()

  @Test
  fun testSampleStrings() {
    testSampleStrings<Requirement>(inputs)
  }

  val fooEx = cn("Foo").expression

  @Test
  fun simpleSourceToApi() {
    assertThat(parse<Requirement>("Foo")).isEqualTo(Min(scaledEx(1, fooEx)))
    assertThat(parse<Requirement>("3 Foo")).isEqualTo(Min(scaledEx(3, fooEx)))
    assertThat(parse<Requirement>("MAX 3 Foo")).isEqualTo(Max(scaledEx(3, fooEx)))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(Min(scaledEx(1, fooEx)).toString()).isEqualTo("Foo")
    assertThat(Min(scaledEx(3, fooEx)).toString()).isEqualTo("3 Foo")
    assertThat(Min(scaledEx(value = 3)).toString()).isEqualTo("3")
    assertThat(Min(scaledEx(value = 3, cn("Megacredit").expression)).toString()).isEqualTo("3")
    assertThat(Max(scaledEx(0, fooEx)).toString()).isEqualTo("MAX 0 Foo")
    assertThat(Max(scaledEx(1, fooEx)).toString()).isEqualTo("MAX 1 Foo")
    assertThat(Max(scaledEx(3, fooEx)).toString()).isEqualTo("MAX 3 Foo")
    assertThat(Max(scaledEx(value = 3)).toString()).isEqualTo("MAX 3 Megacredit")
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
