package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.types.PClassLoader
import org.junit.jupiter.api.Test

private class CanonEffectsTest {
  fun effectsOf(name: String): List<String> {
    val loader = PClassLoader(Canon, true)
    loader.load(GAME)
    val card = loader.load(cn(name))
    loader.frozen = true
    return card.classEffects.map { "${it.effect}" }
  }

  @Test
  fun sabotage() {
    assertThat(effectsOf("Sabotage"))
        .containsExactly(
            "This: -3 Titanium<Anyone>? OR -4 Steel<Anyone>? OR -7 Megacredit<Anyone>?")
  }

  @Test
  fun energy() {
    assertThat(effectsOf("Energy")).containsExactly("ProductionPhase:: Heat<Owner> FROM This")
  }

  @Test
  fun terraformer() {
    assertThat(effectsOf("Terraformer")).containsExactly("This:: (35 TerraformRating<Owner>: Ok!)")
  }

  @Test
  fun gyropolis() {
    val card = effectsOf("Gyropolis")
    assertThat(card)
        .containsExactly(
            "This: CityTile<Owner, LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>!," +
                " PROD[-2 Energy<Owner>!," +
                " Megacredit<Owner>! / VenusTag<Owner>," +
                " Megacredit<Owner>! / EarthTag<Owner>]")
  }

  @Test
  fun e98() {
    assertThat(effectsOf("Elysium_9_8")).containsExactly("Tile<This> BY Owner: ProjectCard<Owner>!")
  }

  @Test
  fun venusian() {
    assertThat(effectsOf("VenusianAnimals"))
        .containsExactly(
            "ScienceTag<Owner>: Animal<Owner, This>.",
            "End: VictoryPoint<Owner>! / Animal<Owner, This>",
        )
  }

  @Test
  fun convertHeat() {
    assertThat(effectsOf("ConvertHeat"))
        .containsExactly("UseAction1<Owner, This>: -8 Heat<Owner>! THEN TemperatureStep.")
  }

  @Test
  fun teractor() {
    assertThat(effectsOf("Teractor"))
        .containsExactly(
            "This: 60 Megacredit<Owner>!",
            "PlayTag<Owner, Class<EarthTag>>:: -3 Owed<Owner>.",
        )
  }

  @Test
  fun immigrantCity() {
    assertThat(effectsOf("ImmigrantCity"))
        .containsExactly(
            "This: PROD[-Energy<Owner>!, -2 Megacredit<Owner>!]," +
                " CityTile<Owner, LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>!",
            "CityTile<Anyone>: PROD[Megacredit<Owner>!]")
  }

  @Test
  fun titanAirScrapping() {
    assertThat(effectsOf("TitanAirScrapping"))
        .containsExactly(
            "UseAction1<Owner, This>: -Titanium<Owner>! THEN 2 Floater<Owner, This>.",
            "UseAction2<Owner, This>: -2 Floater<Owner, This>! THEN TerraformRating<Owner>!",
            "End: 2 VictoryPoint<Owner>!",
        )
  }

  @Test
  fun amc() {
    assertThat(effectsOf("AsteroidMiningConsortium"))
        .containsExactly(
            "This: PROD[-Titanium<Anyone>!, Titanium<Owner>!]",
            "End: VictoryPoint<Owner>!",
        )
  }

  @Test
  fun pets() {
    assertThat(effectsOf("Pets"))
        .containsExactly(
            "This: Animal<Owner, This>.",
            "-Animal<Owner, This>:: Die!",
            "CityTile<Anyone>: Animal<Owner, This>.",
            "End: VictoryPoint<Owner>! / 2 Animal<Owner, This>",
        )
  }

  @Test
  fun aquiferPumping() {
    assertThat(effectsOf("AquiferPumping"))
        .containsExactly(
            "UseAction1<Owner, This>:: Accept<Owner, Class<Steel>>.",
            "UseAction1<Owner, This>: -8 Megacredit<Owner>! THEN OceanTile<WaterArea>.",
        )
  }

  @Test
  fun floaterPrototypes() {
    assertThat(effectsOf("FloaterPrototypes")).containsExactly("This: 2 Floater<Owner>.")
  }
}
