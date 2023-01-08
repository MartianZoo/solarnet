package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

internal interface PetType {
  val petClass: PetClass
  val abstract: Boolean

  fun isSubtypeOf(that: PetType): Boolean

  fun canIntersect(that: PetType): Boolean

  infix fun intersect(that: PetType): PetType

  fun toTypeExpressionFull(): TypeExpression

  data class PetGenericType(
      override val petClass: PetClass,
      val dependencies: DependencyMap
  ) : PetType {
    override val abstract: Boolean = petClass.abstract || dependencies.abstract

    override fun isSubtypeOf(that: PetType) =
        that is PetGenericType &&
        petClass.isSubclassOf(that.petClass)
        && dependencies.specializes(that.dependencies)

    override fun canIntersect(that: PetType): Boolean {
      return try { // TODO yuck
        intersect(that)
        true
      } catch (e: Exception) {
        false
      }
    }

    override fun intersect(that: PetType) =
        PetGenericType(
            petClass.intersect(that.petClass),
            dependencies.intersect((that as PetGenericType).dependencies))

    fun specialize(specs: List<PetType>) =
        copy(dependencies = dependencies.specialize(specs))

    override fun toTypeExpressionFull(): GenericTypeExpression {
      val specs = dependencies.keyToDependency.values.map { it.toTypeExpressionFull() }
      return GenericTypeExpression(petClass.name, specs)
    }

    override fun toString() = toTypeExpressionFull().toString()
  }
}
