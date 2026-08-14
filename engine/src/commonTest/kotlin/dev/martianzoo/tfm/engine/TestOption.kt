package dev.martianzoo.tfm.engine

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

public enum class TestOption : TestSelection {
  TerraformingMars,
  SoloMode,
  MultiplayerMode,
  StandardSoloVariant,
  Tr63SoloVariant,
  CorporateEraExpansion,
  TharsisMapOption,
  HellasMapOption,
  ElysiumMapOption,
  UtopiaPlanitiaMapOption,
  TerraCimmeriaMapOption,
  VenusNextExpansion,
  PreludeExpansion,
  ColoniesExpansion,
  TurmoilCardPack,
  PromoCardPack,
  WorldGovernmentOption;

  public val className: ClassName = cn(name)

  companion object {
    public val DEFAULTS: Set<TestOption> = setOf(TerraformingMars, TharsisMapOption)
  }
}
