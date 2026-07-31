package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.CustomMetric
import dev.martianzoo.api.GameReader
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.ApiUtils.lookUpProductionLevels
import dev.martianzoo.types.Type

internal val utopiaCimmeriaCustomClasses: Set<CustomClass> =
    setOf(UtopiaCimmeriaExpansion.MetalProduction)

/** Custom metrics used by the Utopia Planitia milestones. */
internal object UtopiaCimmeriaExpansion {
  internal object MetalProduction : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val owner = type.expressionFull.arguments.single()
      val production = lookUpProductionLevels(game, owner)
      return production.getValue(cn("Steel")) + production.getValue(cn("Titanium"))
    }
  }
}
