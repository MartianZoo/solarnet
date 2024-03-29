package dev.martianzoo.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.tfm.engine.CanonClassesTest
import dev.martianzoo.tfm.engine.Prod
import org.junit.jupiter.api.Test

class TransformersTest {
  @Test
  fun test() {
    checkApplyDefaults("Heat", "Heat<Owner>!")
    checkApplyDefaults("-5 Heat", "-5 Heat<Owner>!")
    checkApplyDefaults("OceanTile", "OceanTile<WaterArea>.")
    checkApplyDefaults("-OceanTile", "-OceanTile.")
    checkApplyDefaults(
        "CityTile", "CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>), Owner>!")
    checkApplyDefaults("-CityTile", "-CityTile<Owner>!")
    checkApplyDefaults("CityTile<WaterArea>", "CityTile<WaterArea, Owner>!")
    checkApplyDefaults("CityTile<Owner, WaterArea>", "CityTile<WaterArea, Owner>!")
    checkApplyDefaults("CityTile<Anyone, WaterArea>", "CityTile<WaterArea, Anyone>!")
    checkApplyDefaults("CityTile<Player3, WaterArea>", "CityTile<WaterArea, Player3>!")

    checkApplyDefaults("CityTile<This>", "CityTile<This, Owner>!", cn("Area").expression)
    checkApplyDefaults(
        "CityTile<This>",
        "CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>), This>!",
        cn("Owner").expression)

    checkApplyDefaults("OwnedTile", "OwnedTile<Owner>!")
    checkApplyDefaults("Neighbor<OwnedTile>", "Neighbor<OwnedTile<Owner>>!")
    checkApplyDefaults(
        "LandArea(HAS Neighbor<OwnedTile>)", "LandArea(HAS Neighbor<OwnedTile<Owner>>)!")
    checkApplyDefaults(
        "GreeneryTile", "GreeneryTile<LandArea(HAS? Neighbor<OwnedTile<Owner>>), Owner>!")
  }

  private companion object {
    val transformers = (CanonClassesTest.table as MClassLoader).transformers
  }

  private fun checkApplyDefaults(
      original: String,
      expected: String,
      context: Expression = THIS.expression,
  ) {
    val node: Instruction = parse(original)
    val xfd = transformers.insertDefaults(context).transform(node)
    assertThat(xfd.toString()).isEqualTo(expected)
  }

  @Test
  fun testDeprodify_noProd() {
    val s = "Foo<Bar>: Bax OR Qux"
    val e: Effect = parse(s)
    val ep: Effect = Prod.deprodify(transformers.classes).transform(e)
    assertThat(ep.toString()).isEqualTo(s)
  }

  @Test
  fun testDeprodify_simple() {
    val prodden: Effect = parse("This: PROD[Plant / PlantTag]")
    val deprodden: Effect = Prod.deprodify(transformers.classes).transform(prodden)
    assertThat(deprodden.toString()).isEqualTo("This: Production<Class<Plant>> / PlantTag")
  }

  @Test
  fun testDeprodify_lessSimple() {
    val prodden: Effect =
        parse(
            "PROD[Plant]: PROD[Ooh?, Steel. / Ahh, Foo<Xyz FROM " +
                "Heat>, -Qux!, 5 Ahh<Qux> FROM StandardResource], Heat")
    val expected: Effect =
        parse(
            "Production<Class<Plant>>:" +
                " Ooh?, Production<Class<Steel>>. / Ahh, Foo<Xyz FROM Production<Class<Heat>>>," +
                " -Qux!, 5 Ahh<Qux> FROM Production<Class<StandardResource>>, Heat")
    val deprodden: Effect = Prod.deprodify(transformers.classes).transform(prodden)
    assertThat(deprodden).isEqualTo(expected)
  }
}
