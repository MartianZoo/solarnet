package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
data class ComponentDeclaration(
    val className: String,
    val abstract: Boolean,
    val dependencies: List<DependencyDecl> = listOf(),
    val supertypes: Set<TypeExpression> = setOf(),
    val topInvariant: Requirement? = null,
    val otherInvariants: Set<Requirement> = setOf(),
    val effectsRaw: () -> Set<Effect> = { setOf() }, // TODO needed? or pull instead from intf?
    val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration()
) {
  // TODO canonicalize??
  val effects by lazy { effectsRaw() }

  val superclassNames = supertypes.map { it.className }

  data class DependencyDecl(val type: TypeExpression, val classDep: Boolean = false)

  data class DefaultsDeclaration(
      val allDefault: List<TypeExpression> = listOf(),
      val gainDefault: List<TypeExpression> = listOf(),
      val gainIntensity: Intensity? = null)
}
