package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ComponentDef.Defaults
import dev.martianzoo.tfm.pets.Instruction.Intensity

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
data class ComponentDef( // TODO not sure abt data class after complete is gone
    val name: String,
    val abstract: Boolean = false,
    val supertypes: Set<TypeExpression> = setOf(),
    val dependencies: List<Dependency> = listOf(),
    val effects: Set<Effect> = setOf(),
    val defaults: Defaults = Defaults()
) : PetsNode() {
  init {
    if (name == rootName) {
      require(supertypes.isEmpty())
      require(dependencies.isEmpty())
    }
  }

  val superclassNames = supertypes.map { it.className }

  // TODO: this should really enforce rules
  override val children = supertypes + dependencies + effects + defaults

  data class Dependency(val type: TypeExpression, val classDep: Boolean = false) : PetsNode() {
    override val children = listOf(type)
  }

  data class Defaults(
      val typeExpression: TypeExpression? = null,
      val gainType: TypeExpression? = null,
      val gainIntensity: Intensity? = null,
      val removeType: TypeExpression? = null,
      val removeIntensity: Intensity? = null): PetsNode() {
    init {
      require(typeExpression?.className in setOf("This", null))
      require(gainType?.className in setOf("This", null))
      require(removeType?.className in setOf("This", null))
    }
    override val children = listOfNotNull(typeExpression, gainType, removeType)

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
