package dev.martianzoo.tfm.canon

import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.GamePremise
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmAuthority

/** Published Terraforming Mars Authority with typed Terraforming Mars definition registries. */
public object Canon :
    TfmAuthority.Composite(
        StandardFormBundle(
            "TerraformingMars",
            baseCustomClasses,
        ),
        StandardFormBundle(
            "CorporateEraExpansion",
            corporateEraCustomClasses,
        ),
        StandardFormBundle("TharsisMap"),
        StandardFormBundle("HellasElysiumExpansion"),
        StandardFormBundle("UtopiaCimmeriaExpansion"),
        StandardFormBundle("VenusNextExpansion"),
        milestonesAwardsExpansionBundle,
        StandardFormBundle(
            "PreludeExpansion",
            preludeCustomClasses,
        ),
        StandardFormBundle(
            "ColoniesExpansion",
            coloniesCustomClasses,
        ),
        StandardFormBundle("TurmoilExpansion"),
        StandardFormBundle(
            "PromoCardsExpansion",
            promoCardsCustomClasses,
        ),
    ) {
  override fun gamePremise(config: GameConfig): GamePremise =
      super.gamePremise(withDefaults(config))

  private fun withDefaults(config: GameConfig): GameConfig {
    val included = config.includedClassNames.toMutableSet()
    val excluded = config.excludedClassNames
    if (terraformingMars !in excluded) included += terraformingMars
    if (included.intersect(mapOptions).isEmpty() && tharsisMapOption !in excluded) {
      included += tharsisMapOption
    }
    return config.copy(includedClassNames = included)
  }

  private val terraformingMars: ClassName = cn("TerraformingMars")
  private val tharsisMapOption: ClassName = cn("TharsisMapOption")
  private val mapOptions: Set<ClassName> =
      setOf(
          tharsisMapOption,
          cn("HellasMapOption"),
          cn("ElysiumMapOption"),
          cn("UtopiaPlanitiaMapOption"),
          cn("TerraCimmeriaMapOption"),
      )
}
