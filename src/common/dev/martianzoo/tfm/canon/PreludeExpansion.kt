@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Type
import dev.martianzoo.tfm.canon.ApiUtils.getOwner
import dev.martianzoo.tfm.canon.ApiUtils.lookUpProductionLevels
import dev.martianzoo.tfm.canon.BundleContentSelection.Kind.CARDS

private val prelude1CustomClasses: Set<CustomClass> = setOf(Prelude1.LowestProduction)

internal val preludeExpansionBundle: StandardFormBundle =
    StandardFormBundle(
        "PreludeExpansion",
        prelude1CustomClasses,
        mapOf(
            cn("PreludeExpansion") to emptySet(),
            cn("Prelude1CardPack") to
                setOf(BundleContentSelection(cn("PreludeExpansion"), setOf(CARDS))),
        ),
    )

/** Namespace for Prelude 1's custom Pets implementations. */
private object Prelude1 {
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
