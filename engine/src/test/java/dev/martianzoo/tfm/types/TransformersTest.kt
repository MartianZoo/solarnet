package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.instruction
import dev.martianzoo.tfm.types.Transformers.InsertDefaults
import dev.martianzoo.tfm.types.Transformers.ResolveXs
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
    val xfd = InsertDefaults(loader, context).transform(instruction(original))
    assertThat(xfd.toString()).isEqualTo(expected)
  }

  @Test
  fun testXs() {
    val insul = instruction("PROD[-X Heat THEN X Megacredit]")
    val insul8 = ResolveXs(8).transform(insul)
    assertThat(insul8).isEqualTo(instruction("PROD[-8 Heat THEN 8 Megacredit]"))
  }

  @Test
  fun testXs2() {
    val insul = instruction("PROD[-X Heat THEN X]")
    val insul8 = ResolveXs(8).transform(insul)
    assertThat(insul8).isEqualTo(instruction("PROD[-8 Heat THEN 8]"))
  }
}
