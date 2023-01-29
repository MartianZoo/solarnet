package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.Dependency.ClassDependency

data class PType(
    private val pclass: PClass,
    val dependencies: DependencyMap,
    val refinement: Requirement? = null,
) : TypeInfo {
  val isClassType = pclass.name == CLASS

  init {
    if (isClassType) {
      require(dependencies.types.single() is ClassDependency)
    }
  }
  override val abstract = pclass.abstract || dependencies.abstract || refinement != null

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

  override fun toTypeExprFull(): TypeExpr {
    val specs = dependencies.types.map { it.toTypeExprFull() }
    return pclass.name.addArgs(specs).refine(refinement)
  }

  override fun toString() = toTypeExprFull().toString()
}
