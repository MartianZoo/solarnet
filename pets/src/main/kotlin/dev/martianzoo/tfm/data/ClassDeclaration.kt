package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScalarAndType
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
data class ClassDeclaration(
    val id: ClassName,
    val name: ClassName,
    val abstract: Boolean,
    val dependencies: List<DependencyDeclaration> = listOf(),
    val supertypes: Set<GenericTypeExpression> = setOf(),
    val topInvariant: Requirement? = null,
    val otherInvariants: Set<Requirement> = setOf(),
    val effectsRaw: Set<Effect> = setOf(),
    val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),
    val extraNodes: Set<PetNode> = setOf(),
) {
  fun validate() {
    if (name == COMPONENT) {
      require(supertypes.isEmpty())
    } else {
      require(supertypes.isNotEmpty())
    }
    if (supertypes.size > 1) {
      require(COMPONENT.type !in supertypes)
    }
  }

  val superclassNames: List<ClassName> = supertypes.map { it.className }

  val allNodes: Set<PetNode> by lazy {
    setOf<PetNode>() +
        name +
        supertypes +
        dependencies.map { it.type } +
        setOfNotNull(topInvariant) +
        otherInvariants +
        effectsRaw +
        defaultsDeclaration.universalSpecs +
        defaultsDeclaration.gainOnlySpecs +
        extraNodes
  }

  // TODO why unused?
  fun isSingleton() = otherInvariants.any() { requiresOneInstance(it) }

  private fun requiresOneInstance(r: Requirement): Boolean {
    return r is Min && r.sat == ScalarAndType(1, THIS.type) ||
        r is Exact && r.sat == ScalarAndType(1, THIS.type) ||
        r is And && r.requirements.any { requiresOneInstance(it) }
  }

  data class DependencyDeclaration(val type: TypeExpression)

  data class DefaultsDeclaration(
      val universalSpecs: List<TypeExpression> = listOf(),
      val gainOnlySpecs: List<TypeExpression> = listOf(),
      val gainIntensity: Intensity? = null,
  ) {
    companion object {
      fun merge(defs: Collection<DefaultsDeclaration>) =
          DefaultsDeclaration(
              universalSpecs = defs.firstNotNullOfOrNull { it.universalSpecs } ?: listOf(),
              gainOnlySpecs = defs.firstNotNullOfOrNull { it.gainOnlySpecs } ?: listOf(),
              gainIntensity = defs.firstNotNullOfOrNull { it.gainIntensity },
          )
    }
  }
}
