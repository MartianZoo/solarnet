package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.reservedClassNames

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
data class ClassDeclaration(
    val className: String,
    val abstract: Boolean,
    val dependencies: List<DependencyDecl> = listOf(),
    val supertypes: Set<GenericTypeExpression> = setOf(),
    val topInvariant: Requirement? = null,
    val otherInvariants: Set<Requirement> = setOf(),
    val effectsRaw: () -> Set<Effect> = { setOf() }, // TODO needed? or pull instead from intf?
    val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration()
): Definition {
  init { require(className !in reservedClassNames) }

  override val componentName = className // TODO

  override val asClassDeclaration = this

  // TODO canonicalize??
  val effects by lazy { effectsRaw() }

  val superclassNames = supertypes.map { it.className }

  data class DependencyDecl(val upperBound: TypeExpression, val classDependency: Boolean = false)

  data class DefaultsDeclaration(
      val universalSpecs: List<TypeExpression> = listOf(),
      val gainOnlySpecs: List<TypeExpression> = listOf(),
      val gainIntensity: Intensity? = null)
}
