package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

interface PetType {
  val petClass: PetClass
  val dependencies: DependencyMap
  val abstract: Boolean

  fun toTypeExpressionFull(): TypeExpression

  fun isSubtypeOf(that: PetType): Boolean

  fun hasGlbWith(that: PetType): Boolean

  fun glb(that: PetType): PetType

  // a type like Tile.CLASS
  class PetClassType(override val petClass: PetClass) : PetType {
    override val dependencies = DependencyMap()
    override val abstract = petClass.abstract

    override fun toTypeExpressionFull() = ClassExpression(petClass.name)

    override fun isSubtypeOf(that: PetType) =
        that is PetClassType && petClass.isSubtypeOf(that.petClass)

    override fun hasGlbWith(that: PetType): Boolean {
      that as PetClassType
      return petClass.hasGlbWith(that.petClass)
    }

    override fun glb(that: PetType): PetClassType {
      that as PetClassType
      return PetClassType(petClass.glb(that.petClass))
    }

    override fun toString() = toTypeExpressionFull().toString()
  }

  data class PetGenericType(
      override val petClass: PetClass,
      override val dependencies: DependencyMap = DependencyMap()
  ) : PetType {
    override val abstract: Boolean = petClass.abstract || dependencies.abstract

    override fun isSubtypeOf(that: PetType): Boolean =
        that is PetGenericType && petClass.isSubtypeOf(that.petClass) && dependencies.specializes(
            that.dependencies
        )

    override fun glb(that: PetType): PetGenericType {
      that as PetGenericType
      return PetGenericType(
          petClass.glb(that.petClass),
          dependencies.merge(that.dependencies)
      )
    }

    fun specialize(specs: List<TypeExpression>): PetGenericType {
      return try {
        copy(dependencies = dependencies.specialize(specs, petClass.loader))
      } catch (e: RuntimeException) {
        throw RuntimeException("2. PetType is $this", e)
      }
    }

    override fun toString() = toTypeExpressionFull().toString()

    override fun toTypeExpressionFull(): TypeExpression {
      val specs = dependencies.keyToDep.values.map { it.toTypeExpressionFull() }
      return GenericTypeExpression(petClass.name, specs)
    }

    override fun hasGlbWith(type: PetType): Boolean {
      return try {
        glb(type)
        true
      } catch (e: Exception) {
        false
      }
    }
  }
}
