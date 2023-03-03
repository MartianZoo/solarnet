package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Companion.effect
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class CanonEffectsTest {
  fun load(name: String): PClass {
    val loader = PClassLoader(Canon, true)
    loader.load(GAME)
    val card = loader.load(cn(name))
    loader.frozen = true
    return card
  }

  @Test
  fun sabotage() {
    assertThat(load("Sabotage").classEffects)
        .containsExactly(
            effect("This: -3 Titanium<Anyone>? OR -4 Steel<Anyone>? OR -7 Megacredit<Anyone>?"),
        )
  }

  @Test
  fun energy() {
    assertThat(load("Energy").classEffects)
        .containsExactly(
            effect("ProductionPhase:: Heat<Owner> FROM This"),
        )
  }

  @Test
  fun terraformer() {
    assertThat(load("Terraformer").classEffects)
        .containsExactly(
            effect("This:: (35 TerraformRating<Owner>: Ok!)"),
        )
  }

  @Test
  fun polarExplorer() {
    assertThat(load("PolarExplorer").classEffects)
        .containsExactly(effect("This:: (3 OwnedTile<Owner, MarsArea(HAS 8 Row)>: Ok!)"))
  }

  @Test
  fun gyropolis() {
    val card = load("Gyropolis")
    assertThat(card.classEffects)
        .containsExactly(
            effect(
                // TODO remove parens
                "This: CityTile<Owner, LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>!, " +
                    "(-2 Production<Owner, Class<Energy>>!, " +
                    "Production<Owner, Class<Megacredit>>! / VenusTag<Owner>, " +
                    "Production<Owner, Class<Megacredit>>! / EarthTag<Owner>)"))
  }

  @Test
  fun e98() {
    assertThat(load("Elysium_9_8").classEffects)
        .containsExactly(effect("Tile<This> BY Owner: ProjectCard<Owner>!"))
  }

  @Test
  fun venusian() {
    assertThat(load("VenusianAnimals").classEffects)
        .containsExactly(
            effect("ScienceTag<Owner>: Animal<Owner, This>."),
            effect("End: VictoryPoint<Owner>! / Animal<Owner, This>"),
        )
  }

  @Test
  fun convertHeat() {
    assertThat(load("ConvertHeat").classEffects)
        .containsExactly(
            effect("UseAction1<Owner, This>: -8 Heat<Owner>! THEN TemperatureStep."),
        )
  }

  @Test
  fun teractor() {
    assertThat(load("Teractor").classEffects)
        .containsExactly(
            effect("This: 60 Megacredit<Owner>!"),
            effect("PlayTag<Owner, Class<EarthTag>>:: -3 Owed<Owner>."),
        )
  }

  @Test
  fun immigrantCity() {
    assertThat(load("ImmigrantCity").classEffects.toStrings())
        .containsExactly(
            "This: (-Production<Owner, Class<Energy>>!, -2 Production<Owner, Class<Megacredit>>!)" +
                ", CityTile<Owner, LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>!",
            "CityTile<Anyone>: Production<Owner, Class<Megacredit>>!")
  }

  @Test
  fun titanAirScrapping() {
    assertThat(load("TitanAirScrapping").classEffects)
        .containsExactly(
            effect("UseAction1<Owner, This>: -Titanium<Owner>! THEN 2 Floater<Owner, This>."),
            effect(
                "UseAction2<Owner, This>: -2 Floater<Owner, This>! THEN TerraformRating<Owner>!"),
            effect("End: 2 VictoryPoint<Owner>!"),
        )
  }

  @Test
  fun amc() {
    assertThat(load("AsteroidMiningConsortium").classEffects)
        .containsExactly(
            effect(
                "This: -Production<Anyone, Class<Titanium>>!, Production<Owner, Class<Titanium>>!"),
            effect("End: VictoryPoint<Owner>!"),
        )
  }

  @Test
  fun pets() {
    assertThat(load("Pets").classEffects)
        .containsExactly(
            effect("This: Animal<Owner, This>."),
            effect("-Animal<Owner, This>:: Die!"),
            effect("CityTile<Anyone>: Animal<Owner, This>."),
            effect("End: VictoryPoint<Owner>! / 2 Animal<Owner, This>"),
        )
  }

  @Test
  fun aquiferPumping() {
    assertThat(load("AquiferPumping").classEffects)
        .containsExactly(
            effect("UseAction1<Owner, This>:: Accept<Owner, Class<Steel>>."),
            effect("UseAction1<Owner, This>: -8 Megacredit<Owner>! THEN OceanTile<WaterArea>."),
        )
  }

  @Test
  fun floaterPrototypes() {
    assertThat(load("FloaterPrototypes").classEffects)
        .containsExactly(
            effect("This: 2 Floater<Owner>."),
        )
  }
}
