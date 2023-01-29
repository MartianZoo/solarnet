package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.Dependency.ClassDependency

/**
 * The translation of a [TypeExpr] into a "live" type, referencing actual [PClass]es loaded by a
 * [PClassLoader]. These are usually obtained by [PClassLoader.resolveType].
 */
public data class PType internal constructor(
    private val pclass: PClass,
    internal val dependencies: DependencyMap = DependencyMap(),
    internal val refinement: Requirement? = null,
) {
  val isClassType = pclass.name == CLASS

  init {
    if (isClassType) {
      require(dependencies.types.single() is ClassDependency)
    }
  }
  val abstract = pclass.abstract || dependencies.abstract || refinement != null

  fun isSubtypeOf(that: PType) =
      pclass.isSubclassOf(that.pclass) &&
      dependencies.specializes(that.dependencies) &&
      that.refinement in setOf(null, refinement)

  infix fun intersect(that: PType): PType? {
    val intersect: PClass = pclass.intersect(that.pclass) ?: return null
    return PType(
        intersect,
        dependencies.intersect(that.dependencies),
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

  fun specialize(specs: List<PType>): PType = copy(dependencies = dependencies.specialize(specs))

  fun refine(ref: Requirement): PType = copy(refinement = ref)

  fun toTypeExprFull() = pclass.name.addArgs(dependencies.argsAsTypeExprs()).refine(refinement)

  override fun toString() = toTypeExprFull().toString()
}
