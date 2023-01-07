package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

interface PetType {
  val petClass: PetClass
  val dependencies: DependencyMap
  val abstract: Boolean

  fun isSubtypeOf(that: PetType): Boolean

  fun canIntersect(that: PetType): Boolean

  infix fun intersect(that: PetType): PetType

  fun toTypeExpressionFull(): TypeExpression

  // a type like Tile.CLASS
  class PetClassType(override val petClass: PetClass) : PetType {
    override val dependencies = DependencyMap()
    override val abstract = petClass.abstract

    override fun toTypeExpressionFull() = ClassExpression(petClass.name)

    override fun isSubtypeOf(that: PetType) =
        that is PetClassType && petClass.isSubtypeOf(that.petClass)

    override fun canIntersect(that: PetType): Boolean {
      return that is PetClassType && this.petClass.canIntersect(that.petClass)
    }

    override fun intersect(that: PetType): PetClassType {
      that as PetClassType
      return PetClassType(petClass.intersect(that.petClass))
    }

    override fun toString() = toTypeExpressionFull().toString()
  }

  data class PetGenericType(
      override val petClass: PetClass,
      override val dependencies: DependencyMap = DependencyMap()
  ) : PetType {
    override val abstract: Boolean = petClass.abstract || dependencies.abstract

    override fun isSubtypeOf(that: PetType) =
        that is PetGenericType &&
        petClass.isSubtypeOf(that.petClass)
        && dependencies.specializes(that.dependencies)

    override fun canIntersect(that: PetType): Boolean {
      return try { // TODO yuck
        intersect(that)
        true
      } catch (e: Exception) {
        println(e.message)
        e.stackTrace.take(2).forEach(::println)
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
