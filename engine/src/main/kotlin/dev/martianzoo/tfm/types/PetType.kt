package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

interface PetType : TypeInfo {
  val petClass: PetClass // TODO should this really be shared?
  val dependencies: DependencyMap
  val refinement: Requirement?

  fun isSubtypeOf(that: PetType): Boolean

  infix fun intersect(that: PetType): PetType?

  override fun toTypeExpression(): TypeExpression

  data class PetClassLiteral(override val petClass: PetClass) : PetType {
    override val dependencies = DependencyMap()
    override val refinement = null

    override val abstract by petClass::abstract

    override fun isSubtypeOf(that: PetType) =
        that is PetClassLiteral && this.petClass.isSubclassOf(that.petClass)

    override fun intersect(that: PetType): PetType? {
      if (that !is PetClassLiteral) return null
      val inter = (this.petClass intersect that.petClass) ?: return null
      return PetClassLiteral(inter)
    }

    override fun toTypeExpression() = ClassLiteral(petClass.name)
    override fun toString() = toTypeExpression().toString()
  }

  data class PetGenericType(
      override val petClass: PetClass,
      override val dependencies: DependencyMap,
      override val refinement: Requirement?,
  ) : PetType {
    override val abstract: Boolean =
        petClass.abstract || dependencies.abstract || refinement != null

    override fun isSubtypeOf(that: PetType) =
        that is PetGenericType &&
            petClass.isSubclassOf(that.petClass) &&
            dependencies.specializes(that.dependencies) &&
            that.refinement in setOf(null, refinement)

    override fun intersect(that: PetType): PetGenericType? {
      val intersect: PetClass = petClass.intersect(that.petClass) ?: return null
      return PetGenericType(
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

    fun specialize(specs: List<PetType>): PetGenericType {
      return copy(dependencies = dependencies.specialize(specs))
    }

    override fun toTypeExpression(): GenericTypeExpression {
      val specs = dependencies.types.map { it.toTypeExpressionFull() }
      return petClass.name.addArgs(specs).refine(refinement)
    }
    override fun toString() = toTypeExpression().toString()
  }
}
