package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.ast.TypeExpr.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpr.GenericTypeExpr

interface PType : TypeInfo {
  val dependencies: DependencyMap
  val refinement: Requirement?

  fun isSubtypeOf(that: PType): Boolean

  infix fun intersect(that: PType): PType?

  override fun toTypeExprFull(): TypeExpr

  // This is weird, but we want `Class<Class>` to work normally, and but having `Class` actually
  // be in the PClassTable would risk various bugs. So we special case this.
  object ClassClass : PType {
    override val abstract = false
    override val dependencies = DependencyMap()
    override val refinement = null

    override fun isSubtypeOf(that: PType) = that == this || that.toTypeExprFull() == COMPONENT.type
    override fun intersect(that: PType) = if (isSubtypeOf(that)) this else null
    override fun toTypeExprFull() = CLASS.literal
    override fun toString() = toTypeExprFull().toString()
  }

  // Currently, all `Class<...>` are represented with one of these
  data class ClassPType(private val pclass: PClass) : PType {
    override val dependencies = DependencyMap()
    override val refinement = null

    override val abstract by pclass::abstract

    override fun isSubtypeOf(that: PType) =
        that.toTypeExprFull() == COMPONENT.type ||
        that is ClassPType && this.pclass.isSubclassOf(that.pclass)

    override fun intersect(that: PType): PType? {
      if (that !is ClassPType) return null
      val inter = (this.pclass intersect that.pclass) ?: return null
      return ClassPType(inter)
    }

    override fun toTypeExprFull() = ClassLiteral(pclass.name)
    override fun toString() = toTypeExprFull().toString()
  }

  data class GenericPType(
      private val pclass: PClass,
      override val dependencies: DependencyMap,
      override val refinement: Requirement?,
  ) : PType {
    init {
      require(pclass.name != CLASS)
    }
    override val abstract: Boolean = pclass.abstract || dependencies.abstract || refinement != null

    override fun isSubtypeOf(that: PType) =
        that is GenericPType &&
            pclass.isSubclassOf(that.pclass) &&
            dependencies.specializes(that.dependencies) &&
            that.refinement in setOf(null, refinement)

    override fun intersect(that: PType): GenericPType? {
      if (that !is GenericPType) return null
      val intersect: PClass = pclass.intersect(that.pclass) ?: return null
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

    override fun toTypeExprFull(): GenericTypeExpr {
      val specs = dependencies.types.map { it.toTypeExprFull() }
      return pclass.name.addArgs(specs).refine(refinement)
    }

    override fun toString() = toTypeExprFull().toString()
  }
}
