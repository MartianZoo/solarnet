package dev.martianzoo.tfm.tests

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.*

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
  PreludeExpansion,
  Prelude1Deck,
  Prelude2Expansion,
  ColoniesExpansion,
  TurmoilCardPack,
  PromoCardPack,
  WorldGovernmentOption,
  MandatoryVenusVariant;

  internal val className: ClassName = cn(configuredName ?: name)
}
