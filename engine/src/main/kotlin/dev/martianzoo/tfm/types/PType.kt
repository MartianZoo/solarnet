package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

interface PType : TypeInfo {
  val pClass: PClass // TODO should this really be shared?
  val dependencies: DependencyMap
  val refinement: Requirement?

  fun isSubtypeOf(that: PType): Boolean

  infix fun intersect(that: PType): PType?

  override fun toTypeExpression(): TypeExpression

  data class ClassPType(override val pClass: PClass) : PType {
    override val dependencies = DependencyMap()
    override val refinement = null

    override val abstract by pClass::abstract

    override fun isSubtypeOf(that: PType) =
        that is ClassPType && this.pClass.isSubclassOf(that.pClass)

    override fun intersect(that: PType): PType? {
      if (that !is ClassPType) return null
      val inter = (this.pClass intersect that.pClass) ?: return null
      return ClassPType(inter)
    }

    override fun toTypeExpression() = ClassLiteral(pClass.name)
    override fun toString() = toTypeExpression().toString()
  }

  data class GenericPType(
      override val pClass: PClass,
      override val dependencies: DependencyMap,
      override val refinement: Requirement?,
  ) : PType {
    override val abstract: Boolean =
        pClass.abstract || dependencies.abstract || refinement != null

    override fun isSubtypeOf(that: PType) =
        that is GenericPType &&
            pClass.isSubclassOf(that.pClass) &&
            dependencies.specializes(that.dependencies) &&
            that.refinement in setOf(null, refinement)

    override fun intersect(that: PType): GenericPType? {
      val intersect: PClass = pClass.intersect(that.pClass) ?: return null
      return GenericPType(
          intersect,
          dependencies.intersect(that.dependencies),
          combine(this.refinement, that.refinement))
    }

    private fun combine(one: Requirement?, two: Requirement?): Requirement? {
      val x = setOfNotNull(one, two)
      return when (x.size) {
        0 -> null
        1 -> x.first()
        2 -> Requirement.And(x.toList())
        else -> error("imposserous")
      }
    }

    fun specialize(specs: List<PType>): GenericPType {
      return copy(dependencies = dependencies.specialize(specs))
    }

    override fun toTypeExpression(): GenericTypeExpression {
      val specs = dependencies.types.map { it.toTypeExpressionFull() }
      return pClass.name.addArgs(specs).refine(refinement)
    }
    override fun toString() = toTypeExpression().toString()
  }
}
