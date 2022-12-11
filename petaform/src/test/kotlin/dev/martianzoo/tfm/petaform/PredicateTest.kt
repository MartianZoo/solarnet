package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.Predicate.Max
import dev.martianzoo.tfm.petaform.Predicate.Min
import org.junit.jupiter.api.Test

// Most testing is done by AutoTest
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
    Foo
  """.trimIndent().split('\n')

  @Test fun testSampleStrings() {
    inputs.forEach { testRoundTrip<Predicate>(it) }
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
