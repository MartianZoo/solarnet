package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.SystemClasses.OK
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.types.MClassLoader
import dev.martianzoo.types.MClassTable
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

private class CanonEffectsTest {
  fun classEffectsOf(name: String): List<String> {
    val loader = MClassLoader(Canon)
    loader.load(OK)
    loader.load(cn(name))
    return classEffectsOf(name, loader.freeze())
  }

  fun classEffectsOf(name: String, table: MClassTable) =
      table.getClass(cn(name)).classEffects.toStrings()

  @Test
  fun sabotage() {
    assertThat(classEffectsOf("Sabotage"))
        .containsExactly(
            "This: PlayedEvent<Owner, Class<This>> FROM This!",
            "This: -3 Titanium<Anyone>? OR -4 Steel<Anyone>? OR -7 Megacredit<Anyone>?")
  }

  @Test
  fun terraformer() {
    assertThat(classEffectsOf("Terraformer"))
        .containsExactly("This:: (35 TerraformRating<Owner>: Ok)", "End: 5 VictoryPoint<Owner>!")
  }

  @Test
  fun gyropolis() {
    assertThat(classEffectsOf("Gyropolis"))
        .containsExactly(
            "This:: CityTag<Owner, This>!, BuildingTag<Owner, This>!",
            "This: CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>), Owner>!," +
                " PROD[-2 Energy<Owner>!," +
                " Megacredit<Owner>! / VenusTag<Owner>," +
                " Megacredit<Owner>! / EarthTag<Owner>]")
  }

  @Test
  fun e98() {
    assertThat(classEffectsOf("Elysium_9_8"))
        .containsExactly(
            "Tile<This>:: CreateAdjacencies<This>!",
            "Tile<This> BY Owner: ProjectCard<Owner>!",
        )
  }

  @Test
  fun venusian() {
    assertThat(classEffectsOf("VenusianAnimals"))
        .containsExactly(
            "This:: VenusTag<Owner, This>!, ScienceTag<Owner, This>!, AnimalTag<Owner, This>!",
            "ScienceTag<Owner>: Animal<Owner, This>.",
            "End: VictoryPoint<Owner>! / Animal<Owner, This>",
        )
  }

  @Test
  fun convertHeat() {
    assertThat(classEffectsOf("ConvertHeatSA"))
        .containsExactly("UseAction1<Owner, This>: -8 Heat<Owner>! THEN TemperatureStep.")
  }

  @Test
  fun teractor() {
    assertThat(classEffectsOf("Teractor"))
        .containsExactly(
            "This:: EarthTag<Owner, This>!",
            "This: 60 Megacredit<Owner>!",
            "PlayTag<Owner, Class<EarthTag>>:: -3 Owed<Owner, Class<Megacredit>>.",
        )
  }

  @Test
  fun immigrantCity() {
    assertThat(classEffectsOf("ImmigrantCity"))
        .containsExactly(
            "This:: CityTag<Owner, This>!, BuildingTag<Owner, This>!",
            "This: PROD[-Energy<Owner>!, -2 Megacredit<Owner>!]," +
                " CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>), Owner>!",
            "CityTile<Anyone>: PROD[Megacredit<Owner>!]")
  }

  @Test
  fun titanAirScrapping() {
    assertThat(classEffectsOf("TitanAirScrapping"))
        .containsExactly(
            "This:: JovianTag<Owner, This>!",
            "UseAction1<Owner, This>: -Titanium<Owner>! THEN 2 Floater<Owner, This>.",
            "UseAction2<Owner, This>: -2 Floater<Owner, This>! THEN TerraformRating<Owner>!",
            "End: 2 VictoryPoint<Owner>!",
        )
  }

  @Test
  fun amc() {
    assertThat(classEffectsOf("AsteroidMiningConsortium"))
        .containsExactly(
            "This:: JovianTag<Owner, This>!",
            "This: PROD[-Titanium<Anyone>!, Titanium<Owner>!]",
            "End: VictoryPoint<Owner>!",
        )
  }

  @Test
  fun pets() {
    assertThat(classEffectsOf("Pets"))
        .containsExactly(
            "This:: EarthTag<Owner, This>!, AnimalTag<Owner, This>!",
            "This: Animal<Owner, This>.",
            "-Animal<Owner, This>:: Die!",
            "CityTile<Anyone>: Animal<Owner, This>.",
            "End: VictoryPoint<Owner>! / 2 Animal<Owner, This>",
        )
  }
}
