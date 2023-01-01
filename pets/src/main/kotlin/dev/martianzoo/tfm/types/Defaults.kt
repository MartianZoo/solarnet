package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ComponentDef.RawDefaults
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression

data class Defaults(
    val allDeps: DependencyMap = DependencyMap(),
    val allReqs: Requirement? = null,  // TODO get rid of this!
    val gainDeps: DependencyMap = DependencyMap(),
    val gainReqs: Requirement? = null,
    val gainIntensity: Intensity? = null
) {

  companion object {
    fun from(d: RawDefaults, petClass: PetClass) = Defaults(
        toDependencyMap(d.allDefault?.specializations, petClass),
        d.allDefault?.requirement,
        toDependencyMap(d.gainDefault?.specializations, petClass),
        d.gainDefault?.requirement,
        d.gainIntensity)

    private fun toDependencyMap(specs: List<TypeExpression>?, petClass: PetClass): DependencyMap {
      return if (specs == null) {
        DependencyMap()
      } else {
        val type = TypeExpression(petClass.name, specs)
        petClass.loader.resolve(type).dependencies
      }
    }
  }

  // Return a RawDefaults that uses the information from *this* one if present,
  // but otherwise attempt to find agreement among all of `defaultses`.
  internal fun overlayOn(defaultses: List<Defaults>): Defaults {
    return Defaults(
        overlayDMs(defaultses) { it.allDeps },
        overlayReqs(defaultses) { it.allReqs },
        overlayDMs(defaultses) { it.gainDeps },
        overlayReqs(defaultses) { it.gainReqs },
        overlayInts(defaultses) { it.gainIntensity })
  }

  private fun overlayInts(
      defaultses: List<Defaults>,
      extract: (Defaults) -> Intensity?
  ): Intensity? {
    val ints = defaultses.mapNotNull(extract)
    return extract(this) ?:
        when (ints.size) {
          0 -> null
          1 -> ints.first()
          else -> error("conflicting intensities: $ints")
        }
  }

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

  private fun overlayDMs(
      defaultses: List<Defaults>,
      extract: (Defaults) -> DependencyMap
  ): DependencyMap {
    val map = extract(this).keyToType.toMutableMap()
    for (key in defaultses.flatMap { extract(it).keys }.toSet()) {
      if (key !in map) {
        // TODO some orders might work when others don't
        val depMapsWithThisKey = defaultses.map(extract).filter { key in it }
        map[key] = depMapsWithThisKey.map { it[key] }.reduce { a, b -> a.glb(b) }
      }
    }
    return DependencyMap(map)
  }
}
