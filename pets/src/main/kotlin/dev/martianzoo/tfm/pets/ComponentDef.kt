package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ComponentDef.OneDefault
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
data class ComponentDef( // TODO ComponentDecl?
    val className: String,
    val abstract: Boolean,
    val dependencies: List<Dependency> = listOf(),
    val supertypes: Set<TypeExpression> = setOf(),
    val topInvariant: Requirement? = null,
    val otherInvariants: Set<Requirement> = setOf(),
    val rawDefaults: RawDefaults = RawDefaults(), // TODO needed? or pull instead from intf?
    val effectsRaw: () -> Set<Effect> = { setOf() }
) {
  // TODO canonicalize??
  val effects by lazy { effectsRaw() }

  val superclassNames = supertypes.map { it.className }

  data class Dependency(val type: TypeExpression, val classDep: Boolean = false)

  data class RawDefaults(
      val allDefault: OneDefault? = null,
      val gainDefault: OneDefault? = null,
      val gainIntensity: Intensity? = null)

  // TODO just use TypeExpression...
  data class OneDefault(val specializations: List<TypeExpression>, val requirement: Requirement?)
}

fun oneDefault(te: TypeExpression) =
    OneDefault(te.specs, te.refinement)
        .also { require(te.className == "$THIS") }
