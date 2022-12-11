package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.Predicate.Max
import dev.martianzoo.tfm.petaform.Predicate.Min
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
class PredicateTest {
  @Test
  fun simpleSourceToApi() {
    assertThat(PetaformParser.parse<Predicate>("Foo"))
        .isEqualTo(Min(TypeExpression("Foo")))
    assertThat(PetaformParser.parse<Predicate>("3 Foo"))
        .isEqualTo(Min(TypeExpression("Foo"), 3))
    assertThat(PetaformParser.parse<Predicate>("MAX 3 Foo"))
        .isEqualTo(Max(TypeExpression("Foo"), 3))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(Min(TypeExpression("Foo")).toString()).isEqualTo("Foo")
    assertThat(Min(TypeExpression("Foo"), 1).toString()).isEqualTo("1 Foo")
    assertThat(Min(TypeExpression("Foo"), 3).toString()).isEqualTo("3 Foo")
    assertThat(Min(TypeExpression("Megacredit"), 3).toString()).isEqualTo("3 Megacredit")
    assertThat(Max(TypeExpression("Foo"), 0).toString()).isEqualTo("MAX 0 Foo")
    assertThat(Max(TypeExpression("Foo"), 1).toString()).isEqualTo("MAX 1 Foo")
    assertThat(Max(TypeExpression("Foo"), 3).toString()).isEqualTo("MAX 3 Foo")
  }

  val inputs = """
    5
    11
    Xyz
    1, 5
    MAX 5
    11 Xyz
    =11 Ooh
    5 OR Ooh
    MAX 5 Ooh
    5 OR MAX 1
    Ahh OR 5, 5
    Ahh OR MAX 0
    11 Qux, 1 Foo
    MAX 5 Foo OR 1
    11 Foo OR 1 Abc
    11 Foo<Foo<Ooh>>
    5 OR (Foo OR Ooh)
    Foo OR 11 Abc<Foo>
    1 Ooh OR 5 Foo<Abc>
    1 Ooh OR =0 Foo<Bar>
    =1 Bar<Foo, Qux, Wau>
    MAX 1 Xyz OR MAX 1 Abc
    Foo<Eep, Xyz<Ooh>, Ahh>
    5 Foo<Bar>, 1 Abc, 1 Ooh
    Foo<Ahh> OR (11 OR MAX 0)
    Abc OR MAX 5 Ooh<Ooh, Bar>
    1 Ooh(HAS Bar) OR MAX 5 Ahh
    =11 OR (=1, Qux, Foo) OR Eep
    MAX 11 Xyz<Eep> OR MAX 11 Qux
    (1 Foo, =0 Qux<Foo>) OR =0 Ahh
    =0 Ahh<Xyz, Qux<Abc>> OR =0 Foo
    1 Abc, (MAX 1 Foo OR 1) OR 1 Xyz
    =5 OR (MAX 11 Qux<Eep>, Foo<Ahh>)
    =5 Xyz OR MAX 0 Abc<Wau<Qux>, Bar>
    MAX 1 Wau<Ahh(HAS Qux, 5 Qux<Foo>)>
    1 Foo OR ((Bar OR Xyz) OR MAX 1 Bar)
    1 OR (MAX 0 OR (1 Bar, Foo OR 1 Bar))
    Bar<Abc> OR (Bar<Xyz> OR 1 Foo, 5 Ahh)
    =0 Eep OR (Ooh, Foo<Abc>) OR 5 Ooh OR 5
    1 Ooh<Foo> OR Bar<Foo> OR MAX 1 Qux<Eep>
    MAX 1 Foo<Xyz> OR (MAX 5 Eep OR Foo<Bar>)
    (Abc, 1 Bar OR (Xyz, Xyz)), =11, MAX 5 Wau
    11 Xyz<Abc> OR (=1 OR 1 Foo<Bar<Bar>, Foo>)
    ((MAX 5, 1) OR MAX 1 Foo) OR (11 Qux OR Ahh)
    Qux<Foo<Qux>> OR 11 Foo<Ahh<Ooh>, Foo> OR Foo
    (=0 OR 1) OR (Bar<Foo<Xyz>> OR MAX 0 Foo<Xyz>)
    (1 Foo<Bar<Xyz, Bar<Bar>>, Foo, Abc> OR 5) OR 1
    (Ahh<Ooh> OR 1 Foo<Ahh> OR 11) OR (Foo, =11 Xyz)
    (Abc<Xyz, Foo<Abc<Bar>>>, Abc), Qux OR 5 Abc<Qux>
    MAX 0 Abc OR 11 Ooh<Ooh, Bar> OR (5 Ooh OR =5 Qux)
    (11 OR (1 Abc, 1) OR (Qux OR 1)) OR (Ooh<Bar>, Ooh)
    MAX 5 OR 5, MAX 5 OR (=1 Abc OR (1 Qux, Abc)), MAX 0
    (11 OR 5) OR (((Foo OR Foo) OR Qux<Bar>) OR (Foo, 1))
    ((Bar OR 1 Bar) OR (1 Wau OR 5 Ahh)) OR MAX 1 Abc<Xyz>
    =1 Wau<Qux<Foo<Abc<Bar>>>> OR MAX 1 Ahh OR MAX 0 OR Bar
    (Qux, Xyz OR Bar) OR =0 OR Bar<Abc>(HAS Bar) OR Ooh<Xyz>
    MAX 0, Foo OR 5 OR ((Ooh OR Bar) OR Bar) OR Qux<Xyz, Ooh>
    MAX 1 Eep OR Abc OR =1 Qux OR ((Qux, MAX 5) OR 1 Foo<Foo>)
    11 OR (Abc, Ahh<Wau>, 5 Foo<Ooh>) OR Xyz<Bar> OR 1 Qux<Ooh>
    =1 Xyz OR Qux, MAX 1 Ahh OR =11, (1, 1 Qux OR 1 OR Foo<Bar>)
    ((=0 Foo OR MAX 5) OR =1 Ahh<Bar>) OR (=1 OR 1 OR Foo OR Ooh)
    11 Bar<Qux> OR MAX 11 Bar<Ooh<Abc<Abc>, Ahh>> OR Foo<Qux<Qux>>
    ((Abc OR Abc) OR 11 Ahh OR 1 Foo OR MAX 1) OR (1 Foo, 1 Foo, 5)
    Eep, =0 Ahh<Ahh<Abc<Qux<Foo, Abc>>>>, (5 Foo, MAX 0) OR Qux<Abc>
    11 Bar<Ahh<Foo>> OR Abc(HAS =1 Xyz) OR (=0 OR MAX 0 Ooh) OR 1 Qux
    Abc OR 11 OR MAX 0 OR (5 Abc<Bar>, Bar<Foo> OR MAX 0 OR MAX 5 Bar)
    =1 OR (Abc, 1, 1) OR Qux OR (MAX 0 OR =11 Foo OR MAX 0 OR Ooh<Qux>)
    5 Abc OR 11 Qux<Foo<Foo, Qux>, Wau> OR =5 Xyz<Foo> OR (5 Foo, 1 Foo)
    Qux OR (MAX 0 Foo OR 1 Bar OR (Ooh, MAX 0 Xyz) OR MAX 1 Ahh(HAS Xyz))
    MAX 11 OR ((MAX 0 Bar OR Abc) OR MAX 0 Qux), ((MAX 1 Foo, 5 Bar), Xyz)
    =0 Xyz, (Abc OR MAX 0) OR (MAX 0 Xyz OR MAX 0 Foo, 11), MAX 0 Ooh OR =0
    (MAX 5 Xyz<Ooh> OR 1 Foo) OR MAX 0 Abc<Abc, Foo, Foo> OR MAX 11 Xyz<Abc>
    (MAX 1 Qux OR Eep) OR (5 OR MAX 5 OR 11 Qux OR (MAX 1, (Bar, MAX 0 Abc)))
    (1 Bar<Ahh> OR Abc) OR MAX 0 Bar OR (MAX 11 Foo OR =5) OR (1 Qux OR MAX 1)
    1 Wau<Wau<Wau>, Qux<Xyz, Foo<Xyz, Ooh<Xyz>, Ooh>, Abc<Xyz<Ahh<Xyz>>, Qux>>>
    (Qux OR (5, 1 Foo)) OR =0 Bar, (Xyz<Foo>, 1) OR 1 Ooh, 5 Qux OR (Foo, 1 Bar)
    1 Abc<Foo, Abc> OR MAX 1 OR 1 Qux OR (MAX 1 Foo<Foo>, 1 OR MAX 0 Ooh, =0 Qux)
    ((Bar OR Bar) OR MAX 1 Foo) OR 5 Bar<Wau<Qux, Qux>>(HAS MAX 0 Eep) OR Bar OR 1
    Bar OR (=5 Foo OR MAX 5 Ooh) OR ((1 OR (MAX 0, Bar)) OR (Foo, MAX 0)) OR 11 Foo
    MAX 0 OR (11, (1 Abc, MAX 1 Foo)) OR (5 OR ((=0 Bar OR Bar, Bar) OR (Bar, Abc)))
  """

  @Test fun testSampleStrings() {
    inputs.split('\n').forEach { testRoundTrip<Predicate>(it) }
  }

  private fun testRoundTrip(start: String, end: String = start) =
      testRoundTrip<Predicate>(start, end)

  @Test
  fun roundTrips() {
    testRoundTrip("Megacredit")
    testRoundTrip("1 Megacredit")
    testRoundTrip("Plant")
    testRoundTrip("1 Plant")
    testRoundTrip("3 Plant")
    testRoundTrip("MAX 0 Plant")
    testRoundTrip("MAX 1 Plant")
    testRoundTrip("MAX 3 Plant")
    testRoundTrip("CityTile<LandArea>, GreeneryTile<WaterArea>")
    testRoundTrip("PlantTag, MicrobeTag OR AnimalTag")
    testRoundTrip("(PlantTag, MicrobeTag) OR AnimalTag")
  }

  @Test fun testProd() {
    testRoundTrip("PROD[1]")
    testRoundTrip("Steel, PROD[1]")
    testRoundTrip("PROD[Steel, 1]")
    testRoundTrip("PROD[Steel OR 1]")
  }

  @Test fun hairy() {
    val parsed : Predicate = PetaformParser.parse(
        "Adjacency<CityTile<Anyone>, OceanTile> OR 1 Adjacency<OceanTile, CityTile<Anyone>>")
    assertThat(parsed).isEqualTo(
        Predicate.or(
            Min(TypeExpression("Adjacency",
                    TypeExpression("CityTile", TypeExpression("Anyone")),
                    TypeExpression("OceanTile"))),
            Min(TypeExpression("Adjacency",
                    TypeExpression("OceanTile"),
                    TypeExpression("CityTile", TypeExpression("Anyone"))),
                1)
        )
    )
  }
}
