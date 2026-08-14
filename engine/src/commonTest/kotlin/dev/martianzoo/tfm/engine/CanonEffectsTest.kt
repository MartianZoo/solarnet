package dev.martianzoo.tfm.engine

import dev.martianzoo.api.SystemClasses.OK
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Transformers
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.types.ClassLoader
import dev.martianzoo.types.ClassTable
import dev.martianzoo.util.toStrings
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonEffectsTest {
  private fun classEffectsOf(name: String): List<String> {
    val loader = ClassLoader(Canon)
    loader.load(OK)
    loader.load(cn(name))
    return classEffectsOf(cn(name), loader.freeze())
  }

  private fun classEffectsOf(name: dev.martianzoo.pets.ast.ClassName, classTable: ClassTable) =
      Transformers(classTable).classEffects(classTable.getClass(name)).toStrings()

  @Test
  fun compiledByOwnerEffectsHaveResolvableOwnerBindings() {
    val table = ClassLoader(Canon).loadEverything()
    val transformers = Transformers(table)
    val compiledByOwnerEffects =
        table.allClasses().flatMap { mClass ->
          transformers
              .classEffects(mClass)
              .filter {
                val by = it.trigger as? ByTrigger
                by?.by?.className == OWNER && !by.by.complement
              }
              .map { mClass.className to it }
        }

    compiledByOwnerEffects.isNotEmpty() shouldBe true
    compiledByOwnerEffects.forEach { (className, effect) ->
      withClue("$className: $effect") {
        (OWNER in effect.instruction) shouldBe true
        (OWNER in replaceOwnerWith(PLAYER1).transform(effect.instruction)) shouldBe false
      }
    }
  }

  @Test
  fun sabotage() {
    classEffectsOf("Card121")
        .shouldContainExactlyInAnyOrder(
            "This: PlayedEvent<Owner, Class<This>> FROM This.",
            "This: -3 Titanium<Anyone>? OR -4 Steel<Anyone>? OR -7 Megacredit<Anyone>?",
        )
  }

  @Test
  fun terraformer() {
    classEffectsOf("MilestoneBM1")
        .shouldContainExactlyInAnyOrder(
            "This:: (35 TerraformRating<Owner>: Ok)",
            "End: 5 VictoryPoint<Owner>!",
        )
  }

  @Test
  fun gyropolis() {
    classEffectsOf("Card230")
        .shouldContainExactlyInAnyOrder(
            "This:: CityTag<Owner, This>!, BuildingTag<Owner, This>!",
            "This: PROD[-2 Energy<Owner>!," +
                " Megacredit<Owner>! / VenusTag<Owner>," +
                " Megacredit<Owner>! / EarthTag<Owner>]," +
                " CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>), Owner>!",
        )
  }

  @Test
  fun e98() {
    classEffectsOf("Elysium_9_8")
        .shouldContainExactlyInAnyOrder(
            "Tile<This>:: CreateAdjacencies<This>!",
            "Tile<This> BY Owner: ProjectCard<Owner>!",
        )
  }

  @Test
  fun venusian() {
    classEffectsOf("Card259")
        .shouldContainExactlyInAnyOrder(
            "This:: VenusTag<Owner, This>!, ScienceTag<Owner, This>!, AnimalTag<Owner, This>!",
            "ScienceTag<Owner>: Animal<Owner, This>.",
            "End: VictoryPoint<Owner>! / Animal<Owner, This>",
        )
  }

  @Test
  fun convertHeat() {
    classEffectsOf("ConvertHeatSA")
        .shouldContainExactlyInAnyOrder(
            "UseAction1<Owner, This>: -8 Heat<Owner>! THEN TemperatureStep."
        )
  }

  @Test
  fun teractor() {
    classEffectsOf("CardB12")
        .shouldContainExactlyInAnyOrder(
            "This:: EarthTag<Owner, This>!",
            "This: 60 Megacredit<Owner>!",
            "PlayTag<Owner, Class<EarthTag>>:: -3 Owed<Owner, Class<Megacredit>>.",
        )
  }

  @Test
  fun immigrantCity() {
    classEffectsOf("Card200")
        .shouldContainExactlyInAnyOrder(
            "This:: CityTag<Owner, This>!, BuildingTag<Owner, This>!",
            "This: PROD[-Energy<Owner>!, -2 Megacredit<Owner>!]," +
                " CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>), Owner>!",
            "CityTile<Anyone>: PROD[Megacredit<Owner>!]",
        )
  }

  @Test
  fun titanAirScrapping() {
    classEffectsOf("CardC43")
        .shouldContainExactlyInAnyOrder(
            "This:: JovianTag<Owner, This>!",
            "UseAction1<Owner, This>: -Titanium<Owner>! THEN 2 Floater<Owner, This>.",
            "UseAction2<Owner, This>: -2 Floater<Owner, This>! THEN TerraformRating<Owner>!",
            "End: 2 VictoryPoint<Owner>!",
        )
  }

  @Test
  fun amc() {
    classEffectsOf("Card002")
        .shouldContainExactlyInAnyOrder(
            "This:: JovianTag<Owner, This>!",
            "This: PROD[-Titanium<Anyone>!, Titanium<Owner>!]",
            "End: VictoryPoint<Owner>!",
        )
  }

  @Test
  fun pets() {
    classEffectsOf("Card172")
        .shouldContainExactlyInAnyOrder(
            "This:: EarthTag<Owner, This>!, AnimalTag<Owner, This>!",
            "This: Animal<Owner, This>.",
            "-Animal<Owner, This>:: Die!",
            "CityTile<Anyone>: Animal<Owner, This>.",
            "End: VictoryPoint<Owner>! / 2 Animal<Owner, This>",
        )
  }
}
