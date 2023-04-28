package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
public data class ClassDeclaration(
    override val className: ClassName,
    public val shortName: ClassName = className,
    public val abstract: Boolean = true,
    public val dependencies: List<Expression> = listOf(),
    public val supertypes: Set<Expression> = setOf(),
    public val invariants: Set<Requirement> = setOf(),
    public val effects: Set<Effect> = setOf(),
    public val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),
    internal val extraNodes: Set<PetNode> = setOf(),
) : HasClassName {
  init {
    require(supertypes.none { it.descendantsOfType<Requirement>().any() }) { supertypes }
  }

  public data class DefaultsDeclaration(
      val universalSpecs: List<Expression> = listOf(),
      val gainOnlySpecs: List<Expression> = listOf(),
      val removeOnlySpecs: List<Expression> = listOf(),
      val gainIntensity: Intensity? = null,
      val removeIntensity: Intensity? = null,
      val forClass: ClassName? = null,
  ) {
    companion object {
      fun merge(defs: Collection<DefaultsDeclaration>): DefaultsDeclaration {
        val forClass: ClassName? = defs.mapNotNull { it.forClass }.singleOrNull()
        val universal = defs.map { it.universalSpecs }.firstOrNull { it.any() } ?: listOf()
        val gain = defs.map { it.gainOnlySpecs }.firstOrNull { it.any() } ?: listOf()
        val remove = defs.map { it.removeOnlySpecs }.firstOrNull { it.any() } ?: listOf()
        return DefaultsDeclaration(
            universalSpecs = universal,
            gainOnlySpecs = gain,
            removeOnlySpecs = remove,
            gainIntensity = defs.firstNotNullOfOrNull { it.gainIntensity },
            removeIntensity = defs.firstNotNullOfOrNull { it.removeIntensity },
            forClass = forClass,
        )
      }
    }

    internal val allNodes: Set<PetNode> = (universalSpecs + gainOnlySpecs + removeOnlySpecs).toSet()
  }

  // EVERYTHING BAGEL

  public val allNodes: Set<PetNode> by lazy {
    setOf<PetNode>() +
        className +
        shortName +
        supertypes +
        dependencies +
        invariants +
        effects +
        defaultsDeclaration.allNodes +
        extraNodes
  }
}
