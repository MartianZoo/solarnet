package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ComponentDef.Defaults
import dev.martianzoo.tfm.pets.SpecialComponent.COMPONENT
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.util.toSetStrict

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
data class ComponentDef(
    val name: String,
    val abstract: Boolean = false,
    val supertypes: Set<TypeExpression> = setOf(),
    val dependencies: List<Dependency> = listOf(),
    private val effectsRaw: Set<Effect> = setOf(),
    val defaults: Defaults = Defaults()
) {
  init {
    if (name == "$COMPONENT") {
      require(supertypes.isEmpty())
      require(dependencies.isEmpty())
    } else {
      // require(supertypes.isNotEmpty()) // TODO
    }
  }

  // Canonicalize -- *currently* only spelling out QEs
  val effects = effectsRaw.map(::spellOutQes).toSetStrict()

  val superclassNames = supertypes.map { it.className }

  data class Dependency(val type: TypeExpression, val classDep: Boolean = false)

  data class Defaults(
      val typeExpression: TypeExpression? = null,
      val gainType: TypeExpression? = null,
      val gainIntensity: Intensity? = null,
      val removeType: TypeExpression? = null,
      val removeIntensity: Intensity? = null) {
    init {
      require(typeExpression?.className in setOf("$THIS", null))
      require(gainType?.className in setOf("$THIS", null))
      require(removeType?.className in setOf("$THIS", null))
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
  // require(set.size <= 1) // TODO yipes
  return set.firstOrNull()
}
