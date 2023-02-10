package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Companion.effect
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

private class CanonEffectsTest {
  fun load(name: String): PClass {
    val loader = PClassLoader(Canon, true)
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

  @Disabled
  @Test
  fun gyropolis() {
    val card = load("Gyropolis")
    assertThat(card.classEffects)
        .containsExactly(
            effect(
                "This: CityTile<LandArea(HAS MAX 0 CityTile<Anyone>), Owner>!, " +
                    "-2 Production<Owner, Class<Energy>>, " +
                    "Production<Owner, Class<Megacredit>>! / VenusTag<Owner>, " +
                    "Production<Owner, Class<Megacredit>>! / EarthTag<Owner>"))
  }

  @Disabled
  @Test
  fun e98() {
    assertThat(load("Elysium_9_8").classEffects)
        .containsExactly(effect("Tile<This> BY Player: ProjectCard<Player>!"))
  }

  @Disabled
  @Test
  fun venusian() {
    assertThat(load("VenusianAnimals").classEffects)
        .containsExactly(
            effect("ScienceTag<Owner>: Animal<This, Owner>."),
            effect("End: VictoryPoint<Owner>! / Animal<This, Owner>"),
        )
  }

  @Disabled
  @Test
  fun convertHeat() {
    assertThat(load("ConvertHeat").classEffects)
        .containsExactly(
            effect("UseAction1<Player, This>: -8 Heat<Player>! THEN TemperatureStep."),
        )
  }

  @Disabled
  @Test
  fun teractor() {
    assertThat(load("Teractor").classEffects)
        .containsExactly(
            effect("This: 60 Megacredit<Owner>!"),
            effect("PlayTag<Class<EarthTag>, Owner>:: -3 Owed<Owner>."),
        )
  }

  @Disabled
  @Test
  fun immigrantCity() {
    assertThat(load("ImmigrantCity").classEffects)
        .containsExactly(
            effect(
                "This: -Production<Owner, Class<Energy>>!, -2 Production<Owner, " +
                    "Class<Megacredit>>!, CityTile<LandArea(HAS MAX 0 CityTile<Anyone>), Owner>!"),
            effect("CityTile<Anyone>: Production<Owner, Class<Megacredit>>!"),
        )
  }

  @Disabled
  @Test
  fun titanAirScrapping() {
    assertThat(load("TitanAirScrapping").classEffects)
        .containsExactly(
            effect("UseAction1<This, Owner>: -Titanium<Owner>! THEN 2 Floater<This, Owner>."),
            effect(
                "UseAction2<This, Owner>: -2 Floater<This, Owner>! THEN TerraformRating<Owner>!"),
            effect("End: 2 VictoryPoint<Owner>!"),
        )
  }

  @Disabled
  @Test
  fun amc() {
    assertThat(load("AsteroidMiningConsortium").classEffects)
        .containsExactly(
            effect(
                "This: -Production<Anyone, Class<Titanium>>!, Production<Class<Titanium>, Owner>!"),
            effect("End: VictoryPoint<Owner>!"),
        )
  }

  @Disabled
  @Test
  fun pets() {
    assertThat(load("Pets").classEffects)
        .containsExactly(
            effect("This: Animal<This, Owner>."),
            effect("-Animal<This, Owner>:: Die!"),
            effect("CityTile<Anyone>: Animal<This, Owner>."),
            effect("End: VictoryPoint<Owner>! / 2 Animal<This, Owner>"),
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

  @Disabled
  @Test
  fun floaterPrototypes() {
    assertThat(load("FloaterPrototypes").classEffects)
        .containsExactly(
            effect("This: 2 Floater<Owner, ResourcefulCard<Class<Floater>>, Owner>."),
        )
  }

  @Disabled
  @Test
  fun merger() {
    assertThat(load("Merger").classEffects)
        .containsExactly(
            effect(
                "This: CorporationCard<Owner>, PlayCard<Owner, Class<CorporationCard>, " +
                    "Class<CardFront>>!, -42 Megacredit<Owner>"),
        )
  }
}
