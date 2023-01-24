package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.types.PetType.PetGenericType

/** A class that has been loaded by a [PetClassLoader]. */
public data class PetClass(
    private val declaration: ClassDeclaration,
    internal val directSuperclasses: List<PetClass>,
    private val loader: PetClassLoader,
) {

  public val id: ClassName by declaration::id
  public val name: ClassName by declaration::name
  public val abstract: Boolean by declaration::abstract

  // HIERARCHY

  public val directSupertypes: Set<PetGenericType> by lazy {
    loader.resolveAll(declaration.supertypes)
  }

  public fun isSubclassOf(that: PetClass): Boolean =
      this == that || directSuperclasses.any { it.isSubclassOf(that) }

  public val directSubclasses: Set<PetClass> by lazy {
    require(loader.frozen)
    loader.loadedClasses().filter { this in it.directSuperclasses }.toSet()
  }

  public val allSubclasses: Set<PetClass> by lazy {
    require(loader.frozen)
    loader.loadedClasses().filter { this in it.allSuperclasses }.toSet()
  }

  public val allSuperclasses: Set<PetClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  public val intersectionType: Boolean by lazy {
    if (directSuperclasses.size < 2) {
      false
    } else {
      require(loader.frozen)
      val sharesAllMySuperclasses =
          loader.loadedClasses().filter { petClass ->
            directSuperclasses.all { petClass.isSubclassOf(it) }
          }
      sharesAllMySuperclasses.all { it.isSubclassOf(this) }
    }
  }

  public infix fun intersect(that: PetClass): PetClass? =
      when {
        this.isSubclassOf(that) -> this
        that.isSubclassOf(this) -> that
        else -> {
          val inters =
              allSubclasses.filter {
                it.intersectionType &&
                    this in it.directSuperclasses &&
                    that in it.directSuperclasses
              }
          if (inters.size == 1) inters.first() else null
        }
      }

  // DEPENDENCIES

  internal val directDependencyKeys: Set<Dependency.Key> by lazy {
    declaration.dependencies.indices.map { Dependency.Key(this, it) }.toSet()
  }

  internal val allDependencyKeys: Set<Dependency.Key> by lazy {
    (directSuperclasses.flatMap { it.allDependencyKeys } + directDependencyKeys).toSet()
  }

  /** Least upper bound of all types with petClass==this */
  public val baseType: PetGenericType by lazy {
    val newDeps =
        directDependencyKeys.associateWith {
          val typeExpression = declaration.dependencies[it.index].type
          Dependency(it, loader.resolve(typeExpression))
        }
    val deps = DependencyMap.intersect(directSupertypes.map { it.dependencies })
    val allDeps = deps.intersect(DependencyMap(newDeps))
    require(allDeps.keys == allDependencyKeys)
    PetGenericType(this, allDeps, null)
  }

  // DEFAULTS

  internal val defaults: Defaults by lazy {
    if (name == COMPONENT) {
      Defaults.from(declaration.defaultsDeclaration, this, loader)
    } else {
      val rootDefaults = loader[COMPONENT].defaults
      defaultsIgnoringRoot().overlayOn(listOf(rootDefaults))
    }
  }

  private fun defaultsIgnoringRoot(): Defaults = // TODO hack
  if (name == COMPONENT) {
        Defaults()
      } else {
        Defaults.from(declaration.defaultsDeclaration, this, loader)
            .overlayOn(directSuperclasses.map { it.defaultsIgnoringRoot() })
      }

  // OTHER

  // includes abstract
  internal fun isSingleton(): Boolean =
      declaration.otherInvariants.any { it.requiresThis() } ||
          directSuperclasses.any { it.isSingleton() }

  override fun toString() = "$name"
}
