package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.Predicate.Max
import dev.martianzoo.tfm.petaform.Predicate.Min
import org.junit.jupiter.api.Test

class PredicateTest {
  fun printEm() {
    val set = sortedSetOf<String>(Comparator.comparing { it.length })
    val gen = PetaformGenerator()
    for (i in 1..10000) {
      set += gen.makeRandomNode<Predicate>().toString()
    }
    set.forEach(::println)
  }

  @Test fun barrage() {
    val gen = PetaformGenerator()
    for (i in 1..10000) {
      assertThat(gen.testRandom<Predicate>()).isTrue()
    }
  }

  @Test
  fun simpleSourceToApi() {
    assertThat(PetaformParser.parse<Predicate>("Foo"))
        .isEqualTo(Min(TypeExpression("Foo")))
    assertThat(PetaformParser.parse<Predicate>("3 Foo"))
        .isEqualTo(Min(TypeExpression("Foo"), 3))
    assertThat(PetaformParser.parse<Predicate>("3"))
        .isEqualTo(Min(TypeExpression("Megacredit"), 3))
    assertThat(PetaformParser.parse<Predicate>("MAX 3 Foo"))
        .isEqualTo(Max(TypeExpression("Foo"), 3))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(Min(TypeExpression("Foo")).toString()).isEqualTo("Foo")
    assertThat(Min(TypeExpression("Foo"), 1).toString()).isEqualTo("Foo")
    assertThat(Min(TypeExpression("Foo"), 3).toString()).isEqualTo("3 Foo")
    assertThat(Min(TypeExpression("Megacredit"), 3).toString()).isEqualTo("3")
    assertThat(Max(TypeExpression("Foo"), 0).toString()).isEqualTo("MAX 0 Foo")
    assertThat(Max(TypeExpression("Foo"), 1).toString()).isEqualTo("MAX 1 Foo")
    assertThat(Max(TypeExpression("Foo"), 3).toString()).isEqualTo("MAX 3 Foo")
    assertThat(Max(TypeExpression("Megacredit"), 3).toString()).isEqualTo("MAX 3")
  }

  val inputs = """
    Abc
    3 Xyz
    =3 Xyz
    =11 Xyz
    Bat<Bat>
    MAX 1 Bat
    MAX 11 Xyz
    =1 Qux<Baz>
    =11 Wau<Wau>
    Wau<Bar, Foo>
    MAX 3 Abc<Wau>
    MAX 11 Bar<Bar>
    Wau OR MAX 1 Wau
    11 Baz, MAX 1 Abc
    Bat OR =1 Wau<Xyz>
    =11 Qux(HAS =1 Xyz)
    3 Abc<Wau, Bat, Wau>
    =1 Abc<Baz<Baz, Xyz>>
    Xyz(HAS =1 Abc) OR Bat
    Foo(HAS MAX 3 Bar<Bar>)
    MAX 3 Baz(HAS MAX 3 Foo)
    3 Foo<Foo<Xyz>, Bat, Abc>
    11 Qux(HAS MAX 1 Qux<Baz>)
    3 Bat<Abc> OR Baz<Bat, Bar>
    MAX 1 Bat<Qux(HAS Qux), Baz>
    3 Baz(HAS Qux(HAS MAX 3 Wau))
    (3 Bat OR MAX 11 Bat) OR 3 Qux
    11 Baz<Foo(HAS MAX 1 Bat), Baz>
    MAX 3 Bat OR (Wau<Baz> OR 3 Bar)
    Foo<Foo, Bar, Xyz> OR =1 Wau<Qux>
    11 Xyz OR Wau(HAS =3 Baz) OR 3 Qux
    (3 Foo OR =1 Bar<Wau>) OR MAX 3 Baz
    MAX 11 Abc OR MAX 3 Wau OR MAX 3 Xyz
    Bar<Wau<Qux>(HAS =1 Abc)>(HAS =1 Xyz)
    MAX 3 Baz, MAX 1 Bar, =1 Qux<Baz, Foo>
    Xyz<Baz>, 11 Baz<Qux<Foo>> OR MAX 3 Xyz
    MAX 1 Qux(HAS MAX 1 Bat), MAX 1 Baz, Abc
    3 Foo OR Foo OR MAX 11 Bar<Foo, Bat, Qux>
    MAX 3 Abc OR 3 Bat OR (MAX 1 Bat OR 3 Bar)
    Bat OR (Wau(HAS MAX 3 Bat), 11 Baz, 11 Bat)
    MAX 1 Abc<Xyz, Abc, Baz<Bat, Xyz<Wau, Bar>>>
    (11 Qux OR 3 Baz) OR MAX 3 Qux(HAS MAX 3 Xyz)
    Xyz<Bat<Bat<Bat, Foo>>, Bat, Qux(HAS =11 Xyz)>
    =1 Wau<Foo(HAS MAX 11 Qux<Bat, Bar<Abc>, Bat>)>
    Wau OR Xyz OR (MAX 11 Foo, MAX 11 Baz) OR 11 Baz
    MAX 3 Wau OR Bar(HAS Foo OR (3 Bar OR MAX 1 Foo))
    3 Bar<Abc, Wau<Baz<Bar<Bat>, Bar, Baz>, Xyz>, Abc>
    Wau<Baz<Bat>, Bar, Baz> OR (MAX 1 Bat<Qux>, =3 Foo)
    11 Xyz(HAS =1 Wau<Qux>(HAS 3 Abc)) OR =11 Baz OR Bar
    MAX 1 Xyz, Qux<Bat, Qux>, Xyz OR Xyz, 3 Qux<Bar<Baz>>
    MAX 1 Xyz OR MAX 3 Bat<Wau, Qux, Qux<Baz>(HAS 11 Abc)>
    (Bar, Bat, Bar<Abc, Xyz, Xyz>) OR (3 Xyz OR MAX 11 Abc)
    11 Abc, 11 Abc, MAX 1 Qux OR 3 Bar, (MAX 11 Foo, 11 Bat)
    3 Xyz<Bar> OR MAX 11 Bar OR (11 Foo, =3 Wau) OR MAX 1 Abc
    =11 Bat(HAS 11 Baz<Qux>), =11 Qux OR 11 Foo, 3 Foo, =3 Bar
    Baz<Wau, Abc, Bar>(HAS (Wau, ((Xyz, Bar), MAX 1 Qux)), Baz)
    11 Foo<Qux, Bar, Abc<Wau, Foo, Abc<Abc(HAS Xyz), Bar, Abc>>>
    MAX 11 Wau<Bat, Baz, Xyz>, =1 Abc<Bar, Xyz<Abc>> OR MAX 1 Abc
    (Bat OR Bar OR 3 Foo OR (3 Xyz, MAX 11 Foo, Bat)) OR MAX 3 Baz
    (11 Foo OR 3 Abc<Bat>) OR (Bar OR 11 Abc<Wau, Bar, Abc> OR Xyz)
    =3 Bat<Bar<Wau, Bat<Qux>, Bat>(HAS 3 Xyz)> OR Foo<Bar, Foo, Qux>
    11 Xyz OR MAX 11 Baz<Bat<Bar, Bat, Abc<Bar<Xyz, Abc>>>, Baz, Qux>
    11 Wau<Abc<Bat<Qux, Abc>>> OR Baz, 3 Qux OR =1 Bat, MAX 1 Abc<Wau>
    Baz OR 3 Wau<Abc(HAS Xyz OR (MAX 1 Baz<Xyz, Abc, Baz>, MAX 1 Abc))>
    (Foo OR Abc, MAX 1 Wau) OR MAX 3 Wau OR (=1 Bat OR Bar) OR MAX 1 Wau
    3 Bar<Abc<Baz, Bat<Foo>, Foo(HAS Abc)>, Qux, Bar(HAS Abc)>, MAX 3 Xyz
    3 Qux OR MAX 11 Qux<Abc(HAS Baz)> OR ((MAX 1 Baz OR Foo) OR MAX 1 Wau)
    MAX 1 Abc OR (3 Abc OR MAX 1 Wau OR Wau, 3 Foo, =11 Qux, Bar<Bat<Xyz>>)
    Wau<Xyz<Abc<Baz<Bar>>, Bat(HAS 11 Foo<Bar, Wau, Wau> OR Baz<Bar>)>, Abc>
    Bat<Xyz, Baz, Foo<Bar, Bat<Bar, Foo, Baz(HAS MAX 1 Wau<Baz, Bat, Bar>)>>>
    (Foo<Foo> OR Baz OR Bar, 11 Wau<Foo, Bat> OR 3 Qux, MAX 11 Bat), MAX 1 Wau
    Wau(HAS 11 Qux<Bat<Bar, Xyz>(HAS 3 Baz OR 11 Foo OR MAX 11 Xyz<Baz, Abc>)>)
    =11 Qux(HAS 11 Bar(HAS Bar<Abc> OR Foo) OR (=11 Bat<Baz> OR Xyz), MAX 1 Abc)
    ((MAX 11 Abc OR MAX 1 Xyz<Bat<Qux, Foo, Abc>>) OR MAX 11 Baz, 11 Abc), =1 Foo
    Bar<Bar<Bar, Wau<Foo>(HAS MAX 1 Xyz<Bar>), Foo<Abc<Bat, Abc>, Bat, Foo>>, Xyz>
    3 Xyz(HAS 11 Baz OR 3 Qux OR MAX 11 Bar OR (Xyz OR (MAX 3 Bat OR 11 Wau<Qux>)))
    Xyz OR (3 Bar<Bat, Qux<Bar>, Bar>, MAX 3 Xyz<Xyz, Foo, Wau>) OR 11 Abc OR 11 Abc
    3 Foo(HAS 11 Wau, 3 Bar<Foo<Abc(HAS 11 Wau, 11 Qux), Baz, Baz<Wau>>, Wau>, 3 Abc)
    Bar(HAS 11 Foo), =1 Bar<Abc, Abc<Qux, Xyz<Bat, Abc<Qux>>, Bar>>(HAS =1 Foo OR Bat)
    (Xyz, Abc), (MAX 3 Bat<Baz, Xyz>, MAX 11 Wau(HAS =1 Qux)), MAX 1 Bar(HAS MAX 1 Bar)
    (=11 Xyz<Xyz, Qux, Wau<Qux(HAS Bat OR 11 Baz), Bat, Wau>> OR Qux) OR 3 Qux OR 11 Bat
    Baz OR Bat OR Bat OR MAX 3 Xyz<Wau, Bat<Bat, Bar, Wau<Foo>>, Xyz>(HAS MAX 1 Wau<Wau>)
    MAX 1 Bat(HAS MAX 1 Qux<Baz<Bat, Foo<Baz, Qux, Baz>>(HAS =1 Qux)>(HAS 11 Abc, =3 Bat))
    (MAX 3 Xyz<Xyz<Bat, Wau<Qux>(HAS =11 Qux<Baz, Bat, Qux>)>> OR MAX 1 Baz, 11 Baz) OR Wau
    MAX 1 Bar(HAS 3 Qux<Bat>), MAX 1 Xyz, =11 Foo<Xyz<Xyz>>, (Baz, =3 Bar, 11 Wau, Xyz<Bar>)
    11 Qux(HAS 11 Bar, 3 Bat, 11 Wau, (MAX 1 Abc, (11 Xyz(HAS Foo<Xyz>), MAX 11 Xyz) OR Xyz))
    (3 Wau, MAX 3 Bar<Foo>, 11 Abc, 3 Bar) OR Bat<Wau, Bar<Abc, Bat, Xyz>, Baz> OR =1 Foo<Bat>
    3 Foo<Qux, Foo>(HAS Xyz, MAX 1 Qux, Wau(HAS MAX 1 Xyz), MAX 1 Baz<Wau, Xyz<Baz, Bar<Wau>>>)
    MAX 3 Abc(HAS 11 Bat OR (3 Abc OR 11 Bar) OR Bar<Bar(HAS =1 Wau), Foo(HAS Wau), Foo> OR Abc)
    ((MAX 1 Abc OR (((3 Xyz OR MAX 1 Wau) OR Baz) OR MAX 3 Xyz<Qux>)) OR Baz OR MAX 1 Baz) OR Abc
    =3 Bat<Foo> OR MAX 11 Foo OR Wau OR (3 Wau<Foo, Bat, Bat<Baz<Abc, Xyz>, Abc>>, MAX 3 Bat), Bat
    ((Abc OR Bar) OR 11 Xyz) OR (Xyz<Bar, Xyz> OR 3 Xyz<Bar>, =3 Wau, =3 Wau<Bat, Qux<Xyz>> OR Wau)
    =1 Bat OR MAX 1 Bar<Qux, Qux> OR (Wau OR MAX 1 Foo OR (MAX 3 Xyz<Bar, Abc> OR Xyz)) OR MAX 1 Wau
    11 Bar OR (Xyz<Wau(HAS MAX 11 Bat<Qux, Wau>), Abc, Xyz> OR (=3 Xyz OR Xyz) OR 11 Bar(HAS =1 Abc))
    Wau<Abc, Qux<Bar<Foo<Foo>, Bat, Wau<Xyz>>>(HAS =1 Bar<Wau<Abc>>, 11 Bar<Xyz<Bar, Bat>, Foo, Bat>)>
    Xyz<Xyz<Abc>, Bat, Wau>(HAS =3 Bar<Xyz, Baz, Baz<Foo(HAS Abc<Xyz, Foo(HAS MAX 3 Foo)>), Xyz, Abc>>)
    =3 Wau OR (3 Abc<Bat>, (MAX 11 Baz OR MAX 1 Xyz, 11 Qux OR ((11 Qux<Foo>, MAX 1 Wau) OR MAX 1 Bat)))
  """.trimIndent().split('\n')

  @Test fun test() {
    assertThat(inputs.filterNot { checkRoundTrip(it) }).isEmpty()
  }

  private fun testRoundTrip(start: String, end: String = start) =
      assertThat(PetaformParser.parse<Predicate>(start).toString()).isEqualTo(end)

  private fun checkRoundTrip(start: String, end: String = start) =
      PetaformParser.parse<Predicate>(start).toString() == end

  @Test
  fun roundTrips() {
    testRoundTrip("0")
    testRoundTrip("1")
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

  @Test fun testProd() {
    testRoundTrip("PROD[1]")
    testRoundTrip("Steel, PROD[1]")
    testRoundTrip("PROD[Steel, 1]")
    testRoundTrip("PROD[Steel OR 1]")
  }

  @Test fun hairy() {
    val parsed : Predicate = PetaformParser.parse(
        "Adjacency<CityTile<Anyone>, OceanTile> OR Adjacency<OceanTile, CityTile<Anyone>>")
    // This is kinda absurd
    assertThat(parsed).isEqualTo(
        Predicate.Or(
            Min(TypeExpression("Adjacency",
                    TypeExpression("CityTile",
                        TypeExpression("Anyone")),
                    TypeExpression("OceanTile")),
                1),
            Min(TypeExpression("Adjacency",
                    TypeExpression("OceanTile"),
                    TypeExpression("CityTile",
                        TypeExpression("Anyone"))),
                1)
        )
    )
  }
}
