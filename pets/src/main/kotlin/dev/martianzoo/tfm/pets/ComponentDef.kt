package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ComponentDef.Defaults
import dev.martianzoo.tfm.pets.Instruction.Intensity

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
data class ComponentDef(
    val name: String,
    val abstract: Boolean = false,
    val supertypes: Set<TypeExpression> = setOf(),
    val dependencies: List<Dependency> = listOf(),
    val effects: Set<Effect> = setOf(),
    val defaults: Defaults = Defaults()
) {
  init {
    if (name == rootName) {
      require(supertypes.isEmpty())
      require(dependencies.isEmpty())
    }
  }

  val superclassNames = supertypes.map { it.className }

  data class Dependency(val type: TypeExpression, val classDep: Boolean = false)

  data class Defaults(
      val typeExpression: TypeExpression? = null,
      val gainType: TypeExpression? = null,
      val gainIntensity: Intensity? = null,
      val removeType: TypeExpression? = null,
      val removeIntensity: Intensity? = null) {
    init {
      require(typeExpression?.className in setOf("This", null))
      require(gainType?.className in setOf("This", null))
      require(removeType?.className in setOf("This", null))
    }

    fun merge(others: Collection<Defaults>): Defaults {
      val defaultses = listOf(this) + others
      return Defaults(
          getZeroOrOne(defaultses) { it.typeExpression },
          getZeroOrOne(defaultses) { it.gainType },
          getZeroOrOne(defaultses) { it.gainIntensity },
          getZeroOrOne(defaultses) { it.removeType },
          getZeroOrOne(defaultses) { it.removeIntensity })
    }
  }
}

private fun <T> getZeroOrOne(defaultses: List<Defaults>, extractor: (Defaults) -> T?): T? {
  val set = defaultses.mapNotNull(extractor).toSet()
  require(set.size <= 1)
  return set.firstOrNull()
}
