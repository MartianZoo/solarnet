package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.Raw
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.classNames

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
public data class ClassDeclaration(
    override val className: ClassName,
    val shortName: ClassName = className,
    val abstract: Boolean = true,
    val dependencies: List<Expression> = listOf(),
    val supertypes: Set<Expression> = setOf(), // TODO do fancy Component stuff elsewhere?
    val invariants: Set<Raw<Requirement>> = setOf(),
    private val effectsIn: Set<Raw<Effect>> = setOf(),
    val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),
    val extraNodes: Set<PetNode> = setOf(),
) : HasClassName {
  init {
    require(supertypes.none { it.hasAnyRefinements() }) { supertypes }
  }
  // DEPENDENCIES

  private fun bareNamesInDependenciesList(): Sequence<ClassName> =
      (dependencies + supertypes.flatMap { it.arguments })
          .asSequence()
          .flatMap { it.descendantsOfType<Expression>() }
          .filter { it.simple }
          .distinct()
          .classNames()

  public val bareNamesInDependencies: Set<ClassName> by lazy {
    bareNamesInDependenciesList().sorted().toSet()
  }

  // EFFECTS

  data class EffectDeclaration(
      val effect: Raw<Effect>,
      val depLinkages: Set<ClassName>,
      val triggerLinkages: Set<ClassName> = setOf(),
  )

  public val effects: List<EffectDeclaration> by lazy {
    effectsIn.map {
      val depLinkages = bareNamesInEffects[it.unprocessed]!!.intersect(bareNamesInDependencies)
      EffectDeclaration(it, depLinkages, triggerLinkages[it.unprocessed]!!)
    }
  }

  public val bareNamesInEffects: Map<Effect, Set<ClassName>> by lazy {
    effectsIn.map { it.unprocessed }.associateWith(::simpleClassNamesIn)
  }

  public val triggerLinkages: Map<Effect, Set<ClassName>> by lazy {
    effectsIn.map { it.unprocessed }.associateWith {
      simpleClassNamesIn(it.trigger).intersect(simpleClassNamesIn(it.instruction))
    }
  }

  private fun simpleClassNamesIn(node: PetNode): Set<ClassName> =
      node.descendantsOfType<Expression>().filter { it.simple }.classNames().toSet()

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
        shortName +
        supertypes +
        dependencies +
        invariants.map { it.unprocessed } +
        effectsIn.map { it.unprocessed } +
        defaultsDeclaration.allNodes +
        extraNodes
  }
}
