package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Predicate
import dev.martianzoo.tfm.petaform.api.Predicate.Max
import dev.martianzoo.tfm.petaform.api.Predicate.Min
import org.junit.jupiter.api.Test

class PredicateTest {
  @Test
  fun simpleSourceToApi() {
    assertThat(PetaformParser.parse<Predicate>("Foo"))
        .isEqualTo(Min(Expression("Foo")))
    assertThat(PetaformParser.parse<Predicate>("3 Foo"))
        .isEqualTo(Min(Expression("Foo"), 3))
    assertThat(PetaformParser.parse<Predicate>("3"))
        .isEqualTo(Min(Expression("Megacredit"), 3))
    assertThat(PetaformParser.parse<Predicate>("MAX 3 Foo"))
        .isEqualTo(Max(Expression("Foo"), 3))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(Min(Expression("Foo")).petaform).isEqualTo("Foo")
    assertThat(Min(Expression("Foo"), 1).petaform).isEqualTo("Foo")
    assertThat(Min(Expression("Foo"), 3).petaform).isEqualTo("3 Foo")
    assertThat(Min(Expression("Megacredit"), 3).petaform).isEqualTo("3")
    assertThat(Max(Expression("Foo"), 1).petaform).isEqualTo("MAX 1 Foo")
    assertThat(Max(Expression("Foo"), 3).petaform).isEqualTo("MAX 3 Foo")
    assertThat(Max(Expression("Megacredit"), 3).petaform).isEqualTo("MAX 3")
  }

  @Test
  fun roundTrips() {
    testRoundTrip("OceanTile")
    testRoundTrip("OceanTile<WaterArea>")
    testRoundTrip("MAX 1024 OceanTile")
    testRoundTrip("CityTile<LandArea>, GreeneryTile<WaterArea>")
    testRoundTrip("PlantTag, MicrobeTag OR AnimalTag")

    testRoundTrip("1 OceanTile", "OceanTile")
  }

  private fun testRoundTrip(start: String, end: String = start) {
    val parse: Predicate = PetaformParser.parse(start)
    assertThat(parse.petaform).isEqualTo(end)
  }

  @Test fun testProd() {
    testRoundTrip("PROD[1]")
    testRoundTrip("Steel, PROD[1]")
    testRoundTrip("PROD[Steel, 1]")
  }

  @Test fun hairy() {
    val parsed : Predicate = PetaformParser.parse(
        "Adjacency<CityTile<Anyone>, OceanTile> OR Adjacency<OceanTile, CityTile<Anyone>>")
    // This is kinda absurd
    assertThat(parsed).isEqualTo(
        Predicate.Or(
            Min(Expression("Adjacency",
                    Expression("CityTile",
                        Expression("Anyone")),
                    Expression("OceanTile")),
                1),
            Min(Expression("Adjacency",
                    Expression("OceanTile"),
                    Expression("CityTile",
                        Expression("Anyone"))),
                1)
        )
    )
  }
}
