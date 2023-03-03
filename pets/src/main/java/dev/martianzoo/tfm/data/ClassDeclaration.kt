package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
public data class ClassDeclaration(
    override val className: ClassName,
    val id: ClassName = className,
    val abstract: Boolean = true,
    val dependencies: List<DependencyDeclaration> = listOf(),
    val supertypes: Set<TypeExpr> = setOf(), // TODO do fancy Component stuff elsewhere?
    val topInvariant: Requirement? = null,
    val otherInvariants: Set<Requirement> = setOf(),
    val effectsRaw: Set<Effect> = setOf(),
    val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),
    val extraNodes: Set<PetNode> = setOf(),
): HasClassName {
  init {
    require(className != THIS)
  }
  fun validate() {
    when (className) {
      COMPONENT -> {
        require(abstract) { className }
        require(dependencies.none()) { className }
        require(supertypes.isEmpty()) { className }
      }

      CLASS -> {
        require(!abstract) { className }
        require(dependencies.single().typeExpr == COMPONENT.type) { className }
      }

      else -> {
        require(supertypes.isNotEmpty()) { className }
      }
    }
    if (supertypes.size > 1) { // TODO check other redundancies
      require(COMPONENT.type !in supertypes) { className }
    }
  }

  val allNodes: Set<PetNode> by lazy {
    setOf<PetNode>() +
        className +
        id +
        supertypes +
        dependencies.map { it.typeExpr } +
        setOfNotNull(topInvariant) +
        otherInvariants +
        effectsRaw +
        defaultsDeclaration.allNodes +
        extraNodes
  }

  public val signatureLinkages: Set<ClassName> by lazy {
    // Cardbound<CardFront<Anyone>> : Owned<Anyone>
    (dependencies.map { it.typeExpr } + supertypes).asSequence()
        .flatMap { it.arguments }
        .flatMap { it.descendantsOfType<TypeExpr>() }
        .filter { it.isTypeOnly }
        .map { it.className }
        .sorted()
        .windowed(2)
        .mapNotNull { it.toSet().singleOrNull() }
        .toSet()
  }

  // TODO why do we even have this lever
  data class DependencyDeclaration(val typeExpr: TypeExpr)

  data class DefaultsDeclaration(
      val universalSpecs: List<TypeExpr> = listOf(),
      val gainOnlySpecs: List<TypeExpr> = listOf(),
      val removeOnlySpecs: List<TypeExpr> = listOf(),
      val gainIntensity: Intensity? = null,
      val removeIntensity: Intensity? = null,
      val forClass: ClassName? = null,
  ) {
    companion object {
      fun merge(defs: Collection<DefaultsDeclaration>): DefaultsDeclaration {
        val forClass: ClassName? = defs.map { it.forClass }.singleOrNull()
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
}
