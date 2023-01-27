package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.types.PType.GenericPType

/** A class that has been loaded by a [PClassLoader]. */
public data class PClass(
    private val declaration: ClassDeclaration,
    internal val directSuperclasses: List<PClass>,
    private val loader: PClassLoader,
) {

  public val id: ClassName by declaration::id
  public val name: ClassName by declaration::name
  public val abstract: Boolean by declaration::abstract

  init {
    require(name != CLASS)
  }

  // HIERARCHY

  public val directSupertypes: Set<GenericPType> by lazy {
    loader.resolveAll(declaration.supertypes)
  }

  public fun isSubclassOf(that: PClass): Boolean =
      this == that || directSuperclasses.any { it.isSubclassOf(that) }

  public val directSubclasses: Set<PClass> by lazy {
    require(loader.frozen)
    loader.loadedClasses().filter { this in it.directSuperclasses }.toSet()
  }

  public val allSubclasses: Set<PClass> by lazy {
    require(loader.frozen)
    loader.loadedClasses().filter { this in it.allSuperclasses }.toSet()
  }

  public val allSuperclasses: Set<PClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  public val intersectionType: Boolean by lazy {
    if (directSuperclasses.size < 2) {
      false
    } else {
      require(loader.frozen)
      val sharesAllMySuperclasses =
          loader.loadedClasses().filter { pclass ->
            directSuperclasses.all { pclass.isSubclassOf(it) }
          }
      sharesAllMySuperclasses.all { it.isSubclassOf(this) }
    }
  }

  public infix fun intersect(that: PClass): PClass? =
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

  /** Least upper bound of all types with pclass==this */
  public val baseType: GenericPType by lazy {
    val newDeps =
        directDependencyKeys.associateWith {
          val depTypeExpr = declaration.dependencies[it.index].typeExpr
          Dependency(it, loader.resolve(depTypeExpr))
        }
    val deps = DependencyMap.intersect(directSupertypes.map { it.dependencies })
    val allDeps = deps.intersect(DependencyMap(newDeps))
    require(allDeps.keys == allDependencyKeys)
    GenericPType(this, allDeps, null)
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
