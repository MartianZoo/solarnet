package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.Dependency.ClassDependency
import dev.martianzoo.tfm.types.Dependency.TypeDependency

/**
 * The translation of a [TypeExpr] into a "live" type, referencing actual [PClass]es loaded by a
 * [PClassLoader]. These are usually obtained by [PClassLoader.resolveType]. These can be abstract.
 * Usages of this type should be fairly unrelated to questions of whether instances exist in a game
 * state.
 */
public data class PType
private constructor(
    public val pclass: PClass,
    internal val allDependencies: DependencyMap = DependencyMap(),
    override val refinement: Requirement? = null,
) : Type {
  internal companion object {
    fun create(
        pclass: PClass,
        allDependenciesUnordered: DependencyMap = DependencyMap(),
        refinement: Requirement? = null,
    ) = PType(pclass, allDependenciesUnordered.reorderBy(pclass.allDependencyKeys), refinement)
  }
  init {
    require(allDependencies.keys.toList() == pclass.allDependencyKeys.toList())
    if (pclass.className == CLASS) {
      require(allDependencies.types.single() is ClassDependency)
    } else {
      require(allDependencies.types.all { it is TypeDependency })
    }
  }
  override val abstract = pclass.abstract || allDependencies.abstract || refinement != null

  override fun isSubtypeOf(that: Type) =
      pclass.isSubclassOf((that as PType).pclass) &&
      allDependencies.specializes(that.allDependencies) &&
      that.refinement in setOf(null, refinement)

  fun intersect(that: PType): PType? {
    val intersect: PClass = pclass.intersect(that.pclass) ?: return null
    return create(
        intersect,
        allDependencies.intersect(that.allDependencies),
        combine(this.refinement, that.refinement))
  }

  private fun combine(one: Requirement?, two: Requirement?): Requirement? {
    val x = setOfNotNull(one, two)
    return when (x.size) {
      0 -> null
      1 -> x.first()
      2 -> And(x.toList())
      else -> error("imposserous")
    }
  }

  override val typeExpr: TypeExpr by lazy {
    val narrowed = allDependencies.minus(pclass.baseType.allDependencies)
    pclass.className.addArgs(narrowed.types.map { it.typeExpr }).refine(refinement)
  }

  override val typeExprFull: TypeExpr by lazy {
    pclass.className.addArgs(allDependencies.types.map { it.typeExprFull }).refine(refinement)
  }

  override fun toString() = typeExprFull.toString()
}