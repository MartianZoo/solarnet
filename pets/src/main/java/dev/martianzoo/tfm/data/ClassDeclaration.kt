package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.util.extras

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
public data class ClassDeclaration(
    override val className: ClassName,
    val id: ClassName = className,
    val abstract: Boolean = true,
    val dependencies: List<DependencyDeclaration> = listOf(),
    val supertypes: Set<Expression> = setOf(), // TODO do fancy Component stuff elsewhere?
    val topInvariant: Requirement? = null,
    val otherInvariants: Set<Requirement> = setOf(),
    private val effectsIn: Set<Effect> = setOf(),
    val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),
    val extraNodes: Set<PetNode> = setOf(),
) : HasClassName {
  init {
    // require(supertypes.none { it.hasAnyRefinements() }) TODO
  }
  // DEPENDENCIES

  // TODO why do we even have this lever
  data class DependencyDeclaration(val expression: Expression)

  private fun bareNamesInDependenciesList(): Sequence<ClassName> =
      (dependencies.map { it.expression } + supertypes.flatMap { it.arguments })
          .asSequence()
          .flatMap { it.descendantsOfType<Expression>() }
          .filter { it.simple }
          .classNames()

  public val bareNamesInDependencies: Set<ClassName> by lazy {
    bareNamesInDependenciesList().sorted().toSet()
  }

  // The class names that participate in some linkage among dependencies
  public val depToDepLinkages: Set<ClassName> by lazy {
    // In this example the class names eligible for linkages are precisely the occurrences of Yes:
    // CLASS Bar<Yes, Qux<Yes>>: Abc<Yes>, Def<Ghi<Yes>>, Xyz

    // In that example, this list will be `Yes`, `Qux<Yes>`, `Yes`, `Ghi<Yes>`
    // val exprsWhoseArgumentsAreEligible =
    //     dependencies.map { it.expression } + supertypes.flatMap { it.arguments }

    // TODO we don't want the No's in `Foo<No, No>` to be linked because they are parallel
    // val extras = exprsWhoseArgumentsAreEligible
    //     .flatMap { expr -> expr.arguments.filter { it.simple }.classNames().extras() }
    bareNamesInDependenciesList().extras().toSet()
  }

  // EFFECTS

  data class EffectDeclaration(val effect: Effect, val linkages: Set<ClassName>)

  public val effects: List<EffectDeclaration> by lazy {
    effectsIn.map {
      EffectDeclaration(it, bareNamesInEffects[it]!!.intersect(bareNamesInDependencies))
    }
  }

  public val bareNamesInEffects: Map<Effect, Set<ClassName>> by lazy {
    effectsIn.associateWith { fx ->
      fx.descendantsOfType<Expression>().filter { it.simple }.classNames().toSet()
    }
  }

  // DEFAULTS

  data class DefaultsDeclaration(
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

  val allNodes: Set<PetNode> by lazy {
    setOf<PetNode>() +
        className +
        id +
        supertypes +
        dependencies.map { it.expression } +
        setOfNotNull(topInvariant) +
        otherInvariants +
        effectsIn +
        defaultsDeclaration.allNodes +
        extraNodes
  }
}
