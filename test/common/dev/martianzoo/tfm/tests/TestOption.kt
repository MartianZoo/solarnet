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
  BeginnerVariant,
  Tharsis("TharsisMap"),
  Hellas("HellasMap"),
  Elysium("ElysiumMap"),
  Amazonis("AmazonisMap"),
  Vastitas("VastitasMap"),
  Utopia("UtopiaMap"),
  Cimmeria("CimmeriaMap"),
  VenusNextExpansion,
  PreludeExpansion,
  PreludeCards("prelude:cards"),
  Prelude2Cards("prelude2:cards"),
  ColoniesExpansion,
  TurmoilExpansion,
  PromoCards("promos:cards"),
  FakeStuffBundle,
  WorldGovernmentRule,
  MandatoryVenusVariant,
  Unsafe;

  internal val cardBundleName: String? =
      configuredName?.takeIf { it.endsWith(":cards") }?.removeSuffix(":cards")
  internal val className: ClassName? =
      if (cardBundleName == null) cn(configuredName ?: name) else null
}
