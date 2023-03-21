package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.ENGINE
import dev.martianzoo.tfm.api.SpecialClassNames.GLOBAL_PARAMETER
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.types.MClassLoader
import org.junit.jupiter.api.Test

private class CanonEffectsTest {
  fun effectsOf(name: String): List<String> {
    val loader = MClassLoader(Canon, true)
    loader.load(ENGINE)
    loader.load(OK) // TODO why?
    loader.load(GLOBAL_PARAMETER)
    val card = loader.load(cn(name))
    loader.frozen = true
    return card.classEffects.map { "${it.effect.element}" }
  }

  @Test
  fun sabotage() {
    assertThat(effectsOf("Sabotage"))
        .containsExactly(
            "This:: -ProjectCard<Owner>!",
            "This: PlayedEvent<Owner, Class<This>> FROM This!",
            "This: -3 Titanium<Anyone>? OR -4 Steel<Anyone>? OR -7 Megacredit<Anyone>?")
  }

  @Test
  fun energy() {
    assertThat(effectsOf("Energy")).containsExactly("ProductionPhase:: Heat<Owner> FROM This!")
  }

  @Test
  fun terraformer() {
    assertThat(effectsOf("Terraformer")).containsExactly("This:: (35 TerraformRating<Owner>: Ok!)")
  }

  @Test
  fun gyropolis() {
    assertThat(effectsOf("Gyropolis").drop(2))
        .containsExactly(
            // "This: CityTag<Owner>!, BuildingTag<Owner>!",
            "This: CityTile<Owner, LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>!," +
                " PROD[-2 Energy<Owner>!," +
                " Megacredit<Owner>! / VenusTag<Owner>," +
                " Megacredit<Owner>! / EarthTag<Owner>]")
  }

  @Test
  fun e98() {
    assertThat(effectsOf("Elysium_9_8"))
        .containsExactly(
            "Tile<This>:: @createAdjacencies(This)",
            "Tile<This> BY Owner: ProjectCard<Owner>!",
        )
  }

  @Test
  fun venusian() {
    assertThat(effectsOf("VenusianAnimals"))
        .containsExactly(
            "This:: -ProjectCard<Owner>!",
            "This:: VenusTag<Owner, This>!, ScienceTag<Owner, This>!, AnimalTag<Owner, This>!",
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
            "This:: -CorporationCard<Owner>!",
            "This:: EarthTag<Owner, This>!",
            "This: 60 Megacredit<Owner>!",
            "PlayTag<Owner, Class<EarthTag>>:: -3 Owed<Owner, Class<Megacredit>>.",
        )
  }

  @Test
  fun immigrantCity() {
    assertThat(effectsOf("ImmigrantCity").drop(2))
        .containsExactly(
            // "This: CityTag<Owner>!, BuildingTag<Owner>!",
            "This: PROD[-Energy<Owner>!, -2 Megacredit<Owner>!]," +
                " CityTile<Owner, LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>!",
            "CityTile<Anyone>: PROD[Megacredit<Owner>!]")
  }

  @Test
  fun titanAirScrapping() {
    assertThat(effectsOf("TitanAirScrapping").drop(1))
        .containsExactly(
            "This:: JovianTag<Owner, This>!",
            "UseAction1<Owner, This>: -Titanium<Owner>! THEN 2 Floater<Owner, This>.",
            "UseAction2<Owner, This>: -2 Floater<Owner, This>! THEN TerraformRating<Owner>!",
            "End: 2 VictoryPoint<Owner>!",
        )
  }

  @Test
  fun amc() {
    assertThat(effectsOf("AsteroidMiningConsortium"))
        .containsExactly(
            "This:: -ProjectCard<Owner>!",
            "This:: JovianTag<Owner, This>!",
            "This: PROD[-Titanium<Anyone>!, Titanium<Owner>!]",
            "End: VictoryPoint<Owner>!",
        )
  }

  @Test
  fun pets() {
    assertThat(effectsOf("Pets").drop(2))
        .containsExactly(
            // "This: EarthTag<Owner>!, AnimalTag<Owner>!",
            "This: Animal<Owner, This>.",
            "-Animal<Owner, This>:: Die!",
            "CityTile<Anyone>: Animal<Owner, This>.",
            "End: VictoryPoint<Owner>! / 2 Animal<Owner, This>",
        )
  }

  @Test
  fun aquiferPumping() {
    assertThat(effectsOf("AquiferPumping").drop(1))
        .containsExactly(
            "This:: BuildingTag<Owner, This>!",
            "UseAction1<Owner, This>:: Accept<Owner, Class<Steel>>.",
            "UseAction1<Owner, This>: -8 Megacredit<Owner>! THEN OceanTile<WaterArea>.", // TODO
        )
  }

  @Test
  fun floaterPrototypes() {
    assertThat(effectsOf("FloaterPrototypes"))
        .containsExactly(
            "This:: -ProjectCard<Owner>!",
            "This:: ScienceTag<Owner, This>!",
            "This: PlayedEvent<Owner, Class<This>> FROM This!", // TODO why this didn't get subst
            "This: 2 Floater<Owner>.")
  }
}
