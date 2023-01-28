package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.types.Dependency.ClassDependency
import dev.martianzoo.tfm.types.Dependency.ClassDependency.Companion.KEY
import dev.martianzoo.tfm.types.Dependency.TypeDependency

/**
 * A class that has been loaded by a [PClassLoader] based on a [ClassDeclaration]. Each loader has
 * its own separate universe of [PClass]es, and each of these [PClass]es knows what loader it came
 * from. This loaded class should be the source for any information you need to know about a class,
 * but the declaration itself can always be looked up if necessary.
 */
public data class PClass(
    private val declaration: ClassDeclaration,
    internal val directSuperclasses: List<PClass>,
    private val loader: PClassLoader,
) {

  /** A short name for this class, such as `"CT"` for `CityTile`; is often the same as [name]. */
  public val id: ClassName by declaration::id

  /** The name of this class, in UpperCamelCase. */
  public val name: ClassName by declaration::name

  /**
   * If true, all types with this as their root are abstract, even when all dependencies are
   * concrete.
   */
  public val abstract: Boolean by declaration::abstract

  // HIERARCHY

  public val directSupertypes: Set<PType> by lazy { loader.resolveAll(declaration.supertypes) }

  public fun isSubclassOf(that: PClass): Boolean =
      this == that || directSuperclasses.any { it.isSubclassOf(that) }

  public val directSubclasses: Set<PClass> by lazy {
    loader.allClasses.filter { this in it.directSuperclasses }.toSet()
  }

  public val allSubclasses: Set<PClass> by lazy {
    loader.allClasses.filter { this in it.allSuperclasses }.toSet()
  }

  public val allSuperclasses: Set<PClass> by lazy {
    (directSuperclasses.flatMap { it.allSuperclasses } + this).toSet()
  }

  public val intersectionType: Boolean by lazy {
    if (directSuperclasses.size < 2) {
      false
    } else {
      val sharesAllMySuperclasses =
          loader.allClasses.filter { pclass -> directSuperclasses.all { pclass.isSubclassOf(it) } }
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
    declaration.dependencies.indices.map { Dependency.Key(name, it) }.toSet()
  }

  internal val allDependencyKeys: Set<Dependency.Key> by lazy {
    (directSuperclasses.flatMap { it.allDependencyKeys } + directDependencyKeys).toSet()
  }

  fun toClassType() = PType(loader.classClass, DependencyMap(mapOf(KEY to ClassDependency(this))))

  /** Least upper bound of all types with pclass==this */
  public val baseType: PType by lazy {
    if (name == CLASS) {
      // base type of Class is Class<Component>
      loader.componentClass.toClassType()
    } else {
      val newDeps =
          directDependencyKeys.associateWith {
            val depTypeExpr = declaration.dependencies[it.index].typeExpr
            TypeDependency(it, loader.resolve(depTypeExpr))
          }
      val deps = DependencyMap.intersect(directSupertypes.map { it.dependencies })
      val allDeps = deps.intersect(DependencyMap(newDeps))
      require(allDeps.keys == allDependencyKeys)
      PType(this, allDeps, null)
    }
  }

  // DEFAULTS

  internal val defaults: Defaults by lazy {
    if (name == COMPONENT) {
      Defaults.from(declaration.defaultsDeclaration, this, loader)
    } else {
      val rootDefaults = loader.componentClass.defaults
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

  fun specialize(map: List<PType>): PType {
    return baseType.specialize(map)
  }
}
