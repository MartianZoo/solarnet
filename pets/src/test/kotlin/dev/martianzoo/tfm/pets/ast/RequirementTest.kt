package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.GameApi
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.testSampleStrings
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class RequirementTest {

  val inputs = """
    0
    11
    Bar
    1, 5
    0 Ahh
    11 Foo
    PROD[1]
    PROD[11]
    PROD[Ahh]
    MAX 11 Eep
    PROD[0 Qux]
    PROD[11 Bar]
    =1 Megacredit
    =11 Megacredit
    PROD[MAX 1 Ooh]
    =0 Ooh<Ooh, Eep>
    MAX 11 Megacredit
    =1 Abc OR PROD[11]
    =11 Xyz(HAS =0 Abc)
    PROD[MAX 1 Abc<Bar>]
    PROD[PROD[MAX 1 Qux]]
    PROD[PROD[0 Foo<Qux>]]
    PROD[5 Qux<Bar> OR Ahh]
    PROD[Foo<Ahh<Foo, Ahh>>]
    PROD[PROD[=1 Megacredit]]
    (5 Foo, MAX 0 Foo, 0), Ahh
    =5 Xyz<Foo>, MAX 1 Xyz<Foo>
    0 Bar, 5 Eep<Foo>, MAX 1 Qux
    MAX 1 Xyz<Eep<Qux<Ahh>>, Wau>
    MAX 11 Bar<Ooh<Xyz, Foo<Bar>>>
    Bar<Foo>(HAS MAX 1 Qux), =1 Bar
    5 Foo<Ahh, Foo, Foo<Qux>(HAS 0)>
    (Abc OR MAX 0 Qux) OR PROD[0 Abc]
    MAX 1 Xyz<Eep<Ooh<Foo<Bar<Bar>>>>>
    MAX 1 Megacredit, PROD[0], Wau<Ahh>
    PROD[0 Abc, Wau, (=0 Megacredit, 1)]
    11 Abc<Bar<Ooh<Bar>, Abc>(HAS 5 Foo)>
    PROD[=5 Megacredit, =0 Xyz, MAX 0 Xyz]
    MAX 1 Eep<Ooh>, =11 Qux, MAX 0 Eep<Ooh>
    (MAX 5 Megacredit, Foo), PROD[MAX 1 Bar]
    PROD[(0 Xyz OR ((0 OR 0 Bar) OR 1)) OR 0]
    PROD[0 OR Abc], ((=1 Eep, Foo), =0 Xyz, 1)
    (MAX 1 Qux, Ooh<Foo>, 0 Abc<Ahh, Qux>), Bar
    Ahh<Xyz, Xyz>, PROD[Qux<Bar<Bar<Foo>>, Bar>]
    (MAX 1 Megacredit, PROD[0]), MAX 0 Megacredit
    MAX 1 Xyz OR (MAX 1 Megacredit, (0 Abc, 0)), 1
    ((5 Bar<Xyz>, Ooh), 1) OR ((Bar, 0 Foo), 5 Bar)
    PROD[(1 OR MAX 1 Ooh, 1), (0, MAX 0 Qux OR Qux)]
    PROD[Foo, =1 Megacredit] OR PROD[1 OR 0 Qux<Bar>]
    MAX 11 Ahh<Wau<Ahh<Ooh, Bar>, Ahh<Qux, Eep>>, Ahh>
    PROD[0 OR MAX 1 Foo] OR PROD[=1 Ooh OR =0 Qux<Qux>]
    (Xyz OR (1 OR Qux), 0 Qux), ((Bar, 0), (Bar, 0 Bar))
    (5, PROD[MAX 1 Foo<Foo>]), ((0, Foo<Foo>), MAX 1 Abc)
    PROD[(0 Abc OR MAX 5 Megacredit) OR (0 Qux OR 0), Foo]
    MAX 1 Eep OR PROD[1] OR MAX 0 Eep OR (1 OR 0, Abc<Foo>)
    PROD[Qux OR MAX 1 Megacredit OR MAX 1 Foo OR 1] OR 0 Xyz
    (MAX 1 Ooh OR MAX 1 Foo<Ooh> OR =1 Foo) OR (1, MAX 1 Ooh)
    (5 Abc OR MAX 5 Qux) OR (Bar OR 0 Bar) OR (MAX 0 Xyz, Abc)
    (MAX 1 Foo OR (Bar OR (0 OR 0 Foo))) OR (=1 Abc, 1, (1, 1))
    (MAX 1 Megacredit OR Qux) OR =1 Megacredit, =11 Abc OR 0 Qux
  """.trimIndent()

  @Test
  fun testSampleStrings() {
    val pass = testSampleStrings<Requirement>(inputs)
    assertThat(pass).isTrue()
  }

  @Test
  fun simpleSourceToApi() {
    assertThat(parse<Requirement>("Foo")).isEqualTo(Min(QuantifiedExpression(gte("Foo"))))
    assertThat(parse<Requirement>("3 Foo")).isEqualTo(Min(QuantifiedExpression(gte("Foo"), 3)))
    assertThat(parse<Requirement>("MAX 3 Foo")).isEqualTo(Max(QuantifiedExpression(gte("Foo"), 3)))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(Min(QuantifiedExpression(gte("Foo"))).toString()).isEqualTo("Foo")
    assertThat(Min(QuantifiedExpression(gte("Foo"), 1)).toString()).isEqualTo("Foo")
    assertThat(Min(QuantifiedExpression(gte("Foo"), 3)).toString()).isEqualTo("3 Foo")
    assertThat(Min(QuantifiedExpression(scalar = 3)).toString()).isEqualTo("3")
    assertThat(Min(QuantifiedExpression(gte("Default"), scalar = 3)).toString()).isEqualTo("3")
    assertThat(Min(QuantifiedExpression(gte("Default"))).toString()).isEqualTo("1")
    assertThat(Max(QuantifiedExpression(gte("Foo"), 0)).toString()).isEqualTo("MAX 0 Foo")
    assertThat(Max(QuantifiedExpression(gte("Foo"))).toString()).isEqualTo("MAX 1 Foo")
    assertThat(Max(QuantifiedExpression(gte("Foo"), 1)).toString()).isEqualTo("MAX 1 Foo")
    assertThat(Max(QuantifiedExpression(gte("Foo"), 3)).toString()).isEqualTo("MAX 3 Foo")
    assertThat(Max(QuantifiedExpression(scalar = 3)).toString()).isEqualTo("MAX 3 Default")
  }

  private fun testRoundTrip(start: String, end: String = start) =
      dev.martianzoo.tfm.pets.testRoundTrip<Requirement>(start, end)

  @Test
  fun roundTrips() {
    testRoundTrip("1", "1")
    testRoundTrip("Default", "1")
    testRoundTrip("1 Default", "1")
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

  @Test
  fun hairy() {
    val parsed: Requirement =
        parse("Adjacency<CityTile<Anyone>, OceanTile> OR 1 Adjacency<OceanTile, CityTile<Anyone>>")
    assertThat(parsed).isEqualTo(Requirement.Or(setOf(Min(QuantifiedExpression(gte("Adjacency",
        gte("CityTile", gte("Anyone")),
        gte("OceanTile")))),
        Min(QuantifiedExpression(gte("Adjacency",
            gte("OceanTile"),
            gte("CityTile", gte("Anyone"))))))))
  }

  // All type expressions with even-length string representations
  // exist and have a count equal to that string's length
  object FakeGame : GameApi {
    override val authority = Canon // why not

    override fun count(type: TypeExpression): Int {
      val length = type.toString().length
      return if (length % 2 == 0) length else 0
    }

    override fun isMet(requirement: Requirement) = requirement.evaluate(this)

    override fun applyChange(
        count: Int,
        gaining: GenericTypeExpression?,
        removing: GenericTypeExpression?,
        cause: Cause?,
    ) {
      TODO("this is just a dumb fake")
    }
  }

  fun evalRequirement(s: String) = assertThat(parse<Requirement>(s).evaluate(FakeGame))!!

  @Test
  fun evaluation() {
    evalRequirement("Foo").isFalse()
    evalRequirement("11 Foo").isFalse()
    evalRequirement("10 Megacredit").isTrue()
    evalRequirement("11 Megacredit").isFalse()
    evalRequirement("Foo<Bar>").isTrue()
    evalRequirement("8 Foo<Bar>").isTrue()
    evalRequirement("9 Foo<Bar>").isFalse()

    evalRequirement("MAX 0 Foo").isTrue()
    evalRequirement("MAX 7 Foo<Bar>").isFalse()
    evalRequirement("MAX 8 Foo<Bar>").isTrue()

    evalRequirement("=0 Foo").isTrue()
    evalRequirement("=1 Foo").isFalse()
    evalRequirement("=8 Foo<Bar>").isTrue()
    evalRequirement("=7 Foo<Bar>").isFalse()

    evalRequirement("10 Megacredit, Foo<Bar>, 8 Foo<Bar>, MAX 0 Foo, " +
        "MAX 8 Foo<Bar>, =0 Foo, =8 Foo<Bar>").isTrue()
    evalRequirement("10 Megacredit, Foo<Bar>, 8 Foo<Bar>, MAX 0 Foo, " +
        "MAX 8 Foo<Bar>, =1 Foo, =8 Foo<Bar>").isFalse()

    evalRequirement("Foo OR 11 Foo OR 11 OR 9 Foo<Bar> OR MAX 7 Foo<Bar> " +
        "OR =1 Foo OR =7 Foo<Bar>").isFalse()
    evalRequirement("Foo OR 11 Foo OR 11 OR 9 Foo<Bar> OR MAX 7 Foo<Bar> " +
        "OR =0 Foo OR =7 Foo<Bar>").isTrue()
  }
}
