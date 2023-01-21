package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.TypeExpression

internal class Defaults(
    val allCasesDependencies: DependencyMap = DependencyMap(),
    val gainOnlyDependencies: DependencyMap = DependencyMap(),
    val gainIntensity: Intensity? = null,
) {

  override fun toString() =
      "{ALL $allCasesDependencies GAIN $gainOnlyDependencies INTENS $gainIntensity}"

  fun isEmpty() = this == EMPTY

  companion object {
    val EMPTY = Defaults()
    fun from(d: DefaultsDeclaration, petClass: PetClass, loader: PetClassLoader): Defaults {
      fun PetClass.toDependencyMap(specs: List<TypeExpression>?) = specs?.let {
        loader.resolve(name.addArgs(it)).dependencies
      } ?: DependencyMap()

      return Defaults(
          petClass.toDependencyMap(d.universalSpecs),
          petClass.toDependencyMap(d.gainOnlySpecs),
          d.gainIntensity,
      )
    }
  }

  // Return a DefaultsDeclaration that uses the information from *this* one if present,
  // but otherwise attempt to find agreement among all of `defaultses`.
  fun overlayOn(defaultses: List<Defaults>): Defaults {
    return Defaults(
        overlayDMs(defaultses) { it.allCasesDependencies },
        overlayDMs(defaultses) { it.gainOnlyDependencies },
        overlayIntensities(defaultses) { it.gainIntensity })
  }

  private fun overlayIntensities(
      defaultses: List<Defaults>,
      extract: (Defaults) -> Intensity?,
  ): Intensity? {
    val override = extract(this)
    if (override != null) return override

    val intensities = defaultses.mapNotNull(extract)
    return when (intensities.size) {
      0 -> null
      1 -> intensities.first()
      else -> error("conflicting intensities: $intensities")
    }
  }

  private fun overlayDMs(
      defaultses: List<Defaults>,
      extract: (Defaults) -> DependencyMap,
  ): DependencyMap {
    val defMap = mutableMapOf<Dependency.Key, Dependency>()
    extract(this).keys.map { it to extract(this)[it]!! }.toMap(defMap)
    for (key in defaultses.flatMap { extract(it).keys }.toSet()) {
      if (key !in defMap) {
        // TODO some orders might work when others don't
        val depMapsWithThisKey = defaultses.map(extract).filter { key in it }
        defMap[key] = depMapsWithThisKey.map { it[key]!! }.reduce { a, b -> a.intersect(b)!! }
      }
    }
    return DependencyMap(defMap)
  }
}
