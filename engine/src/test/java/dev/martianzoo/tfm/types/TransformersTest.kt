package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import org.junit.jupiter.api.Test

class TransformersTest {
  val loader = MClassLoader(Canon).loadEverything()

  @Test
  fun test() {
    checkApplyDefaults("Heat", "Heat<Owner>!")
    checkApplyDefaults("-5 Heat", "-5 Heat<Owner>!")
    checkApplyDefaults("OceanTile", "OceanTile<WaterArea>.")
    checkApplyDefaults("-OceanTile", "-OceanTile.")
    checkApplyDefaults(
        "CityTile", "CityTile<Owner, LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>!")
    // "CT<Owner, LA(HAS MAX 0 NBR<CT<ANY>>)>!" TODO ?
    checkApplyDefaults("-CityTile", "-CityTile<Owner>!")
    checkApplyDefaults("CityTile<WaterArea>", "CityTile<Owner, WaterArea>!")
    checkApplyDefaults("CityTile<Owner, WaterArea>", "CityTile<Owner, WaterArea>!")
    checkApplyDefaults("CityTile<Anyone, WaterArea>", "CityTile<Anyone, WaterArea>!")
    checkApplyDefaults("CityTile<Player3, WaterArea>", "CityTile<Player3, WaterArea>!")

    checkApplyDefaults("CityTile<This>", "CityTile<Owner, This>!", cn("Area").expr)
    checkApplyDefaults(
        "CityTile<This>",
        "CityTile<This, LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>!",
        cn("Owner").expr)

    checkApplyDefaults("OwnedTile", "OwnedTile<Owner>!")
    checkApplyDefaults("Neighbor<OwnedTile>", "Neighbor<OwnedTile<Owner>>!")
    checkApplyDefaults(
        "LandArea(HAS Neighbor<OwnedTile>)", "LandArea(HAS Neighbor<OwnedTile<Owner>>)!")
    checkApplyDefaults(
        "GreeneryTile", "GreeneryTile<Owner, LandArea(HAS Neighbor<OwnedTile<Owner>>)>!")
  }

  private fun checkApplyDefaults(
      original: String,
      expected: String,
      context: Expression = THIS.expr
  ) {
    val xfd = loader.transformers.insertDefaults(context).transform(instruction(original))
    assertThat(xfd.toString()).isEqualTo(expected)
  }

  @Test
  fun testDeprodify_noProd() {
    val s = "Foo<Bar>: Bax OR Qux"
    val e: Effect = Effect.effect(s)
    val ep: Effect = loader.transformers.deprodify().transform(e)
    assertThat(ep.toString()).isEqualTo(s)
  }

  @Test
  fun testDeprodify_simple() {
    val prodden: Effect = Effect.effect("This: PROD[Plant / PlantTag]")
    val deprodden: Effect = loader.transformers.deprodify().transform(prodden)
    assertThat(deprodden.toString()).isEqualTo("This: Production<Class<Plant>> / PlantTag")
  }

  @Test
  fun testDeprodify_lessSimple() {
    val prodden: Effect =
        Effect.effect(
            "PROD[Plant]: PROD[Ooh?, Steel. / Ahh, Foo<Xyz FROM " +
                "Heat>, -Qux!, 5 Ahh<Qux> FROM StandardResource], Heat")
    val expected: Effect =
        Effect.effect(
            "Production<Class<Plant>>:" +
                " Ooh?, Production<Class<Steel>>. / Ahh, Foo<Xyz FROM Production<Class<Heat>>>," +
                " -Qux!, 5 Ahh<Qux> FROM Production<Class<StandardResource>>, Heat")
    val deprodden: Effect = loader.transformers.deprodify().transform(prodden)
    assertThat(deprodden).isEqualTo(expected)
  }
}
