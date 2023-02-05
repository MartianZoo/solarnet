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
public data class PType
internal constructor(
    private val pclass: PClass,
    internal val allDependencies: DependencyMap = DependencyMap(),
    internal val refinement: Requirement? = null,
) {
  val isClassType = pclass.name == CLASS

  init {
    if (isClassType) {
      require(allDependencies.types.single() is ClassDependency)
    }
  }
  val abstract = pclass.abstract || allDependencies.abstract || refinement != null

  fun isSubtypeOf(that: PType) =
      pclass.isSubclassOf(that.pclass) &&
      allDependencies.specializes(that.allDependencies) &&
      that.refinement in setOf(null, refinement)

  infix fun intersect(that: PType): PType? {
    val intersect: PClass = pclass.intersect(that.pclass) ?: return null
    return PType(
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

  fun specialize(specs: List<PType>): PType =
      copy(allDependencies = allDependencies.specialize(specs))

  fun refine(ref: Requirement): PType = copy(refinement = ref)

  fun toTypeExprFull(): TypeExpr {
    return pclass.name.addArgs(allDependencies.argsAsTypeExprs()).refine(refinement)
  }

  fun toTypeExprMinimal(): TypeExpr {
    val narrowed = allDependencies.minus(pclass.baseType.allDependencies)
    return pclass.name.addArgs(narrowed.types.map { it.toTypeExprMinimal() }).refine(refinement)
  }

  override fun toString() = toTypeExprMinimal().toString()
}
