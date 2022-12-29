package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ComponentDef.OneDefault
import dev.martianzoo.tfm.pets.ComponentDef.RawDefaults
import dev.martianzoo.tfm.pets.SpecialComponent.COMPONENT
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Requirement
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
    val rawDefaults: RawDefaults = RawDefaults()
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

  data class RawDefaults(
      val allDefault: OneDefault? = null,
      val gainDefault: OneDefault? = null,
      val gainIntensity: Intensity? = null)
  data class OneDefault(val specializations: List<TypeExpression>, val requirement: Requirement?)
}

fun oneDefault(te: TypeExpression) =
    OneDefault(te.specializations, te.requirement)
        .also { require(te.className == "$THIS") }

private fun <T> getZeroOrOne(rawDefaultses: List<RawDefaults>, extractor: (RawDefaults) -> T?): T? {
  val set = rawDefaultses.mapNotNull(extractor).toSet()
  require(set.size <= 1) // TODO yipes
  return set.firstOrNull()
}
