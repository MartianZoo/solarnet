package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.SpecialComponent.This
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
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
    val dependencies: List<DependencyDeclaration> = listOf(),
    val supertypes: Set<GenericTypeExpression> = setOf(),
    val topInvariant: Requirement? = null,
    val otherInvariants: Set<Requirement> = setOf(),
    val effectsRaw: Set<Effect> = setOf(),
    val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),
    val extraNodes: Set<PetsNode> = setOf(),
) {
  init {
    require(this.className !in reservedClassNames)
  }

  val superclassNames = supertypes.map { it.className }

  val allNodes: Set<PetsNode> by lazy {
    setOf<PetsNode>() +
        supertypes +
        dependencies.map { it.type } +
        setOfNotNull(topInvariant) +
        otherInvariants +
        effectsRaw +
        defaultsDeclaration.universalSpecs +
        defaultsDeclaration.gainOnlySpecs +
        extraNodes
  }

  fun isSingleton() = otherInvariants.any() { requiresOneInstance(it) }

  private fun requiresOneInstance(r: Requirement): Boolean {
    return r is Min && r.qe == QuantifiedExpression(This.type, 1) ||
        r is Exact && r.qe == QuantifiedExpression(This.type, 1) ||
        r is And && r.requirements.any { requiresOneInstance(it) }
  }

  data class DependencyDeclaration(val type: TypeExpression)

  data class DefaultsDeclaration(
      val universalSpecs: List<TypeExpression> = listOf(),
      val gainOnlySpecs: List<TypeExpression> = listOf(),
      val gainIntensity: Intensity? = null,
  )
}
