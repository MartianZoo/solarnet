package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te

interface Dependency {
  val key: DependencyKey
  val abstract: Boolean

  fun specializes(that: Dependency) : Boolean
  fun combine(that: Dependency): Dependency
  fun toTypeExpressionFull(): TypeExpression
  fun acceptsSpecialization(typeExpression: TypeExpression, loader: PetClassLoader): Boolean
  fun specialize(spec: TypeExpression, loader: PetClassLoader): Dependency

  data class TypeDependency(
      override val key: DependencyKey,
      val type: PetType
  ) : Dependency {
    init { require(!key.classDep) }

    override val abstract by type::abstract

    override fun specializes(that: Dependency) =
        type.isSubtypeOf(checkKeys(that).type)

    override fun combine(that: Dependency) =
        copy(type = type.glb(checkKeys(that).type))

    override fun toTypeExpressionFull() =
        type.toTypeExpressionFull()

    override fun acceptsSpecialization(typeExpression: TypeExpression, loader: PetClassLoader): Boolean {
      val providedClass = loader.load(typeExpression.className)
      if (!providedClass.isSubtypeOf(type.petClass) && !type.petClass.isSubtypeOf(providedClass)) {
        return false
      }
      return loader.resolve(typeExpression).hasGlbWith(type)
    }

    override fun specialize(spec: TypeExpression, loader: PetClassLoader): Dependency {
      return copy(type = loader.resolve(spec).glb(type))
    }

    override fun toString() = "$key=${toTypeExpressionFull()}"

    private fun checkKeys(that: Dependency) =
        (that as TypeDependency).also { require(this.key == that.key) }
  }

  // this should be very similar to a type dependency on Class<Foo>.
  data class ClassDependency(
      override val key: DependencyKey,
      val petClass: PetClass
  ) : Dependency {
    init { require(key.classDep) }

    override val abstract by petClass::abstract

    override fun specializes(that: Dependency) =
        petClass.isSubtypeOf(checkKeys(that).petClass)

    override fun combine(that: Dependency) =
        copy(petClass = petClass.glb(checkKeys(that).petClass))

    override fun toTypeExpressionFull() = te(petClass.name)

    override fun acceptsSpecialization(
        typeExpression: TypeExpression,
        loader: PetClassLoader,
    ): Boolean {
      if (typeExpression.specs.isNotEmpty()) {
        return false
      }
      val providedClass = loader[typeExpression.className]
      return petClass.isSubtypeOf(providedClass) || providedClass.isSubtypeOf(petClass)
    }

    override fun specialize(spec: TypeExpression, loader: PetClassLoader): Dependency {
      require(spec.specs.isEmpty())
      return copy(petClass = loader[spec.className].glb(petClass))
    }

    override fun toString() = "$key=$petClass"

    private fun checkKeys(that: Dependency) =
        (that as ClassDependency).also { require(this.key == that.key) }
  }
}
