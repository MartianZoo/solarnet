package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Predicate.MaxPredicate
import dev.martianzoo.tfm.petaform.api.Predicate.MinPredicate
import org.junit.jupiter.api.Test

class PredicateTest {
  @Test
  fun simpleSourceToApi() {
    assertThat(BetterParser().parsePredicate("Foo"))
        .isEqualTo(MinPredicate(Expression("Foo")))
    assertThat(BetterParser().parsePredicate("3 Foo"))
        .isEqualTo(MinPredicate(Expression("Foo"), 3))
    assertThat(BetterParser().parsePredicate("3"))
        .isEqualTo(MinPredicate(Expression("Megacredit"), 3))
    assertThat(BetterParser().parsePredicate("MAX 3 Foo"))
        .isEqualTo(MaxPredicate(Expression("Foo"), 3))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(MinPredicate(Expression("Foo")).asSource).isEqualTo("Foo")
    assertThat(MinPredicate(Expression("Foo"), 1).asSource).isEqualTo("Foo")
    assertThat(MinPredicate(Expression("Foo"), 3).asSource).isEqualTo("3 Foo")
    assertThat(MinPredicate(Expression("Megacredit"), 3).asSource).isEqualTo("3")
    assertThat(MaxPredicate(Expression("Foo"), 1).asSource).isEqualTo("MAX 1 Foo")
    assertThat(MaxPredicate(Expression("Foo"), 3).asSource).isEqualTo("MAX 3 Foo")
    assertThat(MaxPredicate(Expression("Megacredit"), 3).asSource).isEqualTo("MAX 3")
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
    assertThat(BetterParser().parsePredicate(start).asSource).isEqualTo(end)
  }
}
