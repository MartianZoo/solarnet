package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

data class Defaults(
    val allCasesDependencies: DependencyMap = DependencyMap(),
    val gainOnlyDependencies: DependencyMap = DependencyMap(),
    val gainIntensity: Intensity? = null
) {

  companion object {
    fun from(d: DefaultsDeclaration, petClass: PetClass) = Defaults(
        petClass.toDependencyMap(d.universalSpecs),
        petClass.toDependencyMap(d.gainOnlySpecs),
        d.gainIntensity)
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
      extract: (Defaults) -> Intensity?
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
      extract: (Defaults) -> DependencyMap
  ): DependencyMap {
    val map = extract(this).keyToDependency.toMutableMap()
    for (key in defaultses.flatMap { extract(it).keys }.toSet()) {
      if (key !in map) {
        // TODO some orders might work when others don't
        val depMapsWithThisKey = defaultses.map(extract).filter { key in it }
        map[key] = depMapsWithThisKey.map { it[key] }.reduce { a, b -> a.intersect(b) }
      }
    }
    return DependencyMap(map)
  }

  // TODO might need this somewhere
  private fun overlayReqs(
      defaultses: List<Defaults>,
      extract: (Defaults) -> Requirement?
  ): Requirement? {
    extract(this)?.let { return it }
    val reqs = defaultses.mapNotNull(extract)
    return when (reqs.size) {
      0 -> null
      1 -> reqs.first()
      else -> Requirement.And(reqs)
    }
  }
}
