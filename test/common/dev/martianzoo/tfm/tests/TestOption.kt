package dev.martianzoo.tfm.tests

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.*

internal enum class TestOption(private val configuredName: String? = null) : TestSelection {
  TerraformingMars,
  SoloMode,
  MultiplayerMode,
  StandardSoloObjective,
  Tr63SoloObjective,
  CorporateEraExpansion,
  Tharsis("TharsisMap"),
  Hellas("HellasMap"),
  Elysium("ElysiumMap"),
  Amazonis("AmazonisMap"),
  Vastitas("VastitasMap"),
  Utopia("UtopiaMap"),
  Cimmeria("CimmeriaMap"),
  VenusNextExpansion,
  PreludeExpansion,
  Prelude1CardPack,
  Prelude2Expansion,
  Prelude2CardPack,
  ColoniesExpansion,
  TurmoilCardPack,
  PromoCardPack,
  WorldGovernmentRule,
  MandatoryVenusVariant;

  internal val className: ClassName = cn(configuredName ?: name)
}
