package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
data class ClassDeclaration(
    val name: ClassName,
    val id: ClassName = name,
    val abstract: Boolean = true,
    val dependencies: List<DependencyDeclaration> = listOf(),
    val supertypes: Set<TypeExpr> = setOf(),
    val topInvariant: Requirement? = null,
    val otherInvariants: Set<Requirement> = setOf(),
    val effectsRaw: Set<Effect> = setOf(),
    val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),
    val extraNodes: Set<PetNode> = setOf(),
) {
  init {
    require(name != THIS)
  }
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

  val allNodes: Set<PetNode> by lazy {
    setOf<PetNode>() +
        name +
        id +
        supertypes +
        dependencies.map { it.typeExpr } +
        setOfNotNull(topInvariant) +
        otherInvariants +
        effectsRaw +
        defaultsDeclaration.allNodes +
        extraNodes
  }

  data class DependencyDeclaration(val typeExpr: TypeExpr)

  data class DefaultsDeclaration(
      val universalSpecs: List<TypeExpr> = listOf(),
      val gainOnlySpecs: List<TypeExpr> = listOf(),
      val removeOnlySpecs: List<TypeExpr> = listOf(),
      val gainIntensity: Intensity? = null,
      val removeIntensity: Intensity? = null,
  ) {
    companion object {
      fun merge(defs: Collection<DefaultsDeclaration>): DefaultsDeclaration {
        val univ = defs.map { it.universalSpecs }.firstOrNull { it.any() } ?: listOf()
        val gain = defs.map { it.gainOnlySpecs }.firstOrNull { it.any() } ?: listOf()
        val remov = defs.map { it.removeOnlySpecs }.firstOrNull { it.any() } ?: listOf()
        return DefaultsDeclaration(
            universalSpecs = univ,
            gainOnlySpecs = gain,
            removeOnlySpecs = remov,
            gainIntensity = defs.firstNotNullOfOrNull { it.gainIntensity },
            removeIntensity = defs.firstNotNullOfOrNull { it.removeIntensity },
        )
      }
    }

    internal val allNodes: Set<PetNode> = (universalSpecs + gainOnlySpecs + removeOnlySpecs).toSet()
  }
}
