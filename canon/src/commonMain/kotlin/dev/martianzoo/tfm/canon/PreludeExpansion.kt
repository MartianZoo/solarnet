@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.CustomMetric
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.tfm.api.ApiUtils.getOwner
import dev.martianzoo.tfm.api.ApiUtils.lookUpProductionLevels
import dev.martianzoo.types.Type

internal val preludeCustomClasses: Set<CustomClass> = setOf(PreludeExpansion.LowestProduction)

/** Namespace for Prelude's custom Pets implementations. */
internal object PreludeExpansion {
  internal object LowestProduction : CustomMetric() {
    override fun count(game: GameReader, type: Type): Int {
      val resourceClass = type.expressionFull.arguments.single { it.className == CLASS }
      val resource = resourceClass.arguments.single().className
      val prodLevels = lookUpProductionLevels(game, getOwner(game, type).expression)
      val thisProdLevel = prodLevels.getValue(resource)
      return if (prodLevels.values.all { it >= thisProdLevel }) 1 else 0
    }
  }
}
