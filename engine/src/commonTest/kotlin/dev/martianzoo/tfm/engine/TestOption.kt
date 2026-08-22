package dev.martianzoo.tfm.engine

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

public enum class TestOption(private val configuredName: String? = null) : TestSelection {
  TerraformingMars,
  SoloMode,
  MultiplayerMode,
  StandardSoloVariant,
  Tr63SoloVariant,
  CorporateEraExpansion,
  Tharsis("TharsisMap"),
  Hellas("HellasMap"),
  Elysium("ElysiumMap"),
  Utopia("UtopiaMap"),
  Cimmeria("CimmeriaMap"),
  VenusNextExpansion,
  MilestonesAwardsExpansion,
  PreludeExpansion,
  Prelude2Expansion,
  ColoniesExpansion,
  TurmoilCardPack,
  PromoCardPack,
  WorldGovernmentOption;

  public val className: ClassName = cn(configuredName ?: name)
}
